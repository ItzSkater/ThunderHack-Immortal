# ThunderSync Relay

Лёгкий TCP-релей без зависимостей для синхронизации между клиентами
ThunderHack-Immortal (модуль **PenisSync**). Клиенты подключаются, шлют JSON
построчно, а сервер пересылает каждую строку всем остальным клиентам в той же
комнате (`room`). Сам payload сервер не интерпретирует — только поле `room`.

## Запуск на VPS

```bash
# 1. скопировать файл на VPS
scp ThunderSyncRelay.java user@your-vps:~

# 2. собрать (нужна только JDK 17+)
javac ThunderSyncRelay.java

# 3. запустить (порт по умолчанию 7777)
java ThunderSyncRelay 7777
```

Чтобы релей жил после выхода из SSH — запусти в `tmux`/`screen` или сделай
systemd-юнит:

```ini
# /etc/systemd/system/thundersync.service
[Unit]
Description=ThunderSync relay
After=network.target

[Service]
WorkingDirectory=/home/user
ExecStart=/usr/bin/java ThunderSyncRelay 7777
Restart=always
User=user

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable --now thundersync
```

Не забудь открыть порт в фаерволе VPS:

```bash
sudo ufw allow 7777/tcp
```

## Настройка в игре

В модуле **PenisSync**:

- `ServerIP` — IP твоего VPS
- `ServerPort` — `7777` (или какой указал)
- `Room` — любая общая строка-ключ; у тебя и у друга должна совпадать
- `RenderSelf` — рисовать ли на себе (для проверки)

Когда ты и друг указали одинаковый `Room` и включили модуль — друг увидит
PenisESP на твоём игроке (и наоборот), пока модуль включён. При выключении
сервер сам разошлёт `on:false`, и рендер пропадёт.

## Протокол

Клиент → сервер (построчно, UTF-8, `\n`):

```json
{"room":"default","uuid":"<player-uuid>","on":true}
```

Сервер пересылает эту же строку всем остальным в комнате. При отключении
клиента сервер автоматически рассылает `{"room":...,"uuid":...,"on":false}`.
