package net.ganyusbathwater.oririmod.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class SaveGridCommand {

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("savegrid")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                        .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                .then(Commands.argument("basename", StringArgumentType.word())
                                        .executes(ctx -> execute(ctx, 48))
                                        .then(Commands.argument("grid_size", IntegerArgumentType.integer(1, 1024))
                                                .executes(ctx -> execute(ctx, IntegerArgumentType.getInteger(ctx, "grid_size")))
                                        )
                                )
                        )
                );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, int gridSize) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        BlockPos pos1 = BlockPosArgument.getLoadedBlockPos(ctx, "pos1");
        BlockPos pos2 = BlockPosArgument.getLoadedBlockPos(ctx, "pos2");
        String basename = StringArgumentType.getString(ctx, "basename");

        ServerLevel level = source.getLevel();
        StructureTemplateManager manager = level.getServer().getStructureManager();

        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());

        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        int height = maxY - minY + 1;
        int count = 0;

        for (int x = minX; x <= maxX; x += gridSize) {
            for (int z = minZ; z <= maxZ; z += gridSize) {
                int currentSizeX = Math.min(gridSize, maxX - x + 1);
                int currentSizeZ = Math.min(gridSize, maxZ - z + 1);

                BlockPos currentOrigin = new BlockPos(x, minY, z);
                BlockPos currentSize = new BlockPos(currentSizeX, height, currentSizeZ);

                ResourceLocation templateId = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, basename + "_" + (x - minX) + "_" + (z - minZ));
                StructureTemplate template = manager.getOrCreate(templateId);

                template.fillFromWorld(level, currentOrigin, currentSize, true, Blocks.STRUCTURE_VOID);
                manager.save(templateId);

                count++;
            }
        }

        final int finalCount = count;
        source.sendSuccess(() -> Component.literal("Saved " + finalCount + " structure files successfully for " + basename + "!"), true);
        return count;
    }
}
