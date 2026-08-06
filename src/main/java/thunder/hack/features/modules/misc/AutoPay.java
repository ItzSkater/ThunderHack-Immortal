package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Formatting;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Periodically checks the balance via /bal and pays a fixed amount (or the whole
 * balance) to a target player via /pay every N seconds.
 *
 * Parses balance lines like "Баланс : 545 тыс." or "Balance: 1 млн" — supports
 * тыс/k (x1000), млн/m (x1_000_000), млрд/b (x1_000_000_000) suffixes.
 */
public final class AutoPay extends Module {
    private final Setting<String> target = new Setting<>("Target", "Nickname");
    private final Setting<Mode> mode = new Setting<>("Amount", Mode.Fixed);
    private final Setting<Integer> amount = new Setting<>("FixedAmount", 10000, 1, 1_000_000_000, v -> mode.getValue() == Mode.Fixed);
    private final Setting<Integer> interval = new Setting<>("IntervalSec", 30, 1, 600);
    private final Setting<Boolean> onlyIfEnough = new Setting<>("OnlyIfEnough", true, v -> mode.getValue() == Mode.Fixed);
    private final Setting<String> balanceCommand = new Setting<>("BalanceCommand", "bal");
    private final Setting<String> payCommand = new Setting<>("PayCommand", "pay");

    private static final Pattern BALANCE = Pattern.compile(
            "(?iu)(?:баланс|balance)\\s*:?\\s*([0-9][0-9\\s.,]*)\\s*(тыс|млн|млрд|k|m|b)?");

    private final Timer timer = new Timer();
    private long lastBalance = -1;

    public AutoPay() {
        super("AutoPay", Category.MISC);
    }

    @Override
    public void onEnable() {
        lastBalance = -1;
        timer.reset();
        requestBalance();
    }

    @Override
    public void onUpdate() {
        if (fullNullCheck()) return;
        if (!timer.passedMs(interval.getValue() * 1000L)) return;
        timer.reset();

        // refresh balance for the next cycle
        requestBalance();

        String nick = target.getValue().trim();
        if (nick.isEmpty() || nick.equalsIgnoreCase("Nickname")) {
            sendMessage(Formatting.RED + "Set a Target nickname");
            return;
        }

        long pay;
        if (mode.getValue() == Mode.All) {
            if (lastBalance <= 0) return; // nothing known/left to send
            pay = lastBalance;
        } else {
            pay = amount.getValue();
            if (onlyIfEnough.getValue() && lastBalance >= 0 && lastBalance < pay) return;
        }

        if (pay <= 0) return;
        mc.player.networkHandler.sendChatCommand(payCommand.getValue() + " " + nick + " " + pay);
    }

    private void requestBalance() {
        if (mc.player == null) return;
        mc.player.networkHandler.sendChatCommand(balanceCommand.getValue());
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (!(e.getPacket() instanceof GameMessageS2CPacket packet)) return;
        String msg = packet.content().getString();

        Matcher m = BALANCE.matcher(msg);
        if (!m.find()) return;

        String number = m.group(1).replace(" ", "").replace(" ", "").replace(",", ".");
        String suffix = m.group(2);
        try {
            double value = Double.parseDouble(number);
            double mult = switch (suffix == null ? "" : suffix.toLowerCase()) {
                case "тыс", "k" -> 1_000d;
                case "млн", "m" -> 1_000_000d;
                case "млрд", "b" -> 1_000_000_000d;
                default -> 1d;
            };
            lastBalance = (long) (value * mult);
        } catch (NumberFormatException ignored) {
        }
    }

    public enum Mode {
        Fixed, All
    }
}
