# ThunderMesh — зашифрованный P2P чат и шаринг координат

Сетевая часть для модуля **Mesh** в ThunderHack-Immortal. Даёт приватный чат и
обмен координатами между друзьями с THI **поверх любого MC-сервера** (или вообще
без него), с end-to-end шифрованием.

## Как это работает

- **Шифрование E2E.** Все сообщения шифруются AES-256-GCM ключом, выведенным из
  пароля `Room` (SHA-256). Никто без пароля комнаты прочитать не может — включая
  сервер.
- **P2P, где возможно.** Клиенты обмениваются публичными адресами через сервер и
  пробивают NAT (UDP hole-punching), после чего сообщения идут **напрямую** друг
  другу.
- **Fallback через сервер.** Где NAT строгий (симметричный) и пробить нельзя,
  сообщение идёт через сервер — но **только как шифротекст**, сервер его не читает.
- **Сервер «слепой».** Он группирует пиров по `roomId`, который является *хэшем*
  пароля комнаты, поэтому даже названия комнаты не знает.

То есть это честный P2P с шифрованием, но с надёжным fallback — работает у всех,
а не только там, где NAT добрый.

## Запуск сервера на VPS

```bash
scp ThunderMeshServer.java user@your-vps:~
ssh user@your-vps
javac ThunderMeshServer.java
java ThunderMeshServer 7778        # UDP, порт по умолчанию 7778
```

Открыть порт (UDP!):

```bash
sudo ufw allow 7778/udp
```

Держать живым — `tmux`/`screen` или systemd:

```ini
# /etc/systemd/system/thundermesh.service
[Unit]
Description=ThunderMesh server
After=network.target
[Service]
WorkingDirectory=/home/user
ExecStart=/usr/bin/java ThunderMeshServer 7778
Restart=always
User=user
[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable --now thundermesh
```

## В игре

Модуль **Mesh** (категория Misc):

- `ServerIP` — IP твоего VPS
- `ServerPort` — `7778`
- `Room` — общий пароль комнаты (у всех друзей одинаковый — это и есть ключ шифрования)
- `Nick` — имя в чате (пусто = ник майнкрафта)

Включаешь модуль → команды:

- `.mc <текст>` — отправить сообщение в комнату
- `.mpos` — поделиться своими координатами

Входящие печатаются в твой майн-чат с префиксом `[Mesh]`.

## Протокол (UDP, первый байт = тип)

```
0x00 PUNCH     []                                      — пробивка NAT (no-op)
0x01 REGISTER  [roomId:16][peerId:16]                  клиент -> сервер
0x02 DATA      [roomId:16][senderPeerId:16][blob...]   в обе стороны (blob = AES-GCM)
0x03 PEERLIST  [count:1]({peerId:16}{ipv4:4}{port:2})* сервер -> клиент
```

`blob` = `nonce(12) || AES-256-GCM(ciphertext+tag)`. Открытый текст — JSON:
`{"t":"chat","n":"ник","m":"текст","id":"<dedup-id>"}` или
`{"t":"coords","n":"ник","x":..,"y":..,"z":..,"dim":"OW","id":..}`.
