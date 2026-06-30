use directories::ProjectDirs;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

/// Persisted launcher settings. Mirrors the fields shown in the UI.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(default)]
pub struct AppConfig {
    pub nick: String,
    pub ram_mb: u32,
    pub java_path: String,        // empty = let lighty auto-manage (Temurin)
    pub jvm_args: String,
    pub game_dir: String,         // empty = default per-OS instance dir
    pub mc_version: String,
    pub fabric_loader: String,    // empty = latest stable fabric loader
    pub resolution: String,
    pub close_on_launch: bool,
    pub auto_update: bool,
    pub fullscreen: bool,
}

impl Default for AppConfig {
    fn default() -> Self {
        Self {
            nick: "Player".into(),
            ram_mb: 4096,
            java_path: String::new(),
            jvm_args: "-XX:+UseG1GC -XX:+UnlockExperimentalVMOptions".into(),
            game_dir: String::new(),
            mc_version: "1.21".into(),
            fabric_loader: String::new(),
            resolution: "1920x1080".into(),
            close_on_launch: true,
            auto_update: true,
            fullscreen: false,
        }
    }
}

impl AppConfig {
    fn dirs() -> Option<ProjectDirs> {
        ProjectDirs::from("gg", "thi", "THILauncher")
    }

    pub fn config_path() -> PathBuf {
        Self::dirs()
            .map(|d| d.config_dir().join("config.json"))
            .unwrap_or_else(|| PathBuf::from("thi-launcher-config.json"))
    }

    pub fn load() -> Self {
        let path = Self::config_path();
        std::fs::read_to_string(&path)
            .ok()
            .and_then(|s| serde_json::from_str(&s).ok())
            .unwrap_or_default()
    }

    pub fn save(&self) -> anyhow::Result<()> {
        let path = Self::config_path();
        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent)?;
        }
        std::fs::write(&path, serde_json::to_string_pretty(self)?)?;
        Ok(())
    }

    /// Resolve the effective game directory (instance dir) for the mod folder.
    pub fn resolved_game_dir(&self) -> PathBuf {
        if !self.game_dir.trim().is_empty() {
            return PathBuf::from(shellexpand(&self.game_dir));
        }
        Self::dirs()
            .map(|d| d.data_dir().join("instance"))
            .unwrap_or_else(|| PathBuf::from(".thi/instance"))
    }
}

/// Minimal `~` expansion so users can type `~/.minecraft`.
fn shellexpand(p: &str) -> String {
    if let Some(rest) = p.strip_prefix("~/") {
        if let Some(home) = directories::UserDirs::new().map(|u| u.home_dir().to_path_buf()) {
            return home.join(rest).to_string_lossy().into_owned();
        }
    }
    p.to_string()
}
