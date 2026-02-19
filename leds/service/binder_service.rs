use crate::awinic_sysfs;
use async_trait::async_trait;
use binder::{Interface, Result, Status};
use vendor_lineage_leds::aidl::vendor::lineage::leds::ILeds::ILedsAsyncServer;

pub struct LedsService;

impl Interface for LedsService {}

#[allow(non_snake_case)]
#[async_trait]
impl ILedsAsyncServer for LedsService {
    async fn r#isEnabled<'a>(&'a self) -> Result<bool> {
        log::debug!("isEnabled()...");
        let enabled = awinic_sysfs::is_enabled().map_err(Into::<Status>::into)?;
        log::debug!("isEnabled(): {enabled}");

        Ok(enabled)
    }

    async fn r#setEnabled<'a>(&'a self, enabled: bool) -> Result<()> {
        log::debug!("setEnable({enabled})");
        awinic_sysfs::set_enabled(enabled).map_err(Into::<Status>::into)?;

        Ok(())
    }

    async fn r#getBrightness<'a>(&'a self) -> Result<i32> {
        log::debug!("getBrightness()...");
        let brightness = awinic_sysfs::get_brightness().map_err(Into::<Status>::into)?;
        log::debug!("getBrightness(): {brightness}");

        Ok(brightness)
    }

    async fn r#setBrightness<'a>(&'a self, brightness: i32) -> Result<()> {
        log::debug!("setBrightness({brightness})");
        awinic_sysfs::set_brightness(brightness).map_err(Into::<Status>::into)?;

        Ok(())
    }

    async fn r#getCurrentEffect<'a>(&'a self) -> Result<i32> {
        log::debug!("getCurrentEffect()...");
        let effect_index = awinic_sysfs::get_current_effect().map_err(Into::<Status>::into)?;
        log::debug!("getCurrentEffect(): {effect_index}");

        Ok(effect_index)
    }

    async fn r#setCurrentEffect<'a>(&'a self, effectIndex: i32) -> Result<()> {
        log::debug!("setCurrentEffect({effectIndex})");
        awinic_sysfs::set_effect(effectIndex).map_err(Into::<Status>::into)?;

        Ok(())
    }

    async fn r#getAvailableEffects<'a>(&'a self) -> Result<Vec<String>> {
        log::debug!("getAvailableEffects()...");
        let available_effects = awinic_sysfs::get_available_effects().map_err(Into::<Status>::into)?;
        log::debug!("getAvailableEffects(): [");
        for effect in &available_effects {
            log::debug!("    {effect}");
        }
        log::debug!("]");

        Ok(available_effects)
    }

    async fn r#getColor<'a>(&'a self, led_index: i32) -> Result<i32> {
        log::debug!("getColor({led_index})...");
        let color = awinic_sysfs::get_color(led_index)
            .map_err(Into::<Status>::into)?
            .unwrap_or(0);
        log::debug!("getColor({led_index}): {color}");

        Ok(color)
    }

    async fn r#setColor<'a>(&'a self, led_index: i32, rgb: i32) -> Result<()> {
        let rgb = rgb & 0xFFFFFF;

        log::debug!("setColor({led_index:06X}, {rgb})");
        awinic_sysfs::set_color(led_index, rgb).map_err(Into::<Status>::into)?;

        Ok(())
    }

    async fn r#getAllColors<'a>(&'a self) -> Result<Vec<i32>> {
        log::debug!("getAllColors(): [");
        let colors = awinic_sysfs::get_all_colors().map_err(Into::<Status>::into)?;
        for color in &colors {
            log::debug!("\t{color:06X}");
        }
        log::debug!("]");

        Ok(colors)
    }

    async fn r#getFrequency<'a>(&'a self) -> Result<i32> {
        log::debug!("getFrequency()...");
        let frequency = awinic_sysfs::get_frequency().map_err(Into::<Status>::into)?;
        log::debug!("getFrequency(): {frequency}");

        Ok(frequency)
    }

    async fn r#setFrequency<'a>(&'a self, frequency: i32) -> Result<()> {
        log::debug!("setFrequency({frequency})");
        awinic_sysfs::set_frequency(frequency).map_err(Into::<Status>::into)?;

        Ok(())
    }

    async fn r#flushConfig<'a>(&'a self, useCustomColors: bool) -> Result<()> {
        log::debug!("flushConfig({useCustomColors})");
        awinic_sysfs::flush_config(useCustomColors).map_err(Into::<Status>::into)?;

        Ok(())
    }
}
