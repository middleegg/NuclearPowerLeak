package Npl.newSth;

import arc.func.Cons;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.graphics.*;

/**
 * 故障艺术（Glitch Art）模块化特效
 * =================================================
 *   作用：
 *   模拟数字故障/信号干扰视觉效果，包含：
 *     ① RGB通道分离（红/青/绿三色偏移重影）
 *     ② 扫描线（CRT显示器水平条纹）
 *     ③ 像素化马赛克（数字块状噪点）
 *     ④ 画面撕裂（水平切片随机位移）
 *     ⑤ 脉冲抖动（整体位置随机跳动）
 *     ⑥ 数字噪块（随机闪烁的彩色方块）
 * =================================================
 *   用法：
 *     GlitchEffect glitch = new GlitchEffect(45f, 200f){{
 *         coreColor = Color.valueOf("FF00FF");
 *         separationStrength = 12f;
 *         scanlineIntensity = 0.7f;
 *         tearCount = 5;
 *     }};
 *     glitch.at(x, y);
 */
public class GlitchEffect extends Effect {

    /* ==========================================================
     *                  可调参数（用户可写）
     * ========================================================== */

    // ---------- 主视觉 ----------
    /** 核心色（RGB通道分离的主色调，默认洋红） */
    public Color coreColor = Color.valueOf("FF00FF");
    /** 对比色（通道分离的另一色，默认青色） */
    public Color contrastColor = Color.valueOf("00FFFF");
    /** 高亮色（扫描线/噪点高光，默认白） */
    public Color highlightColor = Color.white;

    // ---------- RGB通道分离 ----------
    /** 通道分离强度（像素偏移量） */
    public float separationStrength = 8f;
    /** 通道分离方向（0=水平, 90=垂直） */
    public float separationAngle = 0f;

    // ---------- 扫描线 ----------
    /** 扫描线强度 0~1（0=无扫描线，1=明显条纹） */
    public float scanlineIntensity = 0.5f;
    /** 扫描线间距（像素） */
    public float scanlineSpacing = 4f;
    /** 扫描线粗细（像素） */
    public float scanlineThickness = 1f;
    /** 扫描线滚动速度（0=静止） */
    public float scanlineScroll = 2f;

    // ---------- 画面撕裂 ----------
    /** 撕裂带数量（水平切片数） */
    public int tearCount = 4;
    /** 撕裂最大位移（像素） */
    public float tearMaxOffset = 15f;
    /** 撕裂带高度范围（最小值, 像素） */
    public float tearMinHeight = 3f;
    /** 撕裂带高度范围（最大值, 像素） */
    public float tearMaxHeight = 12f;

    // ---------- 马赛克/像素化 ----------
    /** 马赛克块大小（像素，0=关闭） */
    public float mosaicSize = 0f;
    /** 马赛克出现概率 0~1（用于噪点马赛克） */
    public float mosaicChance = 0.3f;

    // ---------- 数字噪块 ----------
    /** 噪块数量 */
    public int noiseBlockCount = 8;
    /** 噪块最大尺寸（像素） */
    public float noiseBlockSize = 6f;
    /** 噪块闪烁频率（每tick变化概率） */
    public float noiseFlickerRate = 0.3f;

    // ---------- 脉冲抖动 ----------
    /** 抖动强度（整体位移幅度） */
    public float jitterStrength = 3f;
    /** 抖动频率（每tick抖动概率） */
    public float jitterRate = 0.5f;

    // ---------- 中心图形 ----------
    /** 是否绘制中心故障核心图形 */
    public boolean drawCore = true;
    /** 核心半径 */
    public float coreRadius = 15f;
    /** 核心图形复杂度（边数，0=圆，3=三角，4=方，5=五角...） */
    public int coreSides = 4;
    /** 核心旋转速度（度/tick，0=静止） */
    public float coreRotationSpeed = 45f;

    // ---------- 整体控制 ----------
    /** 整体尺寸缩放 */
    public float sizeScale = 1f;
    /** 效果淡出起始进度（0~1，从此处开始淡出） */
    public float fadeStart = 0.6f;

    // ---------- 偏移 ----------
    public float offsetX, offsetY;

    /* ==========================================================
     *                  构造器
     * ========================================================== */

    public GlitchEffect() {
        this.lifetime = 45f;
        this.clip = 200f;
    }

    public GlitchEffect(float lifetime, float clip) {
        this.lifetime = lifetime;
        this.clip = clip;
    }

    public GlitchEffect(float lifetime, float clip, Cons<GlitchEffect> cons) {
        this(lifetime, clip);
        cons.get(this);
    }

    @Override
    public void init() {
        float maxExtent = coreRadius * 2f + separationStrength * 2f + tearMaxOffset + jitterStrength * 2f + noiseBlockSize;
        this.clip = Math.max(this.clip, maxExtent * sizeScale + 20f);
    }

    @Override
    public void render(EffectContainer e) {
        float fin = e.fin();
        float fout = e.fout();
        float time = e.time;
        float ox = e.x;
        float oy = e.y;

        // —— 脉冲抖动：整体随机位移 ——
        float jitterX = 0f, jitterY = 0f;
        if (jitterStrength > 0f && jitterRate > 0f && Mathf.chance(jitterRate)) {
            jitterX = Mathf.randomSeedRange((long)(time * 10f), jitterStrength);
            jitterY = Mathf.randomSeedRange((long)(time * 10f + 1000L), jitterStrength);
        }
        ox += jitterX;
        oy += jitterY;

        // —— 淡出透明度 ——
        float alpha = 1f;
        if (fin > fadeStart) {
            alpha = 1f - (fin - fadeStart) / (1f - fadeStart);
        }
        alpha = Mathf.clamp(alpha);

        // —— 计算分离偏移 ——
        float sepX = 0f, sepY = 0f;
        if (separationAngle == 0f) {
            sepX = separationStrength * (0.5f + 0.5f * Mathf.sin(time * 3f));
        } else {
            sepY = separationStrength * (0.5f + 0.5f * Mathf.sin(time * 3f));
        }

        // —————————— ① 画面撕裂（水平切片随机位移） ——————————
        if (tearCount > 0 && tearMaxOffset > 0f) {
            Draw.z(Layer.effect);
            for (int i = 0; i < tearCount; i++) {
                float seed1 = Mathf.randomSeed((long)(e.id + i * 7L));
                float seed2 = Mathf.randomSeed((long)(e.id + i * 13L + 500L));
                // 撕裂带位置（从中心向上偏移）
                float tearCenterY = (seed1 - 0.5f) * coreRadius * 2.5f;
                float tearH = Mathf.lerp(tearMinHeight, tearMaxHeight, seed2);
                // 撕裂位移（随时间变化 + 随机）
                float tearOffset = Mathf.sin(time * (3f + seed1 * 5f) + seed2 * Mathf.PI2) * tearMaxOffset * seed1;
                tearOffset *= Interp.pow2Out.apply(Mathf.lerp(0.3f, 1f, fin)); // 中间最强

                // 绘制撕裂带（简化为彩色细矩形）
                Color tearColor = i % 3 == 0 ? coreColor : (i % 3 == 1 ? contrastColor : highlightColor);
                Draw.color(tearColor, alpha * 0.6f);
                // 撕裂带主体
                Fill.rect(ox + tearOffset, oy + tearCenterY,
                    coreRadius * 1.5f * sizeScale, tearH * sizeScale);
                // 撕裂带边缘亮线
                Draw.color(highlightColor, alpha * 0.8f);
                Lines.stroke(1f);
                Lines.line(ox + tearOffset - coreRadius * 0.75f * sizeScale, oy + tearCenterY,
                    ox + tearOffset + coreRadius * 0.75f * sizeScale, oy + tearCenterY);
            }
        }

        // —————————— ② RGB通道分离（三色偏移重影） ——————————
        Draw.z(Layer.effect + 0.1f);

        // 通道1：红色偏移
        Draw.color(coreColor, alpha * 0.7f);
        drawGlitchShape(e, ox - sepX * sizeScale, oy - sepY * sizeScale, time, fin, 1f);

        // 通道2：青色偏移
        Draw.color(contrastColor, alpha * 0.7f);
        drawGlitchShape(e, ox + sepX * sizeScale, oy + sepY * sizeScale, time, fin, -1f);

        // 通道3：高亮偏移（少量）
        Draw.color(highlightColor, alpha * 0.4f);
        drawGlitchShape(e, ox + sepX * 0.5f * sizeScale, oy + sepY * 0.5f * sizeScale, time, fin, 0.5f);

        // —————————— ③ 中心故障核心图形 ——————————
        if (drawCore) {
            Draw.z(Layer.effect + 0.2f);
            // 核心轮廓
            float coreR = coreRadius * sizeScale * (0.8f + 0.2f * Mathf.sin(time * 8f));
            float coreRot = coreRotationSpeed * time;

            // 核心形状描边
            Draw.color(highlightColor, alpha * 0.9f);
            Lines.stroke(2f * (1f - fin * 0.5f));
            if (coreSides <= 0) {
                Lines.circle(ox, oy, coreR);
            } else {
                Lines.poly(ox, oy, coreSides, coreR, coreRot);
            }

            // 核心内部填充（半透明）
            Draw.color(coreColor, alpha * 0.3f);
            if (coreSides <= 0) {
                Fill.circle(ox, oy, coreR * 0.6f);
            } else {
                Fill.poly(ox, oy, coreSides, coreR * 0.6f, coreRot);
            }

            // 核心中心闪烁点
            if (Mathf.chance(0.6f)) {
                Draw.color(highlightColor, alpha);
                Fill.circle(ox, oy, 2f * sizeScale * (1f + 0.5f * Mathf.sin(time * 15f)));
            }
        }

        // —————————— ④ 扫描线 ——————————
        if (scanlineIntensity > 0f) {
            Draw.z(Layer.effect + 0.3f);
            float scrollOffset = scanlineScroll * time;
            float range = coreRadius * 3f * sizeScale;
            for (float y = -range; y <= range; y += scanlineSpacing) {
                float scrollY = y + scrollOffset % scanlineSpacing;
                // 扫描线透明度随距离中心衰减
                float distFade = 1f - Math.abs(y) / range;
                float lineAlpha = alpha * scanlineIntensity * distFade * distFade;
                if (lineAlpha > 0.02f) {
                    Draw.color(highlightColor, lineAlpha);
                    Lines.stroke(scanlineThickness);
                    Lines.line(ox - range, oy + scrollY, ox + range, oy + scrollY);
                }
            }
        }

        // —————————— ⑤ 像素化马赛克噪点 ——————————
        if (mosaicSize > 0f && mosaicChance > 0f) {
            Draw.z(Layer.effect + 0.15f);
            float range = coreRadius * 2f * sizeScale;
            for (float mx = -range; mx <= range; mx += mosaicSize) {
                for (float my = -range; my <= range; my += mosaicSize) {
                    if (Mathf.chance(mosaicChance * 0.3f)) {
                        Color noiseColor = Mathf.chance(0.5f) ? coreColor : contrastColor;
                        Draw.color(noiseColor, alpha * 0.5f * Mathf.random(0.5f, 1f));
                        Fill.rect(ox + mx, oy + my, mosaicSize * 0.9f, mosaicSize * 0.9f);
                    }
                }
            }
        }

        // —————————— ⑥ 数字噪块（随机闪烁方块） ——————————
        if (noiseBlockCount > 0) {
            Draw.z(Layer.effect + 0.25f);
            for (int i = 0; i < noiseBlockCount; i++) {
                // 噪块位置（基于seed，但随时间闪烁）
                float seedX = Mathf.randomSeed((long)(e.id + i * 37L + (long)(time * noiseFlickerRate * 5f)));
                float seedY = Mathf.randomSeed((long)(e.id + i * 53L + 200L + (long)(time * noiseFlickerRate * 5f)));
                float seedSize = Mathf.randomSeed((long)(e.id + i * 71L + 400L + (long)(time * noiseFlickerRate * 5f)));

                float nx = ox + (seedX - 0.5f) * coreRadius * 3f * sizeScale;
                float ny = oy + (seedY - 0.5f) * coreRadius * 2f * sizeScale;
                float ns = noiseBlockSize * seedSize * sizeScale;

                // 噪块颜色
                Color nc;
                int colorPick = ((int)Mathf.randomSeed((long)(e.id + i + (long)(time * 10f)))) % 3;
                nc = colorPick == 0 ? coreColor : (colorPick == 1 ? contrastColor : highlightColor);

                // 闪烁效果
                if (Mathf.chance(1f - noiseFlickerRate * 0.5f)) {
                    Draw.color(nc, alpha * (0.4f + seedSize * 0.6f));
                    Fill.rect(nx, ny, ns, ns);
                }
            }
        }

        // —————————— ⑦ 边缘闪光（模拟信号溢出） ——————————
        if (Mathf.chance(0.2f)) {
            Draw.z(Layer.effect + 0.35f);
            float flashR = coreRadius * 1.8f * sizeScale;
            Draw.color(contrastColor, alpha * 0.15f);
            Lines.stroke(3f);
            Lines.circle(ox, oy, flashR);
        }

        Draw.reset();
    }

    /**
     * 绘制故障形状（用于RGB通道分离的重复绘制）
     */
    private void drawGlitchShape(EffectContainer e, float cx, float cy, float time, float fin, float mult) {
        float r = coreRadius * sizeScale * (0.9f + 0.1f * Mathf.sin(time * 6f));
        float rot = coreRotationSpeed * time * mult;

        // 主形状
        if (coreSides <= 0) {
            Lines.stroke(2f * (1f - fin * 0.3f));
            Lines.circle(cx, cy, r);
        } else {
            Lines.stroke(2f * (1f - fin * 0.3f));
            Lines.poly(cx, cy, coreSides, r, rot);
        }

        // 内部交叉线（模拟数据干扰）
        if (Mathf.chance(0.5f)) {
            Lines.stroke(1f);
            Lines.line(cx - r, cy, cx + r, cy);
        }
        if (Mathf.chance(0.3f)) {
            Lines.stroke(1f);
            Lines.line(cx, cy - r, cx, cy + r);
        }

        // 随机短干扰线
        for (int i = 0; i < 3; i++) {
            if (Mathf.chance(0.4f)) {
                float ang = Mathf.randomSeed((long)(e.id + i * 17L + (long)(time * 3f))) * 360f;
                float len = r * (0.3f + 0.7f * Mathf.randomSeed((long)(e.id + i * 23L)));
                Lines.stroke(1.5f);
                Lines.line(
                    cx + Angles.trnsx(ang, r * 0.2f), cy + Angles.trnsy(ang, r * 0.2f),
                    cx + Angles.trnsx(ang, len), cy + Angles.trnsy(ang, len)
                );
            }
        }
    }
}
