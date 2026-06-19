# ThunderHack-Immortal

A Fabric 1.21 fork of [ThunderHack-Recode](https://github.com/Pan4ur/ThunderHack-Recode)
focused on bringing the legendary ThunderHack back to life with modern
features ported from other clients.

> [!WARNING]
> ThunderHack-Immortal is provided for **educational purposes only**. Using
> any client mod that gives you an advantage over other players may get you
> banned. You use it at your own risk.

> [!NOTE]
> Configs from the original ThunderHack-Recode load as-is — the mod id
> (`thunderhack`) and the config folder (`ThunderHackRecode`) are unchanged.

## What's new in Immortal

### Combat
- **Neural KillAura** — port of LiquidBounce NextGen's MLP-based aim
  smoothing. Built on DJL + PyTorch and ships the original LB `.params`
  weights so the network behaves identically.
- **Aura** — defaults tuned for snappier hits (CPS 11–16, attack tick
  limit 7) plus a new **Vega** rotation mode: a single-tick flick that
  consumes ~85% of the remaining yaw delta with jitter, then arcs pitch
  in via the existing acceleration term.
- **TargetStrafe** — added a **Fast** toggle with `FastMultiplier`
  (1.1–4×) on the strafe speed for blink-style orbiting.
- **TargetTP** — burst-teleports the player around `Aura.target` at up
  to 60 tp/s with configurable spoof packets, leaving Aura to own
  targeting/attacks. Built on `ClickTP`'s `PositionAndOnGround` pattern
  (no ender pearls). **Vanilla / weak-anticheat servers only** — Spartan,
  Matrix and Grim will rubber-band you.

### Movement (Vegaline ports)
- **VClip** — vertical clip on enable (configurable power, auto-disables).
- **AirJump** — `Vanilla` and `GroundSpoof` modes for mid-air jumps.
- **AirStuck** — zero velocity and cancel outbound position packets so
  you freeze mid-air.
- **HighJump** — boost jump strength on `EventPlayerJump`.
- **PhantomDash** — sneak-burst dash with configurable factor / dash
  ticks / push ticks.

## Roadmap

- [ ] Port to Minecraft 1.21.11
- [ ] Integrate Baritone pathfinding (Meteor/standalone)
- [ ] Port the rest of the interesting Vegaline modules
  (BackTrack, MiniMap, CaveFinder, DeathBox, AntiCrystal, …)
- [ ] Community-requested features from the original ThunderHack issues
  (Smart Distance KillAura, Grim velocity bypass, transparent ArrayList,
  Auto-Totem improvements, …)

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for
   Minecraft 1.21.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api).
3. Grab the latest jar from
   [Releases](https://github.com/ItzSkater/ThunderHack-Immortal/releases)
   and drop it in `.minecraft/mods/`.

## Building from source

```bash
git clone https://github.com/ItzSkater/ThunderHack-Immortal.git
cd ThunderHack-Immortal
./gradlew build
```

The jar lands in `build/libs/thunderhack-immortal-<version>.jar`.

## Credits

- [Pan4ur/ThunderHack-Recode](https://github.com/Pan4ur/ThunderHack-Recode)
  — the base client this is forked from.
- [CCBlueX/LiquidBounce](https://github.com/CCBlueX/LiquidBounce) NextGen —
  Neural KillAura architecture and shipped `.params` weights.
- Vegaline — source of the movement port ideas (VClip, AirJump, AirStuck,
  HighJump, PhantomDash) and the Vega rotation pattern.
