package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;

import java.util.Random;

/**
 * Burst-teleports the player to randomised points around {@link Aura#target}
 * so quickly the opponent can never aim back. Built on the packet pattern
 * from {@link thunder.hack.features.modules.movement.ClickTP} — pure
 * position spoof, no ender pearls. Pairs cleanly with Aura since it leaves
 * targeting/attack to Aura and only handles the teleport.
 *
 * Only for servers without a real anti-cheat: vanilla position packets are
 * trusted, but Spartan/Matrix/Grim will rubber-band you.
 */
public final class TargetTP extends Module {

    public final Setting<Float> radius = new Setting<>("Radius", 2.5f, 1.5f, 5.0f);
    public final Setting<Float> heightOffset = new Setting<>("HeightOffset", 0f, -1.5f, 1.5f);
    public final Setting<Integer> tpsPerSecond = new Setting<>("TPsPerSecond", 25, 5, 60);
    public final Setting<Integer> spoofPackets = new Setting<>("SpoofPackets", 3, 0, 20);
    public final Setting<Mode> mode = new Setting<>("Mode", Mode.Circle);
    public final Setting<Boolean> onlyWhenAttacking = new Setting<>("OnlyWhenAttacking", true);
    public final Setting<Boolean> requireAura = new Setting<>("RequireAura", true);
    public final Setting<Boolean> safeOnly = new Setting<>("SafeBlocksOnly", true);

    private final Timer tpTimer = new Timer();
    private final Random rng = new Random();
    private double angle;

    public TargetTP() {
        super("TargetTP", Category.COMBAT);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onSync(EventSync event) {
        if (mc.player == null || mc.world == null) return;

        Entity target = Aura.target;
        if (target == null) return;
        if (requireAura.getValue() && !ModuleManager.aura.isEnabled()) return;
        if (onlyWhenAttacking.getValue() && !ModuleManager.aura.isEnabled()) return;

        long intervalMs = Math.max(1L, 1000L / Math.max(1, tpsPerSecond.getValue()));
        if (!tpTimer.passedMs(intervalMs)) return;

        Vec3d destination = pickDestination(target);
        if (destination == null) return;

        teleport(destination);
        tpTimer.reset();
    }

    private Vec3d pickDestination(Entity target) {
        double r = radius.getValue();
        double centerX = target.getX();
        double centerY = target.getY() + heightOffset.getValue();
        double centerZ = target.getZ();

        if (mode.getValue() == Mode.Circle) {
            angle += Math.toRadians(rng.nextDouble() * 30.0 + 60.0);
            if (angle > Math.PI * 2.0) angle -= Math.PI * 2.0;
            double x = centerX + Math.cos(angle) * r;
            double z = centerZ + Math.sin(angle) * r;
            return validate(new Vec3d(x, centerY, z));
        } else {
            for (int tries = 0; tries < 6; ++tries) {
                double a = rng.nextDouble() * Math.PI * 2.0;
                double x = centerX + Math.cos(a) * r;
                double z = centerZ + Math.sin(a) * r;
                Vec3d candidate = validate(new Vec3d(x, centerY, z));
                if (candidate != null) return candidate;
            }
            return null;
        }
    }

    private Vec3d validate(Vec3d pos) {
        if (!safeOnly.getValue()) return pos;
        Box box = mc.player.getBoundingBox().offset(pos.subtract(mc.player.getPos()));
        if (!mc.world.isSpaceEmpty(box)) return null;
        return pos;
    }

    private void teleport(Vec3d to) {
        double curY = mc.player.getY();
        for (int i = 0; i < spoofPackets.getValue(); ++i) {
            sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX(), curY, mc.player.getZ(), false));
        }
        sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(to.x, to.y, to.z, false));
        mc.player.setPosition(to.x, to.y, to.z);
    }

    @Override
    public void onDisable() {
        angle = 0;
    }

    public enum Mode {
        Circle, Random
    }
}
