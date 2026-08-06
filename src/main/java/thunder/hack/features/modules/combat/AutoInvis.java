package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventAfterRotate;
import thunder.hack.events.impl.EventPostSync;
import thunder.hack.events.impl.PostPlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;

/**
 * Keeps an Invisibility effect up automatically. Can drink a drinkable
 * Invisibility potion from the hotbar and/or throw a splash Invisibility potion
 * at your feet, depending on the mode.
 */
public final class AutoInvis extends Module {
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Both);
    private final Setting<Integer> refresh = new Setting<>("RefreshSeconds", 0, 0, 30);
    private final Setting<Boolean> onlyOnGround = new Setting<>("OnlyOnGround", true);
    private final Setting<Boolean> pauseAura = new Setting<>("PauseAura", false);
    private final Setting<Integer> delay = new Setting<>("Delay", 500, 0, 3000);

    private final Timer timer = new Timer();
    private boolean drinking;
    private int prevSlot = -1;
    private boolean spoofed;

    public AutoInvis() {
        super("AutoInvis", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        stopDrink();
        spoofed = false;
    }

    private boolean needInvis() {
        StatusEffectInstance e = mc.player.getStatusEffect(StatusEffects.INVISIBILITY);
        return e == null || e.getDuration() <= refresh.getValue() * 20;
    }

    // ---------------- drinking ----------------

    @EventHandler
    public void onUpdate(PostPlayerUpdateEvent e) {
        if (fullNullCheck()) return;

        boolean allowDrink = mode.getValue() == Mode.Drink || mode.getValue() == Mode.Both;

        // finish / cancel an in-progress drink
        if (drinking) {
            if (allowDrink && needInvis() && mc.player.isUsingItem() && isHoldingInvisDrink()) {
                mc.options.useKey.setPressed(true);
            } else {
                stopDrink();
            }
            return;
        }

        if (!allowDrink) return;
        if (!needInvis()) return;
        if (onlyOnGround.getValue() && !mc.player.isOnGround()) return;
        if (!timer.passedMs(delay.getValue())) return;
        if (mc.player.isUsingItem()) return; // don't interrupt eating/blocking

        int slot = findSlot(false);
        if (slot != -1) startDrink(slot);
    }

    private void startDrink(int slot) {
        prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;
        sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        if (pauseAura.getValue()) ModuleManager.aura.pause();
        drinking = true;
        mc.options.useKey.setPressed(true);
    }

    private void stopDrink() {
        if (!drinking) return;
        mc.options.useKey.setPressed(false);
        if (prevSlot != -1) {
            mc.player.getInventory().selectedSlot = prevSlot;
            sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
            prevSlot = -1;
        }
        drinking = false;
        timer.reset();
    }

    private boolean isHoldingInvisDrink() {
        return isInvisPotion(mc.player.getMainHandStack(), false);
    }

    // ---------------- splashing (AutoBuff-style) ----------------

    private boolean needSplash() {
        if (!needInvis()) return false;
        if (mode.getValue() == Mode.Splash) return findSlot(true) != -1;
        if (mode.getValue() == Mode.Both) return findSlot(false) == -1 && findSlot(true) != -1;
        return false;
    }

    @EventHandler
    public void onPostRotationSet(EventAfterRotate event) {
        if (drinking) return;
        if (needSplash()) {
            mc.player.setPitch(90);
            spoofed = true;
        }
    }

    @EventHandler
    public void onPostSync(EventPostSync e) {
        if (drinking) return;
        if (!needSplash()) return;
        if (onlyOnGround.getValue() && !mc.player.isOnGround()) return;
        if (!timer.passedMs(delay.getValue()) || !spoofed) return;

        int slot = findSlot(true);
        if (slot != -1) {
            if (pauseAura.getValue()) ModuleManager.aura.pause();
            sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
            timer.reset();
        }
        spoofed = false;
    }

    // ---------------- helpers ----------------

    private int findSlot(boolean splash) {
        for (int i = 0; i < 9; i++) {
            if (isInvisPotion(mc.player.getInventory().getStack(i), splash)) return i;
        }
        return -1;
    }

    private boolean isInvisPotion(ItemStack stack, boolean splash) {
        if (stack == null || stack.isEmpty()) return false;
        boolean isSplash = stack.getItem() instanceof SplashPotionItem;
        if (splash != isSplash) return false;
        if (!splash && stack.getItem() != Items.POTION) return false;

        PotionContentsComponent contents = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
        for (StatusEffectInstance effect : contents.getEffects()) {
            if (effect.getEffectType() == StatusEffects.INVISIBILITY) return true;
        }
        return false;
    }

    public enum Mode {
        Drink, Splash, Both
    }
}
