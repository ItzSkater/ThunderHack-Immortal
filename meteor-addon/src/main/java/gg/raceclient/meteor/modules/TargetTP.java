package gg.raceclient.meteor.modules;

import gg.raceclient.meteor.RaceAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class TargetTP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-distance").description("Only target players within this range.").defaultValue(30).min(5).sliderRange(5, 64).build());

    private final Setting<Double> keepDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("keep-distance").description("Stop this far from the target.").defaultValue(3).min(1).sliderRange(1, 6).build());

    private final Setting<Integer> steps = sgGeneral.add(new IntSetting.Builder()
        .name("steps").description("Position packets per tick.").defaultValue(8).min(1).sliderRange(1, 40).build());

    private final Setting<Double> stepLength = sgGeneral.add(new DoubleSetting.Builder()
        .name("step-length").description("Blocks per step.").defaultValue(0.8).min(0.2).sliderRange(0.2, 2).build());

    public TargetTP() {
        super(RaceAddon.CATEGORY, "target-tp", "Rushes the nearest player with a burst of position packets. Anti-cheat dependent.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        PlayerEntity target = nearestPlayer();
        if (target == null) return;

        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        if (flat <= keepDistance.get()) return;

        double goal = flat - keepDistance.get();
        dx /= flat; dz /= flat;

        double moved = 0;
        int i = 0;
        while (i < steps.get() && moved < goal) {
            double step = Math.min(stepLength.get(), goal - moved);
            double nx = mc.player.getX() + dx * step;
            double nz = mc.player.getZ() + dz * step;
            double ny = mc.player.getY();
            mc.player.setPosition(nx, ny, nz);
            // NOTE: PlayerMoveC2SPacket.PositionAndOnGround(...) arg list can differ
            // between MC versions (a horizontalCollision flag was added later). If this
            // fails to compile against 26.1.2, adjust the constructor call.
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(nx, ny, nz, mc.player.isOnGround()));
            moved += step;
            i++;
        }
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
