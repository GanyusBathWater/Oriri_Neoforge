import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class GenerateRecipes {
    public static void main(String[] args) throws IOException {
        String baseDir = "src/main/resources/data/oririmod/recipe";
        new File(baseDir).mkdirs();

        // Looting
        for (int i = 1; i <= 5; i++) {
            String json = """
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "A": { "item": "oririmod:iron_stick" },
    "B": { "item": "minecraft:echo_shard" },
    "C": { "item": "oririmod:mana_manifestation" },
    "L": { "item": "minecraft:leather" },
    "E": {
      "type": "neoforge:components",
      "components": {
        "minecraft:stored_enchantments": {
          "levels": { "minecraft:looting": %d }
        }
      },
      "item": "minecraft:enchanted_book"
    }
  },
  "pattern": [
    "ALA",
    "BEB",
    "ACA"
  ],
  "result": {
    "count": 1,
    "id": "oririmod:soul_harvester_looting_upgrade",
    "components": {
      "minecraft:custom_data": "{oriri_level:%d}"
    }
  }
}""".formatted(i, i);
            write(baseDir + "/soul_harvester_looting_upgrade_" + i + ".json", json);
        }

        // Speed
        for (int i = 1; i <= 3; i++) {
            String json = """
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "A": { "item": "oririmod:iron_stick" },
    "B": { "item": "minecraft:echo_shard" },
    "C": { "item": "oririmod:mana_manifestation" },
    "P": [
      { "type": "neoforge:components", "components": { "minecraft:potion_contents": { "potion": "minecraft:swiftness" } }, "item": "minecraft:potion" },
      { "type": "neoforge:components", "components": { "minecraft:potion_contents": { "potion": "minecraft:swiftness" } }, "item": "minecraft:splash_potion" },
      { "type": "neoforge:components", "components": { "minecraft:potion_contents": { "potion": "minecraft:swiftness" } }, "item": "minecraft:lingering_potion" }
    ],
    "E": {
      "type": "neoforge:components",
      "components": {
        "minecraft:stored_enchantments": {
          "levels": { "minecraft:soul_speed": %d }
        }
      },
      "item": "minecraft:enchanted_book"
    }
  },
  "pattern": [
    "APA",
    "BEB",
    "ACA"
  ],
  "result": {
    "count": 1,
    "id": "oririmod:soul_harvester_speed_upgrade",
    "components": {
      "minecraft:custom_data": "{oriri_level:%d}"
    }
  }
}""".formatted(i, i);
            write(baseDir + "/soul_harvester_speed_upgrade_" + i + ".json", json);
        }

        // Fire Aspect
        String fireJson = """
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "A": { "item": "oririmod:iron_stick" },
    "B": { "item": "minecraft:echo_shard" },
    "C": { "item": "oririmod:mana_manifestation" },
    "F": { "item": "oririmod:fire_crystal" },
    "E": {
      "type": "neoforge:components",
      "components": {
        "minecraft:stored_enchantments": {
          "levels": { "minecraft:fire_aspect": 1 }
        }
      },
      "item": "minecraft:enchanted_book"
    }
  },
  "pattern": [
    "AFA",
    "BEB",
    "ACA"
  ],
  "result": {
    "count": 1,
    "id": "oririmod:soul_harvester_fire_aspect_upgrade",
    "components": {
      "minecraft:custom_data": "{oriri_level:1}"
    }
  }
}""";
        write(baseDir + "/soul_harvester_fire_aspect_upgrade_1.json", fireJson);

        // XP Upgrade
        String xpJson = """
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "A": { "item": "oririmod:iron_stick" },
    "B": { "item": "minecraft:echo_shard" },
    "C": { "item": "oririmod:mana_manifestation" },
    "G": { "item": "minecraft:glass_bottle" },
    "E": {
      "type": "neoforge:components",
      "components": {
        "minecraft:stored_enchantments": {
          "levels": { "oririmod:teacher": 1 }
        }
      },
      "item": "minecraft:enchanted_book"
    }
  },
  "pattern": [
    "AGA",
    "BEB",
    "ACA"
  ],
  "result": {
    "count": 1,
    "id": "oririmod:soul_harvester_xp_upgrade",
    "components": {
      "minecraft:custom_data": "{oriri_level:1}"
    }
  }
}""";
        write(baseDir + "/soul_harvester_xp_upgrade_1.json", xpJson);

        // Player Kill Upgrade
        String pkJson = """
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "A": { "item": "oririmod:iron_stick" },
    "B": { "item": "minecraft:echo_shard" },
    "C": { "item": "oririmod:mana_manifestation" },
    "S": { "item": "minecraft:iron_sword" },
    "E": { "item": "oririmod:the_emperor" }
  },
  "pattern": [
    "ASA",
    "BEB",
    "ACA"
  ],
  "result": {
    "count": 1,
    "id": "oririmod:soul_harvester_player_kill_upgrade",
    "components": {
      "minecraft:custom_data": "{oriri_level:1}"
    }
  }
}""";
        write(baseDir + "/soul_harvester_player_kill_upgrade_1.json", pkJson);
    }

    private static void write(String file, String content) throws IOException {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
        }
    }
}
