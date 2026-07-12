#version 330 core

out vec4 fragColor;

uniform samplerBuffer curves;
uniform int curveCount;

uniform vec2 atlasOrigin;
uniform vec2 cellSize;
uniform vec4 glyphBounds;
uniform float fontScale;
uniform vec2 glyphOffsetPx;

uniform int rgbSubpixel;
uniform int sampleBase;
uniform int samplesPerPass;

const float EPSILON = 1e-6;
const int MAX_SAMPLES_PER_PASS = 4;

vec2 evaluateQuadratic(vec2 p0, vec2 p1, vec2 p2, float t) {
    float mt = 1.0 - t;
    return mt * mt * p0 + 2.0 * mt * t * p1 + t * t * p2;
}

int rootContribution(float t, vec2 p0, vec2 p1, vec2 p2, vec2 samplePoint) {
    if (t < 0.0 || t >= 1.0) return 0;

    vec2 hit = evaluateQuadratic(p0, p1, p2, t);
    if (hit.x <= samplePoint.x) return 0;

    float derivativeY = 2.0 * (p0.y - 2.0 * p1.y + p2.y) * t
                      + 2.0 * (p1.y - p0.y);

    if (abs(derivativeY) <= EPSILON) return 0;
    return derivativeY > 0.0 ? 1 : -1;
}

int curveContribution(vec2 p0, vec2 p1, vec2 p2, vec2 samplePoint) {
    float a = p0.y - 2.0 * p1.y + p2.y;
    float b = 2.0 * (p1.y - p0.y);
    float c = p0.y - samplePoint.y;

    if (abs(a) <= EPSILON) {
        if (abs(b) <= EPSILON) return 0;
        return rootContribution(-c / b, p0, p1, p2, samplePoint);
    }

    float discriminant = b * b - 4.0 * a * c;
    if (discriminant <= EPSILON) {
        return 0;
    }

    float root = sqrt(discriminant);
    float inverse = 0.5 / a;
    float t0 = (-b - root) * inverse;
    float t1 = (-b + root) * inverse;

    return rootContribution(t0, p0, p1, p2, samplePoint)
         + rootContribution(t1, p0, p1, p2, samplePoint);
}

bool isInsideGlyph(vec2 samplePoint) {
    int winding = 0;

    for (int index = 0; index < curveCount; index++) {
        vec4 firstTexel = texelFetch(curves, index * 2);
        vec4 secondTexel = texelFetch(curves, index * 2 + 1);

        vec2 p0 = firstTexel.xy;
        vec2 p1 = firstTexel.zw;
        vec2 p2 = secondTexel.xy;

        winding += curveContribution(p0, p1, p2, samplePoint);
    }

    return winding != 0;
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec2 random2(vec2 pixel, int sampleIndex) {
    float index = float(sampleIndex);
    return vec2(
        hash12(pixel + vec2(index * 0.754877666, index * 0.569840296)),
        hash12(pixel.yx + vec2(index * 0.438289, index * 0.913733 + 19.19))
    );
}

vec2 atlasSampleToFont(vec2 localBottomSample) {
    vec2 localTopSample = vec2(localBottomSample.x, cellSize.y - localBottomSample.y);

    return vec2(
        glyphBounds.x + (localTopSample.x - glyphOffsetPx.x) / fontScale,
        glyphBounds.w - (localTopSample.y - glyphOffsetPx.y) / fontScale
    );
}

float sampleCoverage(vec2 localBottomSample) {
    return isInsideGlyph(atlasSampleToFont(localBottomSample)) ? 1.0 : 0.0;
}

void main() {
    vec2 pixel = floor(gl_FragCoord.xy - atlasOrigin);
    vec3 coverage = vec3(0.0);

    for (int localSample = 0; localSample < MAX_SAMPLES_PER_PASS; localSample++) {
        if (localSample >= samplesPerPass) break;

        int absoluteSample = sampleBase + localSample;
        vec2 randomValue = random2(pixel, absoluteSample);

        if (rgbSubpixel != 0) {
            const float regionWidth = 0.55;
            float redX   = 1.0 / 6.0 + (randomValue.x - 0.5) * regionWidth;
            float greenX = 0.5       + (randomValue.x - 0.5) * regionWidth;
            float blueX  = 5.0 / 6.0 + (randomValue.x - 0.5) * regionWidth;

            coverage.r += sampleCoverage(pixel + vec2(redX, randomValue.y));
            coverage.g += sampleCoverage(pixel + vec2(greenX, randomValue.y));
            coverage.b += sampleCoverage(pixel + vec2(blueX, randomValue.y));
        } else {
            float value = sampleCoverage(pixel + randomValue);
            coverage += vec3(value);
        }
    }

    coverage /= float(samplesPerPass);
    fragColor = vec4(coverage, max(coverage.r, max(coverage.g, coverage.b)));
}
