package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import thunder.hack.events.impl.EventTick;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

/**
 * Freezes the player midair by zeroing velocity and cancelling outbound
 * position/rotation packets while the module is enabled. Ported from
 * Vegaline.
 */
public class AirStuck extends Module {
    private final Setting<Boolean> stopRotation = new Setting<>("StopRotation", true);
    private final Setting<Boolean> cancelTeleport = new Setting<>("CancelTeleport", false);

    private float lockedYaw;
    private float lockedPitch;

    public AirStuck() {
        super("AirStuck", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) return;
        lockedYaw = mc.player.getYaw();
        lockedPitch = mc.player.getPitch();
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (mc.player == null) return;
        mc.player.setVelocity(0, 0, 0);
        mc.player.fallDistance = 0f;
        if (stopRotation.getValue()) {
            mc.player.setYaw(lockedYaw);
            mc.player.setPitch(lockedPitch);
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (mc.player == null) return;
        if (e.getPacket() instanceof PlayerMoveC2SPacket)
            e.cancel();
        else if (cancelTeleport.getValue() && e.getPacket() instanceof TeleportConfirmC2SPacket)
            e.cancel();
    }
}
