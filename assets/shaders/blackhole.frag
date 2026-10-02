// ============================================================
// 黑洞扭曲 shader（简化版，单黑洞，避免老 AMD 驱动对数组 uniform 的崩溃）
// 用法：在 BlackHoleSystem.postDraw 时把整帧 FrameBuffer 用本 shader blit 回屏幕。
//   - 每个 fragment 根据到黑洞中心的距离做径向位移（吸入感）
//   - 中心 0.5r 内强制暗化到纯黑（事件视界）
//   - 边缘紫光晕在 r ~ 2.5r 范围内做辉光叠加
// ============================================================

varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec2 u_resolution;
uniform vec2 u_campos;
uniform float u_time;
uniform vec2 u_center;    // 黑洞世界坐标
uniform float u_radius;   // 黑洞半径 r
uniform float u_strength; // 强度 0~1

void main(){
    vec2 worldCoords = v_texCoords * u_resolution + u_campos;
    vec2 uv = v_texCoords;

    vec2 toPix = worldCoords - u_center;
    float d = length(toPix);
    float r = u_radius;
    float s = u_strength;

    vec2 dispWorld = vec2(0.0);
    float darken = 0.0;
    float glow = 0.0;

    float outer = r * 2.5;
    if(d > 0.01 && d < outer && s > 0.001){
        vec2 inward = -toPix / d;
        float t = d / outer;
        float mag = (1.0 - t) * (1.0 - t) * 0.5 * s * r;
        dispWorld = inward * mag;

        if(d < r * 0.5){
            darken = s * smoothstep(r * 0.5, r * 0.2, d);
        }

        float ringA = smoothstep(outer, r * 0.9, d) * (1.0 - smoothstep(r * 0.9, r * 1.6, d));
        glow = ringA * s;
    }

    vec2 dispUV = dispWorld / u_resolution;
    vec4 color = texture2D(u_texture, uv + dispUV);

    color.rgb = mix(color.rgb, vec3(0.0), darken);

    float pulse = 0.7 + 0.3 * sin(u_time * 6.0);
    vec3 purple = vec3(0.65, 0.2, 1.0);
    color.rgb += purple * glow * 0.45 * pulse;

    gl_FragColor = color;
}
