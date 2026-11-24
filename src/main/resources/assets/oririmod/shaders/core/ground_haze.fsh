#version 150

uniform sampler2D Sampler0; // Seamless mipmapped 3-channel FBM cloud noise (R=banks, G=swirls, B=wisps)
uniform sampler2D Sampler1; // Blitted exact-format 3D scene depth buffer
uniform sampler2D Sampler2; // Unified 289x289 RGBA16F heightfield: R=floorY-mapBaseY, G=fogHeight, B=density, A=mask
uniform sampler2D Sampler3; // Unified 289x289 RGBA8 sRGB-lit fog color: RGB=color

uniform mat4 InvProjMat;
uniform mat4 InvViewMat;
uniform vec4 ColorModulator;
uniform vec2 DepthBufferSize;
uniform vec3 CamWorldPos;   // Wrapped mod 256 camera XZ + exact Y
uniform vec3 MapRelOrigin;  // (mapOriginX - camX, mapBaseY - camY, mapOriginZ - camZ)
uniform float MapSize;      // 289.0
uniform vec2 FogYBounds;    // Camera-relative (minFogY - camY, maxFogTopY - camY) across active map
uniform float FogTime;

in vec2 vScreenUv;

out vec4 fragColor;

// Evaluates seamless 3-octave domain-warped world-space mist density & soft internal highlight.
// Uses explicit textureLod (100% safe inside non-uniform raymarch loops — zero 2x2 quad artifacts!).
vec2 sampleMistField(vec2 worldXZ, float detailAmt, float mipLod) {
    const float SCALE_1 = 12.0 / 256.0;    // 21.33-block billowing cloud banks
    const float SCALE_2 = 19.0 / 256.0;    // 13.47-block counter-drifting swirls (coprime to 12)
    const float SCALE_M =  4.0 / 256.0;    // 64.00-block macro atmospheric mist rivers

    vec2 uv1 = worldXZ * SCALE_1 + vec2(FogTime * 0.011, FogTime * 0.0065);
    vec3 n1 = textureLod(Sampler0, uv1, mipLod).rgb;

    vec2 warp = (n1.rg - 0.5) * 0.24;
    vec2 uv2 = worldXZ * SCALE_2 + warp + vec2(-FogTime * 0.009, FogTime * 0.0125);
    vec3 n2 = textureLod(Sampler0, uv2, mipLod).rgb;

    vec2 uvMacro = worldXZ * SCALE_M + vec2(FogTime * 0.004, -FogTime * 0.003);
    float macro = textureLod(Sampler0, uvMacro, max(0.0, mipLod - 1.0)).r;

    float mist = n1.r * 0.42 + n2.g * 0.36 + n2.b * 0.22;
    float rawCloudMod = mix(0.66, 1.24, mist) * mix(0.82, 1.16, macro);
    float rawHighlight = mix(0.95, 1.04, n1.r * 0.55 + n2.g * 0.45);

    float cloudMod = mix(0.96, rawCloudMod, detailAmt);
    float highlight = mix(1.0, rawHighlight, detailAmt);
    return vec2(cloudMod, highlight);
}

void main() {
    vec2 screenUV = clamp(gl_FragCoord.xy / max(DepthBufferSize, vec2(1.0)), vec2(0.0005), vec2(0.9995));

    // 1. Reconstruct exact camera-relative 3D ray and scene hit distance from 3D depth buffer
    float sceneDepth = texture(Sampler1, screenUV).r;
    vec4 clipPos = vec4(screenUV * 2.0 - 1.0, sceneDepth * 2.0 - 1.0, 1.0);
    vec4 viewH = InvProjMat * clipPos;
    vec3 viewPos = viewH.xyz / max(abs(viewH.w), 1e-6);
    vec3 sceneRelWorld = (InvViewMat * vec4(viewPos, 0.0)).xyz;

    float rawDist = length(sceneRelWorld);
    if (rawDist <= 0.001) {
        discard;
    }
    vec3 rayDir = sceneRelWorld / rawDist;
    // Allow full visibility out to 155 blocks (covers the entire ~140-block crater dungeon from afar!)
    float tScene = (sceneDepth >= 0.99999) ? 155.0 : min(rawDist, 155.0);

    // 2. Analytic ray-AABB clipping against the active fog heightfield volume [minX..maxX, minY..maxY, minZ..maxZ]
    //    Focuses 100% of raymarch steps right inside the fog layer — even when looking down from 140 blocks away!
    float tMin = 0.0;
    float tMax = tScene;

    // Vertical Y-slab clip [FogYBounds.x, FogYBounds.y]
    if (abs(rayDir.y) > 1e-5) {
        float invY = 1.0 / rayDir.y;
        float t1 = FogYBounds.x * invY;
        float t2 = FogYBounds.y * invY;
        tMin = max(tMin, min(t1, t2));
        tMax = min(tMax, max(t1, t2));
    } else if (0.0 < FogYBounds.x || 0.0 > FogYBounds.y) {
        discard;
    }

    // Horizontal XZ map-box clip [MapRelOrigin.xz, MapRelOrigin.xz + MapSize]
    vec2 boxMinXZ = MapRelOrigin.xz;
    vec2 boxMaxXZ = MapRelOrigin.xz + vec2(MapSize);
    if (abs(rayDir.x) > 1e-5) {
        float invX = 1.0 / rayDir.x;
        float t1 = boxMinXZ.x * invX;
        float t2 = boxMaxXZ.x * invX;
        tMin = max(tMin, min(t1, t2));
        tMax = min(tMax, max(t1, t2));
    } else if (0.0 < boxMinXZ.x || 0.0 > boxMaxXZ.x) {
        discard;
    }
    if (abs(rayDir.z) > 1e-5) {
        float invZ = 1.0 / rayDir.z;
        float t1 = boxMinXZ.y * invZ;
        float t2 = boxMaxXZ.y * invZ;
        tMin = max(tMin, min(t1, t2));
        tMax = min(tMax, max(t1, t2));
    } else if (0.0 < boxMinXZ.y || 0.0 > boxMaxXZ.y) {
        discard;
    }

    if (tMax <= tMin + 0.005) {
        discard;
    }

    // Cap marched thickness cleanly at 44 blocks (distant rays already enter right at the fog top via tMin!)
    float marchSpan = min(tMax - tMin, 44.0);
    tMax = tMin + marchSpan;

    // Interleaved Gradient Noise per-pixel jitter eliminates step banding completely with zero flicker
    float jitter = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));

    // Adaptive step count: 10 steps for crisp thin layers (< 6m), 16 steps for longer horizontal marches
    int steps = (marchSpan < 6.0) ? 10 : 16;
    float stepLen = marchSpan / float(steps);
    float invMapSize = 1.0 / MapSize;
    float detailAmt = smoothstep(0.05, 0.30, abs(rayDir.y));

    vec3 accumColor = vec3(0.0);
    float transmittance = 1.0;

    for (int i = 0; i < 16; i++) {
        if (i >= steps) break;
        float t = tMin + (float(i) + jitter) * stepLen;
        vec3 pRel = rayDir * t;

        vec2 mapUV = (pRel.xz - MapRelOrigin.xz) * invMapSize;
        if (mapUV.x <= 0.001 || mapUV.x >= 0.999 || mapUV.y <= 0.001 || mapUV.y >= 0.999) {
            continue;
        }

        vec4 geo = textureLod(Sampler2, mapUV, 0.0);
        float localMask = geo.a;
        if (localMask <= 0.003) {
            continue;
        }

        float floorRelY = geo.r + MapRelOrigin.y;
        float fogH = max(geo.g, 0.5);
        float h = pRel.y - floorRelY;
        // Allow small 0.35-block cushion below smoothed floor (slabs/stairs) while keeping basements dry
        if (h >= fogH || h < -0.35) {
            continue;
        }

        float u = clamp(1.0 - max(h, 0.0) / fogH, 0.0, 1.0);
        // Smooth quadratic vertical profile: richest on the floor, strictly 0.0 at fog top H
        float vertProfile = u * u;
        float localDen = geo.b;
        if (vertProfile * localDen <= 0.002) {
            continue;
        }

        float mipLod = clamp(log2(max(1.0, t * 0.10)) + (1.0 - detailAmt) * 2.2, 0.0, 5.0);
        vec2 mistInfo = sampleMistField(CamWorldPos.xz + pRel.xz, detailAmt, mipLod);
        vec3 stepCol = textureLod(Sampler3, mapUV, 0.0).rgb * mistInfo.y;

        // Smooth logarithmic damping with distance marched inside fog prevents any horizon brightness spike
        // while keeping both close-up feet fog and 140-block crater overview rich and atmospheric!
        float distInSlab = t - tMin;
        float horizonDamp = 1.0 / (1.0 + distInSlab * 0.14);

        float sigma = 0.42 + 0.54 * localDen;
        float stepTau = stepLen * vertProfile * localMask * mistInfo.x * sigma * horizonDamp;
        float stepAlpha = 1.0 - exp(-stepTau);

        accumColor += transmittance * stepAlpha * stepCol;
        transmittance *= (1.0 - stepAlpha);
        if (transmittance < 0.24) {
            break;
        }
    }

    float rawAlpha = 1.0 - transmittance;
    if (rawAlpha <= 0.003) {
        discard;
    }

    // Soft contact feather only on ultra-close slivers (< 0.25m) so walls & glass have zero hard edge
    float contactFade = smoothstep(0.0, 0.25, tScene);
    vec3 finalRGB = clamp(accumColor / rawAlpha, 0.0, 1.0) * ColorModulator.rgb;
    float finalAlpha = clamp(rawAlpha * contactFade * ColorModulator.a, 0.0, 0.68);
    if (finalAlpha <= 0.003) {
        discard;
    }

    fragColor = vec4(finalRGB, finalAlpha);
}
