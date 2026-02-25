package net.ganyusbathwater.oririmod.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class SpawnAoEIndicatorPacket {
    private final BlockPos center;
    private final float radius;
    private final int durationTicks;
    private final int argbColor;

    public SpawnAoEIndicatorPacket(BlockPos center, float radius, int durationTicks, int argbColor) {
        this.center = center;
        this.radius = radius;
        this.durationTicks = durationTicks;
        this.argbColor = argbColor;
    }

    public static void encode(SpawnAoEIndicatorPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.center);
        buf.writeFloat(pkt.radius);
        buf.writeInt(pkt.durationTicks);
        buf.writeInt(pkt.argbColor);
    }

    public static SpawnAoEIndicatorPacket decode(FriendlyByteBuf buf) {
        return new SpawnAoEIndicatorPacket(
                buf.readBlockPos(),
                buf.readFloat(),
                buf.readInt(),
                buf.readInt());
    }

    public BlockPos getCenter() {
        return center;
    }

    public float getRadius() {
        return radius;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public int getArgbColor() {
        return argbColor;
    }
}
