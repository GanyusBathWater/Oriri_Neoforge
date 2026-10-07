import json
import os

base_dir = "C:/Users/denni/IdeaProjects/Oriri_Neoforge/src/main/resources/data/oririmod/recipe"
os.makedirs(base_dir, exist_ok=True)

def generate_recipe(filename, top_center_key, top_center_def, center_key, center_def, result_id, level):
    recipe = {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "key": {
            "A": { "item": "oririmod:iron_stick" },
            "B": { "item": "minecraft:echo_shard" },
            "C": { "item": "oririmod:mana_manifestation" }
        },
        "pattern": [
            f"A{top_center_key}A",
            f"B{center_key}B",
            f"ACA"
        ],
        "result": {
            "count": 1,
            "id": f"oririmod:{result_id}",
            "components": {
                "minecraft:custom_data": f"{{oriri_level:{level}}}"
            }
        }
    }
    recipe["key"][top_center_key] = top_center_def
    recipe["key"][center_key] = center_def

    with open(os.path.join(base_dir, filename), "w") as f:
        json.dump(recipe, f, indent=2)

# Looting Upgrades (1-5)
for i in range(1, 6):
    generate_recipe(
        f"soul_harvester_looting_upgrade_{i}.json",
        "L", { "item": "minecraft:leather" },
        "E", {
            "type": "neoforge:components",
            "components": {
                "minecraft:stored_enchantments": {
                    "levels": { "minecraft:looting": i }
                }
            },
            "item": "minecraft:enchanted_book"
        },
        "soul_harvester_looting_upgrade",
        i
    )

# Speed Upgrades (1-3)
for i in range(1, 4):
    generate_recipe(
        f"soul_harvester_speed_upgrade_{i}.json",
        "P", [
            { "type": "neoforge:components", "components": { "minecraft:potion_contents": { "potion": "minecraft:swiftness" } }, "item": "minecraft:potion" },
            { "type": "neoforge:components", "components": { "minecraft:potion_contents": { "potion": "minecraft:swiftness" } }, "item": "minecraft:splash_potion" },
            { "type": "neoforge:components", "components": { "minecraft:potion_contents": { "potion": "minecraft:swiftness" } }, "item": "minecraft:lingering_potion" }
        ],
        "E", {
            "type": "neoforge:components",
            "components": {
                "minecraft:stored_enchantments": {
                    "levels": { "minecraft:soul_speed": i }
                }
            },
            "item": "minecraft:enchanted_book"
        },
        "soul_harvester_speed_upgrade",
        i
    )

# Fire Aspect
generate_recipe(
    "soul_harvester_fire_aspect_upgrade_1.json",
    "F", { "item": "oririmod:fire_crystal" },
    "E", {
        "type": "neoforge:components",
        "components": {
            "minecraft:stored_enchantments": {
                "levels": { "minecraft:fire_aspect": 1 }
            }
        },
        "item": "minecraft:enchanted_book"
    },
    "soul_harvester_fire_aspect_upgrade",
    1
)

# XP Upgrade (Teacher)
generate_recipe(
    "soul_harvester_xp_upgrade_1.json",
    "G", { "item": "minecraft:glass_bottle" },
    "E", {
        "type": "neoforge:components",
        "components": {
            "minecraft:stored_enchantments": {
                "levels": { "oririmod:teacher": 1 }
            }
        },
        "item": "minecraft:enchanted_book"
    },
    "soul_harvester_xp_upgrade",
    1
)

# Player Kill Upgrade
generate_recipe(
    "soul_harvester_player_kill_upgrade_1.json",
    "S", { "item": "minecraft:iron_sword" },
    "E", { "item": "oririmod:the_emperor" },
    "soul_harvester_player_kill_upgrade",
    1
)

print("JSONs generated successfully.")
