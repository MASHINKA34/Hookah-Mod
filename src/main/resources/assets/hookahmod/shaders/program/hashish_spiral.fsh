#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D SpiralSampler;
uniform sampler2D TripSampler;

in vec2 texCoord;

uniform vec2 InSize;
uniform float Intensity;
uniform float TripTime;
uniform float RemainingRatio;

out vec4 fragColor;

vec2 rotate2d(vec2 point, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return vec2(point.x * c - point.y * s, point.x * s + point.y * c);
}

vec4 sampleScene(vec2 uv) {
    return texture(DiffuseSampler, clamp(uv, vec2(0.0), vec2(1.0)));
}

void main() {
    float strength = clamp(Intensity, 0.0, 1.0);
    vec2 center = vec2(0.5);
    vec2 centered = texCoord - center;
    float radius = length(centered);
    float edge = smoothstep(0.04, 0.82, radius);
    float stableCenter = smoothstep(0.0, 0.18, radius);

    float spiral = edge * edge * (1.45 + radius * 4.25);
    float pulse = sin(TripTime * 1.35 + radius * 9.0) * 0.035;
    float angle = strength * stableCenter * (spiral + pulse);
    vec2 uv = center + rotate2d(centered, angle);

    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    float blur = strength * (0.7 + edge * 1.8);
    vec2 blurX = vec2(texel.x * blur * 2.0, 0.0);
    vec2 blurY = vec2(0.0, texel.y * blur * 2.0);

    vec4 base = sampleScene(uv) * 0.44;
    base += sampleScene(uv + blurX) * 0.14;
    base += sampleScene(uv - blurX) * 0.14;
    base += sampleScene(uv + blurY) * 0.14;
    base += sampleScene(uv - blurY) * 0.14;

    vec2 shift = vec2(0.0032, -0.0024) * strength * (0.4 + edge) * (0.8 + RemainingRatio * 0.4);
    float red = sampleScene(uv + shift).r;
    float green = base.g;
    float blue = sampleScene(uv - shift).b;
    vec3 shifted = vec3(red, green, blue);

    vec3 tint = vec3(0.78, 1.08, 0.72);
    vec3 color = mix(base.rgb, shifted * tint, strength * 0.42);
    color += vec3(0.02, 0.06, 0.015) * strength * edge;

    vec2 overlayUv = center + rotate2d(centered, -angle * 0.45 + TripTime * 0.015);
    vec4 spiralOverlay = texture(SpiralSampler, clamp(overlayUv, vec2(0.0), vec2(1.0)));
    vec4 tripOverlay = texture(TripSampler, clamp(texCoord, vec2(0.0), vec2(1.0)));
    float spiralMask = max(max(spiralOverlay.r, spiralOverlay.g), spiralOverlay.b) * spiralOverlay.a;
    float tripMask = max(max(tripOverlay.r, tripOverlay.g), tripOverlay.b) * tripOverlay.a;
    color = mix(color, color + spiralOverlay.rgb * 0.10, strength * edge * spiralMask);
    color = mix(color, color + tripOverlay.rgb * 0.035, strength * stableCenter * tripMask);

    fragColor = vec4(color, 1.0);
}
