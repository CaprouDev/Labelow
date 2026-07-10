#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform vec4 color;
uniform float size;

void main()
{
    vec2 uv = texCoord * size;
    vec2 center = vec2(size * 0.5);

    float d = length(uv - center);
    float alpha = 1.0 - smoothstep(size * 0.5 - 1.0, size * 0.5, d);

    fragColor = vec4(color.rgb, color.a * alpha);
}