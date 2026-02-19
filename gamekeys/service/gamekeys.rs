use std::task::Context;
use evdev::{Device, InputEvent, KeyCode};
use futures::{ready, Stream};
use tokio::io::unix::AsyncFd;
use tokio::io::Interest;
use std::collections::vec_deque::VecDeque;
use std::io;
use std::pin::Pin;
use std::task::Poll;

#[derive(Debug)]
pub enum EventType {
    Open,
    Close,
    Press,
    Release,
}

#[derive(Debug)]
pub struct Event {
    pub r#type: EventType,
    pub slot: u32,
}

fn map_event(ev: InputEvent) -> Option<Event> {
    match ev.destructure() {
        evdev::EventSummary::Key(_, key_code, value) => {
            match key_code {
                KeyCode::KEY_F1 => Some(Event { slot: 0, r#type: if value == 1 { EventType::Press } else { EventType::Release } }),
                KeyCode::KEY_F2 => Some(Event { slot: 1, r#type: if value == 1 { EventType::Press } else { EventType::Release } }),
                KeyCode::KEY_F3 => if value == 1 { Some(Event { slot: 0, r#type: EventType::Open }) } else { None },
                KeyCode::KEY_F4 => if value == 1 { Some(Event { slot: 0, r#type: EventType::Close }) } else { None },
                KeyCode::KEY_F5 => if value == 1 { Some(Event { slot: 1, r#type: EventType::Open }) } else { None },
                KeyCode::KEY_F6 => if value == 1 { Some(Event { slot: 1, r#type: EventType::Close }) } else { None },
                _ => None,
            }
        },
        _ => None,
    }
}

pub struct GameKeyEventStream {
        fd: AsyncFd<Device>,
        pending: VecDeque<Event>,
}

impl GameKeyEventStream {
    pub fn new() -> io::Result<Self> {
        let device = evdev::enumerate()
            .map(|(_, device)| device)
            .find(|device| device.name() == Some("xm_gamekey"))
            .ok_or(io::Error::new(io::ErrorKind::NotFound, "xm_gamekey device not found"))?;

        device.set_nonblocking(true)?;

        Ok(Self { fd: AsyncFd::with_interest(device, Interest::READABLE)?, pending: VecDeque::new() })
    }
}

impl Stream for GameKeyEventStream {
    type Item = io::Result<Event>;

    fn poll_next(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        let this = self.get_mut();

        if let Some(event) = this.pending.pop_front() {
            return Poll::Ready(Some(Ok(event)));
        }

        loop {
            let mut guard = match ready!(this.fd.poll_read_ready_mut(cx)) {
                Ok(guard) => guard,
                Err(err) => return Poll::Ready(Some(Err(err)))
            };

            match guard.try_io(|device| {
                let events = device.get_mut().fetch_events()?;
                Ok(events.into_iter().map(|x| x).map(map_event).flatten().collect::<VecDeque<_>>())
            }) {
                Ok(Ok(mut events)) => {
                    if let Some(ev) = events.pop_front() {
                        this.pending = events;
                        return Poll::Ready(Some(Ok(ev)));
                    }

                    continue;
                }
                Ok(Err(err)) => return Poll::Ready(Some(Err(err))),
                Err(_) => continue
            }
        }
    }
}
