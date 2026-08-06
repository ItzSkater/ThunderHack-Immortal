package gg.raceclient.catlean

import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import su.catlean.api.addon.feature.AddonModule
import su.catlean.api.event.events.player.PlayerUpdateEvent
import su.catlean.gofra.Flow

/**
 * Rushes the nearest player by sending a burst of position packets each tick,
 * stepping toward them until [keepDistance] away.
 *
 * This is inherently anti-cheat dependent: a server that validates movement
 * speed will rubber-band or flag large per-tick jumps. Keep [steps] /
 * [stepLength] modest on strict servers.
 *
 * NOTE: the ServerboundMovePlayerPacket.Pos(...) constructor arg list changed
 * across MC versions (a horizontalCollision flag was added in later 1.21.x). If
 * this fails to compile against your CatLean/MC 26.2 mappings, adjust the
 * constructor call below to match.
 */
object TargetTP : AddonModule("target-tp", "movement", listOf("ttp")) {
    private var maxDistance by setting("max-distance", 30f, 5f..64f)
    private var keepDistance by setting("keep-distance", 3f, 1f..6f)
    private var steps by setting("steps", 8, 1..40)
    private var stepLength by setting("step-length", 0.8f, 0.2f..2f)

    @Flow
    private fun onPlayerUpdate(event: PlayerUpdateEvent) {
        val lp = player as? LocalPlayer ?: return
        val target = lp.nearestEnemy(maxDistance) ?: return

        var dx = target.x - lp.x
        var dz = target.z - lp.z
        val flat = Math.hypot(dx, dz)
        if (flat <= keepDistance) return

        val goal = flat - keepDistance
        dx /= flat; dz /= flat

        var moved = 0.0
        var i = 0
        while (i < steps && moved < goal) {
            val step = minOf(stepLength.toDouble(), goal - moved)
            val nx = lp.x + dx * step
            val nz = lp.z + dz * step
            val ny = lp.y
            lp.setPos(nx, ny, nz)
            lp.connection.send(ServerboundMovePlayerPacket.Pos(nx, ny, nz, lp.onGround()))
            moved += step
            i++
        }
    }
}
