#version 120

uniform sampler2D iChannel0;

void main() {
    vec4 baseTexture = texture2D(iChannel0, gl_TexCoord[0].st);
    gl_FragColor = baseTexture;
}