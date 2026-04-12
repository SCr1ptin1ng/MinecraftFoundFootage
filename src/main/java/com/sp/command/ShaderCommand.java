package com.sp.command;

import com.sp.SPBRevampedClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

public class ShaderCommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("shaders")
                        .then(ClientCommandManager.literal("on")
                                .executes(context -> {
                                    SPBRevampedClient.setShadersEnabled(true);
                                    context.getSource().sendFeedback(Text.literal("Shaders enabled"));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("off")
                                .executes(context -> {
                                    SPBRevampedClient.setShadersEnabled(false);
                                    context.getSource().sendFeedback(Text.literal("Shaders disabled"));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("toggle")
                                .executes(context -> {
                                    boolean enabled = SPBRevampedClient.toggleShadersEnabled();
                                    context.getSource().sendFeedback(Text.literal(enabled ? "Shaders enabled" : "Shaders disabled"));
                                    return 1;
                                }))
        ));
    }
}
