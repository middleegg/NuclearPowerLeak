package Npl.content;

import static mindustry.Vars.*;
import static mindustry.type.ItemStack.*;
import Npl.newSth.*;
import Npl.newSth.walls.*;
import Npl.content.*;
import Npl.newSth.consumes.*;
import Npl.content.Azer;
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


public class NuBlocks {
    // ============= 静态方块声明：以后在这儿加就行 =============
    public static Block
            //crafting
            MagnetPurifier,monoSiliCrystalFactory,coinProducer, exchange,CompressionChamber,HeatMaker, strangeLiquidExtractionRoom,
            seedCollector,rubberGrower,heatRelaxation,distillationRoom,silverPlatingRoom,Grinder,nuclearFluidCollector,
            thalliumCompoundCrucible,OxygenLiquefactionRoom,uraniumPurificationRoom,MagneticStormStabiliser,MagentEnergyStation,
            UraniumPrecipitationRoom,
            //effect
            antiStealthRadar,BulletAccelerator,FederalJuniorCore,FederalSubCore,FederalContainer,FederalWarehouse,
            JuniorMender,MenderProjector,SeniorMender,DefenceShieldProjector,OverloadedThrowor,SeniorOverloaded,
            OverloadDefenceTower,ConstructionField,
            //power
            ElectricalNode,PowerCapacitor,OriginalElectronics,SteamElectronics,FloatingCapacitor,RestoreMotor,RadioisotopeGenerator,
            DepletedUraniumPower,DepletedUraniumCapacitor,UraniumPowerAppliance,
            //wall
            bigIronWall,bigIronLargeWall,energyStorageWall,energyStorageLargeWall,IllusionGate,IllusionLargeGate,
            frailPolyesterWall,frailPolyesterLargeWall,magneticPullWall,magneticPullLargeWall,
            floatWall,floatLargeWall,rubberWall,rubberLargeWall,alkSliverWall,alkSliverLargeWall,
            thallideWall,thallideLargeWall,uraniumWall,uraniumLargeWall,energyShield,Lotus,
            //transport
            bigIronDuct,bigIronRouter,bigIronOverFlow,bigIronUnderFlow,basicUnloader,bigIronJunction,bigIronBridge,
            ThermalConductor,GaintThermalConductor,floatDuct,floatRouter,floatOverFlow,floatUnderFlow,floatJuction,
            floatBridge,UnitCarryingPoint,UnitUnloadingContainer,SwiftConveyor,UnifiedDrive,
            //liquid
            bigIronPump,floatPump,OrganicConduit,OrganicJunction,OrganicRouter,OrganicBridge,FloatConduic,FloatJuntion,
            FloatBridge,FloatRouter,FloatTank,
            //units
            ExperimentalMachineryUnitFactory,ExperimentalUnitReconstructionFactory,MechanicalAssemblyFactory,AirshipAssemblyFactory,
            ShipAssemblyFactory,ParadoxUnitAssemblyFactory,TerminalUnitAssemblyFactory,bigIronUnitConveyor,floatUnitConvryor,
            floatUnitRouter,ParadoxAssemblyModule,TerminalAssemblyModule,NuclearAssemblyParts,AbsurdAssemblyParts,
            ThalliumAssemblyParts,BuildingConstructor,SpecialUnitFactory,PaleUnitFactory,
            //logic

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
            consumePower(5f);
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
            drawer = new DrawMulti(new DrawRegion("-bottom"),new DrawRegion("-top"),new DrawDefault(),
                    new DrawParticles(){{
                        color = Color.valueOf("616161");
                        sides = 12;
                        x = 0f;
                        y = 0f;
                        alpha = 0.5f;
                        particles = 15;
                        particleRotation = 0f;
                        particleLife = 60f;
                        particleRad = 6;
                        particleSize = 3;
                        fadeMargin = 0.4f;
                        rotateScl = 3f;
                        reverse = true;
                        poly = true;
                        particleInterp = Interp.circleOut;
                    }});
        }};
        coinProducer = new CoinProducerBlock("coin-producer") {{
            requirements(Category.crafting, with(
                    NuItems.bigIron,  200,
                    NuItems.monoSiliCrystal, 150,
                    NuItems.pumice,  160,
                    NuItems.alkSliver,120,
                    NuItems.rubber,  80
            ));
            size = 2;
            health = 800;
            hasItems = hasPower = true;
            itemCapacity = 500;
            craftTime = 120f;       // 2 秒一次
            coinPerCraft = 10;       // 每次 +5 coins
            consumeItems(ItemStack.with(NuItems.bigIron,  100));
            consumePower(4f);
        }};
        exchange = new HunfuBlock("exchange") {{
            requirements(Category.crafting, with(
                    NuItems.bigIron,  100,
                    NuItems.monoSiliCrystal,  100,
                    NuItems.pumice,  200,
                    NuItems.alkSliver , 90,
                    NuItems.rubber,  50,
                    NuItems.magent,  45
            ));
            size = 3;
            health = 1400;
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
                    NuItems.bigIron,60
            ));
            outputItem = new ItemStack(Items.graphite,3);
            hasItems = true;
            craftTime = 120f;
            health = 800;
            size = 2;
            consumeItems(ItemStack.with(NuItems.Tcoal,3));
            craftEffect = Fx.blastExplosion;
        }};
        HeatMaker = new HeatProducer("HeatMaker") {{
            requirements(Category.crafting, with(NuItems.pumice,90,NuItems.monoSiliCrystal,100,NuItems.sulFurFrag,50,NuItems.magent,30));
            consumePower(3.333333f);
            craftEffect = Fx.lava;
            size = 2;
            health = 800;
            hasPower = true;
            ambientSound = Sounds.loopSmelter;
            researchCostMultiplier = 0.5f;
            drawer = new DrawMulti(new DrawDefault(),new DrawHeatOutput(){{
                heatColor = NuColor.HeatColor;
            }});
            rotate = true;
            heatOutput = 3;
            craftTime = 60f;
        }};
        strangeLiquidExtractionRoom = new HeatCrafter("strangeLiquidExtractionRoom") {{
            requirements(Category.crafting, with(NuItems.monoSiliCrystal,160,NuItems.pumice,150,Items.graphite,200));
            hasLiquids = true;
            liquidCapacity = 200;
            size = 2;
            health = 800;
            consumePower(6f);
            researchCostMultiplier = 0.5f;
            maxEfficiency = 5;
            heatRequirement = 5;
            ambientSound = Sounds.loopExtract;
            ambientSoundVolume = 0.06f;
            outputLiquid = new LiquidStack(NuLiquid.strangeLiquid,0.1f);
            hasPower = true;
            craftTime = 240f;
            drawer = new DrawMulti(new DrawHeatInput(){{
                heatColor = NuColor.HeatColor;
            }},new DrawRegion("-bottom"),new DrawLiquidTile(NuLiquid.strangeLiquid, 4f),new DrawDefault(), new DrawParticles(){{
                color = NuColor.DespColor;
                alpha = 0.6f;
                particleSize = 4f;
                particles = 10;
                particleRad = 12f;
                particleLife = 140f;
            }});
        }};
        seedCollector = new GenericCrafter("seedCollector") {{
            requirements(Category.crafting, with(Items.graphite,450,NuItems.pumice,250,NuItems.magent,50));
            hasItems = true;
            craftTime = 450f;
            health = 800;
            size = 2;
            buildTime = 40f;
            drawer = new DrawMulti(new DrawRegion("-rotator"){{
                rotateSpeed = 2f;
                spinSprite = true;
            }}, new DrawDefault(),new DrawRegion("-top"),new DrawRegion("-bottom"));
            ambientSound = Sounds.plantBreak;
            itemCapacity = 60;
            outputItem = new ItemStack(NuItems.rubberFrag,8);
            consumePower(4.8f);
            consumeItems(ItemStack.with(NuItems.oriRubber,2));
            craftEffect = Fx.smeltsmoke;
        }};
        rubberGrower = new HeatCrafter("rubberGrower") {{
            requirements(Category.crafting,with(NuItems.monoSiliCrystal,100,NuItems.bigIron,300,NuItems.pumice,150,NuItems.sulFurFrag,90));
            size = 2;
            buildTime = 50f;
            drawer = new DrawMulti(new DrawHeatInput(){{}});
            craftTime = 600f;
            consumePower(7.5f);
            outputItem = new ItemStack(NuItems.rubber,2);
            drawer = new DrawMulti(new DrawHeatInput(){{
                heatColor = NuColor.HeatColor;
            }},new DrawDefault(),new DrawGlowRegion("-heat"){{
                blending = Blending.additive;
                color = NuColor.HeatConColor;
            }},new DrawRegion("-bottom"));
            ambientSound = Sounds.loopSmelter;
            consumeItems(ItemStack.with(NuItems.rubberFrag,1));
            craftEffect = Fx.smeltsmoke;
            itemCapacity = 40;
            heatRequirement = 10;
            maxEfficiency = 3;
        }};
        heatRelaxation = new HeatProducer("heatRelaxation") {{
            requirements(Category.crafting,with(NuItems.monoSiliCrystal,100,NuItems.magent,75,NuItems.pumice,150,NuItems.rubber,50));
            size = 2;
            heatOutput = 15;
            craftTime = 600f;
            researchCostMultiplier = 0.5f;
            drawer = new DrawMulti(new DrawRegion("-bottom"),new DrawHeatInput(){{
                heatColor = NuColor.HeatColor;
            }},new DrawDefault());
            craftEffect = Fx.lava;
            rotate = true;
            consumeItems(ItemStack.with(NuItems.rubber,1));
            ambientSound = Sounds.loopSmelter;
            ambientSoundVolume = 0.1f;
            itemCapacity = 20;
        }};
        distillationRoom = new GenericCrafter("distillationRoom") {{
            requirements(Category.crafting,with(NuItems.bigIron,200,NuItems.monoSiliCrystal,125,NuItems.pumice,75,Items.graphite,150,NuItems.rubber,50));
            size = 3;
            health = 1200;
            craftTime = 180f;
            itemCapacity = 60;
            buildTime = 25f;
            updateEffect = Fx.smoke;
            consumePower(10f);
            researchCostMultiplier = 0.5f;
            consumeItems(ItemStack.with(NuItems.Tcoal,3));
            consumeLiquids(LiquidStack.with(Liquids.water,0.2));
            craftEffect = new MultiEffect (new WaveEffect(){{
                sizeFrom = 0f;
                sizeTo = 32f;
                colorFrom = NuColor.PaleColor;
                colorTo = NuColor.DarkColor;
                lifetime = 120f;
                layer = 120f;
            }},Fx.smokePuff,Fx.steamCoolSmoke);
            outputItem = new ItemStack(Items.graphite,6);
        }};
        silverPlatingRoom = new HeatCrafter("silverPlatingRoom") {{
            requirements(Category.crafting,with(NuItems.bigIron,300,NuItems.monoSiliCrystal,300,NuItems.pumice,150,NuItems.magent,100));
            heatRequirement = 5;
            maxEfficiency = 3;
            ambientSound = Sounds.loopSmelter;
            ambientSoundVolume = 0.24f;
            size = 2;
            health = 800;
            itemCapacity = 30;
            buildTime = 25f;
            updateEffect = Fx.smoke;
            consumePower(12f);
            consumeItems(ItemStack.with(NuItems.pumice,3,Items.sand,5));
            craftTime = 145f;
            researchCostMultiplier = 0.5f;
            outputItem = new ItemStack(NuItems.alkSliver,1);
            craftEffect = Fx.blastExplosion;
        }};
        Grinder = new HunfuBlock("grinder"){{
            requirements(Category.crafting,with(
                    NuItems.bigIron,300,
                    NuItems.monoSiliCrystal,250,
                    NuItems.pumice,175,
                    NuItems.alkSliver,100
            ));
            health = 800;
            size = 2;
            consumePower(9f);
            researchCostMultiplier = 0.75f;
            craftEffect = NuFx.PaleSmoke;
            itemCapacity = 40;
            ambientSound = Sounds.loopSmelter;
            ambientSoundVolume = 0.24f;
            buildTime = 30f;
            plans = Seq.with(
                    new HunfuBlock.Plan(
                         with(NuItems.oriUranium,2),
                         140f,
                         with(NuItems.sulFurFrag,2,NuItems.uranCrystal,2)
                    ),
                    new HunfuBlock.Plan(
                         with(Items.pyratite,4),
                         60*1.2f,
                         with(NuItems.sulFurFrag,2,NuItems.rubberFrag,3),
                         null,
                         LiquidStack.with(Liquids.water, 6),
                         0,
                         0
                    )
            );
        }};
        nuclearFluidCollector = new GenericCrafter("nuclearFluidCollector") {{
           requirements(Category.crafting,with(
                   NuItems.pumice,150,
                   NuItems.magent,300,
                   NuItems.alkSliver,90,
                   Items.graphite,200
           ));
           ambientSound = Sounds.loopGrind;
           ambientSoundVolume = 0.2f;
           size = 2;
           buildTime = 25f;
           health = 800;
           itemCapacity = 20;
           consumeItems(ItemStack.with(NuItems.uranCrystal,2));
           consumeLiquids(LiquidStack.with(Liquids.water,1f));
           outputLiquid = new LiquidStack(NuLiquid.nuclearFluid,1f);
           craftTime = 180f;
           liquidCapacity = 500f;
           craftEffect = new ParticleEffect(){{
               particles = 8;
               cone = 180;
               lenFrom = 15f;
               lenTo = 0f;
               spin = 3f;
               sizeFrom = 4f;
               sizeTo = 0f;
               colorFrom = NuColor.SailColor;
               colorTo = NuColor.SailBackColor;
               lifetime = 60f;
               layer =110f;
           }};
           consumePower(10f);
        }};
        thalliumCompoundCrucible = new HeatCrafter("thalliumCompoundCrucible") {{
            requirements(Category.crafting,with(
                    NuItems.bigIron,400,
                    NuItems.monoSiliCrystal,280,
                    NuItems.alkSliver,50,
                    NuItems.magent,300,
                    NuItems.pumice,300
            ));
            health = 1200;
            size = 3;
            craftTime = 300f;
            heatRequirement = 10;
            maxEfficiency = 6;
            ambientSound = Sounds.loopSmelter;
            ambientSoundVolume = 0.24f;
            researchCostMultiplier = 0.5f;
            outputItem = new ItemStack(NuItems.thallide,3);
            consumePower(12f);
            consumeItems(ItemStack.with(NuItems.sulFurFrag,6,NuItems.Tcoal,5));
            craftEffect = new MultiEffect(new RadialEffect(){{
                amount = 4;
                rotationSpacing = 90f;
                rotationOffset = 55f;
                lengthOffset = 12f;
                effect =NuFx.ThallideConsumeSmoke;
            }},new RadialEffect(){{
                amount = 4;
                rotationSpacing = 90f;
                rotationOffset = 50f;
                lengthOffset = 12f;
                effect = NuFx.ThallideConsumeSmoke;
            }});
            itemCapacity = 60;
        }};
        OxygenLiquefactionRoom =new HeatCrafter("OxygenLiquefactionRoom") {{
            requirements(Category.crafting,with(
                    NuItems.monoSiliCrystal,300,
                    NuItems.bigIron,400,
                    NuItems.alkSliver,100,
                    NuItems.magent,200
            ));
            heatRequirement = 5;
            maxEfficiency = 6;
            ambientSound = Sounds.loopSmelter;
            ambientSoundVolume = 0.2f;
            craftTime = 120f;
            researchCostMultiplier = 0.5f;
            consumeItems(ItemStack.with(NuItems.magent,1f));
            consumeLiquids(LiquidStack.with(Liquids.water,0.1f));
            consumePower(20f);
            outputLiquid = new LiquidStack(NuLiquid.liquidOxygen,0.05f);
            hasLiquids = hasItems = true;
            itemCapacity = 10;
            liquidCapacity = 200;
            drawer = new DrawMulti(new DrawHeatInput(){{
                heatColor = NuColor.HeatColor;
            }},new DrawRegion("-bottom"),new DrawLiquidTile(NuLiquid.strangeLiquid, 4f),new DrawDefault(), new DrawParticles(){{
                color = NuColor.PaleColor;
                alpha = 0.6f;
                particleSize = 8f;
                particles = 10;
                particleRad = 12f;
                particleLife = 140f;
            }});
        }};
        uraniumPurificationRoom = new GenericCrafter("uraniumPurificationRoom") {{
            requirements(Category.crafting,with(
                    NuItems.pumice,350,
                    NuItems.uranCrystal,200,
                    NuItems.rubber,125,
                    NuItems.alkSliver,75,
                    NuItems.magent,200
            ));
            researchCostMultiplier = 0.5f;
            itemCapacity = 20;
            hasItems = true;
            drawer = new DrawMulti(new DrawRegion("-bottom"),new DrawDefault(), new DrawLiquidTile(){{
                drawLiquid = Liquids.water;
                padding = 3f;
            }});
            consumePower(20f);
            consumeItems(ItemStack.with(NuItems.oriUranium,2));
            consumeLiquids(LiquidStack.with(Liquids.water,0.5f));
            outputItem = new ItemStack(NuItems.uranium,2);
            craftEffect = new MultiEffect(new RadialEffect(){{
                amount = 4;
                rotationSpacing = 90f;
                effect = Fx.surgeCruciSmoke;
            }},new ParticleEffect(){{
                particles = 8;
                cone = 180f;
                lenFrom = 32f;
                lenTo = 2f;
                spin = 6f;
                sizeTo =3f;
                colorFrom = NuColor.SailColor;
                colorTo = Color.valueOf("ffffff");
                lifetime = 80f;
                layer =100f;
            }});
        }};
        MagneticStormStabiliser = new StormCrafterBlock("MagenticStormStabiliser") {{
            requirements(Category.crafting, with(
                    NuItems.bigIron, 550,
                    NuItems.sulFurFrag, 500,
                    NuItems.pumice, 145,
                    NuItems.thallide,75,
                    NuItems.alkSliver,100
            ));
            size = 3;
            health = 1600;
            hasItems = hasPower = true;
            // 合成本身速度独立（和三阶段视觉不挂钩，用户可自改）
            craftTime = 3f*60;
            outputItem = new ItemStack(NuItems.bottledMagenticStorm, 1);
            consumeItems(ItemStack.with(Items.pyratite, 3,NuItems.pumice,2,NuItems.magent,2));
            consumePower(40f);
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
        MagentEnergyStation = new HeatProducer("MagenticEnergyStation") {{
            requirements(Category.crafting, with(
               NuItems.bigIron,400,
               NuItems.monoSiliCrystal,300,
               NuItems.magent,200,
               Items.graphite,300,
               NuItems.alkSliver,150
            ));
            heatOutput = 10;
            size = 3;
            health = 1200;
            hasItems = hasPower = true;
            outputItem = new ItemStack(NuItems.magent,6);
            consumePower(10f);
            consumeItems(ItemStack.with(NuItems.bigIron,3));
            consumeLiquids(LiquidStack.with(Liquids.water,1f));
            itemCapacity = 60;
            drawer = new DrawMulti(new DrawRegion("-bottom"),new DrawLiquidTile(){{
                drawLiquid = Liquids.water;
            }},new DrawCircles(){{
                color = NuColor.PaleColor;
                strokeMax = 3.25f;
                radius =10f;
                amount = 4;
                timeScl = 200f;
            }},new DrawRegion("-center"),new DrawCells(){{
                color = NuColor.DespColor;
                particleColorFrom = NuColor.DespColor;
                particleColorTo = NuColor.DespBackColor;
                particles =40;
                range =10f;
            }},new DrawDefault(),new DrawHeatOutput(){{
                heatColor = NuColor.HeatColor;
            }},new DrawGlowRegion("-glow"){{
                color = NuColor.BombColor;
                alpha = 0.7f;
            }});
        }};
        UraniumPrecipitationRoom = new HeatProducer("UraniumPrecipitationRoom") {{
            requirements(Category.crafting, with(
                    NuItems.magent, 200,
                    NuItems.pumice, 300,
                    NuItems.monoSiliCrystal, 450,
                    NuItems.alkSliver,150,
                    NuItems.uranium,100
            ));
            researchCostMultiplier = 1.6f;
            itemCapacity = 60;
            liquidCapacity = 1000;
            drawer = new DrawMulti(new DrawRegion("-bottom"),new DrawLiquidTile(){{
                drawLiquid = NuLiquid.nuclearFluid;
                padding = 3f;
            }},new DrawDefault(),new DrawHeatOutput(){{
                heatColor = NuColor.HeatColor;
            }});
            heatOutput = 20;
            size = 3;
            health = 1500;
            hasItems = hasPower = true;
            consumePower(20f);
            consumeLiquids(LiquidStack.with(NuLiquid.nuclearFluid,1f,NuLiquid.strangeLiquid,0.2f));
            consumeItems(ItemStack.with(NuItems.sulFurFrag,3));
            outputItem = new ItemStack(NuItems.uranium,8);
            craftTime = 300f;
            craftEffect = new MultiEffect(new ParticleEffect(){{
               particles = 8;
               cone = 90f;
               lenFrom = 32f;
               lenTo = 0f;
               spin = 6f;
               sizeFrom = 7f;
               sizeTo = 0f;
               colorFrom = NuColor.SailColor;
               colorTo = NuColor.SailBackColor;
               lifetime =100f;
               layer = 100f;
            }},new WaveEffect(){{
                sizeFrom = 0f;
                sizeTo = 48f;
                colorFrom = NuColor.SailColor;
                colorTo = NuColor.SailBackColor;
                lifetime = 60f;
                layer = 90f;
            }});
        }};


        antiStealthRadar = new AntiStealthRadar("anti-stealth-radar") {{
            requirements(Category.effect, with(
                NuItems.bigIron,    150,   // 结构铁壳
                NuItems.frailPolyester,200,
                NuItems.monoSiliCrystal,240,
                NuItems.magent,25
            ));
            size            = 2;                // 2×2 占地
            health          = 3000;             // 血量（雷达要堆高血量，避免被偷袭一下就没）
            fogRadius       = 14;               // 开雾 14 格（比原版雷达大一点）
            detectionRange = fogRadius*0.8f;
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
        FederalJuniorCore = new CoreBlock("FederalJuniorCore"){{
            requirements(Category.effect, BuildVisibility.coreZoneOnly, with(
                    NuItems.bigIron,2500,
                    NuItems.monoSiliCrystal, 800,
                    Items.graphite , 2000
            ));
            alwaysUnlocked = true;
            isFirstTier = true;
            unitType = FederalUnitTypes.survive;
            health = 6500;
            itemCapacity = 6000;
            size = 3;
            buildCostMultiplier = 1f;
            unitCapModifier = 12;
        }};
        FederalSubCore = new CoreBlock("FederalSubCore"){{
            requirements(Category.effect, BuildVisibility.coreZoneOnly, with(
                    NuItems.bigIron,5000,
                    NuItems.monoSiliCrystal,3500,
                    NuItems.pumice,4000,
                    Items.graphite,3000,
                    NuItems.magent,2500
            ));
            alwaysUnlocked = false;
            isFirstTier = false;
            unitType = FederalUnitTypes.resurrection;
            health = 12000;
            size = 4;
            buildCostMultiplier = 1f;
            researchCostMultiplier = 1.2f;
            unitCapModifier = 20;
        }};
        FederalContainer = new StorageBlock("FederalContainer"){{
            requirements(Category.effect, with(NuItems.magent,150));
            size = 2;
            itemCapacity = 1000;
            scaledHealth = 500;
        }};
        FederalWarehouse = new StorageBlock("FederalWarehouse"){{
            requirements(Category.effect, with(NuItems.magent,300,NuItems.alkSliver,100));
            size = 3;
            itemCapacity = 3500;
            scaledHealth = 800;
        }};
        JuniorMender = new RegenProjector("JuniorMender"){{
            requirements(Category.effect, with(
                    NuItems.monoSiliCrystal,50,
                    NuItems.bigIron,45
            ));
            size = 1;
            range = 16;
            health = 400;
            baseColor = NuColor.SailColor;
            optionalMultiplier = 1.5f;
            optionalUseTime = 60f*9;
            consumePower(1f);
            consumeItem(NuItems.monoSiliCrystal).boost();
            healPercent = 1f / 80f;
            Color col = NuColor.SailBackColor;
            drawer = new DrawMulti(new DrawRegion("-bottom"),new DrawDefault(), new DrawGlowRegion(){{
                color = Color.sky;
            }}, new DrawPulseShape(false){{
                layer = Layer.effect;
                color = col;
            }}, new DrawShape(){{
                layer = Layer.effect;
                radius = 3.5f;
                useWarmupRadius = true;
                timeScl = 2f;
                color = col;
            }});
        }};
        MenderProjector = new RegenProjector("MenderProjector"){{
            requirements(Category.effect, with(
                    NuItems.bigIron,200,
                    NuItems.monoSiliCrystal,150,
                    NuItems.magent,140
            ));
            size = 2;
            range = 40;
            health = 1600;
            baseColor = NuColor.SailColor;
            consumePower(2.5f);
            optionalMultiplier = 2.4f;
            optionalUseTime = 60f*16;
            consumeItem(NuItems.rubber).boost();
            healPercent = 1f / 60f;
            Color col = NuColor.SailBackColor;
            drawer = new DrawMulti(new DrawRegion("-bottom"),new DrawDefault(), new DrawGlowRegion(){{
                color = Color.sky;
            }}, new DrawPulseShape(false){{
                layer = Layer.effect;
                color = col;
            }}, new DrawShape(){{
                layer = Layer.effect;
                radius = 6f;
                useWarmupRadius = true;
                timeScl = 2f;
                color = col;
            }});
        }};
        SeniorMender = new RegenProjector("SeniorMender"){{
            requirements(Category.effect, with(
               NuItems.monoSiliCrystal,500,
               NuItems.magent,200,
               NuItems.pumice,150,
               NuItems.alkSliver,50
            ));
            size = 3;
            range = 80;
            health = 4000;
            baseColor = NuColor.SailColor;
            consumePower(8f);
            optionalMultiplier = 3.6f;
            optionalUseTime = 60f*24;
            consumeItem(NuItems.alkSliver).boost();
            consumeLiquid(NuLiquid.strangeLiquid,0.1f);
            healPercent = 1f / 30f;
            Color col = NuColor.SailBackColor;
            drawer = new DrawMulti(new DrawRegion("-bottom"),new DrawDefault(), new DrawGlowRegion(){{
                color = Color.sky;
            }}, new DrawPulseShape(false){{
                layer = Layer.effect;
                color = col;
            }}, new DrawShape(){{
                layer = Layer.effect;
                radius = 10f;
                useWarmupRadius = true;
                timeScl = 2f;
                color = col;
            }});
        }};
        DefenceShieldProjector = new ForceProjector("DefenceShieldProjector"){{
            requirements(Category.effect, with(
                    NuItems.pumice, 100,
                    NuItems.rubber, 75,
                    NuItems.magent, 125
            ));
            armor = 20;
            size = 2;
            phaseRadiusBoost = 40f;
            radius = 64f;
            shieldHealth = 2800f;
            sides = 8;
            consumeCoolant = true;
            cooldownNormal = 1.5f;
            cooldownLiquid = 1.2f;
            cooldownBrokenBase = 0.35f;
            itemConsumer = consumeItem(NuItems.rubber).boost();
            consumePower(10f);
        }};
        OverloadedThrowor = new OverdriveProjector("OverloadedThrowor"){{
            requirements(Category.effect, with(
                    NuItems.pumice, 200,
                    NuItems.rubber, 130,
                    NuItems.magent, 150
            ));
            consumePower(12f);
            size = 2;
            range = 120f;
            speedBoost = 1.5f;
            speedBoostPhase = 1f;
            ambientSoundVolume = 0.12f;
            phaseRangeBoost = 40f;
            hasBoost = false;
            consumeItem(NuItems.rubber).boost();
        }};
        SeniorOverloaded = new OverdriveProjector("SeniorOverloaded"){{
            requirements(Category.effect, with(
                    NuItems.pumice, 400,
                    NuItems.uranium, 130,
                    NuItems.thallide, 150,
                    NuItems.alkSliver,200
                    ));
            consumePower(25f);
            size = 3;
            range = 280f;
            phaseRangeBoost = 80f;
            speedBoost = 2.8f;
            speedBoostPhase = 2f;
            useTime = 300f;
            ambientSoundVolume = 0.12f;
            hasBoost = false;
            consumeItem(NuItems.alkSliver).boost();
        }};
        OverloadDefenceTower = new PointDefenseTurret("OverloadDefenceTower"){{
            requirements(Category.effect, with(
                    NuItems.rubber,50,
                    NuItems.monoSiliCrystal,150,
                    NuItems.alkSliver,90,
                    NuItems.uranium,100
                    ));
            scaledHealth = 800;
            range = 240f;
            hasPower = true;
            consumePower(15f);
            size = 2;
            shootLength = 10f;
            bulletDamage = 1000f;
            reload = 0f;
            rotateSpeed = 360f;
            retargetTime = 0.001f;
            beamEffect = Fx.hitLaserBlast;
            color = NuColor.CoreElseColor;
            shootEffect = new MultiEffect(new WaveEffect(){{
                colorFrom = NuColor.SurvivalColor;
                colorTo = NuColor.SurvivalBackColor;
                sizeFrom = 0f;
                sizeTo = 40f;
                strokeFrom = 2f;
                strokeTo = 0f;
                sides = 8;
                interp = Interp.circleOut;
                lifetime = 20f;
            }},new WaveEffect(){{
                colorFrom = NuColor.BloodColor;
                colorTo = NuColor.BloodBackColor;
                sizeFrom = 0f;
                sizeTo = 4f;
                strokeFrom = 5f;
                strokeTo = 4f;
                sides = 8;
                interp = Interp.circleOut;
                lifetime = 20f;
            }},new ParticleEffect(){{
                sizeFrom = 5f;
                sizeTo = 0f;
                particles = 1;
                length = 0f;
                baseLength = 0f;
                colorFrom = NuColor.PaleColor;
                colorTo = NuColor.CoreColor;
                lifetime = 20f;
                interp = Interp.circleIn;
            }});
        }};
        ConstructionField = new BuildTurret("ConstructionField"){{
            requirements(Category.effect, with(NuItems.monoSiliCrystal, 150,
                    NuItems.rubber, 40,
                    NuItems.pumice, 160));
            outlineColor = Pal.darkOutline;
            range = 240f;
            size = 2;
            buildSpeed = 2.5f;
            consumePower(8f);
            consumeLiquid(NuLiquid.strangeLiquid, 0.15f);
        }};


        ElectricalNode = new PowerNode("ElectricalNode"){{
            requirements(Category.power, with(
                    NuItems.monoSiliCrystal, 2,
                    NuItems.bigIron, 3));
            maxNodes = 15;
            laserRange = 12;
            underBullets = true;
            crushFragile = true;
        }};
        PowerCapacitor = new Battery("PowerCapacitor"){{
            requirements(Category.power, with(
                    NuItems.monoSiliCrystal,4,
                    NuItems.bigIron, 30
            ));
            consumePowerBuffered(20000f);
            baseExplosiveness = 0.4f;
        }};
        OriginalElectronics = new ConsumeGenerator("OriginalElectronics"){{
            requirements(Category.power, with(NuItems.bigIron,50,
                    NuItems.monoSiliCrystal,45
            ));
            powerProduction =1.5f;
            itemDuration = 300f;
            size = 1;
            health = 400;
            ambientSound = Sounds.loopSmelter;
            ambientSoundVolume = 0.03f;
            generateEffect = Fx.generatespark;
            consume(new ConsumeItemFlammable());
            consume(new ConsumeItemExplode());
            itemDurationMultipliers.put(NuItems.Tcoal, 2f);
            consumePowerBuffered(1000f);
            drawer = new DrawMulti(new DrawDefault(), new DrawWarmupRegion());
        }};
        SteamElectronics = new ConsumeGenerator("SteamElectronics"){{
            requirements(Category.power, with(NuItems.bigIron,200,
                    NuItems.monoSiliCrystal,140,
                    NuItems.magent,90,
                    NuItems.frailPolyester,75
            ));
            powerProduction =6f;
            itemDuration = 560f;
            size = 2;
            health = 1200;
            ambientSound = Sounds.loopSmelter;
            ambientSoundVolume = 0.03f;
            generateEffect = Fx.generatespark;
            consume(new ConsumeItemFlammable());
            consume(new ConsumeItemExplode());
            consumeLiquid(Liquids.water, 0.2f);
            itemDurationMultipliers.put(NuItems.Tcoal, 1f);
            consumePowerBuffered(2000f);
            drawer = new DrawMulti(new DrawDefault(), new DrawWarmupRegion());
        }};
        FloatingCapacitor = new Battery("FloatingCapacitor"){{
            requirements(Category.power, with(
                    NuItems.monoSiliCrystal,40,
                    NuItems.bigIron,200,
                    NuItems.pumice,90,
                    NuItems.magent,35
            ));
            size = 2;
            consumePowerBuffered(100000f);
            baseExplosiveness = 0.65f;
        }};
        RestoreMotor = new ConsumeGenerator("RestoreMotor"){{
            requirements(Category.power, with(NuItems.pumice,50,
                    NuItems.monoSiliCrystal,100,
                    NuItems.magent,35,
                    NuItems.bigIron,160
            ));
            powerProduction = 8f;
            itemDuration = 420f;
            size = 2;
            health = 1600;
            ambientSound = Sounds.loopSmelter;
            ambientSoundVolume = 0.03f;
            generateEffect = Fx.generatespark;
            consume(new ConsumeItemReversible());
            consumePowerBuffered(4000f);
            drawer = new DrawMulti(new DrawDefault(), new DrawWarmupRegion());
        }};
        DepletedUraniumPower = new HeaterGenerator("DepletedUraniumPower"){{
            requirements(Category.power, with(
                    NuItems.pumice, 250,
                    NuItems.thallide, 30,
                    NuItems.magent, 150,
                    NuItems.monoSiliCrystal, 500
                    ));
            size = 3;
            liquidCapacity = 80f;
            outputLiquid = new LiquidStack(NuLiquid.nuclearFluid, 20f / 60f);
            explodeOnFull = true;
            heatOutput = 20f;
            consumeLiquid(Liquids.water, 10f / 60f);
            consumeItem(NuItems.oriUranium);
            itemDuration = 60f * 5f;
            itemCapacity = 10;
            explosionRadius = 12;
            explosionDamage = 5000;
            explodeEffect = new MultiEffect(Fx.bigShockwave, new WrapEffect(Fx.titanSmoke,NuLiquid.nuclearFluid.color));
            explodeSound = Sounds.explosionReactorNeoplasm;
            powerProduction = 40f;
            ambientSound = Sounds.loopBio;
            ambientSoundVolume = 0.2f;
            explosionPuddles = 80;
            explosionPuddleRange = tilesize * 7f;
            explosionPuddleLiquid = NuLiquid.nuclearFluid;
            explosionPuddleAmount = 200f;
            explosionMinWarmup = 0.5f;
            consumeEffect = new RadialEffect(NuFx.NuclearConsumeSmoke, 4, 90f, 54f / 4f);
            drawer = new DrawMulti(
                    new DrawRegion("-bottom"),
                    new DrawLiquidTile(Liquids.water, 3f),
                    new DrawCircles(){{
                        color = NuColor.SailColor;
                        strokeMax = 3.25f;
                        radius = 39f / 4f;
                        amount = 5;
                        timeScl = 200f;
                    }},
                    new DrawRegion("-center"),
                    new DrawCells(){{
                        color = NuColor.SailColor;
                        particleColorFrom = NuColor.SailColor;
                        particleColorTo = NuColor.SailBackColor;
                        particles = 50;
                        range = 4f;
                    }},
                    new DrawDefault(),
                    new DrawHeatOutput(),
                    new DrawGlowRegion("-glow"){{
                        color = NuColor.SailColor;
                        alpha = 0.7f;
                    }}
            );
        }};
        DepletedUraniumCapacitor =  new Battery("DepletedUraniumCapacitor"){{
            requirements(Category.power, with(
                    NuItems.pumice,150,
                    NuItems.magent,200,
                    NuItems.alkSliver,90,
                    NuItems.uranium,35
            ));
            consumePowerBuffered(1000000f);
            baseExplosiveness = 2f;
            size = 3;
        }};
        RadioisotopeGenerator = new ConsumeGenerator("RadioisotopeGenerator"){{
            requirements(Category.power, with(NuItems.alkSliver,50,
                    NuItems.monoSiliCrystal,200,
                    NuItems.magent,120,
                    NuItems.pumice,160
            ));
            powerProduction = 15f;
            itemDuration = 600f;
            size = 3;
            health = 4000;
            ambientSound = Sounds.loopSmelter;
            ambientSoundVolume = 0.03f;
            generateEffect = Fx.generatespark;
            consume(new ConsumeItemRadioactive());
            itemDurationMultipliers.put(NuItems.uranium, 210f / 14f);
            consumePowerBuffered(10000f);
            drawer = new DrawMulti(new DrawDefault(), new DrawWarmupRegion());
        }};
        UraniumPowerAppliance = new NuclearReactor("UraniumPowerAppliance"){{
            requirements(Category.power, with(
                    NuItems.pumice, 350,
                    NuItems.monoSiliCrystal, 300,
                    Items.graphite, 300,
                    NuItems.uranium, 50,
                    NuItems.frailPolyester, 100
            ));
            ambientSound = Sounds.loopThoriumReactor;
            ambientSoundVolume = 0.11f;
            size = 4;
            health = 9000;

            // ========== 燃料（原版 NuclearReactor 用 fuelItem 字段，不是 consumeItem）==========
            consumeItem(NuItems.uranium);          // 指定铀为燃料（原版默认是 Items.thorium）
            fuelItem = NuItems.uranium;
            itemDuration = 600f;                 // 每 600 tick 消耗 1 个燃料
            itemCapacity = 40;                   // 能装多少个燃料（原版默认 30）

            // ========== 发电（效率 = 燃料装满度 * powerProduction）==========
            powerProduction = 80f;               // 装满 40 个铀时的峰值功率
            // ========== 热量 & 冷却液（原版自动扣 liquids.current()，不支持指定液体种类）==========
            heating = 0.03f;                     // 满载时每 tick 加热量（原版 0.01f → 调 3 倍，更容易炸）
            coolantPower = 0.5f;                 // 每 1 单位冷却液带走多少热（原版默认 0.5）
            heatOutput = 15f;                    // 向相邻热方块输出的最大热（原版默认 15）
            smokeThreshold = 0.3f;               // heat 超过 30% 开始冒烟（原版默认 0.3）
            flashThreshold = 0.46f;              // heat 超过 46% 灯光闪烁警告（原版默认 0.46）
            ambientCooldownTime = 60f * 20f;     // 没燃料时自然冷却到 0 的时长（原版默认 20 秒）
            liquidCapacity = 40;                 // 冷却液储量（原版默认 30）
            // ========== 爆炸参数（heat >= 1.0 时触发）==========
            explosionShake = 6f;
            explosionShakeDuration = 24f;
            explosionRadius = 30;
            explosionDamage = 2500 * 4;
            explodeEffect = NuFx.ExplosionNuclear;
            explodeSound = Sounds.explosionReactor;
            consumeLiquid(NuLiquid.liquidOxygen,0.025f).update(false);
        }};


        bigIronWall = new Wall ("bigIronWall"){{
                requirements(Category.defense, with(NuItems.bigIron, 6));
                health = 540;
                armor = 10f;
                size = 1;
                lightningChance = 0.02f;
            }};
        bigIronLargeWall = new Wall("bigIronLargeWall"){{
            requirements(Category.defense, with(NuItems.bigIron,24));
            health = 540*4;
            armor = 10f;
            size = 2;
            lightningChance = 0.025f;
        }};
        energyStorageWall = new EnergyShieldWall("energyStorageWall"){{
            requirements(Category.defense, with(NuItems.bigIron, 10, NuItems.monoSiliCrystal, 6));
            health = 650;
            // ① 储电（consumePowerBuffered 自动充电+电网共享，无需手填充电速度）
            powerCapacity  = 2400f;
            // ② 满电自恢复（1=每秒回1HP，1:1 比例）
            fullPowerRegenPerSec = 1f;
            // ③ 护盾接口（★ 默认 shieldEnabled=false 就是关的，想启用改成 true 再填半径）
            shieldEnabled = false;
            shieldRadius  = 110f;
            shieldSides   = 2;
            shieldColor   = null;  // null = 队伍颜色
        }};
        energyStorageLargeWall = new EnergyShieldWall("energyStorageLargeWall"){{
            size = 2;
            requirements(Category.defense, with(NuItems.bigIron, 40, NuItems.monoSiliCrystal, 24));
            health = 650 * 4;
            powerCapacity  = 2400f * 4;
            fullPowerRegenPerSec = 2f;
            shieldEnabled = false;
            shieldRadius  = 150f;
        }};
        IllusionGate = new AutoDoor("illusionGate"){{
            requirements(Category.defense, with(NuItems.bigIron, 5, NuItems.monoSiliCrystal,1));
            health = 700;
            armor = 18f;
            size = 1;
        }};
        IllusionLargeGate = new AutoDoor("illusionLargeGate"){{
            requirements(Category.defense, with(NuItems.bigIron, 20, NuItems.monoSiliCrystal,4));
            health = 700*4;
            armor = 18f;
            size = 2;
        }};
        frailPolyesterWall = new Wall("frailPolyesterWall"){{
            requirements(Category.defense, with(NuItems.frailPolyester, 6));
            health = 600;
            chanceDeflect = 1f;
            size = 1;
            flashHit = true;
        }};
        frailPolyesterLargeWall = new Wall("frailPolyesterLargeWall"){{
            requirements(Category.defense, with(NuItems.frailPolyester, 6));
            health = 600*4;
            chanceDeflect = 1f;
            size = 2;
            flashHit = true;
        }};
        // ===================== 磁力墙（受击按概率吸引周围敌方单位）=====================
        magneticPullWall = new MagneticPullWall("magneticPullWall"){{
            requirements(Category.defense, with(NuItems.magent, 10, NuItems.bigIron, 8));
            health = 520;
            lightningChance = 0.03f;  // 兼容原版 Wall 的闪电反击
            // 磁力参数
            pullChance   = 0.28f;     // 每次受击 28% 概率触发
            pullRadius   = 160f;      // 吸引半径（20 格）
            pullStrength = 3.8f;      // 拉力强度
            pullDuration = 22f;       // 触发一次持续约 22 tick
            pullEffect   = Fx.steam;  // 触发视觉特效，可换成 NuFx.xxx
        }};
        magneticPullLargeWall = new MagneticPullWall("magneticPullLargeWall"){{
            size = 2;
            requirements(Category.defense, with(NuItems.magent, 40, NuItems.bigIron, 32));
            health = 520 * 4;
            lightningChance = 0.035f;
            pullChance   = 0.32f;
            pullRadius   = 220f;      // 27.5 格
            pullStrength = 5.0f;
            pullDuration = 28f;
            pullEffect   = Fx.steam;
        }};
        floatWall = new Wall("floatWall"){{
            requirements(Category.defense, with(NuItems.pumice, 6));
            health = 1000;
            size = 1;
        }};
        floatLargeWall = new Wall("floatLargeWall"){{
            requirements(Category.defense, with(NuItems.pumice,24));
            health = 1000*4;
            size = 2;
        }};
        rubberWall = new Wall("rubberWall"){{
            requirements(Category.defense, with(NuItems.rubber, 5, NuItems.frailPolyester, 2));
            health = 750;
            size = 1;
            insulated = true;
            absorbLasers = true;
            schematicPriority = 10;
        }};
        rubberLargeWall = new Wall("rubberLargeWall"){{
            requirements(Category.defense,with(NuItems.rubber,20,NuItems.frailPolyester,8));
            health = 750*4;
            size = 2;
            insulated = true;
            absorbLasers = true;
            schematicPriority = 10;
        }};
        alkSliverWall = new  PowerTurret("alkSliverWall"){{
            requirements(Category.defense,with(NuItems.alkSliver,6));
            health = 1200;
            size = 1;
            range = 16f;
            shootCone = 360f;
            reload = 30f;
            consumePower(0.5f);
            shootType = new BasicBulletType(0f,40f){{
                pierceCap = 90;
                lifetime = 45;
                instantDisappear = true;
                splashDamage = 80f;
                splashDamageRadius = 16f;
                shootEffect = new WaveEffect(){{
                    sizeFrom = 0f;
                    sizeTo = 16f;
                    colorFrom = NuColor.PaleColor;
                    colorTo = NuColor.PaleBackColor;
                    strokeTo = 0f;
                    strokeFrom = 2f;
                }};
                status = NuStatus.radiation;
                statusDuration = 60f*30;
                hitEffect = Fx.none;
                despawnEffect = new RadialEffect(){{
                    amount = 1;
                    rotationSpacing = 90f;
                    rotationOffset = 55f;
                    lengthOffset = 12f;
                    effect =NuFx.ConsumeSmoke;
                }};
            }};
        }};
        alkSliverLargeWall = new PowerTurret("alkSliverLargeWall"){{
            requirements(Category.defense,with(NuItems.alkSliver,24));
            health = 1200*4;
            size = 2;
            range = 24f;
            shootCone = 360f;
            reload = 30f;
            consumePower(0.75f);
            shootType = new BasicBulletType(0f,54f){{
                pierceCap = 120;
                lifetime = 25;
                instantDisappear = true;
                splashDamage = 80f;
                splashDamageRadius = 16f;
                shootEffect = new WaveEffect(){{
                    sizeFrom = 0f;
                    sizeTo = 16f;
                    colorFrom = NuColor.PaleColor;
                    colorTo = NuColor.PaleBackColor;
                    strokeTo = 0f;
                    strokeFrom = 2f;
                    lifetime = 25f;
                }};
                status = NuStatus.radiation;
                statusDuration = 60f*30;
                hitEffect = Fx.none;
                despawnEffect = new RadialEffect(){{
                    amount = 1;
                    rotationSpacing = 90f;
                    rotationOffset = 55f;
                    lengthOffset = 12f;
                    effect =NuFx.ConsumeSmoke;
                }};
            }};
        }};
        thallideWall = new Wall("thallideWall"){{
            requirements(Category.defense, with(NuItems.thallide,6));
            health = 1800;
            size = 1;
        }};
        thallideLargeWall = new Wall("thallideLargeWall"){{
            requirements(Category.defense, with(NuItems.thallide,24));
            health = 1800*4;
            size = 2;
        }};
        uraniumWall = new ShieldWall("uraniumWall"){{
            requirements(Category.defense, with(NuItems.uranium,6));
            health = 2000;
            size = 1;
            shieldHealth = 1000;
            hasPower = true;
            consumePower(0.1f);
            chanceDeflect = 20f;
        }};
        uraniumLargeWall = new ShieldWall("uraniumLargeWall"){{
            requirements(Category.defense, with(NuItems.uranium,24));
            health = 2000*4;
            size = 2;
            shieldHealth = 1000*4;
            hasPower = true;
            consumePower(0.1f);
            chanceDeflect = 20f;
        }};
        energyShield = new BaseShield("energyShield"){{
            size = 2;
            requirements(Category.defense, with(NuItems.uranium,4, NuItems.thallide, 20));
            health = 1600*4;
            radius = 12f;
            consumePower(0.2f);
        }};
        Lotus = new Wall("Lotus"){{
            size = 2;
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