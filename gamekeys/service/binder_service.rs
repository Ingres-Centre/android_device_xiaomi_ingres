use binder::{ExceptionCode, Interface, Result, Status, Strong, unstable_api::AsNative};
use futures::StreamExt;
use std::future::Future;
use std::io;
use std::sync::Arc;
use tokio::sync::Mutex;
use vendor_lineage_gamekeys::aidl::vendor::lineage::gamekeys::{
    GameKeyEvent::GameKeyEvent, GameKeyEventType::GameKeyEventType, IGameKeys::IGameKeysAsyncServer, IGameKeysEventListener::IGameKeysEventListener
};

use crate::gamekeys::{EventType, GameKeyEventStream};

pub struct GameKeysService {
    listeners: Arc<Mutex<Vec<Strong<dyn IGameKeysEventListener>>>>,
}

impl Interface for GameKeysService {}

impl GameKeysService {
    pub fn new() -> Self {
        Self { listeners: Arc::new(Mutex::new(Vec::new())) }
    }

    fn invalid_argument_status(message: &str) -> Status {
        Status::new_exception_str(ExceptionCode::ILLEGAL_ARGUMENT, Some(message))
    }

    pub fn event_loop_task(&self) -> io::Result<impl Future<Output = io::Result<()>>> {
        let mut stream = GameKeyEventStream::new()?;
        let listeners = Arc::clone(&self.listeners);

        return Ok(async move {
            loop {
                let ev = match stream.next().await {
                    Some(ev) => ev?,
                    None => continue,
                };

                let aidl_event = GameKeyEvent {
                    id: ev.slot as i32,
                    r#type: match &ev.r#type {
                        EventType::Open => GameKeyEventType::OPEN,
                        EventType::Close => GameKeyEventType::CLOSE,
                        EventType::Press => GameKeyEventType::DOWN,
                        EventType::Release => GameKeyEventType::UP,
                    },
                };

                for listener in listeners.lock().await.iter() {
                    let _ = listener.onGameKeyEvent(&aidl_event);
                }

                log::debug!("{:?}", ev);
            }
        });
    }
}

#[allow(non_snake_case)]
#[async_trait::async_trait]
impl IGameKeysAsyncServer for GameKeysService {
    async fn r#registerEventListener<'a, 'l1>(
        &'a self,
        listener: &'l1 Strong<dyn IGameKeysEventListener>,
    ) -> Result<()> {
        let listener_addr = listener.as_binder().as_native() as usize;
        log::debug!("Registering GameKey listener {listener_addr}");

        let mut listeners_lock = self.listeners.lock().await;

        if listeners_lock.contains(listener) {
            log::warn!("GameKey listener {listener_addr} is already registered");
            return Err(Self::invalid_argument_status("Listener is already registered"));
        }

        listeners_lock.push(Strong::clone(listener));

        Ok(())
    }

    async fn r#unregisterEventListener<'a, 'l1>(
        &'a self,
        listener: &'l1 Strong<dyn IGameKeysEventListener>,
    ) -> Result<()> {
        let listener_addr = listener.as_binder().as_native() as usize;
        log::debug!("Unregistering GameKey listener {listener_addr}");

        let mut listeners_lock = self.listeners.lock().await;

        if let Some(index) = listeners_lock
            .iter()
            .enumerate()
            .find(|(_, item)| Strong::eq(item, listener))
            .map(|(index, _)| index)
        {
            listeners_lock.remove(index);
        } else {
            log::warn!("GameKey listener {listener_addr} is not registered");
            return Err(Self::invalid_argument_status(
                "Listener is not registered",
            ));
        }

        Ok(())
    }
}
