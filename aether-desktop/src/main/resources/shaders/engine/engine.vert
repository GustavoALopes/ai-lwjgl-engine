#version 410 core

layout(location = 0) in vec3 p;
layout(location = 1) in vec2 uv;
layout(location = 2) in vec3 normal;
uniform int importedModel;

uniform mat4 mvp;
uniform mat4 model;

out vec2 vUv;
out vec3 w;
out vec3 n;

void main() {
    vUv = uv;
    w = (model * vec4(p, 1.0)).xyz;
    n = importedModel == 1 ? normalize(transpose(inverse(mat3(model))) * normal)
                           : normalize(mat3(model) * normalize(p));
    gl_Position = mvp * vec4(p, 1.0);
}
