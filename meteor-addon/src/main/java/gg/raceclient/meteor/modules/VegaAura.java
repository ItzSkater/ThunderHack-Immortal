package gg.raceclient.meteor.modules;

import gg.raceclient.meteor.RaceAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Standalone kill-aura using a Vega-style rotation (smooth yaw step with slight
 * jitter). Meteor's built-in KillAura does not expose a rotation-plugin API, so
 * this ships the Vega rotation as its own aura module. (Mojmap.)
 */
public class VegaAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range").description("Attack range.").defaultValue(4).min(1).sliderRange(1, 6).build());

    private final Setting<Double> yawStep = sgGeneral.add(new DoubleSetting.Builder()
        .name("yaw-step").description("Max yaw movement per tick (Vega feel).").defaultValue(45).min(1).sliderRange(1, 180).build());

    private final Setting<Double> jitter = sgGeneral.add(new DoubleSetting.Builder()
        .name("jitter").description("Random yaw jitter added each tick.").defaultValue(2.5).min(0).sliderRange(0, 10).build());

    private final Setting<Boolean> onlyWhenReady = sgGeneral.add(new BoolSetting.Builder()
        .name("only-attack-when-cooldown-full").description("Attack only at full attack cooldown.").defaultValue(true).build());

    private final Random random = new Random();
    private float rotationYaw, rotationPitch;

    public VegaAura() {
        super(RaceAddon.CATEGORY, "vega-aura", "Kill-aura with a Vega-style rotation.");
    }

    @Override
    public void onActivate() {
        if (mc.player != null) {
            rotationYaw = mc.player.getYRot();
            rotationPitch = mc.player.getXRot();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        Player target = nearestTarget();
        if (target == null) return;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 to = target.getEyePosition();
        double dx = to.x - eye.x;
        double dy = to.y - eye.y;
        double dz = to.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        float yawDelta = wrap(targetYaw - rotationYaw);
        float step = (float) Math.min(Math.abs(yawDelta), yawStep.get() + random(-jitter.get(), jitter.get()));
        rotationYaw += yawDelta > 0 ? step : -step;
        rotationPitch += (targetPitch - rotationPitch) * 0.6f;
        rotationPitch = Math.max(-90f, Math.min(90f, rotationPitch));

        Rotations.rotate(rotationYaw, rotationPitch);

        if (mc.player.distanceTo(target) <= range.get()) {
            if (!onlyWhenReady.get() || mc.player.getAttackStrengthScale(0.5f) >= 1f) {
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    private Player nearestTarget() {
        Player best = null;
        double bestDist = range.get() + 2;
        for (Player p : mc.level.players()) {
            if (p == mc.player || !p.isAlive()) continue;
            double d = mc.player.distanceTo(p);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    private float wrap(float deg) {
        deg %= 360f;
        if (deg >= 180f) deg -= 360f;
        if (deg < -180f) deg += 360f;
        return deg;
    }

    private double random(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }
}
