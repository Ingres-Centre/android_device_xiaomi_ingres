use anyhow::Context;
use crate::binder_service::TouchInjectorService;
use binder_tokio::TokioRuntime;
use vendor_lineage_touchinjector::{aidl::vendor::lineage::touchinjector::ITouchInjector::BnTouchInjector, binder::BinderFeatures};
use tokio::runtime::Runtime;
use log::LevelFilter;

mod counter;
mod binder_service;
mod stream;
mod touch_emulator;
mod touch_merger;

mod build_config {
    include!(concat!(env!("OUT_DIR"), "/cfg_source_device_name.rs"));
    include!(concat!(env!("OUT_DIR"), "/cfg_additional_slots_count.rs"));
}

fn main() -> anyhow::Result<()> {
    let _init_success = logger::init(
        logger::Config::default()
            .with_tag_on_device("vendor.lineage.touchinjector-service")
            .with_max_level(LevelFilter::Debug),
    );

    log::info!("Startup...");

    let runtime = Runtime::new()?;

    let svc = match runtime.block_on(async {
        TouchInjectorService::new()
            .await
            .context("Failed to create TouchInjectorService")
    }) {
        Ok(svc) => svc,
        Err(err) => {
            log::error!("{:?}", err);
            return Err(err);
        }
    };

    binder::ProcessState::start_thread_pool();
    log::info!("Binder thread pool has been started!");

    let name = "vendor.lineage.touchinjector.ITouchInjector/default";
    let async_binder = BnTouchInjector::new_async_binder(
        svc,
        TokioRuntime(runtime.handle().clone()),
        BinderFeatures::default(),
    );

    binder::add_service(name, async_binder.as_binder())
        .context("Failed to add ITouchInjector binder service")?;
    log::info!("Binder service '{}' added successfully!", name);

    binder::ProcessState::join_thread_pool();

    Ok(())
}
