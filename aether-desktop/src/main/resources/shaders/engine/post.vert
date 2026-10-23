#version 410 core

out vec2 uv;

void main() {
    vec2 p[3] = vec2[](vec2(-1, -1), vec2(3, -1), vec2(-1, 3));
    uv = p[gl_VertexID] * 0.5 + 0.5;
    gl_Position = vec4(p[gl_VertexID], 0, 1);
}
