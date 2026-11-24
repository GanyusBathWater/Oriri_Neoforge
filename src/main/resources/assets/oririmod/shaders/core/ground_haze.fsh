#version 150

uniform sampler2D Sampler0; // Seamless mipmapped 3-channel FBM cloud noise (R=banks, G=swirls, B=wisps)
uniform sampler2D Sampler1; // Blitted exact-format 3D scene depth buffer
uniform sampler2D Sampler2; // Ground 289x289 RGBA16F heightfield: R=floorY-mapBaseY, G=fogHeight, B=density, A=mask
uniform sampler2D Sampler3; // Ground 289x289 RGBA8 sRGB-lit fog color: RGB=color
uniform sampler2D Sampler4; // Volume 289x289 RGBA16F 3D room map: R=bottomY-volBaseY, G=height, B=density, A=mask
uniform sampler2D Sampler5; // Volume 289x289 RGBA8 sRGB-lit room fog color: RGB=color

uniform mat4 InvProjMat;
uniform mat4 InvViewMat;
uniform vec4 ColorModulator;
uniform vec2 DepthBufferSize;
uniform vec3 CamWorldPos;   // Wrapped mod 256 camera XZ + exact Y
uniform vec3 MapRelOrigin;  // Ground map: (mapOriginX - camX, mapBaseY - camY, mapOriginZ - camZ)
uniform vec3 VolRelOrigin;  // Volume map: (volOriginX - camX, volBaseY - camY, volOriginZ - camZ)
uniform float MapSize;      // 289.0
uniform vec2 FogYBounds;    // Camera-relative (minFogY - camY, maxFogTopY - camY) for ground fog
uniform vec2 VolYBounds;    // Camera-relative (minBottomY - camY, maxTopY - camY) for 3D volume fog
uniform float HasGroundFog; // 1.0 if ground/biome fog active, else 0.0
uniform float HasVolumeFog; // 1.0 if 3D room/volume fog active, else 0.0
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

// True 3D swirling mist field for tall room-filling & floating mid-air fog volumes:
// adds subtle vertical Y parallax across octaves so tall halls billow organically in 3D instead of streaking vertically.
vec2 sampleMistField3D(vec3 worldPos, float mipLod) {
    const float SCALE_1 = 10.0 / 256.0;
    const float SCALE_2 = 17.0 / 256.0;

    vec2 yShift1 = vec2(worldPos.y * 0.016, -worldPos.y * 0.011);
    vec2 uv1 = worldPos.xz * SCALE_1 + yShift1 + vec2(FogTime * 0.009, FogTime * 0.0055);
    vec3 n1 = textureLod(Sampler0, uv1, mipLod).rgb;

    vec2 warp = (n1.rg - 0.5) * 0.26;
    vec2 yShift2 = vec2(-worldPos.y * 0.021, worldPos.y * 0.014);
    vec2 uv2 = worldPos.xz * SCALE_2 + yShift2 + warp + vec2(-FogTime * 0.007, FogTime * 0.010);
    vec3 n2 = textureLod(Sampler0, uv2, mipLod).rgb;

    float mist = n1.r * 0.44 + n2.g * 0.36 + n2.b * 0.20;
    float cloudMod = mix(0.72, 1.24, mist);
    float highlight = mix(0.95, 1.05, n1.r * 0.55 + n2.g * 0.45);
    return vec2(cloudMod, highlight);
}

void main() {
    bool useGround = (HasGroundFog > 0.5);
    bool useVolume = (HasVolumeFog > 0.5);
    if (!useGround && !useVolume) {
        discard;
    }

    vec2 screenUV = clamp(gl_FragCoord.xy / max(DepthBufferSize, vec2(1.0)), vec2(0.0005), vec2(0.9995));

    // 1. Reconstruct exact camera-relative 3D ray and scene hit distance from 3D depth buffer
    float sceneDepth = textureLod(Sampler1, screenUV, 0.0).r;
    vec4 clipPos = vec4(screenUV * 2.0 - 1.0, sceneDepth * 2.0 - 1.0, 1.0);
    vec4 viewH = InvProjMat * clipPos;
    vec3 viewPos = viewH.xyz / max(abs(viewH.w), 1e-6);
    vec3 sceneRelWorld = (InvViewMat * vec4(viewPos, 0.0)).xyz;

    float rawDist = length(sceneRelWorld);
    if (rawDist <= 0.001) {
        discard;
    }
    vec3 rayDir = sceneRelWorld / rawDist;
    float tScene = (sceneDepth >= 0.99999) ? 155.0 : min(rawDist, 155.0);

    // 2. Analytic ray-AABB clipping against the union of active fog layers
    float unionMinY = useGround ? (useVolume ? min(FogYBounds.x, VolYBounds.x) : FogYBounds.x) : VolYBounds.x;
    float unionMaxY = useGround ? (useVolume ? max(FogYBounds.y, VolYBounds.y) : FogYBounds.y) : VolYBounds.y;

    vec2 boxMinXZ = useGround ? (useVolume ? min(MapRelOrigin.xz, VolRelOrigin.xz) : MapRelOrigin.xz) : VolRelOrigin.xz;
    vec2 boxMaxXZ = useGround ? (useVolume ? max(MapRelOrigin.xz, VolRelOrigin.xz) : MapRelOrigin.xz) : VolRelOrigin.xz;
    boxMaxXZ += vec2(MapSize);

    float tMin = 0.0;
    float tMax = tScene;

    if (abs(rayDir.y) > 1e-5) {
        float invY = 1.0 / rayDir.y;
        float t1 = unionMinY * invY;
        float t2 = unionMaxY * invY;
        tMin = max(tMin, min(t1, t2));
        tMax = min(tMax, max(t1, t2));
    } else if (0.0 < unionMinY || 0.0 > unionMaxY) {
        discard;
    }

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

    float maxSpan = useVolume ? 52.0 : 44.0;
    float marchSpan = min(tMax - tMin, maxSpan);
    tMax = tMin + marchSpan;

    // Interleaved Gradient Noise per-pixel jitter eliminates step banding completely with zero flicker
    float jitter = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));

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
        float distInSlab = t - tMin;

        float totalStepTau = 0.0;
        vec3 weightedCol = vec3(0.0);

        // ── Layer A: Ground-hugging fog (natural biomes + FogSourceBlock) ──
        if (useGround && pRel.y >= FogYBounds.x && pRel.y <= FogYBounds.y) {
            vec2 mapUV = (pRel.xz - MapRelOrigin.xz) * invMapSize;
            if (mapUV.x > 0.001 && mapUV.x < 0.999 && mapUV.y > 0.001 && mapUV.y < 0.999) {
                vec4 geo = textureLod(Sampler2, mapUV, 0.0);
                float localMask = geo.a;
                if (localMask > 0.003) {
                    float floorRelY = geo.r + MapRelOrigin.y;
                    float fogH = max(geo.g, 0.5);
                    float h = pRel.y - floorRelY;
                    if (h < fogH && h >= -0.35) {
                        float u = clamp(1.0 - max(h, 0.0) / fogH, 0.0, 1.0);
                        float vertProfile = u * u;
                        float localDen = geo.b;
                        if (vertProfile * localDen > 0.002) {
                            float mipLod = clamp(log2(max(1.0, t * 0.10)) + (1.0 - detailAmt) * 2.2, 0.0, 5.0);
                            vec2 mistInfo = sampleMistField(CamWorldPos.xz + pRel.xz, detailAmt, mipLod);
                            vec3 gCol = textureLod(Sampler3, mapUV, 0.0).rgb * mistInfo.y;
                            float horizonDamp = 1.0 / (1.0 + distInSlab * 0.14);
                            float sigma = 0.42 + 0.54 * localDen;
                            float gTau = stepLen * vertProfile * localMask * mistInfo.x * sigma * horizonDamp;
                            totalStepTau += gTau;
                            weightedCol += gCol * gTau;
                        }
                    }
                }
            }
        }

        // ── Layer B: 3D Floating / Room-Filling Volume Fog (VolumeFogSourceBlock) ──
        if (useVolume && pRel.y >= VolYBounds.x && pRel.y <= VolYBounds.y) {
            vec2 volUV = (pRel.xz - VolRelOrigin.xz) * invMapSize;
            if (volUV.x > 0.001 && volUV.x < 0.999 && volUV.y > 0.001 && volUV.y < 0.999) {
                vec4 vGeo = textureLod(Sampler4, volUV, 0.0);
                float vMask = vGeo.a;
                if (vMask > 0.003) {
                    float vBotRelY = vGeo.r + VolRelOrigin.y;
                    float vHeight = max(vGeo.g, 0.5);
                    float hFromBot = pRel.y - vBotRelY;
                    float hFromTop = vHeight - hFromBot;
                    if (hFromBot > 0.0 && hFromTop > 0.0) {
                        // Solid floors/ceilings extend 1.1 blocks into solid geometry (so room fills flush),
                        // while open mid-air bounds feather softly over ~1.8 blocks!
                        float feather = min(1.8, vHeight * 0.32);
                        float vertFeather = smoothstep(0.0, feather, hFromBot) * smoothstep(0.0, feather, hFromTop);
                        float vDen = vGeo.b;
                        if (vertFeather * vDen > 0.002) {
                            // Atmospheric & playable: keeps 0.6..3.0m around player clear, thickens smoothly across room
                            float nearClear = smoothstep(0.55, 3.0, t);
                            float mipLod = clamp(log2(max(1.0, t * 0.09)), 0.0, 4.5);
                            vec2 mist3D = sampleMistField3D(CamWorldPos + pRel, mipLod);
                            vec3 vCol = textureLod(Sampler5, volUV, 0.0).rgb * mist3D.y;
                            float roomDamp = 1.0 / (1.0 + distInSlab * 0.035);
                            float sigmaVol = 0.055 + 0.095 * vDen;
                            float vTau = stepLen * vertFeather * vMask * nearClear * mist3D.x * sigmaVol * roomDamp;
                            totalStepTau += vTau;
                            weightedCol += vCol * vTau;
                        }
                    }
                }
            }
        }

        if (totalStepTau > 0.0005) {
            float stepAlpha = 1.0 - exp(-totalStepTau);
            vec3 stepCol = weightedCol / totalStepTau;
            accumColor += transmittance * stepAlpha * stepCol;
            transmittance *= (1.0 - stepAlpha);
            if (transmittance < 0.20) {
                break;
            }
        }
    }

    float rawAlpha = 1.0 - transmittance;
    if (rawAlpha <= 0.003) {
        discard;
    }

    float contactFade = smoothstep(0.0, 0.25, tScene);
    vec3 finalRGB = clamp(accumColor / rawAlpha, 0.0, 1.0) * ColorModulator.rgb;
    float maxCap = useVolume ? 0.74 : 0.68;
    float finalAlpha = clamp(rawAlpha * contactFade * ColorModulator.a, 0.0, maxCap);
    if (finalAlpha <= 0.003) {
        discard;
    }

    fragColor = vec4(finalRGB, finalAlpha);
}
