package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import thunder.hack.events.impl.EventTick;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

/**
 * Sneak-burst dash. Holding sneak for a short window (push) then releasing
 * launches the player forward at a multiplied speed for a few ticks.
 * Ported from Vegaline.
 */
public class PhantomDash extends Module {
    private final Setting<Float> speedFactor = new Setting<>("SpeedFactor", 4.5f, 1.5f, 8.0f);
    private final Setting<Integer> maxDashTicks = new Setting<>("DashTicks", 20, 5, 40);
    private final Setting<Integer> pushLimitTicks = new Setting<>("PushTicks", 3, 1, 10);

    private int pushTicks;
    private int prevPushTicks;
    private int dashTicks;
    private int slowingTicks;

    public PhantomDash() {
        super("PhantomDash", Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (mc.player == null) return;

        prevPushTicks = pushTicks;

        if (mc.player.isSneaking()) {
            if (++pushTicks == 1)
                slowingTicks = pushLimitTicks.getValue();
        } else {
            if (pushTicks != 0 && pushTicks < maxDashTicks.getValue())
                dashTicks = (int) (maxDashTicks.getValue() * (1.0f - (float) slowingTicks / pushLimitTicks.getValue()));
            pushTicks = 0;
        }

        if (slowingTicks > 0) slowingTicks--;
        if (pushTicks >= maxDashTicks.getValue()) dashTicks = 0;
        if (dashTicks > 0) dashTicks--;

        boolean slowing = slowingTicks > 0 && pushTicks > prevPushTicks;
        boolean dash = dashTicks > 0;

        if (slowing) {
            applyHorizontal(1.0 / speedFactor.getValue());
        } else if (dash && MovementUtility.isMoving()) {
            applyHorizontal(speedFactor.getValue());
        }
    }

    private void applyHorizontal(double factor) {
        if (mc.player == null) return;
        double vx = mc.player.getVelocity().x * factor;
        double vz = mc.player.getVelocity().z * factor;
        mc.player.setVelocity(vx, mc.player.getVelocity().y, vz);
    }

    @Override
    public void onDisable() {
        pushTicks = prevPushTicks = dashTicks = slowingTicks = 0;
    }
}
