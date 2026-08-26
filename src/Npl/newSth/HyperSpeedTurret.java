package Npl.newSth;

import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.meta.*;

/**
 * HyperSpeedTurret —— 「越打同一个目标越快，脱战按秒衰减」的攻速叠层炮塔（继承 PowerTurret）。
 * <p>
 * 叠层规则：<b>直接控制每发子弹的等效 reloadMultiplier 倍率</b>。
 *   同目标每打一发 → curReloadMul += 0.05，最高 maxReloadMul（默认 3.0）
 *   无目标每秒     → curReloadMul -= decayPerSecond（默认 0.15），直到底 1.0
 *   目标切换       → 按 resetOnTargetSwitch（默认 true）直接回 1.0
 *
 * 真正"让 reload 加速"的方法：
 *   不再在 updateShooting 里"补 reloadCounter"，而是**直接重写 updateReload()**，
 *   把 Mindustry 原版公式改写成：
 *
 *     reloadCounter += delta * efficiency * baseReloadSpeed() * peekAmmo().reloadMultiplier * curReloadMul
 *
 *   这个公式与用户要求的「每发 +0.05 让下一发 reloadMultiplier 叠加到 3」完全等价。
 *
 * 配套子弹首选 {@link HyperSpeedBulletType}（PointBulletType）。
 *
 * ============== 使用范例（在 NuBlocks.load() 里） ==============
 * <pre>{@code
 * awnlessSpike = new HyperSpeedTurret("awnlessSpike"){{
 *     requirements(Category.turret, with(
 *         NuItems.bigIron, 80, Items.silicon, 120, Items.plastanium, 50
 *     ));
 *     size = 2;
 *     health = 2400;
 *     range = 180f;
 *     reload = 30f;                     // 基础 reload 30 tick
 *     shootCone = 20f;
 *     shake = 2f;
 *     recoil = 2f;
 *     shootSound = Sounds.shoot;
 *     consumePower(8f);
 *
 *     // —— 叠层参数（下面都是默认值，演示用可以不加）——
 *     perHitBonus     = 0.05f;          // 每打同目标 +5%
 *     maxReloadMul    = 3.0f;           // 最高 3 倍
 *     decayPerSecond  = 0.15f;          // 无目标每秒掉 15%
 *
 *     // —— 推荐用 HyperSpeedBulletType，吃满 v^1.1 伤害增益 ——
 *     shootType = new HyperSpeedBulletType(5f, 60f){{
 *         damageScale = 1.5f;            // 默认就是 1.5
 *         speedExp    = 1.1f;            // 默认就是 1.1
 *         hitSize = 8f;
 *         lifetime = 60f;
 *         pierce = true;
 *         pierceCap = 3;
 *         // — PointBulletType 拖尾参数（和 FederalUnitTypes 中其它 Point 弹保持一致）
 *         trailEffect   = NuFx.DespSmokeTail;
 *         trailSpacing  = 3f;
 *         trailInterval = 2f;
 *         hitEffect = Fx.flakExplosion;
 *         shootEffect = Fx.shootBig;
 *     }};
 * }};
 * }</pre>
 */
public class HyperSpeedTurret extends PowerTurret {

    /* ============== 可配置：叠层规则 ============== */

    /** 每发射同目标一发，curReloadMul 加多少（默认 +0.05 → 等效 reloadMultiplier 又乘 1.05 倍） */
    public float perHitBonus = 0.05f;

    /** curReloadMul 叠加上限（默认 3.0 即 300% 速度） */
    public float maxReloadMul = 3.0f;

    /** 没目标时每秒线性衰减多少（默认 0.15 → 每秒 15%），直到回 1.0 */
    public float decayPerSecond = 0.15f;

    /** 目标切换后是否直接清空回到 1.0（默认 true）。false = 换目标也只按 decayPerSecond 衰减 */
    public boolean resetOnTargetSwitch = true;

    /* ============== 构造 ============== */

    public HyperSpeedTurret(String name){
        super(name);
    }

    @Override
    public void setStats(){
        super.setStats();

        int stackToCap = (int) Math.ceil((maxReloadMul - 1f) / perHitBonus);
        float backToBaseSec = (maxReloadMul - 1f) / decayPerSecond;
        stats.add(Stat.abilities,
                "同目标叠层：每发等效 reloadMultiplier +"
                        + Strings.autoFixed(perHitBonus * 100f, 0)
                        + "%，最高 " + Strings.autoFixed(maxReloadMul * 100f, 0) + "%（约 " + stackToCap + " 发叠满）"
                        + "；无目标每秒衰减 " + Strings.autoFixed(decayPerSecond * 100f, 0) + "%（叠满回底约 " + Strings.autoFixed(backToBaseSec, 1) + " 秒）");

        if (shootType instanceof HyperSpeedBulletType hs){
            stats.add(Stat.damage,
                    Strings.autoFixed(hs.damage, 2)
                            + " + " + Strings.autoFixed(hs.damageScale, 2)
                            + " × v^" + Strings.autoFixed(hs.speedExp, 2)
                            + "（v = 60 / reload × ammo.reloadMult × efficiency × curReloadMul）");
        }
    }

    /* ============== Build ============== */

    public class HyperSpeedTurretBuild extends PowerTurretBuild {

        /**
         * 当前"等效 reloadMultiplier"倍率。
         *   1.0 = 原版速度
         *   3.0 = 冷却推进 3 倍快（即等效 reloadMultiplier 乘 3）
         */
        public float curReloadMul = 1.0f;

        /** 上一发的目标 identity */
        public Object lastTargetKey = null;

        /** 无目标累计了多少秒（小数），用来做每秒 decayPerSecond 的衰减 */
        public float decayAccum = 0f;

        // ===================================================================
        //  1. updateReload：先调用原版 super.updateReload() 跑完整的 hasAmmo/液体/ammo 门控，
        //     再用"原版这一帧推进了多少 reloadCounter"做差，额外补 (curReloadMul - 1) 倍。
        //     这样原版任何检查都不会跳过，curReloadMul=1 时与纯原版行为完全一致。
        // ===================================================================
        @Override
        public void updateReload(){
            float before = reloadCounter;
            // 原版推进（内部含 hasAmmo / liquidBoost / peekAmmo().reloadMultiplier / efficiency 等全套判断）
            super.updateReload();
            float origInc = reloadCounter - before;
            if (origInc <= 0f || curReloadMul <= 1f) return;

            // 额外再补 (curReloadMul - 1) × 原版推进量 → 总推进 ≈ origInc × curReloadMul
            float extra = origInc * (curReloadMul - 1f);
            reloadCounter += extra;
            // clip: 原版一般只要求 reloadCounter>=1 就会开火，防止极端情况下 float 爆值
            if (reloadCounter > 10f) reloadCounter = 10f;
        }

        // ===================================================================
        //  2. updateTile：目标判定 + 换目标重置 + 无目标衰减
        // ===================================================================
        @Override
        public void updateTile(){
            super.updateTile();

            // — 判定是否还在盯着同一个有效目标 —
            boolean hasValid = false;
            Object curKey = null;
            if (target != null){
                if (target instanceof Unit u && u.isValid() && !u.dead){
                    curKey = new UnitKey(u.id());
                    hasValid = within(u, range());
                } else if (target instanceof Building b && b.isValid() && !b.dead){
                    curKey = new BuildKey(b.tileX(), b.tileY());
                    hasValid = within(b, range());
                }
            }

            if (!hasValid){
                // — 没目标：每秒衰减 decayPerSecond —
                decayAccum += Time.delta;
                while (decayAccum >= 1f && curReloadMul > 1f){
                    curReloadMul = Math.max(1f, curReloadMul - decayPerSecond);
                    decayAccum -= 1f;
                }
                if (curReloadMul <= 1f){ curReloadMul = 1f; decayAccum = 0f; }
                lastTargetKey = null;
                return;
            }

            // — 有目标：换目标则清空 —
            if (resetOnTargetSwitch && lastTargetKey != null && !lastTargetKey.equals(curKey)){
                curReloadMul = 1.0f;
                decayAccum = 0f;
            }
            lastTargetKey = curKey;
            decayAccum = 0f;
        }

        // ===================================================================
        //  3. shoot(type)：每打一发叠 perHitBonus
        // ===================================================================
        @Override
        protected void shoot(BulletType type){
            Object curKey = null;
            if (target instanceof Unit u && u.isValid() && !u.dead){
                curKey = new UnitKey(u.id());
            } else if (target instanceof Building b && b.isValid() && !b.dead){
                curKey = new BuildKey(b.tileX(), b.tileY());
            }

            if (curKey != null){
                boolean same = (lastTargetKey != null) && lastTargetKey.equals(curKey);
                if (same){
                    curReloadMul = Math.min(maxReloadMul, curReloadMul + perHitBonus);
                } else if (resetOnTargetSwitch){
                    // 换了目标：第一发也算 +1 层（避免"换目标后第一发不叠层"）
                    curReloadMul = Math.min(maxReloadMul, 1f + perHitBonus);
                }
                lastTargetKey = curKey;
                decayAccum = 0f;
            }

            super.shoot(type);
        }

        // ===================================================================
        //  4. 给 HyperSpeedBulletType 读取当前 v = 每秒攻击次数
        // ===================================================================
        public float currentShotsPerSecond(){
            if (reload <= 0f) return 1f;
            float base = 60f / reload;
            BulletType ammo = peekAmmo();
            float ammorm = (ammo != null) ? ammo.reloadMultiplier : 1f;
            float eff = Math.max(0.0001f, efficiency);
            return base * ammorm * eff * curReloadMul;
        }
    }

    /* ============== identity keys ============== */
    private record UnitKey(int id){
        @Override public boolean equals(Object o){ return o instanceof UnitKey u && u.id == id; }
        @Override public int hashCode(){ return id; }
    }
    private record BuildKey(int tx, int ty){
        @Override public boolean equals(Object o){ return o instanceof BuildKey b && b.tx == tx && b.ty == ty; }
        @Override public int hashCode(){ return tx * 73856093 ^ ty * 19349663; }
    }
}
