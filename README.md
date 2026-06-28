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
- **Aura** — defaults tuned for snappier hits (CPS 11–16, attack tick
  limit 7) with **FunTime** and **Vega** rotation modes for smooth,
  humanised aim.
- **BackTrack** — records previous entity positions so Aura can hit
  where the target *was*, extending effective reach.
- **AntiKnockback** — cancel, reduce or vanilla-reset incoming knockback
  velocity, explosions and fishing hooks.
- **TargetStrafe** — added a **Fast** toggle with `FastMultiplier`
  (1.1–4×) on the strafe speed for blink-style orbiting.
- **TargetTP** — burst-teleports the player around `Aura.target` at up
  to 60 tp/s with configurable spoof packets, leaving Aura to own
  targeting/attacks. **Vanilla / weak-anticheat servers only**.

### Movement
- **JesusSpeed** — walk on water with Solid, NCP or Vanilla modes.
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
- [ ] Port more movement and utility modules
  (MiniMap, CaveFinder, DeathBox, AntiCrystal, …)
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
-Special thanks to firrty from EnvyWorld for creating and sharing the best ThunderHack configuration. His config has been tested extensively on EnvyWorld and provides an excellent gameplay experience out of the box. 
