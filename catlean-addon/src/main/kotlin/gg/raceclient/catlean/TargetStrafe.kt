package gg.raceclient.catlean

import net.minecraft.client.player.LocalPlayer
import su.catlean.api.addon.feature.AddonModule
import su.catlean.api.event.events.player.PlayerUpdateEvent
import su.catlean.gofra.Flow

/**
 * Circle-strafes around the nearest player, holding a fixed radius. Moves
 * tangentially (perpendicular to the target direction) and nudges in/out to
 * keep the desired distance.
 */
object TargetStrafe : AddonModule("target-strafe", "movement", listOf("ts")) {
    private var radius by setting("radius", 3.5f, 1f..8f)
    private var impulse by setting("impulse", 1f, 0.1f..1f)
    private var clockwise by setting("clockwise", true)
    private var maxDistance by setting("max-distance", 8f, 3f..30f)

    @Flow
    private fun onPlayerUpdate(event: PlayerUpdateEvent) {
        val lp = player as? LocalPlayer ?: return
        val target = lp.nearestEnemy(maxDistance) ?: return

        // vector from the target to us (radius direction)
        var dx = lp.x - target.x
        var dz = lp.z - target.z
        val dist = Math.hypot(dx, dz)
        if (dist < 1.0e-4) return
        dx /= dist; dz /= dist

        // tangential direction = circle around the target
        val dir = if (clockwise) 1 else -1
        var tx = -dz * dir
        var tz = dx * dir

        // correct toward the desired radius
        val radiusError = dist - radius
        tx += dx * (-radiusError) * 0.5
        tz += dz * (-radiusError) * 0.5

        lp.applyWorldMove(tx, tz, impulse)
    }
}
