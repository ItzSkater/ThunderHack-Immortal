package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import thunder.hack.events.impl.EventTick;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records previous bounding-box positions of nearby entities so that
 * KillAura (or any other combat module) can hit "where the entity was",
 * effectively extending the usable hit range on laggy connections.
 *
 * <p>Other modules query {@link #getTrackBoxes(Entity)} to obtain
 * the list of historical bounding boxes for a given entity.</p>
 */
public final class BackTrack extends Module {

    public final Setting<Float> targetRange = new Setting<>("TargetRange", 6.0f, 3.0f, 12.0f);
    public final Setting<Integer> trackTicks = new Setting<>("TrackTicks", 5, 1, 10);
    public final Setting<Boolean> renderTracks = new Setting<>("RenderTracks", true);
    public final Setting<Boolean> onlyPlayers = new Setting<>("OnlyPlayers", true);

    private final Map<Integer, Deque<Box>> trackHistory = new ConcurrentHashMap<>();

    public BackTrack() {
        super("BackTrack", Category.COMBAT);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        int maxTicks = trackTicks.getValue();
        double rangeSq = targetRange.getValue() * targetRange.getValue();

        Set<Integer> seenIds = new HashSet<>();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (onlyPlayers.getValue() && !(entity instanceof PlayerEntity)) continue;
            if (mc.player.squaredDistanceTo(entity) > rangeSq) continue;

            seenIds.add(entity.getId());

            Deque<Box> history = trackHistory.computeIfAbsent(entity.getId(), k -> new ArrayDeque<>());
            history.addLast(entity.getBoundingBox());

            while (history.size() > maxTicks) {
                history.removeFirst();
            }
        }

        trackHistory.keySet().removeIf(id -> !seenIds.contains(id));
    }

    /**
     * Returns the list of tracked historical bounding boxes for the given entity,
     * ordered oldest-first. Returns an empty list if the entity is not being tracked.
     */
    public List<Box> getTrackBoxes(Entity entity) {
        Deque<Box> history = trackHistory.get(entity.getId());
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(history);
    }

    @Override
    public void onDisable() {
        trackHistory.clear();
    }

    @Override
    public String getDisplayInfo() {
        return String.valueOf(trackHistory.size());
    }
}
