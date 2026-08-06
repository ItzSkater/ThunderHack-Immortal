package gg.raceclient.catlean

import net.minecraft.client.player.LocalPlayer
import su.catlean.api.addon.feature.AddonModule
import su.catlean.api.event.events.player.PlayerUpdateEvent
import su.catlean.gofra.Flow

/**
 * Stands still (jumping still allowed) while nobody is aiming at you. As soon as
 * an opponent's crosshair lines up with you within [aimAngle], strafes
 * perpendicular to their line of fire to slip off it.
 */
object NoAimStrafe : AddonModule("no-aim-strafe", "movement", listOf("nas")) {
    private var maxDistance by setting("max-distance", 8f, 3f..30f)
    private var aimAngle by setting("aim-angle", 12f, 1f..45f)
    private var impulse by setting("impulse", 1f, 0.1f..1f)
    private var freezeWhenSafe by setting("freeze-when-safe", true)

    private var side = 1

    override fun onEnable() {
        side = 1
    }

    @Flow
    private fun onPlayerUpdate(event: PlayerUpdateEvent) {
        val lp = player as? LocalPlayer ?: return

        val aimer = lp.findAimer(maxDistance, aimAngle)
        if (aimer != null) {
            // horizontal line of fire: from the aimer to us
            var dx = lp.x - aimer.x
            var dz = lp.z - aimer.z
            val len = Math.hypot(dx, dz)
            if (len < 1.0e-4) return
            dx /= len; dz /= len

            // pick the side that moves us away from where the aimer is drifting
            val look = horizontalLook(aimer.yRot)
            val cross = dx * look[1] - dz * look[0]
            if (cross > 0.02) side = -1 else if (cross < -0.02) side = 1

            // perpendicular to the line of fire
            lp.applyWorldMove(-dz * side, dx * side, impulse)
        } else if (freezeWhenSafe) {
            // nobody aiming: hold ground; jumping / gravity (Y) untouched
            lp.xxa = 0f
            lp.zza = 0f
        }
    }
}
