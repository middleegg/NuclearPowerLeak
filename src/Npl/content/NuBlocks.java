package Npl.content;

import static mindustry.Vars.*;
import static mindustry.type.ItemStack.*;
import Npl.newSth.*;
import Npl.content.NuItems;
import Npl.content.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import mindustry.*;
import mindustry.world.consumers.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.entities.part.DrawPart.*;
import mindustry.entities.part.*;
import mindustry.entities.pattern.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.type.unit.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.campaign.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.blocks.legacy.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;
import mindustry.ui.dialogs.*;
import arc.util.Log;   // 👈 Log 属于 arc 框架，不是 Mindustry！

import static mindustry.Vars.*;
import static mindustry.type.ItemStack.*;


public class NuBlocks {
    // ============= 静态方块声明：以后在这儿加就行 =============
    public static Block
            redenmore,
            conTest,
            Testonme,      // ConfigurableBlock（旧版纯物品多配方）
            hunfuTest,     // ✅ 新加：HunfuBlock 混成工厂（物品+液体双参与）
            antiStealthRadar, // ✅ 反隐探测雷达
            stormCrafter,
            however,
            hyw,  // ✅ 闪电风暴工厂：三阶段视觉 + LightningBullet 发射
            coinProducer, // ✅ 铸币厂（消耗原料生产 coins，直接入系统）
            coinConsumer; // ✅ 高级工厂（消耗 coins 生产物品，直接从系统扣）

    public static void load() {

        Testonme = new ConfigurableBlock("testonme"){{
            requirements(Category.crafting, with(
                    NuItems.bigIron,200,
                    Items.lead,   150,
                    Items.silicon, 60,
                    Items.graphite, 50
            ));
            size = 2;
            health= 1000;
            craftEffect = NuFx.lightningStormFull;
            plans = Seq.with(
                    new Plan(with(NuItems.pumice,3),30f,with(NuItems.bigIron,2,NuItems.sulFurFrag,2)),
                    new Plan(with(NuItems.uranium,4),75f,with(NuItems.uranCrystal,2,NuItems.sulFurFrag,3)),
                    new Plan(with(NuItems.bottledMagenticStorm,1),90f,with(NuItems.magent,2,NuItems.alkSliver,5))
            );
            consumePower(4.5f);
        }};
        redenmore = new GenericCrafter("redenmore") {{
            requirements(Category.crafting, with(NuItems.bigIron,40));
            outputItem = new ItemStack(NuItems.magent,1);
            craftTime =60f;
            hasItems=hasPower=true;
            ambientSound=Sounds.loopGrind;
            ambientSoundVolume=0.025f;
            consumeItems(ItemStack.with(Npl.content.NuItems.bigIron,3));
            consumePower(2.1f);
            size=2;
        }};

        // =================================================================
        // ✅ 风暴合成器（storm-crafter）
        // =================================================================
        // 三阶段视觉 + 真正发射原版 LightningBulletType：
        //   Phase1 [0 ~ 5 秒]：光圈 + 5 粒子一波从中心发散并变大（只跑一次不复用）
        //   Phase2 [5 秒 ~ 约 7 秒]：保留光球，外圈 40 格处弧线扫一圈成完整圆环
        //   Phase3 [圆环填满后永久]：圆环保留 + 高亮扫弧不断再转一圈 + 持续向 360° 发射 LightningBullet
        //
        // 不再使用 craftEffect 调用 Effect（Effect 有 lifetime 无法永久），
        // 而是在 StormCrafterBlock.Building 的 updateTile()/draw() 里自己做状态机和渲染。
        // =================================================================
        stormCrafter = new StormCrafterBlock("storm-crafter") {{
            requirements(Category.crafting, with(
                    NuItems.bigIron, 60,
                    Items.lead,      80,
                    Items.silicon,   40
            ));
            size = 2;
            health = 800;
            hasItems = hasPower = true;
            // 合成本身速度独立（和三阶段视觉不挂钩，用户可自改）
            craftTime = 200f;
            outputItem = new ItemStack(NuItems.magent, 1);
            consumeItems(ItemStack.with(NuItems.bigIron, 2));
            consumePower(3.0f);
            // ==================== 风暴专属字段 ====================
            stormColor       = new Color(0x6F9BFFff);
            stormBrightColor = new Color(0xE3F2FDff);
            stormGlowColor   = new Color(0x3F5FFFaa);
            // Phase1（5 秒）粒子：
            phase1Duration       = 1200f;   // 20 秒
            particleWaveInterval = 30f;    // 每0.5秒一波
            particlePerWave      = 9;      // 每波9个
            particleSpeed        = 1f;
            particleSizeFrom     = 1f;
            particleSizeTo       = 4f;
            particleMaxDist      = 230f;
            // 光球 / 光圈（光球缩小，并降低脉动幅度，避免闪烁）：
            coreSize         = 4f;
            innerRingRadius  = 4f;
            innerRingWidth   = 6f;
            coreGlowLayers   = 3;
            coreGlowMul      = 3f;
            // ★ 降低脉动：之前默认 speed=8, amp=0.1 → 看起来一闪一闪
            //   现在速度降到 1.5（很慢），幅度降到 0.03（几乎不抖）
            corePulseSpeed   = 1.5f;
            corePulseAmp     = 0.03f;
            // 外圈圆环（拉近到 20 格 = 160 px）：
            outerRingRadius       = 40f;   // = 160
            outerRingWidth        = 3f;
            ringSweepSpeedPhase2  = 3f;         // 3°/tick，约 2 秒扫满一圈
            ringSweepSpeedPhase3  = 1.8f;       // 填满后循环扫弧速度
            ringSweepSweepAngle   = 45f;        // 高亮扫弧段长 45°
            ringSweepWidth        = 4f;
            // 光照：
            lightRadius = 260f;

            // ==================== Phase3 发射 LightningBullet ====================
            //   ⚠ 这里用的是 Mindustry 原版 LightningBulletType，
            //     直接走 bt.create(this, team, x, y, angle)，
            //     不是手动画闪电折线（之前画的线铺满全屏那种彻底去掉）。
            lightningBullet = new LightningBulletType() {{
                damage               = 32f;
                lightningLength      = 12;
                lightningLengthRand  = 5;
                lightningColor       = new Color(0xE3F2FDff);
                // 二级分支链闪（连锁伤害）
                lightningType        = new LightningBulletType() {{
                    damage           = 7f;
                    lightningLength  = 10;
                    lightningColor   = new Color(0xB3E5FCff);
                }};
                hittable          = true;
                absorbable         = false;
                collidesGround     = true;
                collidesAir        = true;
                collidesTiles      = true;
            }};
            lightningFireInterval = 15f;        // 每 8 tick 发射一次（更频繁）
            lightningFireCountMin = 3;        // 每次最少 2 条
            lightningFireCountMax = 10;        // 最多 5 条（实际数量随机）
            lightningLengthMin    = 8;       // 闪电最短 8 格
            lightningLengthMax    = 42;      // 闪电最长 22 格
        }};
        hunfuTest = new HunfuBlock("hunfu-test") {{
            requirements(Category.crafting, with(
                    NuItems.bigIron, 100,
                    Items.lead,  80,
                    Items.silicon, 40,
                    Items.graphite, 40
            ));
            size = 2;
            health = 800;
            consumePower(6.0f);   // 混成工厂耗电稍高

            plans = Seq.with(
                    // ─────────────────────────────────────
                    // 配方 1：纯物品（向后兼容 ConfigurableBlock）
                    //   原料：煤×2 + 铜×1  →  产物：钛×2 + 硅×1   （90 tick = 1.5 秒）
                    // ─────────────────────────────────────
                    new HunfuBlock.Plan(
                            with(Items.titanium, 2, Items.silicon, 1),    // outItem（物品产物）
                            90f,                                            // time
                            with(Items.coal, 2, Items.copper, 1)           // requirements（物品原料）
                    ),

                    // ─────────────────────────────────────
                    // 配方 2：纯液体
                    //   原料液体：水×10  →  产物液体：油×3   （120 tick = 2 秒）
                    //   （炼油配方，测试纯液体场景）
                    // ─────────────────────────────────────
                    new HunfuBlock.Plan(
                            null,                                           // outItem = null → 没有物品产物
                            120f,                                           // time
                            null,                                           // requirements = null → 没有物品原料
                            LiquidStack.with(Liquids.oil, 3),                // outLiquid（液体产物）——显式调用 LiquidStack.with 消除歧义
                            LiquidStack.with(Liquids.water, 10)              // inLiquid（液体原料）——显式调用 LiquidStack.with
                    ),

                    // ─────────────────────────────────────
                    // 配方 3：物品 + 液体 混成（真正发挥 HunfuBlock 能力）
                    //   物品原料：铅×2
                    //   液体原料：水×5
                    //   ────────────────
                    //   物品产物：相织物（phase-fabric）×1
                    //   液体产物：炉渣液（slag）×4
                    //   耗时：180 tick = 3 秒
                    // ─────────────────────────────────────
                    new HunfuBlock.Plan(
                            with(Items.phaseFabric, 1),                     // outItem：相织物
                            180f,                                           // time
                            with(Items.lead, 2),                            // requirements：物品原料铅
                            LiquidStack.with(Liquids.slag, 4),               // outLiquid：炉渣液（液体产物）——显式 LiquidStack.with
                            LiquidStack.with(Liquids.water, 5)               // inLiquid：水（液体原料）——显式 LiquidStack.with
                    )
            );
        }};

        // =================================================================
        // ✅ 反隐探测雷达（antiStealthRadar）
        // 功能：
        //   1. 战争迷雾：提供 fogRadius=14 格长期开雾
        //   2. 反隐身：每 0.5 秒在 detectionRange(=fogRadius*tilesize) 范围内
        //      给所有敌方 InvisibleAbility 单位延长 radarRevealedTick，强制其可被炮台锁定
        //   3. 需要 5 功率电力驱动；没电力时只保留开雾，不做反隐扫描
        // =================================================================
        antiStealthRadar = new AntiStealthRadar("anti-stealth-radar") {{
            requirements(Category.effect, with(
                NuItems.bigIron,    120,   // 结构铁壳
                Items.lead,         150,   // 电路基板
                Items.silicon,      240,   // 硅芯片 / 天线信号处理
                Items.metaglass,     80,   // 雷达罩透明玻璃
                Items.plastanium,    40,   // 轻质外壳
                Items.surgeAlloy,    30    // 高阶合金：脉冲探测发射器
            ));
            size            = 2;                // 2×2 占地
            health          = 3000;             // 血量（雷达要堆高血量，避免被偷袭一下就没）
            fogRadius       = 14;               // 开雾 14 格（比原版雷达大一点）
            // detectionRange = 默认 = fogRadius * tilesize（14 * 8 = 112 像素 ≈ 14 格）
            scanTick        = 30f;              // 每 0.5 秒扫一次（越小越灵敏，但耗电/CPU 开销略高）
            revealPerStep   = 60f;              // 每扫一次强制隐身单位显形 1 秒（60 tick），等于"一直在范围内就一直显形"
            consumePower    = true;             // 不供电就不反隐（只开雾）
            consumePower(5f);                   // 耗电 5 功率（配太阳能/燃烧发电就能转）
            rotate          = false;            // 手动无法旋转（天线由 rotateSpeed 自动转）
            rotateSpeed     = 3.6f;             // 天线自转速度（视觉效果）
            glowScl         = 6f;               // 探测中发光层呼吸缩放
            glowMag         = 0.7f;
        }};
        however = new ItemTurret("however"){{
            requirements(Category.turret, with(NuItems.bigIron, 35));
            ammo(
                    NuItems.bigIron,new SpeedDamageBulletType(1.5f, 30){{
                        damageIncrease = 90f;
                        width = 20f;
                        height = 20f;
                        lifetime = 120f;
                        ammoMultiplier = 2;
                        hitEffect = despawnEffect = Fx.hitBulletColor;
                        hitColor = backColor = trailColor = Pal.copperAmmoBack;
                        frontColor = Pal.copperAmmoFront;
                    }}
            );
            shoot = new ShootAlternate(3.5f);
            recoils = 2;
            drawer = new DrawTurret(){{
                for(int i = 0; i < 2; i ++){
                    int f = i;
                    parts.add(new RegionPart("-barrel-" + (i == 0 ? "l" : "r")){{
                        progress = PartProgress.recoil;
                        recoilIndex = f;
                        under = true;
                        moveY = -1.5f;
                    }});
                }
            }};

            shootSound = Sounds.shootDuo;
            recoil = 0.5f;
            shootY = 3f;
            reload = 20f;
            range = 160;
            shootCone = 15f;
            ammoUseEffect = Fx.casing1;
            health = 250;
            inaccuracy = 2f;
            rotateSpeed = 10f;
            coolant = consumeCoolant(0.1f);
            coolantMultiplier = 10f;
            researchCostMultiplier = 0.05f;
            depositCooldown = 2.0f;
            limitRange(5f);
        }};
        hyw = new BulletAcceleratorBlock("hyw"){{
            requirements(Category.defense, with(NuItems.bigIron, 35));
        }};

        // =================================================================
        // ✅ Coin 铸币厂（coin-producer）
        // - 消耗: 铅×2 + 铁×2 + 功率 2.0
        // - 产出: 每次合成直接获得 5 coins（不入核心物品栏，直接进货币池）
        // =================================================================
        coinProducer = new CoinProducerBlock("coin-producer") {{
            requirements(Category.crafting, with(
                Items.lead,      60
            ));
            size = 2;
            health = 600;
            hasItems = hasPower = true;
            craftTime = 120f;       // 2 秒一次
            coinPerCraft = 5;       // 每次 +5 coins
            consumeItems(ItemStack.with(Items.lead, 2));
            consumePower(0.0f);
        }};

        // =================================================================
        // ✅ Coin 消耗工厂（coin-consumer）
        // - 每次合成: 消耗 10 coins（从货币池扣）
        // - 同时消耗: 硅×3 + 功率 3.0
        // - 产出: phase-fabric ×1（高级研究材料）
        // =================================================================
        coinConsumer = new CoinConsumerBlock("coin-consumer") {{
            requirements(Category.crafting, with(
                NuItems.bigIron,  100,
                Items.lead,        80,
                Items.silicon,     60,
                Items.plastanium,  30
            ));
            size = 2;
            health = 700;
            hasItems = hasPower = true;
            craftTime = 180f;              // 3 秒一次
            coinPerCraft = 10;             // 每次消耗 10 coins
            outputItem = new ItemStack(Items.phaseFabric, 1);
            consumeItems(ItemStack.with(Items.silicon, 3));
            consumePower(3.0f);
        }};
    }
}