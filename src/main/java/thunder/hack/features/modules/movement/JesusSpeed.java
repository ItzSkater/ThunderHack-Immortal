package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.FluidBlock;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

public class JesusSpeed extends Module {
    public JesusSpeed() {
        super("JesusSpeed", Category.MOVEMENT);
    }

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.Solid);
    public final Setting<Float> speed = new Setting<>("Speed", 0.5f, 0.1f, 2.0f);
    public final Setting<Boolean> useInLava = new Setting<>("UseInLava", false);

    private boolean yPortUp;

    public enum Mode {
        Solid, NCP, Vanilla
    }

    @Override
    public void onEnable() {
        yPortUp = false;
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (mc.player == null || mc.world == null) return;

        boolean inFluid = mc.player.isTouchingWater() || (useInLava.getValue() && mc.player.isInLava());
        if (!inFluid) return;
        if (mc.player.isSneaking()) return;

        boolean onSurface = isOnFluidSurface();

        switch (mode.getValue()) {
            case Solid -> handleSolid(onSurface);
            case NCP -> handleNCP(onSurface);
            case Vanilla -> handleVanilla();
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (mc.player == null || mc.world == null) return;
        if (mode.getValue() != Mode.NCP) return;

        boolean inFluid = mc.player.isTouchingWater() || (useInLava.getValue() && mc.player.isInLava());
        if (!inFluid || !isOnFluidSurface()) return;

        if (e.getPacket() instanceof PlayerMoveC2SPacket packet) {
            if (packet instanceof PlayerMoveC2SPacket.PositionAndOnGround
                    || packet instanceof PlayerMoveC2SPacket.Full) {
                e.cancel();
                sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(), true
                ));
            }
        }
    }

    private void handleSolid(boolean onSurface) {
        if (onSurface) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
            mc.player.setOnGround(true);

            if (MovementUtility.isMoving()) {
                MovementUtility.setMotion(speed.getValue());
            }
        } else {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.1, mc.player.getVelocity().z);
        }
    }

    private void handleNCP(boolean onSurface) {
        if (onSurface) {
            double yOffset = yPortUp ? 0.02 : -0.02;
            yPortUp = !yPortUp;

            mc.player.setVelocity(mc.player.getVelocity().x, yOffset, mc.player.getVelocity().z);
            mc.player.setOnGround(true);

            if (MovementUtility.isMoving()) {
                MovementUtility.setMotion(speed.getValue());
            }
        } else {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.1, mc.player.getVelocity().z);
        }
    }

    private void handleVanilla() {
        mc.player.setVelocity(mc.player.getVelocity().x, 0.05, mc.player.getVelocity().z);

        if (MovementUtility.isMoving()) {
            MovementUtility.setMotion(speed.getValue());
        }
    }

    private boolean isOnFluidSurface() {
        boolean feetInFluid = mc.world.getBlockState(mc.player.getBlockPos()).getBlock() instanceof FluidBlock;
        boolean headAboveFluid = mc.world.getFluidState(mc.player.getBlockPos().up()).isEmpty();
        return feetInFluid && headAboveFluid;
    }
}
