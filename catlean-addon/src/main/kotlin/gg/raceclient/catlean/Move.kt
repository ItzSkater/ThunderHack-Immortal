package gg.raceclient.catlean

import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.player.Player
import kotlin.math.*

/**
 * Shared vanilla-side helpers used by the movement modules. These lean only on
 * Mojang-mapped Minecraft APIs (position, yRot/xRot, xxa/zza impulses,
 * level().players()) so they don't depend on CatLean internals beyond the
 * `player` handle the addon modules already expose.
 *
 * NOTE: movement is applied by writing the strafe/forward impulses (xxa/zza).
 * That is the standard vanilla hook, but whether it survives depends on WHEN
 * PlayerUpdateEvent fires relative to the player's travel(). If a strafe has no
 * effect in-game, CatLean likely exposes a dedicated movement/strafe event —
 * switch the @Flow handler to that. See README.
 */

/** Turn a desired world XZ direction into forward/strafe impulses for this yaw. */
fun LocalPlayer.applyWorldMove(dx: Double, dz: Double, impulse: Float) {
    val len = sqrt(dx * dx + dz * dz)
    if (len < 1.0e-6) return
    val nx = dx / len
    val nz = dz / len

    val yaw = Math.toRadians(this.yRot.toDouble())
    val fx = -sin(yaw); val fz = cos(yaw)   // forward vector (XZ)
    val rx = -cos(yaw); val rz = -sin(yaw)  // right vector (forward rotated +90°)

    val forward = nx * fx + nz * fz
    val strafe = nx * rx + nz * rz

    this.zza = (forward * impulse).toFloat()
    this.xxa = (strafe * impulse).toFloat()
}

/** Unit horizontal look direction of an entity from its yaw. */
fun horizontalLook(yawDeg: Float): DoubleArray {
    val yaw = Math.toRadians(yawDeg.toDouble())
    return doubleArrayOf(-sin(yaw), cos(yaw))
}

/** The nearest living enemy player within range (friends handled by caller). */
fun LocalPlayer.nearestEnemy(maxDistance: Float): Player? {
    var best: Player? = null
    var bestDist = Float.MAX_VALUE
    for (p in this.level().players()) {
        if (p === this || !p.isAlive) continue
        val d = this.distanceTo(p)
        if (d > maxDistance || d >= bestDist) continue
        bestDist = d
        best = p
    }
    return best
}

/** The player most precisely aiming at us (smallest angle), within range and cone. */
fun LocalPlayer.findAimer(maxDistance: Float, aimAngleDeg: Float): Player? {
    val eye = this.eyePosition
    var best: Player? = null
    var bestAngle = Double.MAX_VALUE
    for (p in this.level().players()) {
        if (p === this || !p.isAlive) continue
        if (this.distanceTo(p) > maxDistance) continue

        val pe = p.eyePosition
        val tx = eye.x - pe.x
        val ty = eye.y - pe.y
        val tz = eye.z - pe.z
        val tl = sqrt(tx * tx + ty * ty + tz * tz)
        if (tl < 1.0e-4) continue

        val yaw = Math.toRadians(p.yRot.toDouble())
        val pitch = Math.toRadians(p.xRot.toDouble())
        val cp = cos(pitch)
        val vx = -sin(yaw) * cp
        val vy = -sin(pitch)
        val vz = cos(yaw) * cp

        val dot = ((vx * tx + vy * ty + vz * tz) / tl).coerceIn(-1.0, 1.0)
        val angle = Math.toDegrees(acos(dot))
        if (angle <= aimAngleDeg && angle < bestAngle) {
            bestAngle = angle
            best = p
        }
    }
    return best
}
