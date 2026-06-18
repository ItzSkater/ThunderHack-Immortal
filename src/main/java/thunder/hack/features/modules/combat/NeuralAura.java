package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.deeplearn.CombatSample;
import thunder.hack.features.deeplearn.DeepLearningEngine;
import thunder.hack.features.deeplearn.ModelRegistry;
import thunder.hack.features.deeplearn.NeuralCombatModel;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Neural KillAura ported from LiquidBounce NextGen.
 *
 * Picks the closest living target within {@code range} and steers the
 * aim using the LB-trained MLP model — the network outputs an
 * incremental (deltaYaw, deltaPitch) per tick which we apply to the
 * player's current rotation. When the rotation is on-target and the
 * attack timer is ready, we send the standard swing + interact packets.
 *
 * Separate from {@link Aura} on purpose: that module is huge and very
 * opinionated; this one is a clean drop-in that only borrows the LB AI
 * smoothing. They can be combined later via the rotation system.
 */
public final class NeuralAura extends Module {

    public final Setting<Float> range = new Setting<>("Range", 4.0f, 1f, 6.0f);
    public final Setting<Float> fov = new Setting<>("FOV", 180f, 1f, 180f);
    public final Setting<Float> outputYawMultiplier = new Setting<>("YawMultiplier", 1.0f, 0.1f, 3.0f);
    public final Setting<Float> outputPitchMultiplier = new Setting<>("PitchMultiplier", 1.0f, 0.1f, 3.0f);
    public final Setting<Float> hitAngle = new Setting<>("HitAngle", 4.0f, 0.1f, 30f);
    public final Setting<Integer> cps = new Setting<>("CPS", 12, 1, 20);
    public final Setting<Boolean> attackPlayers = new Setting<>("Players", true);
    public final Setting<Boolean> attackMobs = new Setting<>("Mobs", false);
    public final Setting<Boolean> silent = new Setting<>("SilentRotate", true);
    public final Setting<Boolean> wallCheck = new Setting<>("WallCheck", false);
    public final Setting<ModelChoice> model = new Setting<>("Model", ModelChoice.M21KC11KP);

    private final Timer attackTimer = new Timer();
    private @Nullable LivingEntity target;
    private float lastVelocityYaw, lastVelocityPitch;

    public NeuralAura() {
        super("NeuralAura", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        DeepLearningEngine.initAsync();
        target = null;
        lastVelocityYaw = 0f;
        lastVelocityPitch = 0f;
    }

    @Override
    public void onDisable() {
        target = null;
    }

    @Override
    public String getDisplayInfo() {
        return target != null ? target.getName().getString() : null;
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onSync(EventSync event) {
        if (mc.player == null || mc.world == null) return;

        target = findTarget();
        if (target == null) return;

        if (!DeepLearningEngine.isInitialized()) {
            // Engine is still loading — fall back to a hard snap so the
            // module remains useful while the MLP warms up.
            steerSnap(target);
            return;
        }

        NeuralCombatModel selected = ModelRegistry.get(model.getValue().resourceName);
        if (selected == null || !selected.isLoaded()) {
            steerSnap(target);
            return;
        }

        steerNeural(selected, target);
    }

    private void steerSnap(LivingEntity victim) {
        float[] yp = aimAt(victim);
        applyRotation(yp[0], yp[1]);
        tryAttack(victim, yp[0], yp[1]);
    }

    private void steerNeural(NeuralCombatModel model, LivingEntity victim) {
        float curYaw = mc.player.getYaw();
        float curPitch = mc.player.getPitch();

        float[] targetYp = aimAt(victim);
        float targetYaw = targetYp[0];
        float targetPitch = targetYp[1];

        Vec3d currentVec = dirVector(curYaw, curPitch);
        Vec3d previousVec = dirVector(curYaw - lastVelocityYaw, curPitch - lastVelocityPitch);
        Vec3d targetVec = dirVector(targetYaw, targetPitch);

        Vec3d playerDiff = mc.player.getPos().subtract(mc.player.prevX, mc.player.prevY, mc.player.prevZ);
        Vec3d targetDiff = victim.getPos().subtract(victim.prevX, victim.prevY, victim.prevZ);
        float distance = (float) mc.player.squaredDistanceTo(victim);

        CombatSample sample = new CombatSample(
                currentVec, previousVec, targetVec,
                lastVelocityYaw, lastVelocityPitch,
                playerDiff, targetDiff,
                distance,
                victim.hurtTime,
                victim.age
        );

        float[] out;
        try {
            out = model.predict(sample.asInput());
        } catch (Throwable t) {
            ThunderHack.LOGGER.warn("[NeuralAura] predict failed, falling back to snap", t);
            steerSnap(victim);
            return;
        }

        float deltaYaw = out[0] * outputYawMultiplier.getValue();
        float deltaPitch = out[1] * outputPitchMultiplier.getValue();

        float newYaw = curYaw + deltaYaw;
        float newPitch = MathHelper.clamp(curPitch + deltaPitch, -89f, 89f);

        lastVelocityYaw = newYaw - curYaw;
        lastVelocityPitch = newPitch - curPitch;

        applyRotation(newYaw, newPitch);
        tryAttack(victim, newYaw, newPitch);
    }

    private void applyRotation(float yaw, float pitch) {
        if (silent.getValue()) {
            // Push the desired aim through a Look-only move packet so
            // the server registers the rotation without spinning the
            // camera client-side.
            sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround()));
        } else {
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }
    }

    private void tryAttack(LivingEntity victim, float yaw, float pitch) {
        if (!attackTimer.passedMs(1000L / Math.max(1, cps.getValue()))) return;

        float[] desired = aimAt(victim);
        float yawErr = Math.abs(MathHelper.wrapDegrees(yaw - desired[0]));
        float pitchErr = Math.abs(pitch - desired[1]);
        if (yawErr > hitAngle.getValue() || pitchErr > hitAngle.getValue()) return;

        double rangeSq = range.getValue() * range.getValue();
        if (mc.player.squaredDistanceTo(victim) > rangeSq) return;

        sendPacket(PlayerInteractEntityC2SPacket.attack(victim, mc.player.isSneaking()));
        sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        attackTimer.reset();
    }

    private @Nullable LivingEntity findTarget() {
        List<LivingEntity> candidates = new ArrayList<>();
        double rangeSq = range.getValue() * range.getValue();

        for (var entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive()) continue;
            if (mc.player.squaredDistanceTo(living) > rangeSq) continue;
            if (living instanceof PlayerEntity p) {
                if (!attackPlayers.getValue()) continue;
                if (Managers.FRIEND.isFriend(p.getGameProfile().getName())) continue;
            } else {
                if (!attackMobs.getValue()) continue;
            }
            if (wallCheck.getValue() && !mc.player.canSee(living)) continue;
            if (!inFov(living)) continue;
            candidates.add(living);
        }

        return candidates.stream()
                .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)))
                .orElse(null);
    }

    private boolean inFov(LivingEntity victim) {
        if (fov.getValue() >= 180f) return true;
        float[] yp = aimAt(victim);
        float diff = Math.abs(MathHelper.wrapDegrees(yp[0] - mc.player.getYaw()));
        return diff <= fov.getValue();
    }

    private float[] aimAt(LivingEntity victim) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d hit = victim.getBoundingBox().getCenter();
        double dx = hit.x - eye.x;
        double dy = hit.y - eye.y;
        double dz = hit.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new float[]{yaw, pitch};
    }

    private static Vec3d dirVector(float yaw, float pitch) {
        float yawRad = yaw * MathHelper.RADIANS_PER_DEGREE;
        float pitchRad = pitch * MathHelper.RADIANS_PER_DEGREE;
        float cosYaw = MathHelper.cos(-yawRad - (float) Math.PI);
        float sinYaw = MathHelper.sin(-yawRad - (float) Math.PI);
        float cosPitch = -MathHelper.cos(-pitchRad);
        float sinPitch = MathHelper.sin(-pitchRad);
        return new Vec3d(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }

    public enum ModelChoice {
        M21KC11KP("21KC11KP"),
        M19KC8KP("19KC8KP");

        public final String resourceName;
        ModelChoice(String resourceName) {
            this.resourceName = resourceName;
        }
    }
}
