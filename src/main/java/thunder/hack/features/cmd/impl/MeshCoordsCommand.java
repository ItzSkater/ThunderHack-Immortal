package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.cmd.Command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class MeshCoordsCommand extends Command {
    public MeshCoordsCommand() {
        super("mpos", "mcoords");
    }

    @Override
    public void executeBuild(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (!ModuleManager.mesh.isEnabled()) {
                sendMessage("Enable the Mesh module first");
                return SINGLE_SUCCESS;
            }
            ModuleManager.mesh.shareCoords();
            return SINGLE_SUCCESS;
        });
    }
}
