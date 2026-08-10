package Npl.content;

import static mindustry.Vars.*;
import static mindustry.type.ItemStack.*;
import Npl.newSth.*;
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
            //crafting
            MagnetPurifier,monoSiliCrystalFactory,stormCrafter, coinProducer, exchange,CompressionChamber,
            //effect
            antiStealthRadar,BulletAccelerator,
            //turret
            however,
            //production
            bigIronDrills,rubberCrusher;
    public static void load() {

        MagnetPurifier = new GenericCrafter("MagentPurifier") {{
            requirements(Category.crafting, with(NuItems.bigIron,100,NuItems.sulFurFrag,75,NuItems.frailPolyester,150,
                    NuItems.monoSiliCrystal,100,Items.graphite,100));
            outputItem = new ItemStack(NuItems.magent,4);
            craftTime =60f;
            hasItems=hasPower=true;
            ambientSound=Sounds.loopGrind;
            ambientSoundVolume=0.025f;
            consumeItems(ItemStack.with(NuItems.bigIron,3,NuItems.Tcoal,1));
            consumePower(2.1f);
            health = 800;
            size=2;
            itemCapacity = 20;
            craftEffect =new ParticleEffect(){{
                particles = 8;
                cone = 360f;
                lenFrom = 12f;
                lenTo = 0f;
                spin = 6;
                sizeFrom = 7f;
                sizeTo = 0f;
                colorFrom = NuColor.PaleConColor;
                colorTo = NuColor.PaleSilverColor;
            }};
        }};
        monoSiliCrystalFactory = new GenericCrafter("monoSiliCrystalFactory") {{
            requirements(Category.crafting, with(NuItems.bigIron,100,Items.graphite,30));
            outputItem = new ItemStack(NuItems.monoSiliCrystal,5);
            hasItems=true;
            ambientSound=Sounds.loopGrind;
            ambientSoundVolume=0.025f;
            consumeItems(ItemStack.with(NuItems.Tcoal,3,Items.sand,2));
            health = 800;
            size=2;
            itemCapacity = 40;
        }};

        stormCrafter = new StormCrafterBlock("storm-crafter") {{
            requirements(Category.crafting, with(
                    NuItems.bigIron, 550,
                    NuItems.sulFurFrag, 500,
                    NuItems.pumice, 145,
                    NuItems.thallide,75,
                    NuItems.alkSliver,100
            ));
            size = 2;
            health = 1500;
            hasItems = hasPower = true;
            // 合成本身速度独立（和三阶段视觉不挂钩，用户可自改）
            craftTime = 22f*60;
            outputItem = new ItemStack(NuItems.bottledMagenticStorm, 1);
            consumeItems(ItemStack.with(Items.pyratite, 3,NuItems.pumice,2));
            consumePower(15.0f);
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
            ringSweepSweepAngle   = 1f;        // 高亮扫弧段长 45°
            ringSweepWidth        = 4f;
            // 光照：
            lightRadius = 260f;
            lightningBullet = new LightningBulletType() {{
                damage               = 100f;
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
        coinProducer = new CoinProducerBlock("coin-producer") {{
            requirements(Category.crafting, with(
                    NuItems.bigIron,  200,
                    NuItems.monoSiliCrystal, 150,
                    NuItems.pumice,  160,
                    NuItems.rubber,  80
            ));
            size = 2;
            health = 800;
            hasItems = hasPower = true;
            itemCapacity = 500;
            craftTime = 120f;       // 2 秒一次
            coinPerCraft = 10;       // 每次 +5 coins
            consumeItems(ItemStack.with(NuItems.bigIron,  100));
            consumePower(1.5f);
        }};
        exchange = new CoinConsumerBlock("exchange") {{
            requirements(Category.crafting, with(
                    NuItems.bigIron,  100,
                    NuItems.monoSiliCrystal,  100,
                    NuItems.pumice,  200,
                    NuItems.magent,  45
            ));
            size = 2;
            health = 1000;
            hasItems = true;
            plans = Seq.with(
                    new Plan(with(NuItems.bigIron,100),60f*10,null,15),
                    new Plan(with(NuItems.monoSiliCrystal,100),60f*20,null,80),
                    new Plan(with(Items.graphite,100),60f*20,null,80),
                    new Plan(with(NuItems.sulFurFrag,100),60f*16,null,60),
                    new Plan(with(NuItems.magent,100),60f*30,null,120),
                    new Plan(with(NuItems.frailPolyester,100),60f*10,null,15),
                    new Plan(with(NuItems.oriRubber,100),60f*16,null,60),
                    new Plan(with(NuItems.oriUranium,100),60f*16,null,60),
                    new Plan(with(NuItems.pumice,100),60f*30,null,120),
                    new Plan(with(NuItems.rubber,100),60f*40,null,200),
                    new Plan(with(NuItems.alkSliver,100),60f*40,null,200),
                    new Plan(with(NuItems.thallide,100),60f*60,null,400),
                    new Plan(with(NuItems.uranium,100),60f*60,null,400),
                    new Plan(with(NuItems.bottledMagenticStorm,100),60f*60,null,400),
                    new Plan(with(NuItems.rubberFrag,100),60f*10,null,30),
                    new Plan(with(Items.pyratite,100),60f*20,null,60)
            );
        }};
        CompressionChamber = new GenericCrafter("CompressionChamber") {{
            requirements(Category.crafting, with(
                    NuItems.bigIron,  100
            ));
            outputItem = new ItemStack(Items.graphite,3);
            hasItems = true;
            craftTime = 120f;
            health = 800;
            size = 2;
            consumeItems(ItemStack.with(NuItems.Tcoal,3));
        }};


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
            consumePower(10f);                   // 耗电 5 功率（配太阳能/燃烧发电就能转）
            rotate          = false;            // 手动无法旋转（天线由 rotateSpeed 自动转）
            rotateSpeed     = 3.6f;             // 天线自转速度（视觉效果）
            glowScl         = 6f;               // 探测中发光层呼吸缩放
            glowMag         = 0.7f;
        }};
        BulletAccelerator = new BulletAcceleratorBlock("BulletAccelerator"){{
            requirements(Category.effect, with(NuItems.bigIron, 35));
            range = 8f;
            boostSpeed = 1.3f;
            baseColor = phaseColor = NuColor.PaleColor;
            consumePower(15f);
            phaseBoostMul = 0.3f;
            phaseRangeBoost = 8f;
            useTime = 600f;
            itemCapacity = 10;
            consumeItem(NuItems.rubber).boost();
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


        rubberCrusher = new WallCrafter("rubberCrusher"){{
            requirements(Category.production, with(Items.graphite, 25));
            consumePower(11 / 60f);
            drillTime = 110f;
            size = 3;
            attribute = NuAttribute.oriRubber;
            output = NuItems.oriRubber;
            fogRadius = 2;
            researchCost = with( Items.graphite, 40);
            ambientSound = Sounds.loopDrill;
            ambientSoundVolume = 0.04f;
        }};
        bigIronDrills = new Drill("bigIronDrills"){{
            requirements(Category.production, with(NuItems.bigIron, 10));
            drillTime = 240f;
            size = 2;
            health = 500;
            tier = 4;
            hardnessDrillMultiplier = 200f;
            itemCapacity = 20;
            hasItems = hasLiquids = true;
            drillEffect = new WaveEffect(){{
                sizeFrom = 0f;
                sizeTo = 20f;
                strokeFrom = 2f;
                strokeTo = 0.2f;
                colorFrom = NuColor.PaleColor;
                colorTo = NuColor.PaleConColor;
                lifetime = 30f;
            }};
        }};
    }
}