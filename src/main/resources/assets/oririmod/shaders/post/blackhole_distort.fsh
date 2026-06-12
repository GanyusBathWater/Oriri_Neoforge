#version 150

uniform sampler2D DiffuseSampler;

/**
 * Screen-space position of the black hole (normalised 0–1 in both axes).
 * Defaults to screen centre (0.5, 0.5) — the player is typically looking
 * directly at the black hole when they are within activation range.
 */
uniform vec2  BlackHoleScreenPos;

/**
 * Controls how strongly pixels are warped toward the black hole.
 * Smaller values (e.g. 0.002–0.004) are subtle; larger values are dramatic.
 */
uniform float DistortStrength;

/**
 * Screen-space radius of the effect in normalised coordinates (0–1).
 * Beyond this radius no distortion is applied.
 */
uniform float DistortRadius;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 uv    = texCoord;
    vec2 delta = uv - BlackHoleScreenPos;
    float dist = length(delta);

    if (dist < DistortRadius && dist > 0.001) {
        // Smooth falloff: distortion is strongest near centre, fades to zero at DistortRadius
        float falloff = 1.0 - smoothstep(DistortRadius * 0.20, DistortRadius, dist);

        // Inward warp: pull UV coordinates toward BlackHoleScreenPos.
        // Squaring falloff gives a sharper, more dramatic pull close to the centre.
        float warpAmount = falloff * falloff * DistortStrength / max(dist, 0.015);
        uv -= normalize(delta) * warpAmount;
    }

    // Clamp to prevent black border artefacts from sampling outside [0,1]
    uv = clamp(uv, 0.001, 0.999);
    fragColor = texture(DiffuseSampler, uv);
}
