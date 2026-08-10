package gg.raceclient.meteor.modules;

import gg.raceclient.meteor.RaceAddon;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoPay extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> target = sgGeneral.add(new StringSetting.Builder()
        .name("target").description("Player to pay.").defaultValue("Nickname").build());

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("amount-mode").description("Fixed amount or the whole balance.").defaultValue(Mode.Fixed).build());

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
        .name("fixed-amount").description("Amount to pay each time.").defaultValue(10000)
        .min(1).sliderRange(1, 1_000_000).visible(() -> mode.get() == Mode.Fixed).build());

    private final Setting<Integer> interval = sgGeneral.add(new IntSetting.Builder()
        .name("interval-seconds").description("Delay between payments.").defaultValue(30)
        .min(1).sliderRange(1, 600).build());

    private final Setting<Boolean> onlyIfEnough = sgGeneral.add(new BoolSetting.Builder()
        .name("only-if-enough").description("Skip if balance < amount.").defaultValue(true)
        .visible(() -> mode.get() == Mode.Fixed).build());

    private final Setting<String> balanceCommand = sgGeneral.add(new StringSetting.Builder()
        .name("balance-command").description("Command (without /) to query balance.").defaultValue("bal").build());

    private final Setting<String> payCommand = sgGeneral.add(new StringSetting.Builder()
        .name("pay-command").description("Command (without /) to pay.").defaultValue("pay").build());

    // "Баланс : 545 тыс.", "Balance: €7 млн." — optional currency symbol before the number
    private static final Pattern BALANCE = Pattern.compile(
        "(?iu)(?:баланс|balance)\\s*:?\\s*[^0-9]*?([0-9][0-9\\s.,]*)\\s*(тыс|млн|млрд|k|m|b)?");

    private int timer;
    private long lastBalance = -1;

    public AutoPay() {
        super(RaceAddon.CATEGORY, "auto-pay", "Checks balance via /bal and pays a target every N seconds.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        lastBalance = -1;
        if (mc.player != null) ChatUtils.sendPlayerMsg("/" + balanceCommand.get());
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        if (++timer < interval.get() * 20) return;
        timer = 0;

        ChatUtils.sendPlayerMsg("/" + balanceCommand.get());

        String nick = target.get().trim();
        if (nick.isEmpty() || nick.equalsIgnoreCase("Nickname")) return;

        long pay;
        if (mode.get() == Mode.All) {
            if (lastBalance <= 0) return;
            pay = lastBalance;
        } else {
            pay = amount.get();
            if (onlyIfEnough.get() && lastBalance >= 0 && lastBalance < pay) return;
        }
        if (pay <= 0) return;

        ChatUtils.sendPlayerMsg("/" + payCommand.get() + " " + nick + " " + pay);
    }

    @EventHandler
    private void onMessage(ReceiveMessageEvent event) {
        Matcher m = BALANCE.matcher(event.getMessage().getString());
        if (!m.find()) return;

        String number = m.group(1).replace(" ", "").replace(" ", "").replace(",", ".");
        String suffix = m.group(2);
        try {
            double v = Double.parseDouble(number);
            double mult = switch (suffix == null ? "" : suffix.toLowerCase()) {
                case "тыс", "k" -> 1_000d;
                case "млн", "m" -> 1_000_000d;
                case "млрд", "b" -> 1_000_000_000d;
                default -> 1d;
            };
            lastBalance = (long) (v * mult);
        } catch (NumberFormatException ignored) {
        }
    }

    public enum Mode {
        Fixed, All
    }
}
