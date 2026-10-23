#version 410 core

in vec2 vUv;
in vec3 w;
in vec3 n;

out vec4 c;

uniform sampler2D tex;
uniform vec3 tint;
uniform float emissive;
uniform int ssao;
uniform int shadows;
uniform int ibl;

void main() {
    vec3 N = normalize(n);
    vec3 L = normalize(vec3(-0.45, 0.8, 0.3));
    float light = max(dot(N, L), 0.0) * 0.72 + 0.28;
    vec3 sky = mix(vec3(0.025, 0.06, 0.14), vec3(0.32, 0.58, 0.95), N.y * 0.5 + 0.5);
    vec3 env = ibl == 1 ? sky * (0.22 + 0.35 * max(N.y, 0.0)) : vec3(0.035);
    float shadow = 1.0;
    if (shadows == 1)
        shadow = 1.0 - 0.52 * exp(-dot(w.xz - vec2(0.1, 0.35), w.xz - vec2(0.1, 0.35)) * 0.32)
                * smoothstep(-1.3, -0.8, w.y);
    float ao = 1.0;
    if (ssao == 1)
        ao = 1.0 - 0.38 * exp(-dot(w.xz - vec2(0.05, 1.2), w.xz - vec2(0.05, 1.2)) * 2.8)
                * smoothstep(-1.3, -0.8, w.y);
    vec3 albedo = texture(tex, vUv).rgb * tint;
    c = vec4(albedo * (light * shadow * ao + env) + albedo * emissive, 1.0);
}
