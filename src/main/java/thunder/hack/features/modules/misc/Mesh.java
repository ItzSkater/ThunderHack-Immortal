package thunder.hack.features.modules.misc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

/**
 * Encrypted P2P chat + coordinate sharing.
 *
 * Uses a "blind" rendezvous server (mesh/ThunderMeshServer.java) on your VPS only
 * to introduce peers and as a fallback path. All payloads are end-to-end
 * AES-256-GCM encrypted with a key derived from the Room passphrase, so the
 * server can never read them. Where NAT allows, messages travel directly
 * peer-to-peer; otherwise they fall back through the (still blind) relay.
 *
 * Commands: .mc <text>  — send chat ;  .mpos — share your coordinates
 */
public class Mesh extends Module {
    private final Setting<String> serverIp = new Setting<>("ServerIP", "127.0.0.1");
    private final Setting<Integer> serverPort = new Setting<>("ServerPort", 7778, 1, 65535);
    private final Setting<String> room = new Setting<>("Room", "default");
    private final Setting<String> nick = new Setting<>("Nick", "");

    private static final byte PUNCH = 0x00;
    private static final byte REGISTER = 0x01;
    private static final byte DATA = 0x02;
    private static final byte PEERLIST = 0x03;

    private volatile DatagramSocket socket;
    private volatile Thread receiver;
    private volatile boolean running;
    private int tickTimer;

    private SecretKey key;
    private byte[] roomId;   // 16 bytes
    private byte[] peerId;   // 16 bytes
    private InetAddress serverAddr;

    private final SecureRandom random = new SecureRandom();
    // peerIdHex -> public endpoint (for direct P2P)
    private final Map<String, InetSocketAddress> peers = new HashMap<>();
    // recent message ids for dedup (direct + relayed arrive twice)
    private final LinkedHashSet<String> seen = new LinkedHashSet<>();

    public Mesh() {
        super("Mesh", Category.MISC);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            sendMessage("Join a world first");
            disable();
            return;
        }
        try {
            key = new SecretKeySpec(sha256("thmesh-key:" + room.getValue()), "AES");
            roomId = Arrays.copyOf(sha256("thmesh-room:" + room.getValue()), 16);
            UUID u = mc.player.getUuid();
            peerId = ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();
            serverAddr = InetAddress.getByName(serverIp.getValue());
            socket = new DatagramSocket();
            running = true;
            receiver = new Thread(this::receiveLoop, "Mesh-rx");
            receiver.setDaemon(true);
            receiver.start();
            sendRegister();
            tickTimer = 0;
            sendMessage("Connected to mesh (room hash " + hex(roomId).substring(0, 8) + ")");
        } catch (Exception e) {
            sendMessage("Mesh init failed: " + e.getMessage());
            disable();
        }
    }

    @Override
    public void onDisable() {
        running = false;
        DatagramSocket s = socket;
        if (s != null) s.close();
        socket = null;
        synchronized (peers) { peers.clear(); }
        synchronized (seen) { seen.clear(); }
    }

    @Override
    public void onUpdate() {
        if (!running || mc.player == null) return;
        if (tickTimer-- <= 0) {
            sendRegister();          // keepalive + refresh peer list
            punchPeers();            // keep NAT mappings open for direct path
            tickTimer = 60;          // ~3s
        }
    }

    // ---------------- public API (used by commands) ----------------

    public void sendChat(String text) {
        if (!running) { sendMessage("Mesh is not enabled"); return; }
        JsonObject o = new JsonObject();
        o.addProperty("t", "chat");
        o.addProperty("n", displayNick());
        o.addProperty("m", text);
        o.addProperty("id", newId());
        broadcast(o.toString());
        sendMessage(Formatting.GRAY + "[me] " + Formatting.WHITE + text);
    }

    public void shareCoords() {
        if (!running) { sendMessage("Mesh is not enabled"); return; }
        if (mc.player == null) return;
        net.minecraft.util.math.BlockPos pos = mc.player.getBlockPos();
        JsonObject o = new JsonObject();
        o.addProperty("t", "coords");
        o.addProperty("n", displayNick());
        o.addProperty("x", pos.getX());
        o.addProperty("y", pos.getY());
        o.addProperty("z", pos.getZ());
        o.addProperty("dim", dimensionName());
        o.addProperty("id", newId());
        broadcast(o.toString());
        sendMessage(Formatting.GRAY + "shared coords: " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }

    // ---------------- networking ----------------

    private void receiveLoop() {
        byte[] buf = new byte[64 * 1024];
        while (running) {
            try {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);
                handlePacket(pkt);
            } catch (Exception e) {
                if (!running) break;
            }
        }
    }

    private void handlePacket(DatagramPacket pkt) {
        int len = pkt.getLength();
        if (len < 1) return;
        byte[] d = pkt.getData();
        byte type = d[0];

        if (type == DATA) {
            if (len < 33) return;
            byte[] blob = Arrays.copyOfRange(d, 33, len);
            byte[] plain;
            try { plain = decrypt(blob); } catch (Exception e) { return; } // not our room / corrupt
            try {
                JsonObject o = JsonParser.parseString(new String(plain, StandardCharsets.UTF_8)).getAsJsonObject();
                String id = o.has("id") ? o.get("id").getAsString() : null;
                if (id != null && !markSeen(id)) return; // duplicate
                dispatch(o);
            } catch (Exception ignored) {
            }
        } else if (type == PEERLIST) {
            parsePeerList(d, len);
        }
    }

    private void dispatch(JsonObject o) {
        String t = o.has("t") ? o.get("t").getAsString() : "";
        String n = o.has("n") ? o.get("n").getAsString() : "?";
        switch (t) {
            case "chat" -> printChat(Formatting.AQUA + n + Formatting.GRAY + ": " + Formatting.WHITE + o.get("m").getAsString());
            case "coords" -> printChat(Formatting.AQUA + n + Formatting.GRAY + " @ " + Formatting.YELLOW
                    + o.get("x").getAsInt() + " " + o.get("y").getAsInt() + " " + o.get("z").getAsInt()
                    + Formatting.GRAY + " (" + o.get("dim").getAsString() + ")");
        }
    }

    private void parsePeerList(byte[] d, int len) {
        if (len < 2) return;
        int count = d[1] & 0xFF;
        int i = 2;
        Map<String, InetSocketAddress> fresh = new HashMap<>();
        for (int k = 0; k < count && i + 22 <= len; k++) {
            String id = hex(Arrays.copyOfRange(d, i, i + 16)); i += 16;
            byte[] ip = Arrays.copyOfRange(d, i, i + 4); i += 4;
            int port = ((d[i] & 0xFF) << 8) | (d[i + 1] & 0xFF); i += 2;
            try {
                fresh.put(id, new InetSocketAddress(InetAddress.getByAddress(ip), port));
            } catch (Exception ignored) {
            }
        }
        synchronized (peers) {
            peers.clear();
            peers.putAll(fresh);
        }
        punchPeers();
    }

    private void broadcast(String json) {
        try {
            byte[] blob = encrypt(json.getBytes(StandardCharsets.UTF_8));
            byte[] frame = ByteBuffer.allocate(1 + 16 + 16 + blob.length)
                    .put(DATA).put(roomId).put(peerId).put(blob).array();
            DatagramSocket s = socket;
            if (s == null) return;
            // relay via server (always-works fallback)
            s.send(new DatagramPacket(frame, frame.length, serverAddr, serverPort.getValue()));
            // direct to known peers (true P2P when NAT allows; dedup handles overlap)
            synchronized (peers) {
                for (InetSocketAddress ep : peers.values())
                    s.send(new DatagramPacket(frame, frame.length, ep.getAddress(), ep.getPort()));
            }
        } catch (Exception e) {
            sendMessage("send failed: " + e.getMessage());
        }
    }

    private void sendRegister() {
        try {
            byte[] frame = ByteBuffer.allocate(1 + 16 + 16).put(REGISTER).put(roomId).put(peerId).array();
            DatagramSocket s = socket;
            if (s != null) s.send(new DatagramPacket(frame, frame.length, serverAddr, serverPort.getValue()));
        } catch (Exception ignored) {
        }
    }

    private void punchPeers() {
        DatagramSocket s = socket;
        if (s == null) return;
        byte[] punch = {PUNCH};
        synchronized (peers) {
            for (InetSocketAddress ep : peers.values()) {
                try { s.send(new DatagramPacket(punch, punch.length, ep.getAddress(), ep.getPort())); }
                catch (Exception ignored) {}
            }
        }
    }

    // ---------------- helpers ----------------

    private void printChat(String text) {
        mc.execute(() -> {
            if (mc.player != null)
                mc.player.sendMessage(Text.of(Formatting.DARK_PURPLE + "[Mesh] " + Formatting.RESET + text));
        });
    }

    private boolean markSeen(String id) {
        synchronized (seen) {
            if (seen.contains(id)) return false;
            seen.add(id);
            if (seen.size() > 256) {
                Iterator<String> it = seen.iterator();
                it.next(); it.remove();
            }
            return true;
        }
    }

    private String displayNick() {
        String n = nick.getValue();
        if (n != null && !n.isBlank()) return n;
        return mc.player != null ? mc.player.getName().getString() : "anon";
    }

    private String dimensionName() {
        if (mc.world == null) return "?";
        String path = mc.world.getRegistryKey().getValue().getPath();
        return switch (path) {
            case "overworld" -> "OW";
            case "the_nether" -> "Nether";
            case "the_end" -> "End";
            default -> path;
        };
    }

    private String newId() {
        return Long.toHexString(random.nextLong()) + Long.toHexString(random.nextLong());
    }

    private byte[] encrypt(byte[] plain) throws Exception {
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
        byte[] ct = c.doFinal(plain);
        return ByteBuffer.allocate(nonce.length + ct.length).put(nonce).put(ct).array();
    }

    private byte[] decrypt(byte[] in) throws Exception {
        byte[] nonce = Arrays.copyOfRange(in, 0, 12);
        byte[] ct = Arrays.copyOfRange(in, 12, in.length);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
        return c.doFinal(ct);
    }

    private static byte[] sha256(String s) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x & 0xFF));
        return sb.toString();
    }
}
