# [Module Name/Theme]

## Recipes

### Shaped: [Item Name]
- **Result**: [ID] ([Count])
- **Pattern**:
  ```
  [Row 1]
  [Row 2]
  [Row 3]
  ```
- **Ingredients**:
  - [Character]: [Item/Tag ID]
- **Unlock**: [Item ID that triggers recipe unlock]

---

### Shapeless: [Item Name]
- **Result**: [ID] ([Count])
- **Ingredients**: [Item/Tag ID], [Item/Tag ID], ...
- **Unlock**: [Item ID]

---

### Smelting/Blasting/Smoking: [Item Name]
- **Input**: [Item ID]
- **Experience**: [Value, e.g., 0.1]
- **Cooking Time**: [Ticks, e.g., 200]
- **Unlock**: [Item ID]

---

### Shortcuts (Automatic Set)
- **Type**: [Stairs / Slab / Wall / Fence / Fence Gate / Door / Trapdoor]
- **Base Material**: [Item/Block ID]

---

## Loot Tables

### Block Drop: [Block Name]
- **Type**: [Self / Single Item / Ore / Leaves / Silk Touch Only]
- **Drop**: [Item ID (if not Self)]
- **Amount**: [e.g., 1 or 1-3]
- **Fortune**: [Yes/No]
- **Note**: [Any special logic, e.g., "Only drops if broken with iron pickaxe or better"]

---

### Mob Drop: [Entity Name]
- **Entity ID**: [ID, e.g., oririmod:scarlet_spider]
- **Drop**: [Item ID]
- **Amount**: [e.g., 0-2]
- **Looting**: [Bonus per level]
- **Note**: [e.g., "Only drops if killed by player"]

---

### Structure Loot: [Chest Name]
- **Location**: [e.g., scarlet_temple/main_chest]
- **Items**:
  - [Item ID]: [Weight], [Amount Range]
  - [Item ID]: [Weight], [Amount Range]
- **Rolls**: [Number of items per chest]
