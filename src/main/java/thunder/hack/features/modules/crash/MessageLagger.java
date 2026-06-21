// Ported from AntiCope/meteor-crash-addon (MIT License)
package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class MessageLagger extends Module {
    private final Setting<Integer> messageLength = new Setting<>("Length", 200, 1, 1000);
    private final Setting<Boolean> keepSending = new Setting<>("KeepSending", false);
    private final Setting<Integer> delay = new Setting<>("Delay", 100, 0, 1000);

    private int timer;

    public MessageLagger() {
        super("MessageLagger", Category.CRASH);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) return;
        if (!keepSending.getValue()) {
            sendLagMessage();
            disable();
        }
        timer = delay.getValue();
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || !keepSending.getValue()) return;
        if (timer <= 0) {
            sendLagMessage();
            timer = delay.getValue();
        } else {
            timer--;
        }
    }

    private void sendLagMessage() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messageLength.getValue(); i++) {
            sb.append((char) ((int) (Math.random() * 0x1D300) + 0x800));
        }
        mc.player.networkHandler.sendChatMessage(sb.toString());
    }
}
