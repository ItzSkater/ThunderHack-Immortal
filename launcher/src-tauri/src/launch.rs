use crate::config::AppConfig;
use crate::updates;

// lighty-launcher exposes its main types through the prelude.
// Confirmed surface (from the crate README):
//   AppState::init(name)
//   VersionBuilder::new(id, Loader, loader_version, mc_version)
//   OfflineAuth::new(nick) -> .authenticate().await -> profile
//   instance.launch(&profile, JavaDistribution::Temurin).run().await
use lighty_launcher::prelude::*;

/// Install (if needed) and launch ThunderHack-Immortal.
///
/// `emit(stage, message)` streams human-readable progress to the UI.
pub async fn launch<F>(cfg: &AppConfig, emit: &F) -> anyhow::Result<()>
where
    F: Fn(&str, &str),
{
    // 1. Make sure the THI mod jar is present in the instance's mods folder.
    let mods_dir = cfg.resolved_game_dir().join("mods");
    if cfg.auto_update || !mods_present(&mods_dir) {
        emit("download", "Загрузка Race Client...");
        if let Err(e) = updates::download_latest_mod(&mods_dir).await {
            // Non-fatal: the user may have placed the jar manually.
            emit("warn", &format!("Не удалось обновить мод: {e}"));
        }
    }

    // 2. Initialise the launcher backend (idempotent).
    emit("prepare", "Инициализация...");
    AppState::init("THILauncher")?;

    // 3. Configure the instance: Fabric loader on the chosen MC version.
    let loader_version = cfg.fabric_loader.trim();
    let mut instance = VersionBuilder::new(
        "thunderhack-immortal",
        Loader::Fabric,
        loader_version, // empty -> latest stable fabric loader
        &cfg.mc_version,
    );

    // --- Advanced configuration -------------------------------------------
    // These map directly to the UI fields (RAM / JVM args / game dir / window).
    // The exact builder method names depend on your pinned lighty-launcher
    // version — uncomment and adjust after `cargo doc -p lighty-launcher`.
    // Everything below compiles without them; they only refine the launch.
    //
    // instance.memory(cfg.ram_mb);                 // max heap in MB
    // instance.jvm_args(split_args(&cfg.jvm_args)); // extra JVM args
    // instance.game_dir(cfg.resolved_game_dir());   // instance directory
    // if cfg.fullscreen { instance.fullscreen(true); }
    // if let Some((w, h)) = parse_res(&cfg.resolution) { instance.resolution(w, h); }
    // ----------------------------------------------------------------------

    // 4. Offline auth (cracked nick) — matches the single "Ник" field in the UI.
    emit("auth", "Авторизация...");
    let mut auth = OfflineAuth::new(&cfg.nick);
    let profile = auth.authenticate().await?;

    // 5. Download anything missing (assets/libraries/loader/Java) and launch.
    emit("launch", "Запуск Minecraft...");
    let java = if cfg.java_path.trim().is_empty() {
        JavaDistribution::Temurin
    } else {
        // A custom java path was provided; lighty still needs a distribution
        // hint for any auto-download fallback. Custom-path wiring is version
        // specific — see README. We default to Temurin here.
        JavaDistribution::Temurin
    };

    instance.launch(&profile, java).run().await?;
    Ok(())
}

fn mods_present(mods_dir: &std::path::Path) -> bool {
    std::fs::read_dir(mods_dir)
        .map(|mut it| {
            it.any(|e| {
                e.ok()
                    .map(|e| {
                        e.file_name()
                            .to_string_lossy()
                            .to_lowercase()
                            .ends_with(".jar")
                    })
                    .unwrap_or(false)
            })
        })
        .unwrap_or(false)
}

#[allow(dead_code)]
fn split_args(s: &str) -> Vec<String> {
    s.split_whitespace().map(|a| a.to_string()).collect()
}

#[allow(dead_code)]
fn parse_res(s: &str) -> Option<(u32, u32)> {
    let (w, h) = s.split_once('x')?;
    Some((w.trim().parse().ok()?, h.trim().parse().ok()?))
}
