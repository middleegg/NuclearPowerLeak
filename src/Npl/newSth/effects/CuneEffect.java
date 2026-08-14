package Npl.newSth.effects;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.graphics.*;

import static arc.graphics.g2d.Draw.*;

/**
 * 模块化锥形/扇形粒子爆发特效（完全支持自定义贴图）
 * ============================================================
 *
 *  —— 两种使用模式：
 *
 *   ① 原版 ParticleEffect 兼容模式（粒子数、形状等全由字段控制，和原 ParticleEffect 用法一样）
 *   ② 自定义扩散模式（你指定的需求）：
 *        · 以中心为基准，发射点可偏移（offsetX / offsetY）
 *        · 指定一个「基准角度 baseAngle + 角度范围 spreadAngleRange」
 *          → 每个粒子在 [baseAngle - spread/2, baseAngle + spread/2] 之间随机取角度
 *        · 速度有 velocityMin / velocityMax（每 tick 的像素速度），在区间内随机
 *        · 寿命有 lifetimeMin / lifetimeMax（粒子单独的存活 tick），在区间内随机
 *        · 尺寸 sizeFrom → sizeTo 线性插值（按粒子自己的进度）
 *        · 颜色 colorFrom → colorTo 线性插值
 *        · 支持贴图接口（见下）
 *
 * ============================================================
 *  🎨 贴图接口（核心需求：按你的贴图当 cune 粒子）
 * ============================================================
 *  有三种用法，任选其一（优先级从上到下）：
 *
 *  ① 自定义渲染回调（最灵活）—— particleDrawer
 *       new CuneEffect(){{
 *           particleDrawer = (x, y, rot, size, fin, color) -> {
 *               Draw.color(color);
 *               Fill.square(x, y, size, rot);
 *               Draw.reset();
 *           };
 *       }};
 *
 *  ② 直接传 TextureRegion（region）
 *       new CuneEffect(){{
 *           region = unitType.fullIcon;
 *       }};
 *
 *  ③ 填 atlas 名（spriteName）—— 最省事，等你画完图改一行即可
 *       new CuneEffect(){{
 *           spriteName = "你的文件名";   // 放 assets/sprites/你的文件名.png
 *       }};
 *     → 自动从 Core.atlas 找 sprite，找不到就 fallback 到圆形
 *
 * ============================================================
 *  基础用法示例：
 * <pre>
 *   Effect boom = new CuneEffect(){{
 *       lifetime         = 50f;              // 特效总寿命（tick）
 *       clip             = 300f;             // 视口外裁剪半径
 *       particles        = 24;               // 24 颗
 *       baseAngleOffset  = 0f;               // 可结合 at() 的 rotation 参数
 *       spreadAngleRange = 60f;              // ±30° 的扇形
 *       lifetimeMin      = 18f; lifetimeMax  = 36f;    // 寿命 18~36 tick 随机
 *       velocityMin      = 1.5f; velocityMax = 3.5f;   // 速度 1.5~3.5 px/tick
 *       spriteName       = "my_shard";       // 你的贴图
 *       sizeFrom         = 8f; sizeTo        = 1.2f;
 *       colorFrom        = Color.valueOf("A470FF");
 *       colorTo          = Color.valueOf("7AE7FF");
 *       additive         = true;             // 发光叠加
 *   }};
 *   boom.at(x, y);
 * </pre>
 */
public class CuneEffect extends Effect {

    /* ==========================================================
     *                   粒子数量/基础
     * ========================================================== */

    /** 总粒子数（一次 at() 生成多少个） */
    public int particles = 12;

    /** 是否使用 additive blending（发光效果用 true，烟雾用 false） */
    public boolean additive = true;

    /* ==========================================================
     *   🎨 贴图接口：三选一填即可（优先级 particleDrawer > region > spriteName）
     * ========================================================== */

    /**
     * 自定义单粒子渲染回调（优先级最高，设置了就用这个，不画 region/circle）。
     * 参数顺序：(x, y, rotationDeg, size, fin[0..1], color)
     */
    public ParticleDrawer particleDrawer = null;

    /** 直接指定渲染贴图 TextureRegion（优先级第二）。 */
    public TextureRegion region = null;

    /**
     * 从 atlas 中根据名字取贴图（优先级第三，region 和 drawer 都 null 时用）。
     * null/"" 或 atlas 找不到 → fallback 成圆形粒子。
     */
    public String spriteName = "particle";

    /* ==========================================================
     *   位置/角度 扩散（你要的自定义功能）
     * ========================================================== */

    /** 粒子起点（e.x/e.y）的偏移。0 就是从爆炸中心开始。 */
    public float offsetX = 0f;
    public float offsetY = 0f;

    /**
     * 基准角度偏移（度）。
     * 最终基准角度 = at() 传入的 rotation + baseAngleOffset
     *   0 = 不偏移，90 = 相对朝上多偏 90°
     */
    public float baseAngleOffset = 0f;

    /**
     * 角度分布范围（度）。
     *   = 0   → 所有粒子都朝 baseAngle 一条直线飞
     *   = 360 → 全方位圆形扩散（默认）
     *   = 60  → 以 baseAngle 为中心 ±30° 的锥形/扇形
     */
    public float spreadAngleRange = 360f;

    /* ==========================================================
     *   速度 随机范围（你要的 velocityMin/Max 接口）
     * ========================================================== */

    /** 粒子最小速度（像素/tick） */
    public float velocityMin = 1f;
    /** 粒子最大速度（像素/tick） */
    public float velocityMax = 3f;

    /* ==========================================================
     *   寿命 随机范围（你要的 lifetimeMin/Max 接口）
     * ========================================================== */

    /**
     * 单个粒子的最小寿命（tick）。
     * 注意：这是每个粒子各自独立的寿命（不是 Effect 自身 lifetime）。
     */
    public float lifetimeMin = 18f;
    /** 单个粒子的最大寿命（tick） */
    public float lifetimeMax = 40f;

    /* ==========================================================
     *   尺寸/颜色 插值（你指定贴图后也会按 size/color 渲染）
     * ========================================================== */

    /** 粒子尺寸，进度 0→1 从 sizeFrom 插值到 sizeTo */
    public float sizeFrom = 4f;
    public float sizeTo = 0.5f;

    /** 尺寸插值曲线（默认 pow2Out = 先快后慢，像爆炸弹性） */
    public Interp sizeInterp = Interp.pow2Out;

    /** 颜色，进度 0→1 从 colorFrom 插值到 colorTo */
    public Color colorFrom = Color.white.cpy();
    public Color colorTo = Color.white.cpy();

    /** 颜色插值曲线 */
    public Interp colorInterp = Interp.linear;

    /**
     * 是否使用 Effect 总进度 e.fin() 统一驱动所有粒子寿命：
     *   - true  → 所有粒子按特效总进度一起出生→一起消亡（原版 ParticleEffect 的行为）
     *   - false → 每个粒子用自己的随机寿命"独立出生→独立消亡"，更像真正的物理爆炸 ✅ 默认
     */
    public boolean useGlobalFin = false;

    /* ==========================================================
     *   旋转/自旋
     * ========================================================== */

    /** 每 tick 自旋（度）。正=顺时针，负=逆时针，0=不自旋 */
    public float spin = 0f;

    /* ==========================================================
     *   光照（可选，设置了会加 AdditiveLightLayer 光点）
     * ========================================================== */

    public float lightRadius = 0f;
    public Color lightColor = null;
    public float lightOpacity = 0.6f;
    public float lightScl = 2f;

    /* ==========================================================
     * ==========================================================
     *  下面是实现（用户不用看，改字段就够了）
     * ==========================================================
     * ========================================================== */

    protected static final Rand rand = new Rand();

    /** 空参构造：Effect 的无参构造器（给子类自定义用），用户 new 出来后手动写 lifetime/clip。 */
    public CuneEffect() {
        super();
        // Effect() 已经把 this.id 加到 all.size 了，后面用户再赋值 lifetime/clip
    }

    /** 常用快捷构造：直接指定总寿命和裁剪半径。 */
    public CuneEffect(float lifetime, float clipSize) {
        this();
        this.lifetime = lifetime;
        this.clip = clipSize;
    }

    // ========================================================================
    // 核心渲染：override Effect.render()
    //   每帧 Mindustry 会调这个方法，e 里面有 x/y/time/lifetime/id 等信息。
    //   轨迹"不乱跳"的关键：以 e.id 为固定种子 rand.setSeed，每帧同序列。
    // ========================================================================
    @Override
    public void render(EffectContainer e) {
        // ---- 1) 选好要用的贴图/绘制方式 ----
        TextureRegion tex = region;
        if (tex == null && spriteName != null && !spriteName.isEmpty() && Core.atlas.has(spriteName)) {
            tex = Core.atlas.find(spriteName);
        }
        boolean hasDrawer = particleDrawer != null;
        boolean useTex = !hasDrawer && tex != null;
        boolean useCircle = !hasDrawer && tex == null;

        // ---- 2) blend：Draw.blend(Blending.additive) 切入加色；Draw.blend() 无参 = 回到 normal ----
        if (additive) Draw.blend(Blending.additive);

        // ---- 3) 光照（进度 ~0.5 最亮，两头淡）—— 直接画一块加色发光圆盘，不依赖 LightLayer 类 ----
        if (lightRadius > 0f && lightColor != null) {
            float lightF = 1f - Math.abs(e.fin() - 0.5f) * 2f;
            if (lightF > 0.01f) {
                color(Tmp.c1.set(lightColor).a(lightOpacity * lightF));
                Fill.circle(e.x, e.y, lightRadius * lightScl * Interp.fade.apply(e.fin()));
                color();
            }
        }

        // ---- 4) 方向基准 ----
        float globalBaseAngle = e.rotation + baseAngleOffset;
        float halfSpread = spreadAngleRange * 0.5f;

        // ---- 5) 固定种子 → 每帧同随机序列 ----
        rand.setSeed(e.id);

        for (int i = 0; i < particles; i++) {

            // 5.1 四组随机：角度偏移 / 速度 / 寿命 / 自旋微扰
            //     注意：arc.util.Rand 自带 random(min, max) 实例方法（float/int 两版均有）
            float angOff = (spreadAngleRange <= 0.0001f)
                    ? 0f
                    : rand.range(halfSpread);
            float vel = rand.random(velocityMin, velocityMax);
            float plife = rand.random(lifetimeMin, lifetimeMax);
            float spinOff = (spin == 0f) ? 0f : rand.range(spin);

            // 5.2 粒子进度 fin
            float fin;
            if (useGlobalFin) {
                fin = e.fin();
            } else {
                // 独立寿命：超过 plife 说明这颗粒子已经飞完淡出，不画
                if (e.time >= plife) continue;
                fin = e.time / plife;
            }
            if (fin < 0f) fin = 0f;
            if (fin > 1f) fin = 1f;

            // 5.3 飞行距离 = 速度 * 飞行了多少 tick
            float flyT = useGlobalFin ? (e.lifetime * fin) : e.time;
            float dist = vel * flyT;
            float dir = globalBaseAngle + angOff;
            float px = e.x + offsetX + Angles.trnsx(dir, dist);
            float py = e.y + offsetY + Angles.trnsy(dir, dist);

            // 5.4 尺寸 / 颜色
            float sz = Mathf.lerp(sizeFrom, sizeTo, sizeInterp.apply(fin));
            Color c = Tmp.c1.set(colorFrom).lerp(colorTo, colorInterp.apply(fin));
            float fadeA = useGlobalFin ? 1f : (1f - fin * 0.8f);
            c.a *= fadeA;
            if (c.a <= 0.01f) continue;

            // 5.5 粒子 sprite 朝向（含自旋）
            float spriteRot = dir + spinOff * flyT;

            // ---- 6) 绘制（三种分支）----
            if (hasDrawer) {
                particleDrawer.draw(px, py, spriteRot, sz, fin, c);
            } else if (useTex) {
                // 按贴图实际尺寸乘 sz/4 缩放（sz=4 时 1:1）
                float w = tex.width * sz * 0.25f;
                float h = tex.height * sz * 0.25f;
                color(c);
                Draw.rect(tex, px, py, w, h, spriteRot);
                color();
            } else /* useCircle */ {
                color(c);
                Fill.circle(px, py, sz);
                color();
            }
        }

        reset();
        // additive 一定还原，否则整屏后续都会继续发光叠加
        if (additive) Draw.blend();
    }

    // ========================================================================
    //  粒子绘制函数式接口（6 参数，避免用库外自定义接口）
    // ========================================================================
    public interface ParticleDrawer {
        void draw(float x, float y, float rotation, float size, float fin, Color color);
    }
}
