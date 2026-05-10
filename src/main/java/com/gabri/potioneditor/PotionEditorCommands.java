package com.gabri.potioneditor;

import com.mojang.brigadier.CommandDispatcher;
import com.gabri.potioneditor.network.PotionEditorNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PotionEditorCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("potionedit")
            .requires(source -> source.hasPermission(2))
            .executes(ctx -> {
                if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                    ctx.getSource().sendFailure(Component.literal("Only a player can open the Potion Editor."));
                    return 0;
                }

                PotionEditorNetwork.openCatalog(player);
                ctx.getSource().sendSuccess(() -> Component.literal("Potion Editor opened."), false);
                return 1;
            }));
    }
}
