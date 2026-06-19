# Oriri Mod - Elderwoods Dimension World Generation

This document outlines the custom world generation architecture for the Elderwoods dimension. Provide this document to the AI in new chats to ensure full context of the custom math and logic used.

## 1. Core Architecture
The dimension abandons standard Minecraft `NoiseChunk` generation and instead uses a fully custom implementation:
- **`ElderwoodsChunkGenerator`**: The core terrain generator. It manually fills chunks block-by-block using 3D noise math (`fillFromNoise`).
- **`ElderwoodsBiomeSource`**: A custom 3D biome provider. It separates biomes into two layers: surface biomes (e.g., Elderwoods, Scarlet Plains) and underground biomes (e.g., Elysian Abyss, Crystal Caves, Scarlet Caves). The boundary is roughly `y < surfaceY - 4`.

## 2. Terrain Generation & Giant Caverns (`fillFromNoise`)
Instead of using vanilla noise routers, the terrain is generated in `ElderwoodsChunkGenerator.fillFromNoise` by evaluating math for every block from `MIN_Y` up to `surfaceY`:
- **Surface Height**: Controlled by `getSurfaceHeight` using 2D `FastNoise` to create hills and valleys.
- **The Elysian Abyss Cavern**: A massive, interconnected underground cave system. It is defined by `getElysianCavernDensity`.
- **Density Math**: The chunk generator calculates a `density` value. If `density < 0.15`, the block becomes `AIR`.
- **Global Fractal Branches**: The cavern density math evaluates **globally** across the entire underground. Inside the Elysian Abyss biome, the cavern opens up massively. Outside the biome, a `wallGradient = 2.5` forces the terrain to be mostly solid rock, but extreme spikes in the 3D noise function naturally bleed outwards into the surrounding biomes, creating sprawling, organic "little branches" and smaller cave networks without any chunk borders.

## 3. Structure Protection & Stalactite Pillars
Because the giant cavern carves out massive open spaces, surface structures (like the Elysian Sanctuary) would normally generate floating in mid-air.
- **Euclidean Taper**: To prevent this, the chunk generator queries a `ThreadLocal` list of structure `BoundingBox`es. 
- If a chunk is beneath a structure, a mathematical parabolic cone is applied to the cave ceiling. This pulls the ceiling down to `MIN_Y + 10` directly beneath the center of the structure.
- **Perfectly Round**: The taper uses Euclidean distance (`Math.sqrt(x*x + z*z)`) allowing it to smoothly fade out into the natural ceiling even far outside the structure's rectangular bounding box. This creates perfectly round, organic stalactite pillars supporting the structures.

## 4. Custom Carvers (`applyCarvers`)
Vanilla carvers (`minecraft:cave`, `minecraft:canyon`) are deliberately disabled in the custom chunk generator loop to prevent messy terrain overlaps. The loop only permits specific custom carvers:
- **`ElysianAbyssCarver`**: Used specifically to carve deep Ravines into the floor of the Elysian Abyss. It uses the exact same Stalactite Euclidean taper math as `fillFromNoise` (via `getMaxCarveHeight`) to ensure the ravines never accidentally slice through the protective pillars.
- **`ScarletCaveEntranceCarver`**: Spawns large, smooth, bowl-shaped entrances connecting the surface directly to the underground caverns.

## 5. Noise Generation
- **`FastNoise.java`**: All terrain shapes are generated using a custom `FastNoise` implementation (specifically `fbm3D` for caverns and 2D simplex for surface). It is highly optimized and allows for smooth, continuous math that never produces chunk borders.
- **Seed Offsets**: To ensure variety, a deterministic `seedOffset` is added to the X and Z coordinates before passing them into the noise functions.

## Summary for the AI
If you are modifying caves, walls, or pillars in this dimension:
1. **DO NOT** use hard cutoffs like `if (caveNoise > -0.15)`. The terrain relies on continuous math to prevent chunk borders.
2. **DO NOT** use `BoundingBox.minX/maxX` as hard boundaries for mathematical tapers, as this creates square, straight walls. Use Euclidean distance.
3. The "little branches" in the caves are a deliberate result of the global 3D noise fringes pushing through a high `wallGradient`. Do not try to overwrite them with vanilla carvers.
