// Prevents an extra console window on Windows in release.
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod config;
mod updates;
mod launch;

use config::AppConfig;
use tauri::Emitter;

#[tauri::command]
fn get_config() -> AppConfig {
    AppConfig::load()
}

#[tauri::command]
fn save_config(cfg: AppConfig) -> Result<(), String> {
    cfg.save().map_err(|e| e.to_string())
}

#[tauri::command]
async fn check_updates() -> Result<Vec<updates::Release>, String> {
    updates::fetch_releases().await.map_err(|e| e.to_string())
}

/// Launch the game. Streams coarse status lines to the frontend via the
/// `launch://status` event and resolves when the game process has been started.
#[tauri::command]
async fn launch_game(app: tauri::AppHandle, cfg: AppConfig) -> Result<(), String> {
    let emit = move |stage: &str, msg: &str| {
        let _ = app.emit(
            "launch://status",
            serde_json::json!({ "stage": stage, "message": msg }),
        );
    };

    emit("start", "Подготовка...");
    match launch::launch(&cfg, &emit).await {
        Ok(()) => {
            emit("done", "Игра запущена");
            Ok(())
        }
        Err(e) => {
            emit("error", &format!("Ошибка: {e}"));
            Err(e.to_string())
        }
    }
}

fn main() {
    // Workarounds for WebKitGTK on Linux failing to initialise EGL ("Could not
    // create default EGL display: EGL_BAD_PARAMETER, Aborting...") which leaves
    // a grey window with no rendered content. Affects modern Wayland + WebKitGTK
    // 2.40+ on many GPU/driver combos. We force a known-good path:
    //   - run GTK under XWayland instead of native Wayland,
    //   - disable WebKit's DMABUF / compositing renderers,
    //   - leave LIBGL_ALWAYS_SOFTWARE alone (the user can opt in if EGL still
    //     refuses on truly broken GL stacks).
    // Each env var is only set if the user didn't override it.
    #[cfg(target_os = "linux")]
    {
        for (k, v) in [
            ("GDK_BACKEND", "x11"),
            ("WEBKIT_DISABLE_DMABUF_RENDERER", "1"),
            ("WEBKIT_DISABLE_COMPOSITING_MODE", "1"),
        ] {
            if std::env::var_os(k).is_none() {
                std::env::set_var(k, v);
            }
        }
    }

    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![
            get_config,
            save_config,
            check_updates,
            launch_game
        ])
        .run(tauri::generate_context!())
        .expect("error while running THI launcher");
}
