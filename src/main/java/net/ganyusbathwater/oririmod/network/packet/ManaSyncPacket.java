package net.ganyusbathwater.oririmod.network.packet;

import net.minecraft.network.FriendlyByteBuf;

public class ManaSyncPacket {
    private final int mana;
    private final int maxMana;

    public ManaSyncPacket(int mana, int maxMana) {
        this.mana = mana;
        this.maxMana = maxMana;
    }

    // Bequemlichkeit: nur Mana (max wird serverseitig ermittelt)
    public ManaSyncPacket(int mana) {
        this(mana, -1);
    }

    public static void encode(ManaSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.mana);
        buf.writeInt(pkt.maxMana);
    }

    public static ManaSyncPacket decode(FriendlyByteBuf buf) {
        int mana = buf.readInt();
        int max = buf.readInt();
        return new ManaSyncPacket(mana, max);
    }

    public int getMana() {
        return mana;
    }

    public int getMaxMana() {
        return maxMana;
    }
}