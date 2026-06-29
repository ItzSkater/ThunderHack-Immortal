package thunder.hack.features.modules.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Networked PenisESP. When enabled, broadcasts (via a relay running on your VPS)
 * that you have it on; every other ThunderHack-Immortal client in the same room
 * draws the penis on YOUR player entity. See relay/ThunderSyncRelay.java.
 */
public class PenisSync extends Module {
    private final Setting<String> ip = new Setting<>("ServerIP", "127.0.0.1");
    private final Setting<Integer> port = new Setting<>("ServerPort", 7777, 1, 65535);
    private final Setting<String> room = new Setting<>("Room", "default");
    private final Setting<Boolean> renderSelf = new Setting<>("RenderSelf", true);
    private final Setting<Float> penisSize = new Setting<>("PenisSize", 1.5f, 0.1f, 3.0f);
    private final Setting<Float> ballSize = new Setting<>("BallSize", 0.1f, 0.1f, 0.5f);
    private final Setting<Integer> gradation = new Setting<>("Gradation", 30, 20, 100);
    private final Setting<ColorSetting> penisColor = new Setting<>("PenisColor", new ColorSetting(new Color(231, 180, 122, 255)));
    private final Setting<ColorSetting> headColor = new Setting<>("HeadColor", new ColorSetting(new Color(240, 50, 180, 255)));

    // UUIDs that have broadcast on:true (rendered locally)
    private final Set<UUID> active = ConcurrentHashMap.newKeySet();

    private volatile Socket socket;
    private volatile BufferedWriter writer;
    private volatile Thread netThread;
    private volatile boolean running;
    private int heartbeat;

    public PenisSync() {
        super("PenisSync", Category.RENDER);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            sendMessage("Join a world first");
            disable();
            return;
        }
        active.clear();
        running = true;
        netThread = new Thread(this::networkLoop, "PenisSync-net");
        netThread.setDaemon(true);
        netThread.start();
        heartbeat = 0;
    }

    @Override
    public void onDisable() {
        running = false;
        sendState(false);
        closeSocket();
        active.clear();
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        // periodic heartbeat so late-joining peers learn our state
        if (heartbeat-- <= 0) {
            sendState(true);
            heartbeat = 20;
        }
    }

    // ---------------- networking ----------------

    private void networkLoop() {
        while (running) {
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(ip.getValue(), port.getValue()), 5000);
                s.setTcpNoDelay(true);
                socket = s;
                writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
                sendState(true);

                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while (running && (line = in.readLine()) != null) {
                    handleLine(line);
                }
            } catch (IOException ignored) {
                // connection failed / dropped, retry below
            } finally {
                closeSocket();
            }

            if (!running) break;
            try { Thread.sleep(3000); } catch (InterruptedException e) { break; }
        }
    }

    private void handleLine(String line) {
        try {
            JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
            if (!obj.has("uuid")) return;
            UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
            boolean on = obj.has("on") && obj.get("on").getAsBoolean();
            if (on) active.add(uuid);
            else active.remove(uuid);
        } catch (Exception ignored) {
        }
    }

    private void sendState(boolean on) {
        BufferedWriter w = writer;
        if (w == null || mc.player == null) return;
        String msg = "{\"room\":\"" + escape(room.getValue())
                + "\",\"uuid\":\"" + mc.player.getUuid()
                + "\",\"on\":" + on + "}";
        try {
            synchronized (w) {
                w.write(msg);
                w.write('\n');
                w.flush();
            }
        } catch (IOException e) {
            closeSocket();
        }
    }

    private void closeSocket() {
        Socket s = socket;
        if (s != null) {
            try { s.close(); } catch (IOException ignored) {}
        }
        socket = null;
        writer = null;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ---------------- rendering (geometry from PenisESP) ----------------

    @Override
    public void onRender2D(DrawContext event) {
        if (mc.world == null) return;
        for (PlayerEntity player : mc.world.getPlayers()) {
            boolean self = player == mc.player;
            if (self && !renderSelf.getValue()) continue;
            if (!active.contains(player.getUuid())) continue;

            Vec3d base = getBase(player);
            Vec3d forward = base.add(0, player.getHeight() / 2.4, 0).add(Vec3d.fromPolar(0, player.getYaw()).multiply(0.1));
            Vec3d left = forward.add(Vec3d.fromPolar(0, player.getYaw() - 90).multiply(ballSize.getValue()));
            Vec3d right = forward.add(Vec3d.fromPolar(0, player.getYaw() + 90).multiply(ballSize.getValue()));

            drawBall(player, ballSize.getValue(), gradation.getValue(), left, penisColor.getValue().getColorObject(), 0);
            drawBall(player, ballSize.getValue(), gradation.getValue(), right, penisColor.getValue().getColorObject(), 0);
            drawPenis(player, penisSize.getValue(), forward);
        }
    }

    private Vec3d getBase(Entity entity) {
        double x = entity.prevX + ((entity.getX() - entity.prevX) * Render3DEngine.getTickDelta());
        double y = entity.prevY + ((entity.getY() - entity.prevY) * Render3DEngine.getTickDelta());
        double z = entity.prevZ + ((entity.getZ() - entity.prevZ) * Render3DEngine.getTickDelta());
        return new Vec3d(x, y, z);
    }

    private void drawBall(PlayerEntity player, double radius, int grad, Vec3d pos, Color color, int stage) {
        for (float alpha = 0.0f; alpha < Math.PI; alpha += Math.PI / grad) {
            for (float beta = 0.0f; beta < 2.0 * Math.PI; beta += Math.PI / grad) {
                double x1 = pos.getX() + (radius * Math.cos(beta) * Math.sin(alpha));
                double y1 = pos.getY() + (radius * Math.sin(beta) * Math.sin(alpha));
                double z1 = pos.getZ() + (radius * Math.cos(alpha));

                double sin = Math.sin(alpha + Math.PI / grad);
                double x2 = pos.getX() + (radius * Math.cos(beta) * sin);
                double y2 = pos.getY() + (radius * Math.sin(beta) * sin);
                double z2 = pos.getZ() + (radius * Math.cos(alpha + Math.PI / grad));

                Vec3d base = getBase(player);
                Vec3d forward = base.add(0, player.getHeight() / 2.4, 0).add(Vec3d.fromPolar(0, player.getYaw()).multiply(0.1));
                Vec3d vec3d = new Vec3d(x1, y1, z1);

                switch (stage) {
                    case 1 -> { if (!vec3d.isInRange(forward, 0.145)) continue; }
                    case 2 -> { if (vec3d.isInRange(forward, penisSize.getValue() + 0.095)) continue; }
                }

                Render3DEngine.drawLine(vec3d, new Vec3d(x2, y2, z2), color);
            }
        }
    }

    private void drawPenis(PlayerEntity player, double size, Vec3d start) {
        Vec3d copy = start;
        start = start.add(Vec3d.fromPolar(0, player.getYaw()).multiply(0.1));

        List<Vec3d> vecs = getVec3ds(start, 0.1);
        vecs.forEach(vec3d -> {
            if (!vec3d.isInRange(copy, 0.145)) return;
            if (vec3d.isInRange(copy, 0.135)) return;
            Vec3d pos = vec3d.add(Vec3d.fromPolar(0, player.getYaw()).multiply(size));
            Render3DEngine.drawLine(vec3d, pos, penisColor.getValue().getColorObject());
        });

        Vec3d end = start.add(Vec3d.fromPolar(0, player.getYaw()).multiply(size));
        drawBall(player, 0.1, gradation.getValue(), start, penisColor.getValue().getColorObject(), 1);
        drawBall(player, 0.1, gradation.getValue(), end, headColor.getValue().getColorObject(), 2);
    }

    private List<Vec3d> getVec3ds(Vec3d vec3d, double radius) {
        List<Vec3d> vec3ds = new ArrayList<>();
        for (float alpha = 0.0f; alpha < Math.PI; alpha += Math.PI / gradation.getValue()) {
            for (float beta = 0.0f; beta < 2.01f * Math.PI; beta += Math.PI / gradation.getValue()) {
                double x1 = vec3d.getX() + (radius * Math.cos(beta) * Math.sin(alpha));
                double y1 = vec3d.getY() + (radius * Math.sin(beta) * Math.sin(alpha));
                double z1 = vec3d.getZ() + (radius * Math.cos(alpha));
                vec3ds.add(new Vec3d(x1, y1, z1));
            }
        }
        return vec3ds;
    }
}
