package thunder.hack.features.deeplearn;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

/**
 * Java port of LiquidBounce NextGen's CombatSample.
 *
 * Bundles the feature inputs the trained MLP expects: yaw/pitch deltas
 * between the player's previous, current and desired rotation, plus
 * positional context. {@link #asInput()} produces the {@code float[6]}
 * layer-0 vector that matches the .params weights shipped in resources.
 */
public final class CombatSample {

    public final Vec3d currentVector;
    public final Vec3d previousVector;
    public final Vec3d targetVector;
    public final float velocityDeltaYaw;
    public final float velocityDeltaPitch;
    public final Vec3d playerDiff;
    public final Vec3d targetDiff;
    public final float distance;
    public final int hurtTime;
    public final int age;

    public CombatSample(
            @NotNull Vec3d currentVector,
            @NotNull Vec3d previousVector,
            @NotNull Vec3d targetVector,
            float velocityDeltaYaw,
            float velocityDeltaPitch,
            @NotNull Vec3d playerDiff,
            @NotNull Vec3d targetDiff,
            float distance,
            int hurtTime,
            int age
    ) {
        this.currentVector = currentVector;
        this.previousVector = previousVector;
        this.targetVector = targetVector;
        this.velocityDeltaYaw = velocityDeltaYaw;
        this.velocityDeltaPitch = velocityDeltaPitch;
        this.playerDiff = playerDiff;
        this.targetDiff = targetDiff;
        this.distance = distance;
        this.hurtTime = hurtTime;
        this.age = age;
    }

    public float[] asInput() {
        // Matches LiquidBounce: total delta (yaw, pitch), velocity delta
        // (yaw, pitch), combined horizontal speed, distance.
        Yp current = Yp.fromDirection(currentVector);
        Yp target = Yp.fromDirection(targetVector);
        float deltaYaw = MathHelper.wrapDegrees(target.yaw - current.yaw);
        float deltaPitch = target.pitch - current.pitch;

        float speed = (float) (horizontal(targetDiff) + horizontal(playerDiff));

        return new float[]{
                deltaYaw,
                deltaPitch,
                velocityDeltaYaw,
                velocityDeltaPitch,
                speed,
                distance
        };
    }

    private static double horizontal(Vec3d v) {
        return Math.sqrt(v.x * v.x + v.z * v.z);
    }

    /** Yaw/pitch pair derived from a normalised direction vector. */
    private record Yp(float yaw, float pitch) {
        static Yp fromDirection(Vec3d d) {
            double yaw = -Math.toDegrees(Math.atan2(d.x, d.z));
            double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
            double pitch = -Math.toDegrees(Math.atan2(d.y, horiz));
            return new Yp((float) yaw, (float) pitch);
        }
    }
}
