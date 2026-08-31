package Npl.newSth;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.abilities.Ability;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

/**
 * RingAuraAbility —— 通用环形进度条
 *
 * 两种模式：
 *   1. 外部进度源：设置 progressSource（返回 0~1），进度条实时反映外部状态
 *      例：{{ progressSource = () -> myAbility.timer / myAbility.cooldown; }}
 *   2. 内部充能：不设 progressSource，按 cycleTime 秒自动充能循环
 *
 * 视觉：暗色轨道 + 进度弧（颜色随进度从暗色渐变到光色，越满越亮）
 *       走满瞬间白色闪光、中心显示百分比数字
 */
public class RingAuraAbility extends Ability{

    /** 圆环半径 */
    public float radius = 80f;
    /** 内部充能一圈需要的时间（秒），仅 progressSource 为 null 时生效 */
    public float cycleTime = 3f;
    /** 外部进度源（返回 0~1），设置后优先使用 */
    public Prov<Float> progressSource = null;
    /** 进度 0% 时的颜色（暗色） */
    public Color darkColor = Color.valueOf("ff6b1a");
    /** 进度 100% 时的颜色（光色） */
    public Color lightColor = Color.valueOf("ffd37a");
    /** 环线宽 */
    public float strokeWidth = 3f;
    /** 满时闪光颜色 */
    public Color flashColor = Color.white;
    /** 是否在中心显示百分比数字 */
    public boolean showPercent = true;
    /** 闪光持续帧数 */
    public float flashDuration = 18f;

    /** 当前进度 0~1 */
    private float progress = 0f;
    private float flashTimer = 0f;
    private boolean wasFull = false;

    @Override
    public void update(Unit unit){
        super.update(unit);

        float p;
        if(progressSource != null){
            p = Mathf.clamp(progressSource.get());
        }else{
            p = progress + Time.delta / (cycleTime * 60f); // 内部充能，用字段累加，满后重置
            if(p >= 1f) p = 0f;
        }
        // 满 → 未满的瞬间触发闪光
        boolean full = p >= 0.999f;
        if(wasFull && !full){
            flashTimer = flashDuration;
        }
        wasFull = full;
        progress = p;

        if(flashTimer > 0f){
            flashTimer -= Time.delta;
        }
    }

    @Override
    public void draw(Unit unit){
        super.draw(unit);

        float ux = unit.x, uy = unit.y;

        Draw.z(Layer.effect);

        // 暗色轨道（完整圆圈，低透明度）
        Draw.color(darkColor, 0.15f);
        Lines.stroke(strokeWidth * 0.5f);
        Lines.circle(ux, uy, radius);

        // 进度弧：颜色随进度从暗色渐变到光色，接近满时更亮
        Color arcColor = darkColor.cpy().lerp(lightColor, progress);
        float alpha = 0.6f + 0.4f * progress; // 越满越亮
        Draw.color(arcColor, alpha);
        Lines.stroke(strokeWidth);
        Lines.arc(ux, uy, radius, progress);

        // 进度起点小刻度（0° 位置标记）
        float startX = ux + Mathf.cos(-Mathf.halfPi) * radius;
        float startY = uy + Mathf.sin(-Mathf.halfPi) * radius;
        Draw.color(arcColor, 0.5f);
        Fill.circle(startX, startY, strokeWidth * 0.6f);

        // 满时闪光：白色环形高亮，随时间淡出
        if(flashTimer > 0f){
            float f = flashTimer / flashDuration;
            Draw.color(flashColor, f * 0.8f);
            Lines.stroke(strokeWidth * 2f * f);
            Lines.circle(ux, uy, radius);
            Draw.color(flashColor, f * 0.3f);
            Fill.circle(ux, uy, radius * 0.3f * f);
        }

        // 中心百分比数字
        if(showPercent){
            int percent = (int)(progress * 100f);
            Font font = Fonts.outline != null ? Fonts.outline : Fonts.def;
            if(font != null){
                font.setColor(arcColor.r, arcColor.g, arcColor.b, alpha);
                font.draw(percent + "%", ux, uy + font.getLineHeight() / 2f, Align.center);
            }
        }

        Draw.reset();
    }
}
