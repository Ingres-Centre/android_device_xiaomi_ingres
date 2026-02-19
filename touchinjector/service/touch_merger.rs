use crate::counter::IncrementalCounter;
use crate::stream::GrabbedInputEventStream;
use evdev::uinput::VirtualDevice;
use evdev::{AbsInfo, AbsoluteAxisCode, BusType, Device, EventSummary, EventType, InputEvent, InputId, KeyCode, UinputAbsSetup};
use futures::StreamExt;
use tokio::sync::Mutex;
use std::io;
use std::sync::Arc;

pub struct TouchMerger {
    virt_dev: VirtualDevice,

    all_slot_states: Box<[bool]>,
    all_tracking_id: IncrementalCounter<i32>,
    all_current_slot: usize,

    src_slot_count: usize,
    src_current_slot: usize,

    add_slot_count: usize,
}

impl TouchMerger {
    fn create_virtual_device(source_dev: &Device, add_slot_count: usize) -> io::Result<(VirtualDevice, usize)> {
        let mut b = VirtualDevice::builder()?
            .name("touch-injector")
            .input_id(InputId::new(BusType::BUS_VIRTUAL, 0xFFFF, 0x1111, 2))
            .with_properties(source_dev.properties())?;

        b = match source_dev.supported_keys() {
            Some(keys) => b.with_keys(keys)?,
            None => return Err(io::Error::new(io::ErrorKind::Unsupported, "provided input device doesn't support keys"))
        };

        let mut slot_count = None;

        for (axis, mut absinfo) in source_dev.get_absinfo()? {
            if axis == AbsoluteAxisCode::ABS_MT_SLOT {
                slot_count = Some(absinfo.maximum() + 1);

                absinfo = AbsInfo::new(
                    absinfo.value(),
                    absinfo.minimum(),
                    absinfo.maximum() + add_slot_count as i32,
                    absinfo.fuzz(),
                    absinfo.flat(),
                    absinfo.resolution()
                );
            }

            b = b.with_absolute_axis(&UinputAbsSetup::new(axis, absinfo))?;
        }

        if slot_count.is_none() {
            return Err(io::Error::new(io::ErrorKind::Unsupported, "provided input device doesn't support absolute axit multi-touch slots"));
        }

        if let Some(axes) = source_dev.supported_relative_axes() {
            b = b.with_relative_axes(axes)?;
        }

        if let Some(sw) = source_dev.supported_switches() {
            b = b.with_switches(sw)?;
        }

        if let Some(msc) = source_dev.misc_properties() {
            b = b.with_msc(msc)?;
        }

        if let Some(ff) = source_dev.supported_ff() {
            b = b.with_ff(ff)?
                 .with_ff_effects_max(source_dev.max_ff_effects() as u32);
        }

        b.build().map(|dev| (dev, slot_count.unwrap() as usize))
    }

    pub async fn src_dev_task(self_mtx: Arc<Mutex<Self>>, mut ev_stream: GrabbedInputEventStream) -> anyhow::Result<()> {
        loop {
            match ev_stream.next().await {
                None => anyhow::bail!("source device input event stream finished unexpectedly"),
                Some(Err(err)) => anyhow::bail!("source device input event stream is broken: {err:?}"),
                Some(Ok(mut evs)) => {
                    let mut this = self_mtx.lock().await;
                    this.emit(evs.make_contiguous(), false).await?;
                }
            }
        }
    }

    pub async fn emit(&mut self, evs: &[InputEvent], use_additional_slots: bool) -> anyhow::Result<()> {
        let mut queue = Vec::<InputEvent>::with_capacity(evs.len() + 1);

        // Reset slot, if first found abs event is not 'set slot'.
        // As it should be first, if present.
        if !use_additional_slots
            && self.all_current_slot != self.src_current_slot
            && evs.iter()
                .find(|ev| ev.event_type() == EventType::ABSOLUTE)
                .is_some_and(|ev| ev.code() != AbsoluteAxisCode::ABS_MT_SLOT.0) {
            self.all_current_slot = self.src_current_slot;
            queue.push(InputEvent::new_now(EventType::ABSOLUTE.0, AbsoluteAxisCode::ABS_MT_SLOT.0, self.src_current_slot as i32));
        }

        for ev in evs { 
            let mut ev_value = ev.value();

            match ev.clone().destructure() {
                EventSummary::AbsoluteAxis(_, AbsoluteAxisCode::ABS_MT_SLOT, slot) => {
                    let mut slot = slot as usize;

                    if use_additional_slots {
                        if slot + 1 > self.add_slot_count {
                            log::warn!("Input event additional slot {slot} is out of bounds");
                        }

                        slot = self.src_slot_count + slot;
                    } else {
                        if slot + 1 > self.src_slot_count {
                            log::warn!("Input event base slot {slot} is out of bounds");
                        }

                        self.src_current_slot = slot;
                    }

                    // we can just skip useless event as required slot was already selected
                    if self.all_current_slot == slot {
                        continue;
                    }

                    self.all_current_slot = slot;
                    ev_value = slot as i32;
                }

                EventSummary::AbsoluteAxis(_, AbsoluteAxisCode::ABS_MT_TRACKING_ID, tracking_id) => {
                    if tracking_id != 0xFFFFFFFFu32 as i32 {
                        ev_value = self.all_tracking_id.next();
                    }

                    if let Some(state) = self.all_slot_states.get_mut(self.all_current_slot) {
                        *state = tracking_id != (0xFFFFFFFFu32 as i32);
                    }
                }

                EventSummary::Key(_, KeyCode::BTN_TOUCH, _) | EventSummary::Key(_, KeyCode::BTN_TOOL_FINGER, _) => {
                    // don't change device state, if self touch isn't last
                    if self.all_slot_states.iter().enumerate().any(|(index, v)| index != self.all_current_slot && *v) {
                        continue;
                    }
                }

                _ => {}
            }

            queue.push(InputEvent::new_now(ev.event_type().0, ev.code(), ev_value));
        }


        if let Err(err) = self.virt_dev.emit(queue.as_slice()) {
            anyhow::bail!("failed to emit input events: {err:?}");
        }

        Ok(())
    }

    pub fn new() -> anyhow::Result<Arc<Mutex<Self>>> {
        let src_dev_name = crate::build_config::SRC_DEV_NAME;
        let add_slot_count = crate::build_config::ADD_SLOT_COUNT;

        let src_dev = evdev::enumerate()
            .find(|(_, device) | device.name() == Some(src_dev_name))
            .map(|(_, device)| device)
            .ok_or(io::Error::new(io::ErrorKind::NotFound, format!("input device '{src_dev_name}' not found")))?;

        let (virt_dev, src_slot_count) = match Self::create_virtual_device(&src_dev, add_slot_count) {
            Ok(dev) => dev,
            Err(err) => anyhow::bail!("failed to create virtual device: {err:?}")
        };

        let src_stream = match GrabbedInputEventStream::try_from(src_dev) {
            Ok(stream) => stream,
            Err(err) => anyhow::bail!("failed to create input event stream from device: {err:?}")
        };

        let this = Arc::new(Mutex::new(Self {
            virt_dev,
            all_slot_states: vec![false; src_slot_count + add_slot_count].into_boxed_slice(),
            all_tracking_id: IncrementalCounter::new(0),
            all_current_slot: 0,
            src_slot_count,
            src_current_slot: 0,
            add_slot_count,
        }));

        tokio::spawn(Self::src_dev_task(Arc::clone(&this), src_stream));

        Ok(this)
    }

    pub fn get_additional_slots_count(&self) -> usize {
        self.add_slot_count
    }
}
