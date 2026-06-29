package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.cmd.Command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class MeshChatCommand extends Command {
    public MeshChatCommand() {
        super("mc", "mchat");
    }

    @Override
    public void executeBuild(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(arg("text", StringArgumentType.greedyString()).executes(context -> {
            String text = context.getArgument("text", String.class);
            if (!ModuleManager.mesh.isEnabled()) {
                sendMessage("Enable the Mesh module first");
                return SINGLE_SUCCESS;
            }
            ModuleManager.mesh.sendChat(text);
            return SINGLE_SUCCESS;
        }));
    }
}
