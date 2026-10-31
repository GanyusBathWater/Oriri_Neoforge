#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform vec2 ScreenSize;
uniform float ImpactPhase;
uniform float ImpactIntensity;
uniform float ImpactTime;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

void main() {
    vec2 uv = texCoord0;
    vec3 baseCol = texture(Sampler0, uv).rgb;
    float centerLum = luma(baseCol);

    // Sample neighbors for crisp anime silhouette / edge detection
    vec2 texel = 1.75 / max(ScreenSize, vec2(1.0));
    float lumL = luma(texture(Sampler0, uv - vec2(texel.x, 0.0)).rgb);
    float lumR = luma(texture(Sampler0, uv + vec2(texel.x, 0.0)).rgb);
    float lumU = luma(texture(Sampler0, uv - vec2(0.0, texel.y)).rgb);
    float lumD = luma(texture(Sampler0, uv + vec2(0.0, texel.y)).rgb);

    float edge = abs(lumL - lumR) + abs(lumU - lumD);
    edge = smoothstep(0.08, 0.35, edge);

    // High-contrast inverted luminance (Screenshot 2 style, pure B&W)
    // Bright sky & glowing pillar cores become stark black; dark silhouettes & terrain cracks snap to pure white/black
    float invRaw = 1.0 - centerLum;
    float bwInverted = smoothstep(0.20, 0.66, invRaw);
    // Accentuate sharp edges so shockwave rings and horizon lines cut cleanly
    bwInverted = clamp(bwInverted - edge * 0.45, 0.0, 1.0);

    // High-contrast positive B&W flash (for initial micro-flash frame)
    float bwPositive = smoothstep(0.18, 0.62, centerLum + edge * 0.35);

    // Procedural anime impact debris speckles & peripheral radial speed lines
    vec2 centered = (uv - 0.5) * vec2(ScreenSize.x / max(ScreenSize.y, 1.0), 1.0);
    float dist = length(centered);
    float angle = atan(centered.y, centered.x);

    // Sharp peripheral speed-lines radiating from screen center
    float angularCell = floor((angle + 3.14159265) * 72.0);
    float lineSeed = hash(vec2(angularCell, floor(ImpactTime * 24.0)));
    float speedLine = step(0.84, lineSeed) * smoothstep(0.38, 0.85, dist);

    // Fine high-contrast impact dust speckles (like the dark debris specks in Screenshot 2)
    vec2 grainCell = floor(uv * ScreenSize * 0.35);
    float speckle = step(0.965, hash(grainCell + floor(ImpactTime * 30.0))) * smoothstep(0.3, 1.0, bwInverted);

    float finalInv = clamp(bwInverted - speckle * 0.85 + speedLine * (0.5 - bwInverted) * 1.6, 0.0, 1.0);

    // Blend between positive flash (ImpactPhase = 0) and inverted impact frame (ImpactPhase = 1)
    float mono = mix(bwPositive, finalInv, clamp(ImpactPhase, 0.0, 1.0));

    // Extreme contrast punch so it reads as pure graphic black and white
    mono = smoothstep(0.08, 0.92, mono);

    vec3 impactRgb = vec3(mono);
    vec3 outRgb = mix(baseCol, impactRgb, clamp(ImpactIntensity, 0.0, 1.0));

    fragColor = vec4(outRgb, 1.0) * vertexColor * ColorModulator;
}

