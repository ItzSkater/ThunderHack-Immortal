# Race Meteor Addon

Аддон для **Meteor Client `26.1.2`** (MC 1.21.11, JDK 25), портирующий фичи из
Race Client. Основан на официальном
[meteor-addon-template](https://github.com/MeteorDevelopment/meteor-addon-template)
и вдохновлён идеями Meteor Rejects / Trouser Streak.

Категория в Meteor — **Race**. Модули:

| Модуль | Что делает |
|--------|-----------|
| **auto-pay** | Шлёт `/bal`, парсит баланс (`Баланс: €7 млн.`, `Balance: 256 тыс.` — тыс/k, млн/m, млрд/b) и каждые N сек платит игроку `/pay <ник> <сумма>`. Режимы Fixed / All. |
| **target-tp** | Рывок к ближайшему игроку пачкой пакетов позиции. Зависит от анти-чита. |
| **vega-aura** | Kill-aura с **Vega-ротацией** (плавный шаг yaw + джиттер). Отдельный модуль, т.к. Meteor не даёт плагинить ротацию в свою KillAura. |
| **target-strafe** | Быстрый круговой стрейф вокруг ближайшего игрока. |

## Сборка

```bash
cd meteor-addon
gradle build          # или ./gradlew build, если добавишь wrapper
```

Нужна **JDK 25** (Meteor 26.1.2). Meteor тянется из `maven.meteordev.org`.
Готовый jar — в `build/libs/`, кинуть в папку модов рядом с Meteor.

## Оговорки (важно)

Собрать/протестить это в CI-контейнере я не мог — Meteor `26.1.2-SNAPSHOT`,
JDK 25 и loom `1.16-SNAPSHOT` bleeding-edge, их нет в моём окружении. Каркас 1:1
с официальным шаблоном; модули — на подтверждённом Meteor API. Возможные точки
правки (помечены в коде), если релизная CI-сборка ругнётся:

- **`PlayerMoveC2SPacket.PositionAndOnGround(...)`** (TargetTP) — список аргументов
  конструктора менялся между версиями MC.
- **`PlayerMoveEvent.movement`** (TargetStrafe) — имя события/поля движения в
  Meteor 26.1.2 могло измениться.
- **`Rotations.rotate` / `ReceiveMessageEvent.getMessage()`** — если сигнатуры
  другие, поправить импорт/вызов.

Первый релизный билд, скорее всего, потребует пары итераций по логам CI — как
и основной мод.
