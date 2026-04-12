#include veil:material
#include veil:camera
#include veil:fog
#include spb-revamped:shadows
#include spb-revamped:sky
#include spb-revamped:puddles
#include spb-revamped:common

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D DepthSampler;
uniform sampler2D OpaqueSampler;
uniform sampler2D TransparentSampler;
uniform sampler2D TransparentDepthSampler;
uniform sampler2D NormalSampler;
uniform sampler2D HandDepth;
uniform sampler2D MixedSampler;

uniform sampler2D CloudNoise1;
uniform sampler2D CloudNoise2;

uniform sampler2D SSAOSampler;
uniform sampler2D ShadowSampler;
uniform sampler2D ditherSample;
uniform sampler2D NoiseTex;
uniform sampler2D Stars;

uniform sampler2D TransparentCompatSampler;
uniform sampler2D OpaqueCompatSampler;
uniform usampler2D TransparentMatSampler;
uniform usampler2D OpaqueMatSampler;

uniform mat4 viewMatrix;
uniform mat4 IShadowViewMatrix;
uniform mat4 orthographMatrix;
uniform float sunsetTimer;
uniform float GameTime;
uniform vec2 Rand;
uniform vec2 ScreenSize;
uniform vec3 shadowColor;
uniform int isLightning;



in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 prevSampler;
layout(location = 2) out vec3 sun;

float GetLuminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}


//TAA From https://github.com/ComplementaryDevelopment/ComplementaryReimagined/blob/e47b8cf55562bcfacee930fb26ee77978e6035d7/shaders/lib/antialiasing/taa.glsl
//and https://sugulee.wordpress.com/2021/06/21/temporal-anti-aliasingtaa-tutorial/
void main(){
    //Normal Stuff
	vec4 color = texture(DiffuseSampler0, texCoord);

    vec4 transparent = texture(TransparentSampler, texCoord);
    vec4 opaque = texture(OpaqueSampler, texCoord);
    float depth = texture(DepthSampler, texCoord).r;
    vec4 normal = texture(NormalSampler, texCoord);
    vec3 viewPos = viewPosFromDepth(depth, texCoord);


    //Block Entity Stuff
    vec4 compat = texture(TransparentCompatSampler, texCoord);
    vec4 compat2 = texture(OpaqueCompatSampler, texCoord);

    uint Mat = texture(TransparentMatSampler, texCoord).r;
    uint Mat2 = texture(OpaqueMatSampler, texCoord).r;

    if(transparent.a > 0.0 && isBlock(Mat)) {
        color = opaque;
    }

    color *= blur(7.0, 0.001, SSAOSampler, texCoord) * 2.0;

//    #ifdef POOLROOMS
    if (Mat2 != 15) {
        #ifdef SHADOWS
            color = getShadow(color, texCoord, viewPos, normal, ScreenSize, viewMatrix, IShadowViewMatrix, orthographMatrix, NoiseTex, ShadowSampler, ditherSample, sunsetTimer, shadowColor);
        #endif
    } else {
        //Sun
        vec3 rd = viewDirFromUv(texCoord);
        vec3 lightAngled = getLightAngle(IShadowViewMatrix);
        color.rgb += smoothstep(0.998, 1.0, dot(rd, lightAngled));
        if(sunsetTimer < 0.35 || sunsetTimer > 0.65) {
            color.rgb += smoothstep(0.7, 1.0, dot(rd, lightAngled)) * 0.6;
        }
        color.rgb += texture(Stars, rd.xz * 0.9).rgb * clamp(sin(sunsetTimer*3.5), 0.0, 1.0);
    }
//    #endif

    if(compat.a > 0.0 || compat2.a > 0.0){
        color += compat + compat2;
        color.a = min(compat.a + compat2.a, 1.0);
    }

    #ifdef INFINITE_FIELD
        color = linear_fog(color, length(viewPos), -10, 90, vec4(vec3(0.8), 1.0));
    #endif

    #ifdef LEVEL324
        color = linear_fog(color, length(viewPos), -10, 90, vec4(vec3(0), 1));
    #endif

    #ifdef A_PLACE_YOU_DONT_WANT_TO_KNOW
        // Subtle stale-office haze for Level 959 only.
        float level959Depth = length(viewPos);
        vec4 level959FogColor = vec4(0.20, 0.19, 0.17, 1.0);

        color = linear_fog(color, level959Depth, 8.0, 48.0, level959FogColor);

        float level959Dust = texture(NoiseTex, texCoord * 2.0 + vec2(GameTime * 0.0007, GameTime * 0.0004)).r;
        float level959DustAmount = smoothstep(14.0, 58.0, level959Depth) * 0.09;
        color.rgb = mix(color.rgb, level959FogColor.rgb + level959Dust * 0.10, level959DustAmount);
    #endif

    if (isLightning == 1) {
        color = texture(DiffuseSampler0, texCoord);
    }

    fragColor = color;
}
