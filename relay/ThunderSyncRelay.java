import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * ThunderSync relay server.
 *
 * Dependency-free TCP relay for ThunderHack-Immortal client sync (e.g. PenisSync).
 * Clients connect, send newline-delimited JSON lines, and every line is
 * re-broadcast to all OTHER clients that share the same "room" field.
 *
 * The server never interprets the payload beyond the "room" key — it is a dumb
 * relay, so the client controls the schema (uuid / on / etc.).
 *
 * Build:  javac ThunderSyncRelay.java
 * Run:    java ThunderSyncRelay [port]        (default port 7777)
 *
 * Run it on your VPS (e.g. inside tmux/screen or as a systemd service) and point
 * the in-game PenisSync module at  <vps-ip>:<port>  with a shared room key.
 */
public final class ThunderSyncRelay {
    private static final Pattern ROOM = Pattern.compile("\"room\"\\s*:\\s*\"([^\"]*)\"");
    // room -> set of connected clients
    private static final Map<String, Set<Client>> ROOMS = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        int port = 7777;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); }
            catch (NumberFormatException e) { System.err.println("Bad port, using 7777"); }
        }

        ServerSocket server = new ServerSocket(port);
        log("ThunderSync relay listening on port " + port);

        while (true) {
            Socket socket = server.accept();
            Client client = new Client(socket);
            Thread t = new Thread(client, "client-" + socket.getRemoteSocketAddress());
            t.setDaemon(true);
            t.start();
        }
    }

    private static String extractRoom(String line) {
        Matcher m = ROOM.matcher(line);
        return m.find() ? m.group(1) : null;
    }

    private static void broadcast(Client from, String room, String line) {
        Set<Client> peers = ROOMS.get(room);
        if (peers == null) return;
        for (Client c : peers) {
            if (c == from) continue;
            c.send(line);
        }
    }

    private static void join(Client c, String room) {
        c.room = room;
        ROOMS.computeIfAbsent(room, k -> ConcurrentHashMap.newKeySet()).add(c);
        log("join room='" + room + "' peers=" + ROOMS.get(room).size() + " from " + c.addr);
    }

    private static void leave(Client c) {
        if (c.room == null) return;
        Set<Client> peers = ROOMS.get(c.room);
        if (peers != null) {
            peers.remove(c);
            // tell remaining peers this uuid went offline so they stop rendering
            if (c.uuid != null)
                broadcast(c, c.room, "{\"room\":\"" + c.room + "\",\"uuid\":\"" + c.uuid + "\",\"on\":false}");
            if (peers.isEmpty()) ROOMS.remove(c.room);
        }
        log("leave room='" + c.room + "' from " + c.addr);
    }

    private static void log(String msg) {
        System.out.println("[" + new Date() + "] " + msg);
    }

    private static final class Client implements Runnable {
        private final Socket socket;
        private final String addr;
        private volatile BufferedWriter out;
        private volatile String room;
        private volatile String uuid;

        Client(Socket socket) {
            this.socket = socket;
            this.addr = String.valueOf(socket.getRemoteSocketAddress());
        }

        @Override
        public void run() {
            try {
                socket.setTcpNoDelay(true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    String room = extractRoom(line);
                    if (room == null) continue;

                    if (this.room == null) join(this, room);

                    Matcher u = Pattern.compile("\"uuid\"\\s*:\\s*\"([^\"]*)\"").matcher(line);
                    if (u.find()) this.uuid = u.group(1);

                    broadcast(this, room, line);
                }
            } catch (IOException ignored) {
                // client dropped
            } finally {
                leave(this);
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        void send(String line) {
            BufferedWriter w = out;
            if (w == null) return;
            try {
                synchronized (w) {
                    w.write(line);
                    w.write('\n');
                    w.flush();
                }
            } catch (IOException e) {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}
