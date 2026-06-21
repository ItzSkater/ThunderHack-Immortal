// Ported from AntiCope/meteor-crash-addon (MIT License)
package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class EntityCrash extends Module {
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Position);
    private final Setting<Integer> amount = new Setting<>("Amount", 2000, 100, 10000);
    private final Setting<Integer> speed = new Setting<>("Speed", 1337, 50, 10000);

    public EntityCrash() {
        super("EntityCrash", Category.CRASH);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) {
            sendMessage("You must be riding an entity");
            disable();
            return;
        }

        switch (mode.getValue()) {
            case Movement -> {
                for (int i = 0; i < amount.getValue(); i++) {
                    Vec3d v = vehicle.getPos();
                    vehicle.setPos(v.x, v.y + speed.getValue(), v.z);
                    mc.getNetworkHandler().sendPacket(new VehicleMoveC2SPacket(vehicle));
                }
            }
            case Position -> {
                BlockPos start = mc.player.getBlockPos();
                Vec3d end = new Vec3d(start.getX() + .5, start.getY() + 1, start.getZ() + .5);
                vehicle.updatePosition(end.x, end.y - 1, end.z);
                for (int i = 0; i < amount.getValue(); i++) {
                    mc.getNetworkHandler().sendPacket(new VehicleMoveC2SPacket(vehicle));
                }
            }
        }
    }

    public enum Mode {
        Position, Movement
    }
}
