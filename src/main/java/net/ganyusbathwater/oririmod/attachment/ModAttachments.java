package net.ganyusbathwater.oririmod.attachment;

import net.ganyusbathwater.oririmod.OririMod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = 
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, OririMod.MOD_ID);

    public static final Supplier<AttachmentType<List<UUID>>> ACTIVE_SUMMONS = 
        ATTACHMENT_TYPES.register("active_summons", 
            () -> AttachmentType.builder(() -> (List<UUID>) new ArrayList<UUID>()).build());
}
