package net.ganyusbathwater.oririmod.config;

import net.ganyusbathwater.oririmod.util.SummonTargetMode;
import net.neoforged.neoforge.common.ModConfigSpec;

public class OririConfig {
    public static final ModConfigSpec SPEC;
    public static final Common COMMON;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        COMMON = new Common(b);
        SPEC = b.build();
    }

    public static class Common {
        public final Mana mana;
        public final WorldEvents worldEvents;
        public final Summoner summoner;
        public final NaturalDisasters naturalDisasters;

        Common(ModConfigSpec.Builder b) {
            // Mana section
            b.comment("Mana configuration").push("mana");
            mana = new Mana(b);
            b.pop();

            // World events section
            b.comment("World Event configuration").push("world_events");
            worldEvents = new WorldEvents(b);
            b.pop();

            // Summoner section
            b.comment("Summoner Weapon configuration").push("summoner");
            summoner = new Summoner(b);
            b.pop();

            // Natural disasters section
            b.comment("Natural Disaster configuration (Meteor Shower, Earthquake, Lightning Storm, Tornado, Snowstorm)")
                    .push("natural_disasters");
            naturalDisasters = new NaturalDisasters(b);
            b.pop();
        }
    }

    public static class Mana {
        public final ModConfigSpec.IntValue regenIntervalSeconds;
        public final ModConfigSpec.IntValue regenAmount;
        public final ModConfigSpec.IntValue maxMana;

        Mana(ModConfigSpec.Builder b) {
            regenIntervalSeconds = b
                    .comment("Seconds between each mana regeneration step (server-side)")
                    .defineInRange("regenIntervalSeconds", 5, 1, 600);
            regenAmount = b
                    .comment("Amount of mana added every regen interval")
                    .defineInRange("regenAmount", 1, 0, 100);
            maxMana = b
                    .comment("Maximum mana value")
                    .defineInRange("maxMana", 100, 1, 10000);
        }
    }

    public static class WorldEvents {
        public final ModConfigSpec.DoubleValue nightEventChance;
        public final ModConfigSpec.DoubleValue eclipseChance;
        public final ModConfigSpec.IntValue nightEventDuration;
        public final ModConfigSpec.IntValue eclipseDuration;
        public final ModConfigSpec.EnumValue<net.ganyusbathwater.oririmod.util.ShaderCompatMode> overrideVanillaSkyColorForShaders;

        WorldEvents(ModConfigSpec.Builder b) {
            nightEventChance = b
                    .comment(
                            "Chance for a night event (Blood Moon, Green Moon) to occur at the beginning of a night. (0.0 to 1.0)")
                    .defineInRange("nightEventChance", 0.10, 0.0, 1.0);
            eclipseChance = b
                    .comment("Chance for an eclipse to occur during the day. (0.0 to 1.0)")
                    .defineInRange("eclipseChance", 0.05, 0.0, 1.0);
            nightEventDuration = b
                    .comment("Duration of night events in ticks (e.g., Blood Moon). 12000 ticks = 10 minutes.")
                    .defineInRange("nightEventDuration", 12000, 600, 24000);
            eclipseDuration = b
                    .comment("Duration of an eclipse in ticks. 6000 ticks = 5 minutes.")
                    .defineInRange("eclipseDuration", 6000, 600, 24000);
            overrideVanillaSkyColorForShaders = b
                    .comment("Override Vanilla Sky Color to support shader packs during celestial events.", "ON: Always override", "OFF: Never override", "DYNAMIC: Only override when a shader pack is actively enabled")
                    .defineEnum("overrideVanillaSkyColorForShaders", net.ganyusbathwater.oririmod.util.ShaderCompatMode.DYNAMIC);
        }
    }

    /**
     * Settings shared by every natural disaster plus one sub-section per disaster.
     * Leaf names are prefixed per disaster because NeoForge derives the config-screen
     * translation key ("oririmod.configuration.LEAF") from the leaf name only.
     */
    public static class NaturalDisasters {
        public final ModConfigSpec.BooleanValue disastersEnabled;
        public final ModConfigSpec.BooleanValue disasterAnnouncements;

        public final DisasterSettings meteorShower;
        public final ModConfigSpec.IntValue meteorShowerFreshAreaRadius;
        public final ModConfigSpec.IntValue meteorShowerMaxInhabitedTicks;
        public final ModConfigSpec.BooleanValue meteorShowerDestroyBlocks;
        public final ModConfigSpec.BooleanValue meteorShowerAvoidVillages;
        public final ModConfigSpec.IntValue meteorShowerSmallWeight;
        public final ModConfigSpec.IntValue meteorShowerMediumWeight;
        public final ModConfigSpec.IntValue meteorShowerBigWeight;

        public final DisasterSettings earthquake;
        public final ModConfigSpec.IntValue earthquakeFreshAreaRadius;
        public final ModConfigSpec.IntValue earthquakeMaxInhabitedTicks;
        public final ModConfigSpec.BooleanValue earthquakeDestroyBlocks;
        public final ModConfigSpec.IntValue earthquakeCrackLength;
        public final ModConfigSpec.IntValue earthquakeCrackDepth;
        public final ModConfigSpec.DoubleValue earthquakeShakeIntensity;
        public final DisasterSettings lightningStorm;
        public final ModConfigSpec.IntValue lightningStormFreshAreaRadius;
        public final ModConfigSpec.IntValue lightningStormMaxInhabitedTicks;
        public final ModConfigSpec.BooleanValue lightningStormStrikesCreateFire;
        public final ModConfigSpec.IntValue lightningStormStrikeInterval;
        public final ModConfigSpec.BooleanValue lightningStormSuperchargeCreepers;
        public final DisasterSettings tornado;
        public final ModConfigSpec.IntValue tornadoFreshAreaRadius;
        public final ModConfigSpec.IntValue tornadoMaxInhabitedTicks;
        public final ModConfigSpec.BooleanValue tornadoDestroyBlocks;
        public final ModConfigSpec.DoubleValue tornadoMaxBlockHardness;
        public final ModConfigSpec.IntValue tornadoSuctionRadius;
        public final ModConfigSpec.DoubleValue tornadoKnockbackResistanceThreshold;
        public final ModConfigSpec.DoubleValue tornadoShakeIntensity;
        public final DisasterSettings snowstorm;
        public final ModConfigSpec.DoubleValue snowstormExposureSpeed;
        public final ModConfigSpec.IntValue snowstormLeakTolerance;
        public final ModConfigSpec.IntValue snowstormHeatSourceMinLight;
        public final ModConfigSpec.IntValue snowstormCaveDepthThreshold;

        NaturalDisasters(ModConfigSpec.Builder b) {
            disastersEnabled = b
                    .comment("Master switch. If false, no natural disaster will ever start on its own (the debug tool can still force them).")
                    .define("disastersEnabled", true);
            disasterAnnouncements = b
                    .comment("Show a chat message to players when a natural disaster starts or ends.")
                    .define("disasterAnnouncements", true);

            b.comment("Meteor Shower: meteors rain down around players who are travelling through freshly explored land.")
                    .push("meteor_shower");
            meteorShower = new DisasterSettings(b, "meteorShower", 0.025, 1800);
            meteorShowerFreshAreaRadius = b
                    .comment("Radius (in blocks) around a player that must consist of freshly explored chunks for meteors to fall.",
                            "Vanilla only accumulates a chunk's 'inhabited time' within 128 blocks of a player, so 128 matches vanilla exactly.")
                    .defineInRange("meteorShowerFreshAreaRadius", 128, 32, 256);
            meteorShowerMaxInhabitedTicks = b
                    .comment("Maximum 'inhabited time' (ticks players spent nearby) any chunk in that radius may have.",
                            "If a single chunk exceeds this value the area counts as a home/base and NO meteor falls there. 6000 ticks = 5 minutes.")
                    .defineInRange("meteorShowerMaxInhabitedTicks", 6000, 0, 1_000_000);
            meteorShowerDestroyBlocks = b
                    .comment("If false, meteors still explode and damage entities but leave no crater.")
                    .define("meteorShowerDestroyBlocks", true);
            meteorShowerAvoidVillages = b
                    .comment("If true, meteors never target villages.")
                    .define("meteorShowerAvoidVillages", true);
            meteorShowerSmallWeight = b
                    .comment("Relative spawn weight of small meteors (small crater, no star flare).")
                    .defineInRange("meteorShowerSmallWeight", 50, 0, 1000);
            meteorShowerMediumWeight = b
                    .comment("Relative spawn weight of medium meteors (medium crater, star flare and impact pillar).")
                    .defineInRange("meteorShowerMediumWeight", 35, 0, 1000);
            meteorShowerBigWeight = b
                    .comment("Relative spawn weight of big meteors (large crater, star flare, impact pillar and black & white impact frame).")
                    .defineInRange("meteorShowerBigWeight", 15, 0, 1000);
            b.pop();

            b.comment("Earthquake: tears natural-looking cracks into the surface.").push("earthquake");
            earthquake = new DisasterSettings(b, "earthquake", 0.02, 1200);
            earthquakeFreshAreaRadius = b
                    .comment("Radius (in blocks) around a player that must consist of freshly explored chunks for earthquakes to trigger.",
                            "Default is 128 blocks to match vanilla player chunk loading.")
                    .defineInRange("earthquakeFreshAreaRadius", 128, 32, 256);
            earthquakeMaxInhabitedTicks = b
                    .comment("Maximum inhabited time any chunk in that radius may have before being classified as a base/home.",
                            "6000 ticks = 5 minutes.")
                    .defineInRange("earthquakeMaxInhabitedTicks", 6000, 0, 1_000_000);
            earthquakeDestroyBlocks = b
                    .comment("If false, the earthquake rumbles the ground, plays particles and shakes the camera, but does not carve blocks.")
                    .define("earthquakeDestroyBlocks", true);
            earthquakeCrackLength = b
                    .comment("Base length of the main earthquake fissure in blocks.")
                    .defineInRange("earthquakeCrackLength", 130, 30, 300);
            earthquakeCrackDepth = b
                    .comment("Maximum depth of the central part of the fissure in blocks.")
                    .defineInRange("earthquakeCrackDepth", 65, 15, 120);
            earthquakeShakeIntensity = b
                    .comment("Camera shake strength multiplier for earthquakes (0.0 to disable camera shake).")
                    .defineInRange("earthquakeShakeIntensity", 1.0, 0.0, 5.0);
            b.pop();

            b.comment("Lightning Storm: a heavy amount of lightning strikes the surrounding area.").push("lightning_storm");
            lightningStorm = new DisasterSettings(b, "lightningStorm", 0.025, 2400);
            lightningStormFreshAreaRadius = b
                    .comment("Radius (in blocks) around a player that must consist of freshly explored chunks for ground strikes to target them.",
                            "Default is 128 blocks to match vanilla player chunk loading.")
                    .defineInRange("lightningStormFreshAreaRadius", 128, 32, 256);
            lightningStormMaxInhabitedTicks = b
                    .comment("Maximum inhabited time any chunk in that radius may have before being classified as a base/home.",
                            "6000 ticks = 5 minutes.")
                    .defineInRange("lightningStormMaxInhabitedTicks", 6000, 0, 1_000_000);
            lightningStormStrikesCreateFire = b
                    .comment("If false, lightning strikes are visual and damaging to entities but do not ignite fire on blocks.")
                    .define("lightningStormStrikesCreateFire", true);
            lightningStormStrikeInterval = b
                    .comment("Base interval in ticks between lightning strikes near active players (modulated by intensity).")
                    .defineInRange("lightningStormStrikeInterval", 8, 2, 60);
            lightningStormSuperchargeCreepers = b
                    .comment("If true, lightning strikes can charge creepers into charged creepers normally.")
                    .define("lightningStormSuperchargeCreepers", true);
            b.pop();

            b.comment("Tornado: wanders through the world, tears out soft blocks and lifts light entities.").push("tornado");
            tornado = new DisasterSettings(b, "tornado", 0.015, 2400);
            tornadoFreshAreaRadius = b
                    .comment("Radius (in blocks) around a player that must consist of freshly explored chunks for tornadoes to spawn near them.",
                            "Default is 128 blocks to match vanilla player chunk loading.")
                    .defineInRange("tornadoFreshAreaRadius", 128, 32, 256);
            tornadoMaxInhabitedTicks = b
                    .comment("Maximum inhabited time any chunk in that radius may have before being classified as a base/home.",
                            "6000 ticks = 5 minutes.")
                    .defineInRange("tornadoMaxInhabitedTicks", 6000, 0, 1_000_000);
            tornadoDestroyBlocks = b
                    .comment("If false, the tornado pulls entities and creates wind/debris visuals but does not destroy blocks.")
                    .define("tornadoDestroyBlocks", true);
            tornadoMaxBlockHardness = b
                    .comment("Maximum hardness of blocks the tornado can tear apart. Soft blocks like foliage (0.2), dirt/grass (0.5-0.6), glass (0.3) and wood (2.0) are affected; stone and structures are spared.")
                    .defineInRange("tornadoMaxBlockHardness", 1.5, 0.0, 10.0);
            tornadoSuctionRadius = b
                    .comment("Radius in blocks around the tornado in which entities are pulled toward the vortex.")
                    .defineInRange("tornadoSuctionRadius", 24, 8, 64);
            tornadoKnockbackResistanceThreshold = b
                    .comment("Entities with Knockback Resistance at or above this threshold resist being lifted into the funnel.")
                    .defineInRange("tornadoKnockbackResistanceThreshold", 0.75, 0.0, 1.0);
            tornadoShakeIntensity = b
                    .comment("Camera shake strength multiplier when near a tornado (0.0 to disable camera shake).")
                    .defineInRange("tornadoShakeIntensity", 1.0, 0.0, 5.0);
            b.pop();

            b.comment("Snowstorm: a freezing blizzard covering cold biomes.").push("snowstorm");
            snowstorm = new DisasterSettings(b, "snowstorm", 0.04, 3600);
            snowstormExposureSpeed = b
                    .comment("Multiplier for how fast Cold Aura exposure stacks build up when exposed to the snowstorm.")
                    .defineInRange("snowstormExposureSpeed", 1.0, 0.2, 5.0);
            snowstormLeakTolerance = b
                    .comment("Maximum number of sky-exposed block openings reachable within 5 blocks of a player for a roofed house to remain sheltered.",
                            "A value of 2 allows normal doors, windows, and chimneys without compromising the house.")
                    .defineInRange("snowstormLeakTolerance", 2, 0, 10);
            snowstormHeatSourceMinLight = b
                    .comment("Minimum block light level (from campfires, torches, lanterns, furnaces) that creates a safe warm micro-climate under a roof.")
                    .defineInRange("snowstormHeatSourceMinLight", 11, 0, 15);
            snowstormCaveDepthThreshold = b
                    .comment("Number of blocks beneath the surface heightmap where underground caves are protected from surface blizzard winds.")
                    .defineInRange("snowstormCaveDepthThreshold", 8, 2, 32);
            b.pop();
        }
    }

    /** Settings every disaster has: on/off, natural chance, duration and intensity. */
    public static class DisasterSettings {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.DoubleValue chance;
        public final ModConfigSpec.IntValue duration;
        public final ModConfigSpec.DoubleValue intensity;

        DisasterSettings(ModConfigSpec.Builder b, String prefix, double defaultChance, int defaultDuration) {
            enabled = b
                    .comment("If false, this disaster never starts on its own.")
                    .define(prefix + "Enabled", true);
            chance = b
                    .comment("Chance (0.0 to 1.0) that this disaster starts when it is rolled (once per in-game day).")
                    .defineInRange(prefix + "Chance", defaultChance, 0.0, 1.0);
            duration = b
                    .comment("Duration of the disaster in ticks (20 ticks = 1 second).")
                    .defineInRange(prefix + "Duration", defaultDuration, 200, 24000);
            intensity = b
                    .comment("Intensity multiplier. 1.0 = default, 0.5 = half as strong/frequent, 2.0 = twice as strong/frequent.")
                    .defineInRange(prefix + "Intensity", 1.0, 0.1, 5.0);
        }
    }

    public static class Summoner {
        public final ModConfigSpec.EnumValue<SummonTargetMode> targetMode;

        Summoner(ModConfigSpec.Builder b) {
            targetMode = b
                    .comment("Target mode for summoned mobs.",
                            "PVP = attacks everything including other players (except the summoner)",
                            "ALL_MOBS = attacks all mobs (hostile, neutral, passive) but never players",
                            "HOSTILE_ONLY = attacks only hostile mobs (monsters)")
                    .defineEnum("targetMode", SummonTargetMode.HOSTILE_ONLY);
        }
    }
}
