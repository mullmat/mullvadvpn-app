use serde::{Deserialize, Serialize};
use std::str::FromStr;

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Clone)]
#[serde(rename_all = "snake_case")]
pub enum OsVersion {
    Linux,
    Macos(MacosVersion),
    Windows(WindowsVersion),
}

impl std::fmt::Display for OsVersion {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            OsVersion::Linux => f.write_str("Linux"),
            OsVersion::Macos(version) => write!(f, "macOS {}", version.major),
            OsVersion::Windows(version) => write!(f, "Windows {}", version.major),
        }
    }
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Clone)]
pub struct MacosVersion {
    pub major: u32,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Clone)]
pub struct WindowsVersion {
    pub major: u32,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, Clone, Copy)]
#[serde(rename_all = "snake_case")]
pub enum Os {
    Linux,
    Macos,
    Windows,
}

impl FromStr for Os {
    type Err = Box<dyn std::error::Error>;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "linux" => Ok(Os::Linux),
            "macos" => Ok(Os::Macos),
            "windows" => Ok(Os::Windows),
            other => Err(format!("unknown os {other}").into()),
        }
    }
}

impl std::fmt::Display for Os {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Os::Linux => f.write_str("Linux"),
            Os::Macos => f.write_str("macOS"),
            Os::Windows => f.write_str("Windows"),
        }
    }
}
