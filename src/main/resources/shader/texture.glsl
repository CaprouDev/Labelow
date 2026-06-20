#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D iChannel0;
uniform vec4 color;

void main() {
    fragColor = texture(iChannel0, texCoord) * color;
}