varying vec2 v_texCoords;

uniform float u_time;        // 时间（秒）
uniform vec3  u_color;       // 主色（蓝）
uniform float u_intensity;   // 整体强度

void main(){
    vec2 c = v_texCoords - 0.5;
    float dist = length(c);

    // 中心强 → 边缘渐变到 0（圆形柔光遮罩）
    float glow = 1.0 - smoothstep(0.0, 0.5, dist);

    // 流动波纹：角度方向 + 半径方向叠加
    float angle = atan(c.y, c.x);
    float w1 = sin(angle * 6.0 + u_time * 2.0) * 0.5 + 0.5;
    float w2 = sin(dist * 25.0 - u_time * 3.0) * 0.5 + 0.5;
    float flow = w1 * 0.6 + w2 * 0.4;

    // 呼吸脉冲（整体明暗变化）
    float pulse = 0.5 + 0.5 * sin(u_time * 1.5);

    // 最终 alpha = 柔光 × 流动 × 脉冲 × 强度
    float alpha = glow * (0.4 + 0.6 * flow) * (0.6 + 0.4 * pulse) * u_intensity;

    // 颜色：基色 + 流动附加亮度
    vec3 col = u_color + vec3(0.15) * flow;

    gl_FragColor = vec4(col, alpha);
}
