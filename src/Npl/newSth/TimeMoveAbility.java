package Npl.newSth;

import arc.*;
import arc.math.*;
import arc.audio.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.*;
import mindustry.entities.abilities.Ability;
import mindustry.content.*;
import mindustry.entities.*;
import arc.input.*;
import mindustry.content.Fx;

import static mindustry.Vars.*;

/**
 * 【时移 Time Move】单位技能
 * 玩家按住 Shift 键 → 单位瞬移到鼠标位置（超过最大范围会拉回边界）
 * 两端会有 spawnWave 粒子特效 + 护盾音效，冷却结束可再次施放
 * 仅玩家自己控制的单位能放技能（AI 单位不触发避免乱瞬移）
 */
public class TimeMoveAbility extends Ability {
    /* ============ 可调参数（给不同单位用可以传不同值）============ */
    /** 冷却时间（tick，1 秒 = 60 tick） */
    public float reload = 50f;
    /** 最大瞬移距离（像素，1 格 = 64 像素 → 400 ≈ 6.25 格） */
    public float range = 400f;
    /** 释放时的音效 */
    public Sound sound = Sounds.shieldWave;
    /** 音效音量 0~1 */
    public float soundVolume = 0.7f;

    /* ============ 运行时状态（每个单位实例各一份）============ */
    /** 当前冷却剩余时间（tick）：0 = 冷却好，可以放技能 */
    private float cooldownLeft = 0f;
    public TimeMoveAbility(float reload, float cooldownLeft,float range) {
        this.reload = reload;
        this.cooldownLeft = cooldownLeft;
        this.range = range;
    }
    /* ================================================================
     *  以下 2 个方法 = Ability 基础方法（参考 NewHorizon BoostAbility）
     * ================================================================ */

    /**
     * 给每一个新生成的单位"克隆"一份技能对象 + 拷贝参数
     * （Mindustry 规定：每个 Unit 实例要有独立的 Ability 实例，不能共用同一份参数对象）
     */
    @Override
    public TimeMoveAbility copy() {
        // 先调用父类的 copy() 得到一份基础克隆（父类会自动复制它自己的字段）
        TimeMoveAbility out = (TimeMoveAbility) super.copy();
        // 再手动拷贝我们自己定义的字段（运行时状态 cooldownLeft 就不用拷了，新单位都是 0 冷却好的）
        out.reload = this.reload;
        out.range = this.range;
        out.sound = this.sound;
        out.soundVolume = this.soundVolume;
        return out;
    }

    /** 鼠标悬停单位 / 详情面板显示的【技能名称】，走 Bundle 国际化 */
    @Override
    public String localized() {
        // 如果没在 bundle 里写过 ability.nu-time-move.name，就返回默认中文兜底
        if (Core.bundle != null && Core.bundle.has("ability.nu-time-move.name")) {
            return Core.bundle.get("ability.nu-time-move.name");
        }
        return "时移";
    }

    /* ================================================================
     *  下面 = 时移技能真正"做事"的逻辑代码
     * ================================================================ */

    /**
     * Mindustry 每 1/60 秒（1 tick）调用一次 update(Unit)
     * 做三件事：① 冷却倒计时  ② 判断玩家有没有按 Shift  ③ 满足条件就瞬移 + 特效
     */
    @Override
    public void update(Unit u) {
        // —— Step 1: 冷却时间倒计时（Time.delta = 上一帧到这一帧经过了多少 tick，正常是 1）——
        if (cooldownLeft > 0) {
            cooldownLeft -= Time.delta;   // 扣 delta tick
            if (cooldownLeft < 0) cooldownLeft = 0f;   // 扣到负数就截成 0，防浮点误差
        }

        // —— Step 2: 还在冷却 → 什么都不做，直接 return ——
        if (cooldownLeft > 0f) return;

        // —— Step 3: 只允许【玩家自己控制的单位】触发
        //    AI 控制的单位（跟随/巡逻）不允许放技能，否则 AI 会乱按瞬移把单位瞬移飞
        if (!u.isPlayer()) return;

        // —— Step 4: 判断玩家有没有"按技能"（按住 Shift 左右键任意一个）
        //    注：Ability 类没有 isAbility() 方法，这里只用 Shift 触发
        boolean pressed = (Core.input != null
                          && (Core.input.keyDown(KeyCode.shiftLeft)
                              || Core.input.keyDown(KeyCode.shiftRight)));

        if (!pressed) return;   // 没按 → 等下一 tick

        // ================================================================
        //  ★ 走到这里说明：冷却好了 + 是玩家单位 + 按了 Shift = 开始执行瞬移 ★
        // ================================================================

        // —— Step 5: 算"要瞬移到哪里"（基于鼠标世界坐标）——
        float mouseX = Core.input.mouseWorldX();
        float mouseY = Core.input.mouseWorldY();
        float dx = mouseX - u.x;
        float dy = mouseY - u.y;
        float dist = Mathf.dst(dx, dy);   // 勾股定理算距离

        float targetX, targetY;
        if (dist <= range) {
            // 鼠标在范围内 → 直接瞬移过去
            targetX = mouseX;
            targetY = mouseY;
        } else {
            // 鼠标超过范围了 → 沿着"单位→鼠标"的方向，走"刚好 range 那么远"的位置
            //   （把鼠标点"拉回"到 range 半径的圆上，防止超距瞬移）
            float angle = Mathf.angle(dx, dy);           // 角度（0~360）
            targetX = u.x + Angles.trnsx(angle, range);  // 极坐标转 xy
            targetY = u.y + Angles.trnsy(angle, range);
        }

        // —— Step 6: 粒子特效 A：【原地】冒一圈蓝色单位出生圈（告诉玩家"从这里消失了"）——
        //    注：Fx.spawn 是 Mindustry 原版"单位被召唤"的蓝色圆形扩散特效，肯定存在
        Fx.spawn.at(u.x, u.y);

        // —— Step 7: 真正的瞬移 = 直接改单位 x/y 坐标 ——
        u.set(targetX, targetY);

        // —— Step 8: 粒子特效 B：【落点】再冒一圈蓝色出生圈（告诉玩家"落到这里了"）——
        Fx.spawn.at(targetX, targetY);

        // —— Step 9: 播放音效（只在目标点听得到，用 sound.at(x,y,pitch,volume) 立体声）——
        if (sound != null && sound != Sounds.none) {
            sound.at(targetX, targetY, 1f, soundVolume);
        }

        // —— Step 10: 重置冷却（进入 reload tick 的冷却期）——
        cooldownLeft = reload;
    }
}
