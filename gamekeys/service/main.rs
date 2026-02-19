use std::process::ExitCode;
use binder::BinderFeatures;
use binder_tokio::TokioRuntime;
use crate::binder_service::GameKeysService;
use log::LevelFilter;
use vendor_lineage_gamekeys::aidl::vendor::lineage::gamekeys::IGameKeys::BnGameKeys;

mod binder_service;
mod gamekeys;

#[tokio::main(flavor = "multi_thread", worker_threads = 2)]
async fn main() -> ExitCode {
    let _init_success = logger::init(
        logger::Config::default()
            .with_tag_on_device("vendor.lineage.gamekeys-service")
            .with_max_level(LevelFilter::Trace),
    );

    log::info!("Startup...");

    binder::ProcessState::start_thread_pool();
    log::debug!("Binder thread pool has been started!");

    let service_name = "vendor.lineage.gamekeys.IGameKeys/default";
    let service = GameKeysService::new();

    let task = match service.event_loop_task() {
        Ok(task) => task,
        Err(err) => {
            log::error!("Failed to create event loop handler task: {err:?}");
            return ExitCode::FAILURE; 
        }
    };

    if let Err(err) = binder::add_service(
        service_name,
        BnGameKeys::new_async_binder(
            service,
            TokioRuntime(tokio::runtime::Handle::current()),
            BinderFeatures::default(),
        ).as_binder()
    ) {
        log::error!("Failed to add IGameKeys binder service: {err:?}");
        return ExitCode::FAILURE;
    }

    log::debug!("Binder service '{service_name}' added successfully!");

    match task.await {
        Ok(()) => unreachable!(),
        Err(err) => {
            log::error!("GameKey event loop handler task failed: {err:?}");
            return ExitCode::FAILURE;
        },
    }
}
