#version 330 core

in vec2 texCoord;

out vec4 fragColor;

uniform vec4 color;
uniform vec2 rectSize;
uniform float radius;

float roundedBoxSDF(vec2 p, vec2 b, float r)
{
    vec2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main()
{
    vec2 p = texCoord * rectSize - rectSize * 0.5;

    float d = roundedBoxSDF(
        p,
        rectSize * 0.5,
        radius
    );

    float alpha = 1.0 - smoothstep(0.0, 1.0, d);

    fragColor = vec4(color.rgb, color.a * alpha);
}