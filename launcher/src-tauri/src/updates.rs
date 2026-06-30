use serde::{Deserialize, Serialize};

const REPO: &str = "ItzSkater/ThunderHack-Immortal";

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Release {
    pub tag: String,
    pub name: String,
    pub body: String,
    pub published_at: String,
    pub prerelease: bool,
    pub jar_url: Option<String>,
}

#[derive(Deserialize)]
struct GhRelease {
    tag_name: String,
    name: Option<String>,
    body: Option<String>,
    published_at: Option<String>,
    prerelease: bool,
    assets: Vec<GhAsset>,
}

#[derive(Deserialize)]
struct GhAsset {
    name: String,
    browser_download_url: String,
}

fn client() -> reqwest::Client {
    reqwest::Client::builder()
        .user_agent("THILauncher")
        .build()
        .unwrap_or_default()
}

/// Fetch releases from GitHub for the THI repo, newest first.
pub async fn fetch_releases() -> anyhow::Result<Vec<Release>> {
    let url = format!("https://api.github.com/repos/{REPO}/releases?per_page=20");
    let gh: Vec<GhRelease> = client().get(&url).send().await?.json().await?;

    Ok(gh
        .into_iter()
        .map(|r| {
            // pick the main mod jar (skip sources/dev jars)
            let jar_url = r
                .assets
                .iter()
                .find(|a| {
                    let n = a.name.to_lowercase();
                    n.ends_with(".jar") && !n.contains("source") && !n.contains("dev")
                })
                .map(|a| a.browser_download_url.clone());

            Release {
                name: r.name.unwrap_or_else(|| r.tag_name.clone()),
                tag: r.tag_name,
                body: r.body.unwrap_or_default(),
                published_at: r.published_at.unwrap_or_default(),
                prerelease: r.prerelease,
                jar_url,
            }
        })
        .collect())
}

/// Download the mod jar for the latest (non-prerelease) release into `mods_dir`.
pub async fn download_latest_mod(mods_dir: &std::path::Path) -> anyhow::Result<()> {
    let releases = fetch_releases().await?;
    let release = releases
        .iter()
        .find(|r| !r.prerelease)
        .or_else(|| releases.first())
        .ok_or_else(|| anyhow::anyhow!("no releases found"))?;

    let url = release
        .jar_url
        .as_ref()
        .ok_or_else(|| anyhow::anyhow!("release {} has no mod jar asset", release.tag))?;

    std::fs::create_dir_all(mods_dir)?;
    let bytes = client().get(url).send().await?.bytes().await?;

    let filename = url.rsplit('/').next().unwrap_or("thunderhack-immortal.jar");
    std::fs::write(mods_dir.join(filename), &bytes)?;
    Ok(())
}
