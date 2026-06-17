package net.ganyusbathwater.oririmod;

import net.minecraft.world.level.chunk.ChunkAccess;

public class Test {
    public void test(ChunkAccess c) {
        c.getAllStarts();
        c.getAllReferences();
        c.hasAnyStructureReferences();
    }
}
