#version 410 core

layout(location = 0) in vec3 p;
layout(location = 1) in vec2 uv;

uniform mat4 mvp;
uniform mat4 model;

out vec2 vUv;
out vec3 w;
out vec3 n;

void main() {
    vUv = uv;
    w = (model * vec4(p, 1.0)).xyz;
    n = normalize(mat3(model) * normalize(p));
    gl_Position = mvp * vec4(p, 1.0);
}
