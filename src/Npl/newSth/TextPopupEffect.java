package Npl.newSth;

import arc.func.Cons;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.style.TextureRegionDrawable;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import mindustry.ui.*;

/**
 * 模块化文字跳出特效（TextPopupEffect）
 * ============================================================
 * 作用：在 (x,y) 上方弹出一段文字，有弹跳/缩放/淡出/抖动/旋转等动画，
 *       非常适合：暴击数字、伤害跳字、成就达成、单位复活提示、技能名称等。
 *
 * 三阶段动画：
 *   入场（0~popupEnd）：文字从 0 弹性缩放到 scaleFrom→scaleTo
 *     同时 Y 方向上升 yRiseFrom→yRiseTo
 *     抖动：shakeAmplitude 像素
 *
 *   持续（popupEnd~fadeStart）：保持缩放，文字悬浮不移动
 *
 *   退场（fadeStart~1.0）：文字淡出 alpha → 0
 *     可选同时：scale → shrinkEnd
 *
 * 模块化：每段动画都可单独开关
 *   usePopup    = false → 关闭缩放弹入
 *   useShake    = false → 关闭抖动
 *   useRotate   = false → 关闭旋转
 *   useOutline  = false → 关闭黑色描边
 * ============================================================
 * 用法：
 *   Effect e = new TextPopupEffect(){{
 *       text = "复活！";
 *       textColor = Color.valueOf("FF7A59");
 *       textSize = 1.6f;
 *       outlineColor = Color.black;
 *       riseDistance = 50f;
 *       shakeAmplitude = 1.5f;
 *   }};
 *   e.at(unit.x, unit.y);
 */
public class TextPopupEffect extends Effect {

    /* ==========================================================
     *                  核心文字参数
     * ========================================================== */

    /** 文字内容（null 的话用 e.color 做占位，但推荐显式设置）*/
    public String text = "";

    /** 字体缩放（1 = 默认字体大小，2 = 大一倍）*/
    public float textSize = 1.3f;

    /** 文字颜色 */
    public Color textColor = Color.white;

    /* ==========================================================
     *                  描边
     * ========================================================== */

    /** 是否画描边 */
    public boolean useOutline = true;

    /** 描边颜色 */
    public Color outlineColor = Color.black;

    /** 描边偏移像素（0.5 ~ 2 最佳）*/
    public float outlineOffset = 1.6f;

    /** 描边透明度倍率（0~1，越小越自然）*/
    public float outlineAlphaMul = 0.9f;

    /* ==========================================================
     *                  阴影（可选）
     * ========================================================== */

    /** 是否画阴影（在文字右下画个暗色偏移文字）*/
    public boolean useShadow = true;
    public float shadowOffsetX = 2f;
    public float shadowOffsetY = 2f;
    public Color shadowColor = new Color(0x00000066);

    /* ==========================================================
     *                  Y 轴上升
     * ========================================================== */

    /** Y 方向上升多少像素（文字往上飘）*/
    public float riseDistance = 40f;

    /** 上升插值（pow2Out = 有惯性飘起）*/
    public Interp riseInterp = Interp.pow2Out;

    /* ==========================================================
     *                  缩放弹入
     * ========================================================== */

    /** 是否启用弹跳入场 */
    public boolean usePopup = true;

    /** 入场起始缩放（0 = 从无开始，0.8 = 从 80% 开始）*/
    public float popupFrom = 0f;

    /** 入场峰值缩放（> 1 = 超射一下再弹回）*/
    public float popupPeak = 1.3f;

    /** 入场结束后稳定在多少缩放 */
    public float popupTo = 1f;

    /** 入场到什么进度结束（0.3 = 前 30% 时间完成弹入）*/
    public float popupEnd = 0.30f;

    /* ==========================================================
     *                  退场缩小
     * ========================================================== */

    /** 是否在退场时缩小 */
    public boolean useShrink = true;

    /** 退场缩到多少（0 = 缩小到没，1 = 只淡出不缩小）*/
    public float shrinkEnd = 0.2f;

    /** 从多少进度开始退场淡出（0.6 = 60% 后开始淡出）*/
    public float fadeStart = 0.65f;

    /* ==========================================================
     *                  抖动
     * ========================================================== */

    /** 是否启用文字抖动（暴击感）*/
    public boolean useShake = false;

    /** 抖动幅度（像素，越大越抖）*/
    public float shakeAmplitude = 2f;

    /** 抖动频率（越大抖得越快）*/
    public float shakeFrequency = 10f;

    /* ==========================================================
     *                  旋转
     * ========================================================== */

    /** 是否旋转（倾斜角度入场）*/
    public boolean useRotate = false;

    /** 起始角度（°）*/
    public float rotateFrom = -10f;

    /** 结束角度（°，0 = 最后回正）*/
    public float rotateTo = 0f;

    /* ==========================================================
     *                  重力（让文字稍微落下）
     * ========================================================== */

    /** 是否在后半段落下（重力感，y 上升再下掉一点）*/
    public boolean useGravityDrop = false;

    /** 落下多少像素 */
    public float dropDistance = 8f;

    /** 从多少进度开始落下 */
    public float dropStart = 0.55f;

    /* ==========================================================
     *                  整体缩放
     * ========================================================== */

    /** 全局整体倍率（比 textSize 更一步到位）*/
    public float globalScale = 1f;

    /** 全局透明倍率 */
    public float alphaMul = 1f;

    /* ==========================================================
     *                  构造函数
     * ========================================================== */

    /** 默认 */
    public TextPopupEffect() {
        this(50f, 100f, e -> {});
    }

    /** 自定义 lifetime / clip / 自定义初始化 */
    public TextPopupEffect(float lifetime, float clip, Cons<EffectContainer> cons) {
        super(lifetime, clip, e -> {});   // 逻辑 Cons 空占位，实际走 render override
    }

    /* ==========================================================
     *                  渲染
     * ========================================================== */

    @Override
    public void render(EffectContainer e) {
        String t = (text == null || text.isEmpty()) ? "" : text;
        if (t.isEmpty()) return;

        float x = e.x, y = e.y;
        float rot = e.rotation;
        float fin = Mathf.clamp(e.fin());
        float fout = Mathf.clamp(e.fout());
        Color ec = (e.color == null) ? textColor : e.color;

        // ==================== 基础 Alpha ====================
        float alpha;
        if (fin <= fadeStart) {
            alpha = 1f;
        } else {
            float fadeP = Mathf.clamp((fin - fadeStart) / Math.max(0.001f, 1f - fadeStart));
            alpha = 1f - fadeP;
        }
        alpha = Mathf.clamp(alpha * alphaMul);
        if (alpha <= 0.001f) return;

        // ==================== 缩放 ====================
        float scale;
        if (!usePopup) {
            scale = 1f;
        } else if (fin <= popupEnd) {
            // 前 popupEnd：popupFrom → popupPeak → popupTo（用 slope 超射）
            float p = Mathf.clamp(fin / Math.max(0.001f, popupEnd));
            // Interp.swingOut 类似：先超过再回来
            float sp = Interp.swingOut.apply(p);
            scale = Mathf.lerp(popupFrom, popupPeak, sp);
            if (sp > 1f) {
                scale = Mathf.lerp(popupPeak, popupTo, Mathf.clamp((sp - 1f) / 0.3f));
            }
        } else {
            scale = popupTo;
        }

        // 退场缩小
        if (useShrink && fin > fadeStart) {
            float shP = Mathf.clamp((fin - fadeStart) / Math.max(0.001f, 1f - fadeStart));
            scale = Mathf.lerp(popupTo, shrinkEnd, shP);
        }
        scale = Math.max(0.001f, scale * globalScale);

        // ==================== Y 位置 + 重力落下 ====================
        float riseP = riseInterp.apply(Mathf.clamp(fin));
        float yOff = riseDistance * riseP;
        if (useGravityDrop && fin >= dropStart) {
            float dP = Mathf.clamp((fin - dropStart) / Math.max(0.001f, 1f - dropStart));
            yOff = yOff - dropDistance * Interp.pow2In.apply(dP);
        }

        // ==================== 抖动 ====================
        float sx = 0f, sy = 0f;
        if (useShake && shakeAmplitude > 0f) {
            float sk = (fin < fadeStart - 0.05f) ? 1f : (1f - Mathf.clamp((fin - (fadeStart - 0.05f)) / 0.05f));
            sx = Mathf.sin(fin * 360f * shakeFrequency + (long) e.id * 0.73f) * shakeAmplitude * sk;
            sy = Mathf.cos(fin * 360f * (shakeFrequency * 1.3f) + (long) e.id * 0.21f) * shakeAmplitude * 0.7f * sk;
        }

        // ==================== 旋转 ====================
        float angle = (useRotate) ? Mathf.lerp(rotateFrom, rotateTo, Mathf.clamp(fin)) : 0f;

        float finalX = x + sx;
        float finalY = y + yOff + sy;
        Font font = Fonts.outline;   // Mindustry 自带带描边的字体，没有就退 default
        if (font == null) font = Fonts.def;

        float drawScale = scale * textSize;

        // 画阴影（可选）
        if (useShadow && shadowColor.a > 0.01f) {
            drawText(font, t,
                    finalX + shadowOffsetX,
                    finalY - shadowOffsetY,
                    angle, drawScale,
                    shadowColor, alpha * shadowColor.a);
        }

        // 画描边（8 方向偏移 + 比正文稍大的粗细，这里用 4 方向足矣）
        if (useOutline && outlineOffset > 0f) {
            float oa = alpha * outlineAlphaMul;
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    if (ox == 0 && oy == 0) continue;
                    drawText(font, t,
                            finalX + ox * outlineOffset,
                            finalY + oy * outlineOffset,
                            angle, drawScale,
                            outlineColor, oa);
                }
            }
        }

        // 正文
        drawText(font, t, finalX, finalY, angle, drawScale, ec, alpha);
    }

    /**
     * 画一串居中文字（文字以 x,y 为几何中心，用于浮动文字/弹幕）
     */
    private void drawText(Font font, String text, float x, float y,
                          float angleDeg, float scale, Color color, float alpha) {
        if (font == null || text == null || text.isEmpty()) return;
        if (color == null) color = Color.white;
        if (alpha <= 0.001f) return;

        GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        try {
            lay.setText(font, text);
            float w = lay.width;
            float h = lay.height;
            float tx = x - w / 2f;
            float ty = y + h / 2f;

            Draw.reset();
            Draw.color(color, alpha);
            font.getData().setScale(scale);

            // 简化：直接画，不支持旋转字符（rotation 只影响位置偏移）
            font.draw(text, tx, ty);

            font.getData().setScale(1f);
            Draw.color();
        } catch (Throwable ignored) {
        } finally {
            Pools.free(lay);
        }
    }
}
