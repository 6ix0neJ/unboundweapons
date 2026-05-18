package org.minitype.mcmodstest;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.literal;


public class Mcmodstest implements ModInitializer {

    @Override
    public void onInitialize() {
        System.out.println("Hello World Mod Initialized");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(

                    literal("hello").executes(commandContext -> {
                        commandContext.getSource().sendMessage(Text.literal("world!").formatted(Formatting.AQUA));
                        return 1;
                    })

            );
        });
    }
}
