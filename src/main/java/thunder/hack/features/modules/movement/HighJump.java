package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import thunder.hack.events.impl.EventPlayerJump;
import thunder.hack.events.impl.EventTick;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

/**
 * Boosts vertical jump velocity. Ported from Vegaline.
 *
 * Hooks EventPlayerJump to mark the tick a jump occurred, then overrides
 * the upward velocity on the very next EventTick so the modified launch
 * speed survives the vanilla {@code jump()} call.
 */
public class HighJump extends Module {
    private final Setting<Float> power = new Setting<>("Power", 1.2f, 0.5f, 5.0f);
    private final Setting<Boolean> ignoreLiquid = new Setting<>("IgnoreLiquid", true);

    private boolean justJumped;

    public HighJump() {
        super("HighJump", Category.MOVEMENT);
    }

    @EventHandler
    public void onJump(EventPlayerJump e) {
        if (mc.player == null) return;
        if (!ignoreLiquid.getValue() && (mc.player.isInLava() || mc.player.isTouchingWater() || mc.player.isClimbing()))
            return;
        justJumped = true;
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (mc.player == null || !justJumped) return;
        justJumped = false;

        float boost = mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) ? 0.2f : 0f;
        double vy = 0.42f * power.getValue() + boost;
        mc.player.setVelocity(mc.player.getVelocity().x, vy, mc.player.getVelocity().z);
    }

    @Override
    public void onDisable() {
        justJumped = false;
    }
}
