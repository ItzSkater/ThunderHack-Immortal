package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import thunder.hack.events.impl.EventTick;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.injection.accesors.IExplosionS2CPacket;
import thunder.hack.injection.accesors.ISPacketEntityVelocity;
import thunder.hack.setting.Setting;

/**
 * Reduces or completely cancels incoming knockback from entity velocity
 * updates and explosions. Three modes control how the velocity is handled:
 *
 *  - Cancel:  drops the velocity packet entirely.
 *  - Reduce:  multiplies the velocity components by the configured factors.
 *  - Vanilla: lets the packet apply, then zeroes the player velocity next tick.
 */
public final class AntiKnockback extends Module {

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.Cancel);
    public final Setting<Float> horizontal = new Setting<>("Horizontal", 0.0f, 0.0f, 1.0f, v -> mode.getValue() == Mode.Reduce);
    public final Setting<Float> vertical = new Setting<>("Vertical", 0.0f, 0.0f, 1.0f, v -> mode.getValue() == Mode.Reduce);
    public final Setting<Boolean> cancelExplosion = new Setting<>("CancelExplosion", true);
    public final Setting<Boolean> cancelFishHook = new Setting<>("CancelFishHook", true);

    private boolean velocityApplied;

    public AntiKnockback() {
        super("AntiKnockback", Category.COMBAT);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (fullNullCheck()) return;

        // --- Entity velocity (knockback / fish hook) ---
        if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket pac) {
            if (pac.getId() != mc.player.getId()) return;

            switch (mode.getValue()) {
                case Cancel -> e.cancel();
                case Reduce -> {
                    ((ISPacketEntityVelocity) pac).setMotionX((int) (pac.getVelocityX() * horizontal.getValue()));
                    ((ISPacketEntityVelocity) pac).setMotionY((int) (pac.getVelocityY() * vertical.getValue()));
                    ((ISPacketEntityVelocity) pac).setMotionZ((int) (pac.getVelocityZ() * horizontal.getValue()));
                }
                case Vanilla -> velocityApplied = true;
            }
        }

        // --- Explosion knockback ---
        if (e.getPacket() instanceof ExplosionS2CPacket explosion && cancelExplosion.getValue()) {
            switch (mode.getValue()) {
                case Cancel -> {
                    ((IExplosionS2CPacket) explosion).setMotionX(0);
                    ((IExplosionS2CPacket) explosion).setMotionY(0);
                    ((IExplosionS2CPacket) explosion).setMotionZ(0);
                }
                case Reduce -> {
                    ((IExplosionS2CPacket) explosion).setMotionX(((IExplosionS2CPacket) explosion).getMotionX() * horizontal.getValue());
                    ((IExplosionS2CPacket) explosion).setMotionY(((IExplosionS2CPacket) explosion).getMotionY() * vertical.getValue());
                    ((IExplosionS2CPacket) explosion).setMotionZ(((IExplosionS2CPacket) explosion).getMotionZ() * horizontal.getValue());
                }
                case Vanilla -> velocityApplied = true;
            }
        }
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;

        if (mode.getValue() == Mode.Vanilla && velocityApplied) {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            velocityApplied = false;
        }
    }

    @Override
    public void onDisable() {
        velocityApplied = false;
    }

    @Override
    public String getDisplayInfo() {
        return mode.getValue().name();
    }

    public enum Mode {
        Cancel, Reduce, Vanilla
    }
}
