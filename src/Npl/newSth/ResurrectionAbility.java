package Npl.newSth;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.abilities.Ability;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

/**
 * 【复活技能 ResurrectionAbility】
 * =======================================================
 * 功能：
 *   1. 监测单位血量：在单位血量归 0（或者低于 0 刚要触发死亡）的瞬间，
 *      不触发死亡，立刻恢复 healPercent% 的最大血量（默认 20%）。
 *   2. 立即给单位附加 invincibleDuration 秒的无敌（默认 5 秒）
 *      无敌状态下任何伤害都无效（StatusEffects.invincible = healthMultiplier=+∞）
 *   3. 触发复活时弹出用户自定义文本 resurrectText（默认"复活！"）
 *      同时在屏幕底部提示玩家（如果这是玩家控制的单位）
 *   4. 单局只能触发 1 次（用 consumed 标记）
 *   5. 全部可调：healPercent / invincibleDuration / resurrectText / 特效 / 颜色
 *
 * 用法（FederalUnitType / 任何 UnitType）：
 *   abilities.add(new ResurrectionAbility(){{
 *       healPercent         = 0.20f;                // 回 20% 最大血
 *       invincibleDuration  = 60f * 5;              // 5 秒无敌（60 tick = 1 秒）
 *       resurrectText       = "复活！我还没输！";     // 自拟文本
 *       resurrectColor      = Color.valueOf("59F3AA"); // 浅绿色文本/血条色
 *   }});
 * =======================================================
 */
public class ResurrectionAbility extends Ability {

    /* ======================================================
     *                  对外可调参数
     * ====================================================== */

    /** 复活瞬间恢复最大血量的百分比（0.20 = 20%，可改成 0.5 = 半血复活）*/
    public float healPercent = 0.20f;
    /** 复活后多少 tick 无敌（60 = 1 秒，默认 5 秒 = 300 tick） */
    public float invincibleDuration = 60f * 5f;
    /** 复活时弹出的自定义文本（支持 bundle：@ability.xxx.text）*/
    public String resurrectText = "复活！";
    /** 复活时文本颜色、底部提示框的前景色、显示在信息栏的血条前景色 */
    public Color resurrectColor = Pal.heal.cpy();
    /** 复活时播的特效（在单位周围） */
    public Effect resurrectEffect = Fx.shieldApply;
    /** 复活时播放的音效（原版 100% 存在，避免找不到符号；想要静音就写 Sounds.none）*/
    public Sound resurrectSound = Sounds.shieldWave;
    /** 文本在单位头上悬浮多久 tick（默认 2 秒 = 120）*/
    public float floatTextDuration = 60f * 2f;
    /** 要不要在屏幕底部弹大字提示（仅对玩家控制的单位） */
    public boolean showBottomHint = true;
    /** 复活次数：还能复活多少次（默认 1 = 只能复活一次，和原来行为保持一致）
     *  配置示例：
     *    lifes = 0  → 完全不能复活（这个 ability 等于没用）
     *    lifes = 1  → 单局只能复活 1 次（原版）
     *    lifes = 3  → 最多可以复活 3 次（血量归零 3 次都被救回，第 4 次才真死）
     *    lifes < 0  → 无限复活（比如 boss 关卡那种死不了的，每死一次都满血拉回）
     */
    public int lifes = 1;

    /* ======================================================
     *                  运行时状态（内部）
     * ====================================================== */

    /** 还剩多少次可用（<= 0 表示用完； lifes<0 的话这个字段一直保持 -1，永远不扣到 0）*/
    private int lifesLeft;
    /** 无敌剩余倒计时 tick（给 displayBars 显示用，同时保证 update 不会又截胡）*/
    private float invincibleLeft;
    /** 复活文本浮字剩余倒计时（给 draw 画出来用）*/
    private float floatTextLeft;
    /** 浮字要显示的内容（如果 text 有换行符/表情等也要保持）*/
    private transient String floatTextContent;
    /** 记录单位原本的 maxHealth（防止 UnitType 动态改血后我们读不到默认）*/
    private transient float lastMaxHp = -1f;
    /** 复活触发那一瞬间的 x/y（浮字跟着这个位置走）*/
    private transient float tx, ty;

    /* ======================================================
     *                  基础 Ability 生命周期
     * ====================================================== */

    @Override
    public void init(UnitType type) {
        super.init(type);
        // 按配置的 lifes 初始化剩余次数：
        //   lifes < 0 → lifesLeft = -1（永远不扣到 0，无限复活）
        //   lifes = 0 → lifesLeft = 0（完全不能复活）
        //   lifes >= 1 → lifesLeft = lifes（正常多次复活）
        lifesLeft = (lifes < 0) ? -1 : lifes;
        invincibleLeft = 0f;
        floatTextLeft = 0f;
    }

    @Override
    public ResurrectionAbility copy() {
        ResurrectionAbility out = (ResurrectionAbility) super.copy();
        // Color 必须 cpy()，不然所有单位共享一个颜色引用，改一个全部变
        out.resurrectColor = resurrectColor.cpy();
        // 配置字段：从模板拷（让 new ResurrectionAbility{{ lifes=N; }} 这种写法有效）
        out.lifes = this.lifes;
        // 运行时状态不要从模板抄：每个出生的新单位都按 lifes 配置初始化剩余次数
        out.lifesLeft = (out.lifes < 0) ? -1 : out.lifes;
        out.invincibleLeft = 0f;
        out.floatTextLeft = 0f;
        out.lastMaxHp = -1f;
        return out;
    }

    @Override
    public String localized() {
        return Core.bundle == null ? "复活" :
            Core.bundle.get(getBundle(), "复活");
    }

    @Override
    public void addStats(Table t) {
        super.addStats(t);
        // super.addStats 会先读 bundle description，有就自动加了
        t.add(abilityStat("repairspeed", Strings.autoFixed(healPercent * 100f, 0) + "%"));
        t.row();
        t.add(abilityStat("cooldown",     Strings.autoFixed(invincibleDuration / 60f, 1)));
        t.row();
        // 新增：显示复活次数（无限/有限/禁用）
        t.add(abilityStat("units",
            lifes < 0 ? "无限" : (lifes == 0 ? "禁用" : (lifes + " 次"))
        ));
        t.row();
        if (resurrectText != null && !resurrectText.isEmpty()) {
            // 文本也放进 stats 里方便玩家看见写了啥
            t.add("[lightgray]复活台词：[]" + resurrectText).row();
        }
    }

    /** 在单位血条下加一个"复活已就绪/已耗尽"的小条，像 StatusEffect 一样可视化 */
    @Override
    public void displayBars(Unit unit, Table bars) {
        bars.add(new Bar(
            () -> {
                // lifes < 0 → 显示"复活 ∞ 次"（无限模式）
                if (lifes < 0) return "复活剩余：∞";
                // lifes == 0 → 禁用
                if (lifes == 0 || lifesLeft <= 0) return "复活已耗尽";
                // 有限次：显示剩余 X/总 Y
                return "复活剩余 " + lifesLeft + "/" + lifes;
            },
            () -> {
                // 无限 / 还有次数 → 主题色；耗尽 → 灰
                if (lifes < 0) return resurrectColor;
                return (lifesLeft <= 0) ? Color.lightGray : resurrectColor;
            },
            () -> {
                // lifes<0 → 永远 100%（满条）
                if (lifes < 0) return 1f;
                // lifes == 0 → 0%
                if (lifes == 0) return 0f;
                // 有限次：lifesLeft / lifes（把次数映射成比例）
                return Mathf.clamp(lifesLeft / (float) lifes, 0f, 1f);
            }
        )).growX().padBottom(4).row();

        // 如果正在无敌期，再额外加一个无敌倒计时条
        if (invincibleLeft > 0f) {
            float total = Math.max(invincibleDuration, 1f);
            bars.add(new Bar(
                () -> "无敌剩余 " + Strings.autoFixed(invincibleLeft / 60f, 1) + "s",
                () -> Pal.items,
                () -> Mathf.clamp(invincibleLeft / total, 0f, 1f)
            )).growX().padBottom(2).row();
        }
    }

    /* ======================================================
     *                  主逻辑 update（每帧 1/60 秒）
     * ====================================================== */

    @Override
    public void update(Unit unit) {
        // 先记住这只单位的最大血量（第一次 update 时存下来，防止 UnitType 被别的 mod 动态改）
        if (lastMaxHp <= 0f) lastMaxHp = unit.maxHealth;

        // 倒计时：无敌剩余
        if (invincibleLeft > 0f) {
            invincibleLeft -= Time.delta;
            if (invincibleLeft < 0f) invincibleLeft = 0f;
        }
        // 倒计时：浮字剩余
        if (floatTextLeft > 0f) {
            floatTextLeft -= Time.delta;
            if (floatTextLeft < 0f) floatTextLeft = 0f;
        }

        // 剩余次数 = 0（且不是无限模式）→ 啥都别干了，等死就行
        // lifesLeft == 0 且 lifes >= 0 → 用完了
        if (lifes >= 0 && lifesLeft <= 0) return;

        // —— 关键：截胡死亡瞬间 ——
        // Mindustry 的死亡顺序：unit.health <= 0 → UnitComp.killed() → dead=true → ability.death() → remove()
        // 我们的 update() 跑在 killed() 之前的同一帧，所以只要 health <= 0 就立刻在这帧救回来
        if (unit.health <= 0f) {
            // 1. 扣次数（无限模式 lifes<0 的话，lifesLeft=-1，这里直接跳过减法，永远不会到 0）
            if (lifesLeft > 0) lifesLeft--;

            // 2. 拉回血量：healPercent% × 最大血（至少保底拉到 1hp 以防被 float 精度再判死）
            float maxHp = unit.maxHealth > 0 ? unit.maxHealth : Math.max(lastMaxHp, 1f);
            float target = maxHp * Mathf.clamp(healPercent, 0f, 1f);
            if (target < 1f) target = 1f;                    // 至少回 1 点
            unit.health = target;

            // 3. 清除死亡标记（防止下一个逻辑 tick 再进 killed()）
            unit.dead = false;

            // 4. 给无敌：StatusEffects.invincible（healthMultiplier=+∞，等于任何伤害都打不出血）
            unit.apply(StatusEffects.invincible, invincibleDuration);
            invincibleLeft = invincibleDuration;

            // 5. 特效 + 音效（和原版 ForceField shieldApply 一个味道）
            if (resurrectEffect != null) {
                resurrectEffect.at(unit.x, unit.y, Math.max(unit.hitSize, 40f), resurrectColor, this);
            }
            if (resurrectSound != null && resurrectSound != Sounds.none) {
                resurrectSound.at(unit, 1f, 1f);
            }

            // 6. 浮字触发位置 & 内容（存给 draw() 慢慢画）
            tx = unit.x;
            ty = unit.y + unit.hitSize * 0.6f + 12f;
            // 多次复活时，把剩余次数追加进去（比如"复活！(2/3)"），让玩家知道还剩几次
            String baseText = (resurrectText == null || resurrectText.isEmpty()) ? "复活！" : resurrectText;
            if (lifes < 0) {
                floatTextContent = baseText + " (∞)";
            } else if (lifes >= 2) {
                floatTextContent = baseText + " (" + lifesLeft + "/" + lifes + ")";
            } else {
                floatTextContent = baseText; // lifes == 1 时不追加，保持和原版完全一致
            }
            floatTextLeft = floatTextDuration;

            // 7. 屏幕底部弹大字（仅当这个单位是玩家操控的，避免一堆单位复活刷屏）
            if (showBottomHint && unit.isPlayer() && ui != null && ui.hudfrag != null) {
                // HudFragment.setHudText(CharSequence) 会在屏幕顶部/底部 boss 提示位置出 2~3 秒渐隐文字
                try {
                    java.lang.reflect.Method m = ui.hudfrag.getClass().getMethod("setHudText", CharSequence.class);
                    m.invoke(ui.hudfrag, "[#" + resurrectColor + "]" + floatTextContent);
                } catch (Throwable ignored) {
                    // 老版本没有 setHudText 的话直接退化：什么额外的 HUD 都不做，只保留单位头顶浮字
                }
            }
        }
    }

    /* ======================================================
     *                  单位头上方的浮动文字 + 光效
     * ====================================================== */

    @Override
    public void draw(Unit unit) {
        if (floatTextLeft <= 0f || floatTextContent == null) return;

        // 浮字生命周期：0→floatTextDuration，转成 0→1 进度 f
        float total = Math.max(floatTextDuration, 0.0001f);
        float f = 1f - (floatTextLeft / total);  // 0=刚开始弹，1=马上结束
        if (f <= 0f || f >= 1f) return;

        // 1) 浮字位置：慢慢向上升（+20 像素的浮升）
        float rise = Interp.pow2Out.apply(f) * 20f;
        float x = tx;
        float y = ty + rise;

        // 2) 透明度：前 15% 淡入，后 25% 淡出（中段 100% 清晰）
        float a;
        if (f < 0.15f)      a = Mathf.clamp(f / 0.15f, 0f, 1f);
        else if (f > 0.75f) a = Mathf.clamp((1f - f) / 0.25f, 0f, 1f);
        else                a = 1f;

        // 3) 描边文字：先黑色影子描一下，再彩色主字叠上去
        if (!headless) {
            Font font = Fonts.outline;
            GlyphLayout lay = Pools.obtain(GlyphLayout.class, () -> new GlyphLayout(font, floatTextContent));
            lay.setText(font, floatTextContent);
            float scale = 1.1f + 0.25f * Mathf.sin(f * Mathf.PI); // 呼吸式缩放一点点

            // 底部黑边（4 方向偏移 1 像素）
            font.getData().setScale(scale);
            font.getCache().clear();
            font.getCache().setColor(0f, 0f, 0f, a * 0.9f);
            float bx = x - lay.width / 2f;
            float by = y + lay.height / 2f;
            for (int dx = -1; dx <= 1; dx += 2) for (int dy = -1; dy <= 1; dy += 2) {
                font.getCache().addText(floatTextContent, bx + dx, by + dy, lay.width, Align.center, false);
            }
            font.getCache().draw(a);

            // 彩色主文字
            font.getCache().clear();
            font.getCache().setColor(new Color(resurrectColor).a(a));
            font.getCache().addText(floatTextContent, bx, by, lay.width, Align.center, false);
            font.getCache().draw(a);

            Pools.free(lay);
            font.getData().setScale(1f);
        }
    }

    /* ======================================================
     *                  对外辅助方法
     * ====================================================== */

    /** 还剩多少次复活（负数 = 无限次） */
    public int getLifesLeft() { return lifesLeft; }
    /** 是不是已经用完复活次数了（无限次永远返回 false） */
    public boolean consumed() { return lifes >= 0 && lifesLeft <= 0; }

    /** 强制重置（比如某回合/某个核心被占领时手动重开一局再让它可以复活）
     *  - 不传参：按当前 lifes 配置重置为初始次数
     *  - 传 int newLifes：连模板 lifes 配置一起改，再按 newLifes 重置
     */
    public void resetUsed() { lifesLeft = (lifes < 0) ? -1 : lifes; }
    public void resetUsed(int newLifes) { this.lifes = newLifes; resetUsed(); }

    /** 现在是不是复活后的无敌期 */
    public boolean isInvincibleActive() { return invincibleLeft > 0f; }
}
