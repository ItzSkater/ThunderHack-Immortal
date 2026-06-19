package thunder.hack.features.modules.movement;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

/**
 * Vertical clip — instantly nudges the player up/down by sending a single
 * PositionAndOnGround packet, then disables itself. Ported from Vegaline.
 */
public class VClip extends Module {
    private final Setting<Float> power = new Setting<>("Power", 60.0f, -70.0f, 200.0f);

    public VClip() {
        super("VClip", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) return;

        double x = mc.player.getX();
        double y = mc.player.getY() + power.getValue();
        double z = mc.player.getZ();

        sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, mc.player.getY(), z, true));
        sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
        mc.player.setPosition(x, y, z);

        sendMessage("Clipped " + (power.getValue() >= 0 ? "up " : "down ") + power.getValue());
        toggle();
    }
}
