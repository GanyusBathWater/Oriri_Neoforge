package net.ganyusbathwater.oririmod.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public final class ManaCommands {
    private ManaCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(register());
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("mana")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100000))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    ModManaUtil.setMana(p, IntegerArgumentType.getInteger(ctx, "value"));
                                    ctx.getSource().sendSuccess(() -> Component.literal("Mana gesetzt auf " + ModManaUtil.getMana(p)), true);
                                    return 1;
                                }))
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    ModManaUtil.resetMana(p);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Mana zurückgesetzt auf " + ModManaUtil.getMana(p)), true);
                                    return 1;
                                })))
                .then(Commands.literal("setmax")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 100000))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    ModManaUtil.setMaxMana(p, IntegerArgumentType.getInteger(ctx, "value"));
                                    ctx.getSource().sendSuccess(() -> Component.literal("MaxMana gesetzt auf " + ModManaUtil.getMaxMana(p)), true);
                                    return 1;
                                }))
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    ModManaUtil.resetMaxMana(p);
                                    ctx.getSource().sendSuccess(() -> Component.literal("MaxMana zurückgesetzt auf " + ModManaUtil.getMaxMana(p) + " (Config)"), true);
                                    return 1;
                                })))
                .then(Commands.literal("setregen")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    ModManaUtil.setRegenIntervalSeconds(p, IntegerArgumentType.getInteger(ctx, "seconds"));
                                    ctx.getSource().sendSuccess(() -> Component.literal("Regen-Intervall gesetzt auf " + ModManaUtil.getRegenIntervalSeconds(p) + "s"), true);
                                    return 1;
                                }))
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    ModManaUtil.resetRegenIntervalSeconds(p);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Regen-Intervall zurückgesetzt auf " + ModManaUtil.getRegenIntervalSeconds(p) + "s (Config)"), true);
                                    return 1;
                                })));
    }
}