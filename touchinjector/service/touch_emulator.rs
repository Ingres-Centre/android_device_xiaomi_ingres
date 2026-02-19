use crate::touch_merger::TouchMerger;
use evdev::{AbsoluteAxisCode, EventType, InputEvent, KeyCode, SynchronizationCode};
use tokio::sync::Mutex;
use std::fmt;
use std::fmt::Formatter;
use std::sync::Arc;

pub struct TouchEmulator {
    merger_mtx: Arc<Mutex<TouchMerger>>,
    slot_states: Vec<bool>,
}

#[derive(Debug)]
pub enum Error {
    InvalidSlotId,
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut Formatter<'_>) -> fmt::Result {
        match self {
            Error::InvalidSlotId => write!(f, "Invalid slot id!"),
        }
    }
}

impl std::error::Error for Error {}

impl TouchEmulator {
    pub async fn new(merger_mtx: Arc<Mutex<TouchMerger>>) -> Self {
        let slot_count = merger_mtx.lock().await.get_additional_slots_count();

        log::debug!("Creating touch emulator with {slot_count} total slots.");

        let mut slot_states = Vec::new();
        slot_states.resize(slot_count, false);

        Self { merger_mtx, slot_states }
    }

    async fn touch(&mut self, slot: usize, pos: Option<(i32, i32)>) -> anyhow::Result<()> {
        if self.slot_states.len() < slot {
            return Err(Error::InvalidSlotId.into());
        }

        let is_press = pos.is_some();

        if self.slot_states[slot] ^ (!is_press) {
            return Ok(());
        }

        let touched_before = self.slot_states.iter().any(|s| *s);
        self.slot_states[slot] = is_press;
        let touched_after = self.slot_states.iter().any(|s| *s);

        let mut queue = Vec::<InputEvent>::with_capacity(7);

        queue.push(InputEvent::new_now(EventType::ABSOLUTE.0, AbsoluteAxisCode::ABS_MT_SLOT.0, slot as i32));

        queue.push(
            InputEvent::new_now(
                EventType::ABSOLUTE.0,
                AbsoluteAxisCode::ABS_MT_TRACKING_ID.0,
                if is_press { 0 } else { 0xFFFFFFFFu32 as i32 }
            )
        );

        if (is_press && !touched_before) || (!is_press && !touched_after) {
            queue.push(InputEvent::new_now(EventType::KEY.0, KeyCode::BTN_TOUCH.0, is_press as i32));
            queue.push(InputEvent::new_now(EventType::KEY.0, KeyCode::BTN_TOOL_FINGER.0, is_press as i32));
        }

        if let Some((x, y)) = pos {
            queue.push(InputEvent::new_now(EventType::ABSOLUTE.0, AbsoluteAxisCode::ABS_MT_POSITION_X.0, x));
            queue.push(InputEvent::new_now(EventType::ABSOLUTE.0, AbsoluteAxisCode::ABS_MT_POSITION_Y.0, y));
        }

        queue.push(InputEvent::new_now(EventType::SYNCHRONIZATION.0, SynchronizationCode::SYN_REPORT.0, 0));

        self.merger_mtx.lock().await.emit(queue.as_slice(), true).await
    }

    pub async fn begin_touch(&mut self, slot: usize, x: i32, y: i32) -> anyhow::Result<()> {
        log::debug!("Starting touch at [x: {x}, y: {y}] on slot {slot}");
        self.touch(slot, Some((x, y))).await
    }

    pub async fn end_touch(&mut self, slot: usize) -> anyhow::Result<()> {
        log::debug!("Ending touch on slot {slot}");
        self.touch(slot, None).await
    }

    pub async fn end_all_touches(&mut self) {
        for slot_id in 0..self.slot_states.len() {
            if !self.slot_states[slot_id] {
                continue;
            }

            log::debug!("Clearing active touch on slot {slot_id}");
            let _ = self.touch(slot_id, None).await;
        }
    }
}
