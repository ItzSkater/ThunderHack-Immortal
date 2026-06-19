package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import thunder.hack.events.impl.EventTick;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

/**
 * Lets the player jump mid-air. Ported from Vegaline.
 *
 * Two modes:
 *  - Vanilla: simply marks the player as on-ground each tick, allowing
 *    repeated jumps with no exploit pretence.
 *  - GroundSpoof: only spoofs on-ground after sending a normal y-offset
 *    move pair, mimicking Vegaline's "Matrix2" mode.
 */
public class AirJump extends Module {
    public final Setting<Mode> mode = new Setting<>("Mode", Mode.GroundSpoof);

    public AirJump() {
        super("AirJump", Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        switch (mode.getValue()) {
            case Vanilla -> mc.player.setOnGround(true);
            case GroundSpoof -> {
                if (!MovementUtility.isMoving()) return;
                if (mc.player.isOnGround() || mc.player.getVelocity().y >= 0) return;

                Box check = mc.player.getBoundingBox().offset(0, -1.0, 0).expand(0.1, 0, 0.1);
                if (!mc.world.isSpaceEmpty(check)) {
                    mc.player.setOnGround(true);
                    mc.player.fallDistance = 0f;
                    sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
                }
            }
        }
    }

    @Override
    public String getDisplayInfo() {
        return mode.getValue().name();
    }

    public enum Mode { Vanilla, GroundSpoof }
}
