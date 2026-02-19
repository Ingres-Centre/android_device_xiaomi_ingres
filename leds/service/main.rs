use std::process::ExitCode;

use tokio::runtime::Runtime;
use {
    crate::binder_service::LedsService,
    binder_tokio::TokioRuntime,
    log::LevelFilter,
    vendor_lineage_leds::{
        aidl::vendor::lineage::leds::ILeds::BnLeds,
        binder::BinderFeatures,
    },
};

mod binder_service;
mod awinic_sysfs;

fn main() -> ExitCode {
    let _init_success = logger::init(
        logger::Config::default()
            .with_tag_on_device("vendor.lineage.leds-service")
            .with_max_level(LevelFilter::Debug),
    );

    let runtime = match Runtime::new() {
        Ok(rt) => rt,
        Err(err) => {
            log::error!("Unable to create new tokio runtime: {err:?}");
            return ExitCode::FAILURE;
        }
    };

    binder::ProcessState::start_thread_pool();
    log::debug!("Binder thread pool has been started!");

    let service_name = "vendor.lineage.leds.ILeds/default";

    let binder = BnLeds::new_async_binder(
        LedsService,
        TokioRuntime(runtime.handle().clone()),
        BinderFeatures::default(),
    );

    if let Err(err) = binder::add_service(service_name, binder.as_binder()) {
        log::error!("Failed to add ILeds binder service: {err:?}");
        return ExitCode::FAILURE;
    }

    log::debug!("Binder service '{service_name}' added successfully!");

    binder::ProcessState::join_thread_pool();

    ExitCode::SUCCESS
}
