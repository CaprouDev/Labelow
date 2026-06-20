#version 330 core

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 uv;

out vec2 texCoord;

uniform mat4 projection;
uniform vec4 transform;

void main() {
    vec2 pos = position * transform.zw + transform.xy;
    texCoord = uv;
    gl_Position = projection * vec4(pos, 0.0, 1.0);
}