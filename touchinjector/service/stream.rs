use evdev::{Device, EventType, InputEvent, SynchronizationCode};
use futures::Stream;
use std::collections::vec_deque::VecDeque;
use std::io;
use std::pin::Pin;
use std::task::{ready, Context, Poll};
use tokio::io::unix::AsyncFd;
use tokio::io::Interest;

pub struct GrabbedInputEventStream {
    fd: AsyncFd<Device>,
    pending: VecDeque<InputEvent>,
}

impl TryFrom<Device> for GrabbedInputEventStream {
    type Error = io::Error;

    fn try_from(mut dev: Device) -> Result<Self, Self::Error> {
        dev.grab()?;
        dev.set_nonblocking(true)?;

        Ok(Self {
            fd: AsyncFd::with_interest(dev, Interest::READABLE)?,
            pending: VecDeque::with_capacity(32),
        })
    }
}

impl GrabbedInputEventStream {
    fn try_pop_front(&mut self) -> Option<VecDeque<InputEvent>> {
        let sync_index = match self.pending.iter().enumerate().find(|(_index, ev)| {
            ev.event_type() == EventType::SYNCHRONIZATION
                && ev.code() == SynchronizationCode::SYN_REPORT.0
        }) {
            Some((index, _ev)) => index,
            None => return None,
        };

        let mut remaining = self.pending.split_off(sync_index + 1);
        std::mem::swap(&mut self.pending, &mut remaining); // keep remaining events

        Some(remaining)
    }
}

impl Stream for GrabbedInputEventStream {
    // Return pack of input events with SYN_REPORT at the end
    type Item = io::Result<VecDeque<InputEvent>>;

    fn poll_next(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        let mut_self = self.get_mut();

        if let Some(complete_ev) = mut_self.try_pop_front() {
            return Poll::Ready(Some(Ok(complete_ev)));
        }

        loop {
            let mut guard = match ready!(mut_self.fd.poll_read_ready_mut(cx)) {
                Ok(guard) => guard,
                Err(err) => return Poll::Ready(Some(Err(err))),
            };

            match guard.try_io(|dev| Ok(dev.get_mut().fetch_events()?.collect::<VecDeque<_>>())) {
                Ok(Ok(mut evs)) => {
                    mut_self.pending.append(&mut evs);

                    if let Some(complete_ev) = mut_self.try_pop_front() {
                        return Poll::Ready(Some(Ok(complete_ev)));
                    }

                    continue;
                }
                Ok(Err(err)) => return Poll::Ready(Some(Err(err))),
                Err(_) => continue,
            }
        }
    }
}
