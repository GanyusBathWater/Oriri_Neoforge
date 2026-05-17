#version 150

in vec4 vertexColor;
in vec2 vScreenPos;

uniform float CosmicTime;
uniform vec2  ScreenSize;
uniform vec2  TooltipOrigin;
uniform vec2  TooltipSize;
uniform float StyleIndex;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i),             hash(i + vec2(1.0, 0.0)), u.x),
        mix(hash(i+vec2(0.0,1.0)), hash(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

float fbm(vec2 p) {
    float val  = 0.0;
    float amp  = 0.5;
    float maxV = 0.0;
    float freq = 1.0;
    for (int i = 0; i < 5; i++) {
        val  += amp * vnoise(p * freq);
        maxV += amp;
        freq *= 2.13;
        amp  *= 0.48;
    }
    return val / maxV;
}

void main() {
    // ── 1. Tooltip UV [0,1] ───────────────────────────────────────────────────
    vec2 uv = clamp(
        (vScreenPos - TooltipOrigin) / max(TooltipSize, vec2(1.0)),
        0.0, 1.0
    );

    // ── 2. Vignette: four-edge smoothstep forces alpha=0 at every border ─────
    const float FADE = 0.15;
    float vignette = smoothstep(0.0, FADE,       uv.x)
                   * smoothstep(1.0, 1.0 - FADE, uv.x)
                   * smoothstep(0.0, FADE,       uv.y)
                   * smoothstep(1.0, 1.0 - FADE, uv.y);

    // ── 3. Noise coordinates: UV scaled + CosmicTime offset ────────────────────
    // CosmicTime is added DIRECTLY to the noise input coordinates.
    // This shifts the sampling position each frame, producing drifting mist.
    float aspect = TooltipSize.x / max(TooltipSize.y, 1.0);
    vec2 scale   = vec2(aspect * 2.8, 2.8);

    // Primary drift: horizontal scroll + slight vertical waver
    vec2 noiseCoords = uv * scale + vec2(CosmicTime * 0.18, CosmicTime * 0.07);

    // ── 4. Domain-warped fBm ─────────────────────────────────────────────────
    // n1: large-scale cloud base, time-shifted
    float n1 = fbm(noiseCoords);
    // n2: mid swirl domain-warped by n1, with independent time offset
    float n2 = fbm(noiseCoords * 1.4 + n1 * 1.1 + vec2(CosmicTime * 0.09, -CosmicTime * 0.11));
    // n3: fine tendrils, warped by n2, faster time offset
    float n3 = fbm(noiseCoords * 2.2 + n2 * 0.8 + vec2(-CosmicTime * 0.13, CosmicTime * 0.06));

    float density = n1 * 0.50 + n2 * 0.33 + n3 * 0.17;

    // ── 5. Noise-driven alpha × vignette ─────────────────────────────────────
    // Mist is an overlay on top of the vanilla background.
    // We lower the threshold so more mist forms, and multiply to increase its brightness/opacity.
    float mistAlpha = smoothstep(0.0, 0.50, density);
    
    // Add a strong 60% base tint across the whole tooltip, and push the thick mist to max opacity quickly.
    float alpha = clamp(0.60 + mistAlpha * 2.5, 0.0, 1.0) * vignette * vertexColor.a;

    // ── 6. Cosmic colour palette ──────────────────────────────────────────────
    // deep space blues, dark purples, subtle magenta highlights
    float tPal  = fract(density * 2.0 + CosmicTime * 0.08);

    vec3 colA = vec3(0.08, 0.08, 0.50);  // Deep cosmic blue
    vec3 colB = vec3(0.40, 0.10, 0.70);  // Rich violet
    vec3 colC = vec3(0.85, 0.20, 0.70);  // Magenta highlight

    vec3 nebula;
    if (tPal < 0.5) {
        nebula = mix(colA, colB, tPal * 2.0);
    } else {
        nebula = mix(colB, colC, (tPal - 0.5) * 2.0);
    }
    
    // Boost brightness smoothly in dense areas so it glows naturally
    nebula = mix(nebula, nebula * 2.0, smoothstep(0.40, 0.90, density));

    // ── 7. Stars ──────────────────────────────────────────────────────────────
    float starRaw = vnoise(noiseCoords * 8.0);
    float stars   = pow(max(starRaw - 0.84, 0.0) / 0.16, 4.0);
    stars        *= 0.5 + 0.5 * sin(CosmicTime * 5.0 + hash(floor(noiseCoords * 8.0)) * 6.28318);
    nebula       += vec3(stars * 0.7);

    fragColor = vec4(nebula, alpha);
}


