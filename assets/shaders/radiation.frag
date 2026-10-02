varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec2 u_resolution;
uniform vec2 u_campos;
uniform float u_time;

void main(){
    vec2 uv = v_texCoords;

    // 噪声式位移：让画面像透过受污染的空气/玻璃看
    float nx = sin(uv.y * 40.0 + u_time * 3.0) + sin(uv.x * 30.0 - u_time * 2.0);
    float ny = cos(uv.x * 35.0 + u_time * 2.5) + cos(uv.y * 25.0 - u_time * 1.8);
    vec2 distort = vec2(nx, ny) * 0.006;

    // 低频大波浪：模拟热浪/辐射扰动
    distort.x += sin(uv.y * 8.0 + u_time * 1.5) * 0.004;
    distort.y += cos(uv.x * 6.0 + u_time * 1.2) * 0.003;

    vec4 color = texture2D(u_texture, uv + distort);

    // 绿色脉冲底色
    float pulse = 0.5 + 0.5 * sin(u_time * 4.0);
    vec3 green = vec3(0.10, 0.95, 0.35);

    // 边缘更强（径向衰减，中心相对清晰）
    float d = distance(uv, vec2(0.5));
    float edge = smoothstep(0.2, 0.75, d);

    // 叠加绿色：脉冲 + 边缘强化
    float strength = (0.12 + 0.10 * pulse) * (0.6 + 0.8 * edge);
    color.rgb += green * strength;

    // 轻微提亮绿色通道，整体泛绿
    color.g *= 1.08;

    gl_FragColor = color;
}
