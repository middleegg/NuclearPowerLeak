package Npl.newSth;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.gl.Shader;
import arc.util.Time;

public class RadiationShader extends Shader {

    public static final RadiationShader instance = new RadiationShader();

    /** 画面泛色，由 RadiationSystem 每帧写入（取玩家当前所处区块的配色）。 */
    public final Color tint = new Color(0.10f, 0.95f, 0.35f, 1f);
    /** 泛色强度：进区越久越深（0 = 无叠加）。 */
    public float intensity = 0.12f;

    private RadiationShader(){
        super(VERT_SRC, FRAG_SRC);
    }

    @Override
    public void apply(){
        setUniformi("u_texture", 0);
        setUniformf("u_campos", Core.camera.position.x - Core.camera.width / 2f, Core.camera.position.y - Core.camera.height / 2f);
        setUniformf("u_resolution", Core.camera.width, Core.camera.height);
        setUniformf("u_time", Time.time);
        setUniformf("u_tint", tint.r, tint.g, tint.b);
        setUniformf("u_intensity", intensity);
    }

    private static final String VERT_SRC =
        "attribute vec4 a_position;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "varying vec2 v_texCoords;\n" +
        "void main(){\n" +
        "    v_texCoords = a_texCoord0;\n" +
        "    gl_Position = a_position;\n" +
        "}\n";

    private static final String FRAG_SRC =
        "varying vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform vec2 u_resolution;\n" +
        "uniform vec2 u_campos;\n" +
        "uniform float u_time;\n" +
        "uniform vec3 u_tint;\n" +
        "uniform float u_intensity;\n" +
        "void main(){\n" +
        // 抖动幅度已缩减至原来的 20%（系数 ×0.2）
        "    vec2 uv = v_texCoords;\n" +
        "    float nx = sin(uv.y * 40.0 + u_time * 3.0) + sin(uv.x * 30.0 - u_time * 2.0);\n" +
        "    float ny = cos(uv.x * 35.0 + u_time * 2.5) + cos(uv.y * 25.0 - u_time * 1.8);\n" +
        "    vec2 distort = vec2(nx, ny) * 0.0012;\n" +
        "    distort.x += sin(uv.y * 8.0 + u_time * 1.5) * 0.0008;\n" +
        "    distort.y += cos(uv.x * 6.0 + u_time * 1.2) * 0.0006;\n" +
        "    vec4 color = texture2D(u_texture, uv + distort);\n" +
        "    float pulse = 0.5 + 0.5 * sin(u_time * 4.0);\n" +
        "    float d = distance(uv, vec2(0.5));\n" +
        "    float edge = smoothstep(0.2, 0.75, d);\n" +
        // 泛色由 u_tint / u_intensity 驱动：颜色随区块变化，强度随滞留时间变深
        "    float strength = u_intensity * (0.6 + 0.8 * edge) * (0.85 + 0.15 * pulse);\n" +
        "    color.rgb += u_tint * strength;\n" +
        "    gl_FragColor = color;\n" +
        "}\n";
}
