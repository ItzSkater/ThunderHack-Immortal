import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ThunderMesh rendezvous + relay server (UDP).
 *
 * Dependency-free coordination server for the in-game encrypted P2P chat /
 * coordinate sharing (ThunderHack-Immortal "Mesh" module).
 *
 * The server is "blind":
 *   - it groups peers only by a 16-byte ROOM-ID, which is a HASH of the room
 *     passphrase, so it never learns the room name;
 *   - all chat/coords payloads are end-to-end AES-256-GCM encrypted by the
 *     clients, so the server only ever forwards opaque ciphertext.
 *
 * Its job is twofold:
 *   1. RENDEZVOUS: tell every peer the public ip:port of the other peers in the
 *      same room so they can UDP hole-punch and talk directly (true P2P).
 *   2. RELAY FALLBACK: forward encrypted blobs for peers whose NAT refuses a
 *      direct path (symmetric NAT). Still ciphertext, still unreadable here.
 *
 * Wire format (first byte = type):
 *   0x01 REGISTER  [roomId:16][peerId:16]                    client -> server
 *   0x02 DATA      [roomId:16][senderPeerId:16][blob...]     both ways (relay/direct)
 *   0x03 PEERLIST  [count:1]({peerId:16}{ipv4:4}{port:2})*   server -> client
 *
 * Build: javac ThunderMeshServer.java
 * Run:   java ThunderMeshServer [port]      (default 7778)
 */
public final class ThunderMeshServer {
    private static final byte REGISTER = 0x01;
    private static final byte DATA = 0x02;
    private static final byte PEERLIST = 0x03;

    private static final long PEER_TTL_MS = 30_000;

    // roomId(hex) -> (peerId(hex) -> Peer)
    private static final Map<String, Map<String, Peer>> ROOMS = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        int port = 7778;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }

        DatagramSocket socket = new DatagramSocket(port);
        log("ThunderMesh server listening on UDP " + port);

        byte[] buf = new byte[64 * 1024];
        while (true) {
            DatagramPacket pkt = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(pkt);
                handle(socket, pkt);
            } catch (IOException e) {
                log("recv error: " + e.getMessage());
            }
        }
    }

    private static void handle(DatagramSocket socket, DatagramPacket pkt) throws IOException {
        int len = pkt.getLength();
        if (len < 1) return;
        byte[] d = pkt.getData();
        byte type = d[0];
        InetAddress addr = pkt.getAddress();
        int port = pkt.getPort();

        if (type == REGISTER) {
            if (len < 33) return;
            String roomId = hex(d, 1, 16);
            String peerId = hex(d, 17, 16);
            Map<String, Peer> room = ROOMS.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
            room.put(peerId, new Peer(peerId, addr, port, now()));
            prune(room);
            sendPeerList(socket, room, peerId);
        } else if (type == DATA) {
            if (len < 33) return;
            String roomId = hex(d, 1, 16);
            String senderId = hex(d, 17, 16);
            Map<String, Peer> room = ROOMS.get(roomId);
            if (room == null) return;
            // refresh sender mapping (keepalive via traffic)
            Peer sender = room.get(senderId);
            if (sender != null) { sender.addr = addr; sender.port = port; sender.lastSeen = now(); }
            prune(room);
            // forward verbatim to every other peer in the room
            for (Peer p : room.values()) {
                if (p.id.equals(senderId)) continue;
                socket.send(new DatagramPacket(d, len, p.addr, p.port));
            }
        }
    }

    private static void sendPeerList(DatagramSocket socket, Map<String, Peer> room, String exceptId) throws IOException {
        List<Peer> peers = new ArrayList<>();
        for (Peer p : room.values()) if (!p.id.equals(exceptId)) peers.add(p);

        // self (requester) needs the list; recipient is the peer we just registered
        Peer self = room.get(exceptId);
        if (self == null) return;

        int count = Math.min(peers.size(), 255);
        byte[] out = new byte[1 + 1 + count * (16 + 4 + 2)];
        int i = 0;
        out[i++] = PEERLIST;
        out[i++] = (byte) count;
        for (int k = 0; k < count; k++) {
            Peer p = peers.get(k);
            byte[] id = unhex(p.id);
            System.arraycopy(id, 0, out, i, 16); i += 16;
            byte[] ip = p.addr.getAddress();
            if (ip.length != 4) { ip = new byte[]{0, 0, 0, 0}; } // IPv4 only
            System.arraycopy(ip, 0, out, i, 4); i += 4;
            out[i++] = (byte) ((p.port >> 8) & 0xFF);
            out[i++] = (byte) (p.port & 0xFF);
        }
        socket.send(new DatagramPacket(out, out.length, self.addr, self.port));
    }

    private static void prune(Map<String, Peer> room) {
        long t = now();
        room.values().removeIf(p -> t - p.lastSeen > PEER_TTL_MS);
    }

    private static long now() { return System.currentTimeMillis(); }

    private static String hex(byte[] d, int off, int n) {
        StringBuilder sb = new StringBuilder(n * 2);
        for (int i = 0; i < n; i++) sb.append(String.format("%02x", d[off + i] & 0xFF));
        return sb.toString();
    }

    private static byte[] unhex(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++)
            b[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return b;
    }

    private static void log(String m) { System.out.println("[" + new Date() + "] " + m); }

    private static final class Peer {
        final String id;
        volatile InetAddress addr;
        volatile int port;
        volatile long lastSeen;
        Peer(String id, InetAddress addr, int port, long lastSeen) {
            this.id = id; this.addr = addr; this.port = port; this.lastSeen = lastSeen;
        }
    }
}
