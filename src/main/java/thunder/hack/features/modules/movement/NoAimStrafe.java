package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import thunder.hack.events.impl.EventMove;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

import static thunder.hack.core.Managers.FRIEND;

/**
 * Stands still (jumping still allowed) while no enemy is aiming at you. As soon
 * as an opponent's crosshair lines up with you, strafes sideways to slip off
 * their line of fire.
 */
public class NoAimStrafe extends Module {
    private final Setting<Float> maxDistance = new Setting<>("MaxDistance", 8f, 3f, 30f);
    private final Setting<Float> aimAngle = new Setting<>("AimAngle", 12f, 1f, 45f);
    private final Setting<Float> speed = new Setting<>("StrafeSpeed", 0.28f, 0.1f, 0.4f);
    private final Setting<Boolean> freezeWhenSafe = new Setting<>("FreezeWhenSafe", true);
    private final Setting<Boolean> ignoreFriends = new Setting<>("IgnoreFriends", true);

    // last strafe side (+1 / -1) kept with hysteresis so we don't jitter
    private int side = 1;

    public NoAimStrafe() {
        super("NoAimStrafe", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        side = 1;
    }

    @EventHandler
    public void onMove(EventMove event) {
        if (fullNullCheck()) return;
        if (mc.player.getAbilities().flying || mc.player.isSneaking()) return;

        PlayerEntity aimer = findAimer();

        if (aimer != null) {
            // horizontal vector from the aimer to us = the line of fire
            double dx = mc.player.getX() - aimer.getX();
            double dz = mc.player.getZ() - aimer.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1.0E-4) return;
            dx /= len;
            dz /= len;

            // where is the aimer's crosshair drifting relative to dead-on? pick
            // the side that moves us away from that drift (hysteresis avoids jitter)
            Vec3d view = aimer.getRotationVec(1.0f);
            double cross = dx * view.z - dz * view.x; // 2D cross (toMe × view)
            if (cross > 0.02) side = -1;
            else if (cross < -0.02) side = 1;

            // perpendicular to the line of fire, chosen side
            double px = -dz * side;
            double pz = dx * side;

            event.setX(px * speed.getValue());
            event.setZ(pz * speed.getValue());
            event.cancel();
        } else if (freezeWhenSafe.getValue()) {
            // nobody aiming: hold position, leave Y (jumping / gravity) intact
            event.setX(0);
            event.setZ(0);
            event.cancel();
        }
    }

    private PlayerEntity findAimer() {
        if (mc.world == null) return null;
        Vec3d myEye = mc.player.getEyePos();
        double maxDistSq = maxDistance.getValue() * maxDistance.getValue();

        PlayerEntity best = null;
        double bestAngle = Double.MAX_VALUE;

        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive()) continue;
            if (ignoreFriends.getValue() && FRIEND.isFriend(p)) continue;
            if (mc.player.squaredDistanceTo(p) > maxDistSq) continue;

            Vec3d toMe = myEye.subtract(p.getEyePos());
            if (toMe.lengthSquared() < 1.0E-6) continue;
            toMe = toMe.normalize();

            Vec3d view = p.getRotationVec(1.0f);
            double dot = Math.max(-1.0, Math.min(1.0, view.dotProduct(toMe)));
            double angle = Math.toDegrees(Math.acos(dot));

            if (angle <= aimAngle.getValue() && angle < bestAngle) {
                bestAngle = angle;
                best = p;
            }
        }
        return best;
    }
}
