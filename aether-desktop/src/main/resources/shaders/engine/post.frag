#version 410 core

in vec2 uv;

out vec4 c;

uniform sampler2D scene;
uniform float exposure;
uniform int hdr;

void main() {
    vec3 x = texture(scene, uv).rgb;
    if (hdr == 1) {
        x = vec3(1) - exp(-x * exposure);
        x = pow(x, vec3(1.0 / 2.2));
    }
    c = vec4(x, 1);
}
