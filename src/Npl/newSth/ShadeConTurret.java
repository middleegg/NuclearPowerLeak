package Npl.newSth;

import arc.func.*;
import arc.*;
import arc.audio.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.world.meta.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.blocks.defense.turrets.PowerTurret;

/**
 * 阴影控制炮塔 · 继承 PowerTurret
 * <p>
 * 默认子弹 {@link SweepBulletType}（扇形扫描场）：对友方治疗 / 施加护盾，对敌方伤害。
 * <p>
 * 自动按以下优先级识别并瞄准目标（扫描场内的实际伤害 / 治疗 / 护盾由 SweepBulletType.update() 完成）：
 * <pre>
 *   ① 血量未满的友方（单位或建筑）—— 治疗扫描场
 *   ② 敌方（单位或建筑）—— 伤害扫描场
 *   ③ 满血友方单位 —— 缓慢施加护盾（见 SweepBulletType.shield）
 * </pre>
 * 炮塔只负责"瞄准 + 开火"，每发子弹是一个持续 lifetime 的扫描脉冲。
 * <p>
 * 使用时通常在 NuBlocks 里设置 range / reload / powerUse / consumePower 等，
 * 如需自定义扫描参数可直接覆盖 shootType 为新的 SweepBulletType。
 * <p>
 * <b>范例（在 NuBlocks.load() 里定义）：</b>
 * <pre>{@code
 * shadeConTurret = new ShadeConTurret("shadeConTurret"){{
 *     // —— 基础属性 ——
 *     requirements(Category.turret, with(
 *         NuItems.bigIron, 120,
 *         NuItems.monoSiliCrystal, 80,
 *         Items.graphite, 60
 *     ));
 *     size = 2;
 *     health = 1800;
 *     range = 140f;            // 射程 = 扫描半径（init() 会自动把 scanRadius 抬到 ≥ range）
 *     reload = 30f;            // 每发扫描脉冲的间隔（tick）
 *     shake = 0f;
 *     recoil = 0f;
 *     shootSound = Sounds.none;
 *     consumePower(6f);        // 耗电
 *
 *     // —— 自定义扫描子弹参数（覆盖默认）——
 *     shootType = new SweepBulletType(0f){{
 *         heal = 3f;                    // 受伤友方每次触发治疗量
 *         shield = 1.5f;                // 满血友方每次触发护盾增加量
 *         maxShieldRatio = 0.6f;        // 护盾上限 = 60% 最大血量
 *         scanRadius = 140f;            // 扫描半径（一般 ≥ 炮塔 range）
 *         fieldAngle = 100f;            // 扇形角度
 *         damageInterval = 5f;          // 伤害/治疗/护盾触发间隔（tick）
 *         lifetime = 30f;               // 单发扫描脉冲持续时长（tick）
 *         scanColor = NuColor.EnergyColor;
 *         healColor  = NuColor.EnergyLiColor;
 *     }};
 *
 *     // —— 可选：关闭对满血友方的护盾瞄准（只治疗受伤友方 + 攻击敌方）——
 *     // targetShielding = false;
 * }};
 * }</pre>
 */
public class ShadeConTurret extends PowerTurret{

    /** 是否把"满血友方单位"作为兜底目标（用于施加护盾）。false 时只瞄准①②类目标。 */
    public boolean targetShielding = true;

    /** 对友方单位的护盾上限（占单位 maxHealth 的比例）。
     *  此字段会在 init() 时同步到当前 shootType（如果是 SweepBulletType）的 maxShieldRatio。
     *  <p>例：0.5 → 每个友方单位最多叠 50% 最大血量的护盾；
     *  如果所有扫描到的满血友方护盾都已达到上限，炮塔会停止选该目标（从而停止射击，节省电力）。*/
    public float maxShieldRatio = 0.5f;

    public ShadeConTurret(String name){
        super(name);
        // 默认子弹：SweepBulletType。可在 new 时通过 shootType = ... 覆盖。
        shootType = new SweepBulletType(0f){{
            heal = 2f;          // 受伤友方每次触发治疗量
            shield = 1f;        // 满血友方每次触发护盾增加量
            scanRadius = 120f;
        }};
        targetAir = true;
        targetGround = true;
        targetBlocks = true;
        targetHealing = true;   // 使 canHeal() 为真，让原版射击流程允许对友方开火
        playerControllable = false;  // 像 TractorBeamTurret 一样不允许玩家手动瞄准/接管
    }

    @Override
    public void init(){
        super.init();
        // 把炮塔级护盾上限同步到子弹（保持两处一致，避免同时调两遍）
        if(shootType instanceof SweepBulletType s){
            s.maxShieldRatio = maxShieldRatio;
            if(s.scanRadius < range) s.scanRadius = range;
        }
    }

    /** 把扫描子弹的关键参数写进方块信息面板。
     *  注意：setStats 是 Block 类的方法，必须在外层 block 类里覆写，
     *  不能在 Build 子类里覆写（Building 没有 setStats 可覆盖）。 */
    @Override
    public void setStats(){
        super.setStats();
        if(shootType instanceof SweepBulletType s){
            // 修复量（受伤友方每次触发）
            if(s.heal > 0f){
                stats.add(Stat.healing, Strings.autoFixed(s.heal, 2));
            }
            // 护盾：满血友方每次叠加量 + 上限比例
            if(s.shield > 0f){
                stats.add(Stat.abilities,
                    Strings.autoFixed(s.shield, 2)
                    + " / 上限 " + Strings.autoFixed(maxShieldRatio * 100f, 0) + "% maxHealth");
            }
            // PointDefense：每次削减敌弹伤害
            if(s.pdDamage > 0f){
                stats.add(Stat.damage, Strings.autoFixed(s.pdDamage, 2));
            }
        }
    }

    public class ShadeConTurretBuild extends PowerTurretBuild{

        @Override
        protected void findTarget(){
            // 逻辑控制时不覆盖玩家 / 逻辑设定的目标
            if(logicControlled()) return;

            float r = range();
            Team team = this.team;
            float x = this.x, y = this.y;

            // —— ① 血量未满的友方（单位或建筑），选最近的 ——
            Posc best = null; float bestD = Float.MAX_VALUE;
            Unit damagedAlly = Units.closest(team, x, y, r, u -> !u.dead && u.health < u.maxHealth);
            if(damagedAlly != null){ best = damagedAlly; bestD = dst2(damagedAlly); }
            Building damagedAllyBuild = closestBuilding(b -> b.team == team && b.health < b.maxHealth());
            if(damagedAllyBuild != null){
                float d = dst2(damagedAllyBuild);
                if(d < bestD){ best = damagedAllyBuild; bestD = d; }
            }
            if(best != null){ target = best; return; }

            // —— ② 敌方（单位或建筑），选最近的 ——
            best = null; bestD = Float.MAX_VALUE;
            Unit enemyUnit = Units.closestEnemy(team, x, y, r, u -> !u.dead);
            if(enemyUnit != null){ best = enemyUnit; bestD = dst2(enemyUnit); }
            Building enemyBuild = closestBuilding(b -> b.team != team && b.team != Team.derelict);
            if(enemyBuild != null){
                float d = dst2(enemyBuild);
                if(d < bestD){ best = enemyBuild; bestD = d; }
            }
            if(best != null){ target = best; return; }

            // —— ③ 满血友方单位（用于施加护盾）—— 只选护盾还没到上限的，
            //    所有友方护盾都满时 → 没目标 → 不开火省电
            if(targetShielding){
                float msr = maxShieldRatio;
                Unit fullAlly = Units.closest(team, x, y, r,
                    u -> !u.dead
                        && u.health >= u.maxHealth
                        && u.shield < u.maxHealth * msr);
                if(fullAlly != null){ target = fullAlly; return; }
            }

            target = null;
        }
        @Override
        protected boolean validateTarget(){
            // 逻辑控制时交还原版判定，避免影响逻辑射击
            if(logicControlled()) return super.validateTarget();

            Posc t = target;
            if(t == null) return false;
            if(t instanceof Unit u){
                if(u.dead || !u.isValid() || !within(u, range())) return false;
                if(u.team == team && u.health >= u.maxHealth){
                    // 满血友方：只有护盾未达上限才有效，否则放掉让 findTarget 重选或直接不射击
                    return u.shield < u.maxHealth * maxShieldRatio;
                }
                return true;   // 受伤友方 / 敌方单位
            }
            if(t instanceof Building b){
                if(b.dead || !b.isValid() || !within(b, range())) return false;
                if(b.team == team) return b.health < b.maxHealth(); // 友方建筑仅受伤时有效
                return true;   // 敌方建筑
            }
            return false;
        }

        /** 在射程内寻找满足 pred 的最近建筑（排除自己，并尊重 buildingFilter） */
        private Building closestBuilding(Boolf<Building> pred){
            Building[] best = {null};
            float[] bestD = {Float.MAX_VALUE};
            float r = range();
            // 地图加载期 Groups.build 内部的 tree 可能尚未初始化，intersect 会抛 NPE（参考 crash 栈）
            try {
                Groups.build.intersect(x - r, y - r, r * 2f, r * 2f, b -> {
                    if(b == null || b == this) return;            // 排除空引用 + 自己
                    if(b.dead || !b.isValid()) return;
                    if(!buildingFilter.get(b)) return;            // 尊重方块的建筑过滤配置
                    if(!pred.get(b)) return;
                    float d = dst2(b);
                    if(d <= bestD[0] && d <= r * r){
                        bestD[0] = d;
                        best[0] = b;
                    }
                });
            } catch (NullPointerException ignored) {}
            return best[0];
        }
    }
}
