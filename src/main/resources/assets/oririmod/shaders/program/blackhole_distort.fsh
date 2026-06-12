#version 150

uniform sampler2D DiffuseSampler;

/**
 * Screen-space position of the black hole (normalised 0–1 in both axes).
 * Defaults to screen centre (0.5, 0.5) — the player is typically looking
 * directly at the black hole when they are within activation range.
 */
uniform sampler2D DiffuseDepthSampler;

uniform mat4 InvWorldProjMat;

uniform vec3 BlackHoleViewPos;
uniform vec2 BlackHoleScreenPos;

uniform float DistortStrength;
uniform float DistortRadius;

uniform vec2 OutSize;
uniform vec2 ScreenShake;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 uv = texCoord + ScreenShake;
    bool isEventHorizon = false;
    bool isDistorted = false;
    vec2 caOffset = vec2(0.0);

    // 1. Black Hole View Position and Screen Position are now provided directly by Java
    vec3 bhViewPos = BlackHoleViewPos;
    vec2 bhScreenPos = BlackHoleScreenPos;
    
    // Only distort if black hole is in front of camera (Z < 0 in view space)
    if (bhViewPos.z < 0.0) {

        // 2. Un-project current pixel to View Space
        float depth = texture(DiffuseDepthSampler, uv).r;
        vec4 pixelNdcPos = vec4(uv.x * 2.0 - 1.0, uv.y * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
        vec4 pixelViewPos = InvWorldProjMat * pixelNdcPos;
        pixelViewPos /= pixelViewPos.w;

        // 3. Ray-Sphere Intersection in View Space
        vec3 rayOrigin = vec3(0.0);
        vec3 rayDir = normalize(pixelViewPos.xyz);
        vec3 sphereCenter = bhViewPos;
        
        // Physical radius of the distortion effect in the world (approx 1.5 blocks)
        float sphereRadius = DistortRadius * 7.5; 

        vec3 L = sphereCenter - rayOrigin;
        float tca = dot(L, rayDir);

        if (tca > 0.0) {
            float d2 = dot(L, L) - tca * tca;
            float radius2 = sphereRadius * sphereRadius;

            if (d2 < radius2) {
                float thc = sqrt(radius2 - d2);
                float t0 = tca - thc; // Front intersection

                // 4. Depth Occlusion Check
                float pixelDist = length(pixelViewPos.xyz);
                // Distort if the pixel is behind the front of the sphere.
                // Fallback: if depth == 1.0 (sky) OR depth == 0.0 (depth buffer failed to bind), always distort.
                if (pixelDist > t0 || depth > 0.9999 || depth < 0.0001) {
                    
                    // 5. Apply Distortion
                    vec2 delta = uv - bhScreenPos;
                    float aspect = OutSize.x / OutSize.y;
                    delta.x *= aspect;
                    
                    // True circular distance for the pure black void
                    float trueDist = length(delta);
                    
                    // Oval distance to make the distortion thick horizontally and thin vertically
                    vec2 ovalDelta = delta;
                    ovalDelta.y *= 1.45;
                    float ovalDist = length(ovalDelta);
                    
                    float visualRadius = sphereRadius / max(length(sphereCenter), 1.0);
                    
                    // We check if ovalDist < visualRadius to bound the distortion shape
                    if (ovalDist < visualRadius) {
                        // The Event Horizon is a PERFECT CIRCLE
                        if (trueDist < visualRadius * 0.18) {
                            isEventHorizon = true;
                        } else if (ovalDist > 0.001) {
                            // The distortion uses the OVAL distance
                            float falloff = 1.0 - smoothstep(visualRadius * 0.18, visualRadius * 0.85, ovalDist);
                            float warpAmount = pow(falloff, 3.0) * DistortStrength / max(ovalDist, 0.015);
                            
                            vec2 warpDelta = delta;
                            warpDelta.x /= aspect; 
                            
                            vec2 warpDir = normalize(warpDelta);
                            uv -= warpDir * warpAmount;
                            
                            // Calculate Chromatic Aberration offset
                            caOffset = warpDir * (warpAmount * 0.15);
                            isDistorted = true;
                        }
                    }
                }
            }
        }
    }

    vec4 color;
    if (isDistorted) {
        float r = texture(DiffuseSampler, clamp(uv + caOffset, 0.001, 0.999)).r;
        float g = texture(DiffuseSampler, clamp(uv, 0.001, 0.999)).g;
        float b = texture(DiffuseSampler, clamp(uv - caOffset, 0.001, 0.999)).b;
        color = vec4(r, g, b, 1.0);
    } else {
        color = texture(DiffuseSampler, clamp(uv, 0.001, 0.999));
    }

    // Global Screen Desaturation based on proximity to the Black Hole
    float camDist = length(bhViewPos);
    // Drains color completely when within 4 blocks, normal at 20 blocks
    float baseColorFactor = smoothstep(4.0, 20.0, camDist);
    
    // 1. Plasma Protection: Protect highly bright AND saturated pixels (accretion disk particles)
    float minColor = min(min(color.r, color.g), color.b);
    float maxColor = max(max(color.r, color.g), color.b);
    float saturation = (maxColor > 0.0) ? (maxColor - minColor) / maxColor : 0.0;
    float plasmaProtection = smoothstep(0.5, 0.85, saturation) * smoothstep(0.6, 0.9, maxColor);
    
    // 2. Photon Ring Protection: Protect the Chromatic Aberration rainbow effect from turning gray
    float ringProtection = isDistorted ? clamp(length(caOffset) * 150.0, 0.0, 1.0) : 0.0;
    
    float finalColorFactor = max(baseColorFactor, max(plasmaProtection, ringProtection));
    
    // Calculate luminance (grayscale value)
    float luminance = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    vec3 grayscale = vec3(luminance);
    
    // Mix between grayscale and original color
    color.rgb = mix(grayscale, color.rgb, finalColorFactor);

    if (isEventHorizon) {
        color = vec4(0.0, 0.0, 0.0, 1.0);
    }
    
    fragColor = color;
}
