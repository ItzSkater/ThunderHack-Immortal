package gg.raceclient.meteor.modules;

import gg.raceclient.meteor.RaceAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.player.Player;

public class TargetTP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-distance").description("Only target players within this range.").defaultValue(30).min(5).sliderRange(5, 64).build());

    private final Setting<Double> keepDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("keep-distance").description("Stop this far from the target.").defaultValue(3).min(1).sliderRange(1, 6).build());

    private final Setting<Double> stepPerTick = sgGeneral.add(new DoubleSetting.Builder()
        .name("step-per-tick").description("Max blocks to move toward the target each tick.").defaultValue(6).min(0.5).sliderRange(0.5, 20).build());

    public TargetTP() {
        super(RaceAddon.CATEGORY, "target-tp", "Rushes the nearest player. Anti-cheat dependent.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        Player target = nearestPlayer();
        if (target == null) return;

        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        if (flat <= keepDistance.get()) return;

        double move = Math.min(stepPerTick.get(), flat - keepDistance.get());
        double nx = mc.player.getX() + dx / flat * move;
        double nz = mc.player.getZ() + dz / flat * move;
        mc.player.setPos(nx, mc.player.getY(), nz);
    }

    private Player nearestPlayer() {
        Player best = null;
        double bestDist = maxDistance.get();
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
}
