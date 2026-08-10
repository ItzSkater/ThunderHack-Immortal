package gg.raceclient.meteor.modules;

import gg.raceclient.meteor.RaceAddon;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Fast circle-strafe around the nearest player, holding a fixed radius.
 *
 * NOTE: this drives movement by overwriting PlayerMoveEvent.movement (the
 * per-tick displacement). If the field/name differs in Meteor 26.1.2, adjust
 * the handler accordingly.
 */
public class TargetStrafe extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> radius = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius").description("Distance to keep from the target.").defaultValue(3.5).min(1).sliderRange(1, 8).build());

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed").description("Strafe speed (blocks/tick).").defaultValue(0.32).min(0.1).sliderRange(0.1, 0.6).build());

    private final Setting<Boolean> clockwise = sgGeneral.add(new BoolSetting.Builder()
        .name("clockwise").description("Strafe direction.").defaultValue(true).build());

    private final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-distance").description("Only strafe when a player is within this range.").defaultValue(8).min(2).sliderRange(2, 30).build());

    public TargetStrafe() {
        super(RaceAddon.CATEGORY, "target-strafe", "Fast circle-strafe around the nearest player.");
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (mc.player == null || mc.world == null) return;

        PlayerEntity target = nearestPlayer();
        if (target == null) return;

        double dx = mc.player.getX() - target.getX();
        double dz = mc.player.getZ() - target.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 1.0e-4) return;
        dx /= dist; dz /= dist;

        int dir = clockwise.get() ? 1 : -1;
        double tx = -dz * dir;
        double tz = dx * dir;

        // pull toward the desired radius
        double radiusError = dist - radius.get();
        tx += dx * (-radiusError) * 0.5;
        tz += dz * (-radiusError) * 0.5;

        double len = Math.sqrt(tx * tx + tz * tz);
        if (len < 1.0e-4) return;
        tx = tx / len * speed.get();
        tz = tz / len * speed.get();

        event.movement = new Vec3d(tx, event.movement.y, tz);
    }

    private PlayerEntity nearestPlayer() {
        PlayerEntity best = null;
        double bestDist = maxDistance.get();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive()) continue;
            double d = mc.player.distanceTo(p);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }
}
