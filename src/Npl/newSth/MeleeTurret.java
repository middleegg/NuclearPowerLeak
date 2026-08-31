package Npl.newSth;

import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import Npl.content.NuFx;

/**
 * 近战炮塔 · 继承 PowerTurret。
 * <p>
 * 不发射远程子弹，而是当敌人进入 range 时以朝向为方向触发 MeleeBulletType
 * （自带扇形伤害 + 刀光 effect）。表现像挥刀砍人。
 *
 * <p><b>范例（在 NuBlocks.load() 里定义）：</b>
 * <pre>{@code
 * meleeTurret = new MeleeTurret("meleeTurret"){{
 *     requirements(Category.turret, with(Items.copper, 50));
 *     size = 2;
 *     health = 1200;
 *     range = 50f;            // 注意：近战，range 就是刀光长度
 *     reload = 30f;           // 挥砍冷却（tick）
 *     shootSound = Sounds.pierce;
 *     consumePower(2f);
 *
 *     // 用默认 MeleeBulletType 或自定义参数
 *     shootType = new MeleeBulletType(){{
 *         damage = 50f;
 *         slashRange = 50f;     // 跟 range 一致
 *         slashArc = 90f;       // 扇形角度
 *         slashEffect = NuFx.slash;
 *     }};
 * }};
 * }</pre>
 *
 * <p>PowerTurret 的父类 Turret 会自动：
 * <ul>
 *   <li>瞄准目标、检测有效目标（range 内）</li>
 *   <li>reload 完毕后调用 shootType.create() 生成子弹（朝炮塔朝向）</li>
 *   <li>MeleeBulletType.init() 立刻扇形伤害 + 触发刀光 effect</li>
 * </ul>
 * 所以本类本身不需要写复杂逻辑，只做近战语义上的小调整。
 */
public class MeleeTurret extends PowerTurret{

    public MeleeTurret(String name){
        super(name);
        // 默认 bullet：MeleeBulletType（damage 30 / range 40 / arc 80）
        shootType = new MeleeBulletType(){{
            damage = 30f;
            slashRange = 40f;
            slashArc = 80f;
            slashEffect = NuFx.slash;
        }};
        // 近战炮塔不开震动 / 后坐
        shake = 0f;
        recoil = 0f;
    }

    @Override
    public void init(){
        super.init();
        // 让 shootType 的 slashRange 跟炮塔 range 保持一致（避免射程内但刀光够不到）
        if(shootType instanceof MeleeBulletType m){
            if(m.slashRange < range) m.slashRange = range;
        }
    }

    public class MeleeTurretBuild extends PowerTurretBuild{
        // PowerTurretBuild 默认 updateTile + shootout + draw 逻辑已足够：
        //   1. 找目标
        //   2. 朝目标方向旋转
        //   3. reload 完 → shootType.create(this, targetX, targetY, rotation)
        //   4. MeleeBulletType.init 立刻扇形伤害 + slash effect
        // 不需要覆写任何方法。
    }
}
