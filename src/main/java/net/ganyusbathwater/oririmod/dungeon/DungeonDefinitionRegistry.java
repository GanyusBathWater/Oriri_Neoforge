package net.ganyusbathwater.oririmod.dungeon;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Central registry for all DungeonDefinitions.
 * Populated at mod startup (FMLCommonSetupEvent).
 *
 * Each dungeon needs a unique string ID (e.g. "volcano_dungeon"),
 * a display name, description, a dimension key, and a structure NBT path.
 */
public class DungeonDefinitionRegistry {

    private static final Map<String, DungeonDefinition> REGISTRY = new HashMap<>();

    public static void register(DungeonDefinition definition) {
        REGISTRY.put(definition.id(), definition);
    }

    @Nullable
    public static DungeonDefinition get(String id) {
        return REGISTRY.get(id);
    }

    public static Collection<DungeonDefinition> all() {
        return REGISTRY.values();
    }

    /** Called during FMLCommonSetupEvent to populate built-in definitions. */
    public static void init() {
        register(new DungeonDefinition(
                "tutorial_crawl",
                "Tutorial Crawl",
                "Learn the basics of combat and exploration.",
                "[Placeholder] The old training grounds beneath the Keeper's Watch have been overrun. Clear the halls and prove yourself worthy.",
                ResourceLocation.fromNamespaceAndPath("oririmod", "textures/gui/dungeon/tutorial_crawl.png"),
                DungeonDimensions.OVERWORLD_DAY,
                ResourceLocation.fromNamespaceAndPath("oririmod", "tutorial_dungeon")
        ));

        register(new DungeonDefinition(
                "excavation_site_six",
                "Excavation Site VI",
                "Classification: Subterranean Ruin / High-Value Resource Site",
                "Location Designation: Excavation Site VI\n\nClassification: Subterranean Ruin / High-Value Resource Site\n\nGeographic Profile: Concealed within the interior of a standard mountain formation.\n\nHistorical Context: The site contains the remains of a pre-cataclysmic civilization holding significant wealth and artifacts. The structural integrity of the city was compromised by extreme localized dark magic, resulting in severe spatial distortion.\n\nCurrent Status: The entire architectural layout is inverted. Gravity and spatial logic are inconsistent. Currently targeted by Noxian High Command; a Sage has been dispatched for artifact retrieval and has weaponized the subterranean flora.\n\nTactical Directive: Navigate spatial anomalies with caution. Apprehend the target alive.",
                ResourceLocation.fromNamespaceAndPath("oririmod", "textures/gui/dungeon/excavation_site_six.png"),
                DungeonDimensions.OVERWORLD_NIGHT,
                ResourceLocation.fromNamespaceAndPath("oririmod", "excavation_site_six")
        ));
    }
}
