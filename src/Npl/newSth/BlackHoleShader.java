package Npl.newSth;

import arc.Core;
import arc.graphics.gl.Shader;
import arc.util.Log;
import arc.util.Time;

/**
 * 黑洞扭曲 shader（单黑洞版）：全屏后处理，根据到黑洞中心的距离做径向位移。
 * <p>
 * GLSL 源码内联为 Java 字符串常量（参考 {@link RadiationShader}），
 * 避免外部文件读取问题和老 AMD 驱动对数组 uniform 的编译崩溃。
 */
public class BlackHoleShader extends Shader {

    public static BlackHoleShader instance;

    /** 当前帧注册的黑洞参数。 */
    public float centerX, centerY, radius, strength;
    public boolean active = false;

    private BlackHoleShader(){
        super(VERT_SRC, FRAG_SRC);
    }

    public static void load(){
        if(instance != null) return;
        Log.info("[BlackHoleShader] 开始加载（内联源码）...");
        System.out.flush();
        try {
            instance = new BlackHoleShader();
            Log.info("[BlackHoleShader] shader 加载成功");
            System.out.flush();
        } catch(Throwable t){
            Log.err("[BlackHoleShader] shader load failed", t);
            System.out.flush();
        }
    }

    public static boolean isAvailable(){
        return instance != null;
    }

    public void clear(){
        active = false;
        strength = 0f;
    }

    public boolean hasAny(){
        return active;
    }

    /** 注册一个黑洞。每帧只保留一个（取强度最大的）。 */
    public void add(float x, float y, float r, float s){
        if(!active || s > strength){
            centerX = x;
            centerY = y;
            radius = r;
            strength = s;
            active = true;
        }
    }

    @Override
    public void apply(){
        setUniformi("u_texture", 0);
        setUniformf("u_campos",
            Core.camera.position.x - Core.camera.width / 2f,
            Core.camera.position.y - Core.camera.height / 2f);
        setUniformf("u_resolution", Core.camera.width, Core.camera.height);
        setUniformf("u_time", Time.time);
        setUniformf("u_center", centerX, centerY);
        setUniformf("u_radius", radius);
        setUniformf("u_strength", strength);
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
        "uniform vec2 u_center;\n" +
        "uniform float u_radius;\n" +
        "uniform float u_strength;\n" +
        "void main(){\n" +
        "    vec2 worldCoords = v_texCoords * u_resolution + u_campos;\n" +
        "    vec2 uv = v_texCoords;\n" +
        "    vec2 toPix = worldCoords - u_center;\n" +
        "    float d = length(toPix);\n" +
        "    float r = u_radius;\n" +
        "    float s = u_strength;\n" +
        "    vec2 dispWorld = vec2(0.0);\n" +
        "    float darken = 0.0;\n" +
        "    float glow = 0.0;\n" +
        "    float outer = r * 3.5;\n" +
        "    if(d > 0.01 && d < outer && s > 0.001){\n" +
        "        vec2 inward = -toPix / d;\n" +
        "        float t = d / outer;\n" +
        "        float mag = (1.0 - t) * (1.0 - t) * 0.5 * s * r;\n" +
        "        dispWorld = inward * mag;\n" +
        "        if(d < r * 0.5){\n" +
        "            darken = s * smoothstep(r * 0.5, r * 0.2, d);\n" +
        "        }\n" +
        "        float ringA = smoothstep(outer, r * 0.9, d) * (1.0 - smoothstep(r * 0.9, r * 1.6, d));\n" +
        "        glow = ringA * s;\n" +
        "    }\n" +
        "    vec2 dispUV = dispWorld / u_resolution;\n" +
        "    vec4 color = texture2D(u_texture, uv + dispUV);\n" +
        "    color.rgb = mix(color.rgb, vec3(0.0), darken);\n" +
        "    float pulse = 0.7 + 0.3 * sin(u_time * 6.0);\n" +
        "    vec3 purple = vec3(0.65, 0.2, 1.0);\n" +
        "    color.rgb += purple * glow * 0.45 * pulse;\n" +
        "    gl_FragColor = color;\n" +
        "}\n";
}
