# Race Client Launcher

Лаунчер Race Client на **Tauri (Rust)** под **Windows** и **Arch Linux**.
Бэкенд запуска Minecraft — библиотека [`lighty-launcher`](https://github.com/Lighty-Launcher/LightyLauncherLib)
(Fabric, авто-Java, кросс-платформа). UI повторяет мокап (тёмно-золотая тема).

## Возможности

- Оффлайн-вход по нику (cracked), как в мокапе — одно поле «Ник»
- Выбор RAM / Java / JVM-аргументов / директории / Fabric loader
- Авто-загрузка мода Race Client из GitHub Releases (`ItzSkater/ThunderHack-Immortal`)
- Вкладка «Обновления» тянет реальные релизы с GitHub
- Безрамочное окно с кастомными кнопками (как в макете)
- Настройки сохраняются в конфиг ОС

## Структура

```
launcher/
├── src/                  # фронтенд (статика, без сборщика)
│   ├── index.html
│   ├── styles.css
│   └── main.js           # связь с бэкендом через window.__TAURI__
└── src-tauri/            # бэкенд на Rust
    ├── Cargo.toml
    ├── tauri.conf.json
    ├── build.rs
    ├── capabilities/default.json
    └── src/
        ├── main.rs       # Tauri-команды
        ├── config.rs     # сохранение настроек
        ├── updates.rs    # GitHub releases + загрузка мода
        └── launch.rs     # интеграция с lighty-launcher
```

## Предварительные требования

- **Rust** (stable) — https://rustup.rs
- **Tauri CLI**: `cargo install tauri-cli --version "^2"`
- **Windows**: WebView2 (обычно уже есть), MSVC build tools
- **Arch**: `sudo pacman -S webkit2gtk-4.1 base-devel curl wget file openssl appmenu-gtk-module libappindicator-gtk3 librsvg`

## Запуск (dev)

```bash
cd launcher
cargo tauri dev
```

## Сборка релиза

```bash
cd launcher
cargo tauri build
```

Артефакты:
- **Windows**: `src-tauri/target/release/bundle/nsis/*-setup.exe`
- **Arch/Linux**: `src-tauri/target/release/bundle/appimage/*.AppImage` и `deb/*.deb`

> Для Arch можно собрать и нативный пакет — добавь `pacman` в `bundle.targets`
> в `tauri.conf.json`, либо упакуй AppImage.

## Иконки

В корне `launcher/` лежит `app-icon.png` (512×512). Полный набор иконок
(`32x32.png`, `128x128.png`, `icon.ico`, `icon.icns`, …) генерируется один раз:

```bash
cd launcher
cargo tauri icon app-icon.png
```

Чтобы заменить иконку — просто перезапиши `app-icon.png` своим лого и перегенерируй.

## Troubleshooting

### Linux: `EGL_BAD_PARAMETER. Aborting...` / серое окно

Известный баг WebKitGTK 2.40+ на Wayland с некоторыми GPU-конфигами.
Лаунчер уже выставляет `GDK_BACKEND=x11`, `WEBKIT_DISABLE_DMABUF_RENDERER=1`,
`WEBKIT_DISABLE_COMPOSITING_MODE=1`, но если столкнулся на старой сборке или
EGL всё равно не идёт — запусти вручную:

```bash
GDK_BACKEND=x11 \
WEBKIT_DISABLE_DMABUF_RENDERER=1 \
WEBKIT_DISABLE_COMPOSITING_MODE=1 \
./THI.Launcher_*.AppImage
```

Если и это не помогло — добавь программный GL (медленнее, но всегда работает):

```bash
LIBGL_ALWAYS_SOFTWARE=1 GDK_BACKEND=x11 ./THI.Launcher_*.AppImage
```

## Релизный workflow (CI)

В `.github/workflows/launcher-release.yml` настроена сборка под **Windows** и
**Linux** (`.exe`/`.msi`, `.AppImage`/`.deb`). Триггер — релиз с тегом, начинающимся
с `launcher-v`. Шаги:

1. На GitHub: **Releases → Draft a new release**, тег вида `launcher-v0.1.0`.
2. Опубликуй релиз — workflow соберёт артефакты под обе ОС и прикрепит их к нему.
3. Альтернатива: **Actions → Launcher Release → Run workflow**, указать существующий тег.

Workflow мода (`release.yml`) не пересекается с лаунчерским — он смотрит только
на теги без префикса `launcher-`.

## Важно про lighty-launcher

Подтверждённый flow запуска (из README библиотеки) реализован в `launch.rs`:
`AppState::init` → `VersionBuilder::new(.., Loader::Fabric, ..)` →
`OfflineAuth::new(nick).authenticate()` → `instance.launch(&profile, JavaDistribution::Temurin).run()`.

Продвинутые методы билдера (**RAM, JVM-аргументы, game_dir, разрешение,
fullscreen**) различаются между версиями либы — в `launch.rs` они оформлены
отдельным блоком с примерами и закомментированы, чтобы код собирался сразу.
После `cargo doc -p lighty-launcher --open` подставь точные имена методов и
раскомментируй — UI уже собирает все эти значения и передаёт их в бэкенд.

Версия либы запинена в `Cargo.toml` (`lighty-launcher = "26.5.12"`).
