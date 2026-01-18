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
import net.minecraft.world.level.Level;
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
        return Commands.literal("oriri")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("worldevent")
                        .then(Commands.literal("start")
                                .then(Commands.argument("event", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(WorldEventType.values())
                                                        .filter(type -> type != WorldEventType.NONE)
                                                        .map(Enum::name)
                                                        .map(String::toLowerCase),
                                                builder))
                                        .executes(WorldEventCommand::startEvent)))
                        .then(Commands.literal("stop")
                                .executes(WorldEventCommand::stopEvent)));
    }

    private static int startEvent(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();

        // Dimension check: events only allowed in Overworld
        if (level.dimension() != Level.OVERWORLD) {
            context.getSource().sendFailure(Component.literal("Events can only be started in the Overworld — you are likely in a different dimension."));
            return 0;
        }

        WorldEventManager manager = WorldEventManager.get(level);
        String eventName = StringArgumentType.getString(context, "event").toUpperCase();

        try {
            WorldEventType eventType = WorldEventType.valueOf(eventName);
            if (eventType == WorldEventType.NONE) {
                context.getSource().sendFailure(Component.literal("The 'NONE' event cannot be started manually."));
                return 0;
            }

            boolean started = manager.startEvent(eventType, DEFAULT_DURATION_TICKS, level);
            if (!started) {
                context.getSource().sendFailure(Component.literal("Event cannot be started right now (wrong time or another event is active)."));
                return 0;
            }

            context.getSource().sendSuccess(() -> Component.literal("Event '" + eventName.toLowerCase() + "' started."), true);
            return 1;
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("Unknown event: " + eventName.toLowerCase()));
            return 0;
        }
    }

    private static int stopEvent(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();

        // If player is not in Overworld but an event is active, inform them
        if (level.dimension() != Level.OVERWORLD && WorldEventManager.isAnyEventActive()) {
            context.getSource().sendFailure(Component.literal("An event is running in the Overworld; you are in a different dimension."));
            return 0;
        }

        WorldEventManager manager = WorldEventManager.get(level);

        if (WorldEventManager.getActiveEvent() == WorldEventType.NONE) {
            context.getSource().sendFailure(Component.literal("No active event to stop."));
            return 0;
        }

        String eventName = WorldEventManager.getActiveEvent().name().toLowerCase();
        manager.stopEvent(level);
        context.getSource().sendSuccess(() -> Component.literal("Event '" + eventName + "' has been stopped."), true);
        return 1;
    }
}