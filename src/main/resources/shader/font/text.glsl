#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D atlas;
uniform vec4 uvRect;
uniform vec4 color;

void main() {
    vec2 atlasUv = mix(uvRect.xy, uvRect.zw, texCoord);
    vec4 coverage = texture(atlas, atlasUv);

    vec3 premultipliedRgb = color.rgb * color.a * coverage.rgb;
    float alpha = color.a * coverage.a;

    fragColor = vec4(premultipliedRgb, alpha);
}
