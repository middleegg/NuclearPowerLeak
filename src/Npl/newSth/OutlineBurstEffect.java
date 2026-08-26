package Npl.newSth;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import arc.func.Cons;

import static arc.graphics.g2d.Draw.*;

/**
 * OutlineBurstEffect —— "整单位描边爆发 → 结束接下一个特效"
 *
 * 用法：
 *   // 基础：在单位身上触发，描边紫色变粗 40 tick，结束后放 NuFx.ExplosionWhite
 *   NuFx.outlinePurpleBurst.at(unit.x, unit.y, 0, unit);
 *
 *   // 或自定义参数
 *   Effect fx = new OutlineBurstEffect(50f, e -> {}){{
 *       outlineColor = new Color(0xAA44FFff);
 *       outlineFrom  = 1.5f;
 *       outlineTo    = 6.0f;
 *       alphaFrom    = 0.2f;
 *       alphaTo      = 0.9f;
 *       drawWeapons  = true;
 *       nextEffect   = NuFx.RightsExplosion;   // 结束后触发
 *   }};
 *   fx.at(unit.x, unit.y, 0, unit);
 *
 * 行为：
 *   1. 整个 lifetime 期间，父单位每帧被额外画一遍：
 *      - outlineRegion（身体 + 所有 top=false 的武器 outline）
 *      - 颜色 = outlineColor
 *      - 厚度/alpha 在 (outlineFrom, alphaFrom) → (outlineTo, alphaTo) 之间按 e.fin() 线性插值
 *      - 若 useAdditive=true，则用加色混合（更有"能量感"）
 *   2. lifetime 最后一帧（e.fin() >= 1）自动调 nextEffect.at(parent.x, parent.y, e.color, parent)
 *      - nextEffect 可以是任意 Effect，传 null 就跳过
 *
 * 注意！必须把 Effect 挂在父单位上：必须 at(..., parent)，否则不知道去画谁。
 */
public class OutlineBurstEffect extends Effect {

    // —— 颜色 / 厚度
    public Color outlineColor = new Color(0xAA44FFff);
    public float outlineFrom   = 1.0f;   // 初始厚度（相对原始 outlineRadius 的倍数）
    public float outlineTo     = 4.5f;   // 结束厚度
    public float alphaFrom     = 0.25f;  // 初始透明度
    public float alphaTo       = 0.9f;   // 结束透明度
    public Interp interp       = Interp.pow2Out;

    // —— 绘制开关
    public boolean useAdditive = true;   // 使用加色混合（能量感强）
    public boolean drawOutlines = true;  // 总开关：false 就只做延迟触发器
    public boolean drawWeapons = true;   // 是否也画武器 outline（top=false 的武器）

    // —— 结束后触发的下一个特效
    public Effect nextEffect = null;

    /** 上一帧的 e.fin() 进度（用来判断是否刚好到结尾，只触发一次 nextEffect） */
    private float lastFin = -1f;

    public OutlineBurstEffect(float lifetime) {
        super(lifetime, 300f, e -> {});   // clipsize=300，单位最大 ~ hitSize ~50 的 6 倍
    }

    public OutlineBurstEffect(float lifetime, Cons<EffectContainer> cons) {
        super(lifetime, 300f, cons);
    }

    @Override
    public void init() {
        // 保证绘制层在单位本体之上、子弹/特效之下（这样即使和单位同帧也能看到描边在上方）
        clip = Layer.effect + 0.001f;
        super.init();
    }

    @Override
    public void render(EffectContainer e) {
        Unit parent = (e.data instanceof Unit u) ? u : null;
        if (parent == null) return;

        float fin = e.fin();
        float t = interp.apply(fin);

        float outlineScl  = Mathf.lerp(outlineFrom, outlineTo, t);
        float alpha       = Mathf.lerp(alphaFrom, alphaTo, t);

        // ==================== 1. 画加粗紫色描边 ====================
        if (drawOutlines && alpha > 0.001f) {
            if (useAdditive) Draw.blend(Blending.additive);
            Color prev = Tmp.c1.set(Draw.getColor());
            Draw.color(outlineColor, Mathf.clamp(alpha));

            float rot = parent.rotation - 90f;
            UnitType type = parent.type;

            // —— 身体 outline（type.outlineRegion 或 自动生成的 region）
            if (type.outlineRegion != null && type.outlineRegion.found()) {
                drawScaledOutline(type.outlineRegion, parent.x, parent.y, rot, outlineScl, type.outlineRadius);
            }

            // —— 武器 outline（仅 top=false 的武器，它们的 outline 应该出现在身体下面；这里都画一遍，反正画在父图上面层也无所谓）
            if (drawWeapons && type.weapons != null) {
                for (int i = 0; i < type.weapons.size; i++) {
                    Weapon w = type.weapons.get(i);
                    // 武器的位置（不处理 mount 的实际开火旋转，取 unit.rotation，视觉上够了）
                    float wx = parent.x + Angles.trnsx(parent.rotation, w.x, w.y);
                    float wy = parent.y + Angles.trnsy(parent.rotation, w.x, w.y);
                    float wr = parent.rotation - 90f + (w.rotate ? parent.rotation : 0f);
                    if (w.outlineRegion != null && w.outlineRegion.found()) {
                        drawScaledOutline(w.outlineRegion, wx, wy, wr, outlineScl, type.outlineRadius);
                    }
                }
            }

            Draw.color(prev);
            if (useAdditive) Draw.blend();
        }

        // ==================== 2. 最后一帧触发下一个 Effect ====================
        if (nextEffect != null && lastFin < 1f && fin >= 1f) {
            // 这里用 parent.x/y（不是 e.x/y），因为父单位可能在运动中
            nextEffect.at(parent.x, parent.y, e.rotation, e.color == null ? outlineColor : e.color, parent);
        }
        lastFin = fin;
    }

    /**
     * 用缩放方式在贴图四周"显得更厚"。
     * Mindustry 原生 outline 贴图只有固定像素，没有真正的"像素描边扩张"。
     * 这里采用"把 outlineRegion 按 scl 放大 + alpha 叠加 2 次"的方式模拟加粗，
     * 在视觉上能达到明显"更粗的紫色外框"且不依赖额外贴图。
     */
    private void drawScaledOutline(TextureRegion region, float x, float y, float rot, float scl, int baseOutlineRadius) {
        float w = region.width * Draw.scl * scl;
        float h = region.height * Draw.scl * scl;

        // 第一层：最外层，大一点
        Draw.rect(region, x, y, w, h, rot);

        // 第二层：稍小一点（厚度更有层次，像加粗描边）
        float s2 = Math.max(1f, scl * 0.82f);
        Draw.rect(region, x, y, region.width * Draw.scl * s2, region.height * Draw.scl * s2, rot);
    }
}
