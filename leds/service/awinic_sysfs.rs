use std::ops::Deref;
use std::str::FromStr;

macro_rules! path {
    ($attr:expr) => {
        concat!(
            "/sys/class/leds/aw22xxx_led/",
            $attr
        )
    };
}

const ATTR_ENABLE: &str = path!("hwen");
const ATTR_BRIGHTNESS: &str = path!("brightness");
const ATTR_EFFECT: &str = path!("effect");
const ATTR_CONFIG: &str = path!("cfg");
const ATTR_COLORS: &str = path!("rgb");
const ATTR_FREQUENCY: &str = path!("frq");

pub type Result<T> = std::result::Result<T, Error>;

#[derive(Debug)]
pub enum Error {
    Io(std::io::Error),
}

impl From<std::io::Error> for Error {
    fn from(e: std::io::Error) -> Error {
        Self::Io(e)
    }
}

impl Into<binder::Status> for Error {
    fn into(self) -> binder::Status {
        binder::Status::new_exception_str(
            binder::ExceptionCode::SERVICE_SPECIFIC,
            Some(format!("{self}").deref()),
        )
    }
}

impl std::fmt::Display for Error {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::result::Result<(), std::fmt::Error> {
        match self {
            Self::Io(inner) => inner.fmt(f),
        }
    }
}

impl std::error::Error for Error {}

pub fn is_enabled() -> Result<bool> {
    let resp = std::fs::read_to_string(ATTR_ENABLE)?;

    Ok(resp.as_bytes()[5] == b'1')
}

pub fn set_enabled(enabled: bool) -> Result<()> {
    std::fs::write(ATTR_ENABLE, if enabled { "1" } else { "0" })?;

    Ok(())
}

pub fn get_brightness() -> Result<i32> {
    let resp = std::fs::read_to_string(ATTR_BRIGHTNESS)?;

    Ok(i32::from_str(resp.deref()).unwrap()) // I believe in driver
}

pub fn set_brightness(brightness: i32) -> Result<()> {
    std::fs::write(ATTR_BRIGHTNESS, brightness.to_string())?;

    Ok(())
}

pub fn get_current_effect() -> Result<i32> {
    let resp = std::fs::read_to_string(ATTR_EFFECT)?;

    Ok(i32::from_str_radix(&resp[11..13], 16).unwrap())
}

pub fn set_effect(effect: i32) -> Result<()> {
    std::fs::write(ATTR_EFFECT, effect.to_string())?;

    Ok(())
}

pub fn get_available_effects() -> Result<Vec<String>> {
    let resp = std::fs::read_to_string(ATTR_CONFIG)?;

    Ok(resp
        .split('\n')
        .filter_map(|line| {
            if !line.starts_with("cfg") {
                return None;
            }

            line.split_once(" = ").map(|(_, value)| value.to_string())
        })
        .collect())
}

pub fn get_color(led_index: i32) -> Result<Option<i32>> {
    let resp = std::fs::read_to_string(ATTR_COLORS)?;

    Ok(resp.split('\n').find_map(|line| {
        if !i32::from_str(&line[5..6]).is_ok_and(|index| index == led_index) {
            return None;
        }

        // yeah. driver is using BRG format. idk why.
        let brg = i32::from_str_radix(line.split_at_checked(11)?.1, 16).ok()?;

        let b = (brg & 0xFF0000) >> 16;
        let r = (brg & 0x00FF00) >> 8;
        let g = (brg & 0x0000FF) >> 0;

        Some(r << 16 | g << 8 | b << 0)
    }))
}

pub fn set_color(led_index: i32, color: i32) -> Result<()> {
    let r = (color & 0xFF0000) >> 16;
    let g = (color & 0x00FF00) >> 8;
    let b = (color & 0x0000FF) >> 0;

    let brg = b << 16 | r << 8 | g << 0;

    let cmd = format!("{led_index} {brg:06X}");

    std::fs::write(ATTR_COLORS, cmd)?;

    Ok(())
}

pub fn get_all_colors() -> Result<Vec<i32>> {
    let resp = std::fs::read_to_string(ATTR_COLORS)?;

    Ok(resp.split('\n').filter_map(|line| {
        let brg = i32::from_str_radix(line.split_at_checked(11)?.1, 16).ok()?;

        let b = (brg & 0xFF0000) >> 16;
        let r = (brg & 0x00FF00) >> 8;
        let g = (brg & 0x0000FF) >> 0;

        Some(r << 16 | g << 8 | b << 0)
    }).collect())
}

pub fn get_frequency() -> Result<i32> {
    const FRQ_TABLE: [i32; 64] = [0,64,128,192,256,320,384,448,512,576,640,704,768,832,896,960,1024,1088,1152,1216,1280,1344,1408,1472,1536,1600,1664,1728,1792,1856,1920,1984,2048,2112,2176,2240,2304,2368,2432,2496,2560,2624,2688,2752,2816,2880,2944,3008,3072,3136,3200,3264,3328,3392,3456,3520,3584,3648,3712,3776,3840,3904,3968,4032];

    let resp = std::fs::read_to_string(ATTR_FREQUENCY)?;

    Ok(FRQ_TABLE.get(usize::from_str_radix(&resp[8..10], 16).unwrap()).cloned().unwrap_or(0))
}

pub fn set_frequency(frequency: i32) -> Result<()> {
    std::fs::write(ATTR_FREQUENCY, frequency.to_string())?;

    Ok(())
}

pub fn flush_config(use_custom_colors: bool) -> Result<()> {
    std::fs::write(ATTR_CONFIG, if use_custom_colors { "2" } else { "1" })?;

    Ok(())
}
