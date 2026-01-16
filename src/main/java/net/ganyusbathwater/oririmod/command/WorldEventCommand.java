package net.ganyusbathwater.oririmod.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.events.world.WorldEventManager;
import net.ganyusbathwater.oririmod.events.world.WorldEventType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public final class WorldEventCommand {

    private static final int DEFAULT_DURATION_TICKS = 12000; // approx. 10 minutes

    private WorldEventCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(register());
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("worldevent")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("start")
                        .then(Commands.argument("event", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        // Suggest all event types except NONE
                                        Arrays.stream(WorldEventType.values())
                                                .filter(type -> type != WorldEventType.NONE)
                                                .map(Enum::name)
                                                .map(String::toLowerCase),
                                        builder))
                                .executes(WorldEventCommand::startEvent)))
                .then(Commands.literal("stop")
                        .executes(WorldEventCommand::stopEvent));
    }

    private static int startEvent(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        WorldEventManager manager = WorldEventManager.get(level);
        String eventName = StringArgumentType.getString(context, "event").toUpperCase();

        try {
            WorldEventType eventType = WorldEventType.valueOf(eventName);
            if (eventType == WorldEventType.NONE) {
                context.getSource().sendFailure(Component.literal("The event 'NONE' cannot be started manually."));
                return 0;
            }

            manager.startEvent(eventType, DEFAULT_DURATION_TICKS, level);
            context.getSource().sendSuccess(() -> Component.literal("Event '" + eventName.toLowerCase() + "' started."), true);
            return 1;
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("Unknown event: " + eventName.toLowerCase()));
            return 0;
        }
    }

    private static int stopEvent(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        WorldEventManager manager = WorldEventManager.get(level);

        if (manager.getActiveEvent() == WorldEventType.NONE) {
            context.getSource().sendFailure(Component.literal("There is no active event to stop."));
            return 0;
        }

        String eventName = manager.getActiveEvent().name().toLowerCase();
        manager.stopEvent(level);
        context.getSource().sendSuccess(() -> Component.literal("Event '" + eventName + "' has been stopped."), true);
        return 1;
    }
}