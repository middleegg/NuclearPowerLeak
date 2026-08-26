package Npl.content;

import static mindustry.Vars.*;
import static mindustry.type.ItemStack.*;
import Npl.newSth.*;
import Npl.newSth.walls.*;
import Npl.content.*;
import Npl.newSth.consumes.*;
import Npl.newSth.assembler.*;
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
            //crafting,磁铁提纯机，单晶体厂，货币产房，交易所，压缩室，电热制机，*污化室，奇液收集室，碾碎室
            // 采种机，橡胶种植机，热能解放室，*污秽分解室，蒸馏室，镀银室，研磨机，核废液收集器，*圣铁镌刻室,*棱能粉碎器
            // *溯流逆转室,铊化物坩埚，*神泪富集室，氧低温室，铀提纯室，磁暴稳定装置，磁铁化能站，铀沉淀室
            MagentPurifier,monoSiliCrystalFactory,coinProducer, exchange,CompressionChamber,HeatMaker,
            contaminationRoom,strangeLiquidExtractionRoom,crushingRoom,
            seedCollector,rubberGrower,heatRelaxation, dirtyDecompositionRoom, distillationRoom,
            silverPlatingRoom,Grinder,nuclearFluidCollector, sacredIronEngravingRoom,
            prismSmasher,backflowReversalRoom,thalliumCompoundCrucible,
            divineTearsEnrichRoom, OxygenLiquefactionRoom,uraniumPurificationRoom,
            MagenticStormStabiliser,MagentEnergyStation,UraniumPrecipitationRoom,
            //effect
            antiStealthRadar,BulletAccelerator,FederalJuniorCore,FederalSubCore,FederalContainer,FederalWarehouse,
            JuniorMender,MenderProjector,SeniorMender,DefenceShieldProjector,OverloadedThrowor,SeniorOverloaded,
            OverloadDefenceTower,ConstructionField,
            //power
            ElectricalNode,PowerCapacitor,OriginalElectronics,SteamElectronics,FloatingCapacitor,RestoreMotor,
            RadioisotopeGenerator,DepletedUraniumPower,DepletedUraniumCapacitor,UraniumPowerAppliance,
            //wall
            bigIronWall,bigIronLargeWall,energyStorageWall,energyStorageLargeWall,IllusionGate,IllusionLargeGate,
            frailPolyesterWall,frailPolyesterLargeWall,magneticPullWall,magneticPullLargeWall,
            floatWall,floatLargeWall,rubberWall,rubberLargeWall,alkSliverWall,alkSliverLargeWall,
            thallideWall,thallideLargeWall,uraniumWall,uraniumLargeWall,energyShield,Lotus,
            //transport
            bigIronDuct,bigIronRouter,bigIronOverFlow,bigIronUnderFlow,basicUnloader,bigIronJunction,bigIronBridge,
            ThermalConductor,GaintThermalConductor,floatDuct,floatRouter,floatOverFlow,floatUnderFlow,floatJunction,
            floatBridge,UnitCarryingPoint,UnitUnloadingContainer,SwiftConveyor,UnifiedDrive,
            //liquid
            bigIronPump,floatPump,nuclearPowerPump,OrganicConduit,OrganicJunction,OrganicRouter,
            OrganicBridge,FloatConduic,FloatJunction,FloatBridge,FloatRouter,FloatTank,
            //units
            ExperimentalMachineryUnitFactory,ExperimentalUnitReconstructionFactory,MechanicalAssemblyFactory,AirshipAssemblyFactory,
            ShipAssemblyFactory,ParadoxUnitAssemblyFactory,TerminalUnitAssemblyFactory,bigIronUnitConveyor,floatUnitConvryor,
            floatUnitRouter,ParadoxAssemblyModule,TerminalAssemblyModule,NuclearAssemblyParts,AbsurdAssemblyParts,ThalliumAssemblyParts,
            GodForsakenParts,FatedParts,StandaloneParts,CalamityParts,AssemblyPlantModule,BuildingConstructor,
            SpecialUnitFactory,PaleUnitFactory,PaleNumberReconstruction,PaleMultiplyReconstruction,
            PaleExponentReconstruction,PaleImmeasurableReconstruction,
            //logic
            //turret,胜天，立地，万诺，焚毁，欢悦，溯源，库兰，核磁，未见，牵引，息壤，灼伤,浊气，牵越，群山，烟霞
            //熔断，间压，神乾，秽罪，常胜，游龙
            DefeatGod,StandingGround,Wanuo,Incinerate,Joy,TraceSource,Kurao,MPI,NonSeen,Traction,
            BreathSoil,BurnInjured,TurbidAir,CrossTractor,Mountains,MistRosy,CircuitBreak,IntermittentPressure,
            DivineCreation,FilthySin,EverVictorious,SoarDragon,multiTurret,awnlessSpike,
            //production
            bigIronDrill,floatBaseDrill,floatDrill,sliverDrill,prismDrill,nuclearDrill,hotMeltDrill,
            waterSamplingDevice,wallCrusher,droneDrill,
            rubberCrusher,uranCrystalCrusher,fifthCoagulator,prismVenter;
    public static void load() {

        MagentPurifier = new GenericCrafter("MagentPurifier") {{
            requirements(Category.crafting, with(NuItems.bigIron,100,
                    NuItems.sulFurFrag,75,
                    NuItems.monoSiliCrystal,100));
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
            researchCost = with(NuItems.monoSiliCrystal,650,NuItems.bigIron,2000,NuItems.graphite,700);
        }};
        monoSiliCrystalFactory = new GenericCrafter("monoSiliCrystalFactory") {{
            requirements(Category.crafting, with(NuItems.bigIron,100,Items.graphite,30));
            outputItem = new ItemStack(NuItems.monoSiliCrystal,5);
            hasItems=true;
            ambientSound=Sounds.loopGrind;
            ambientSoundVolume=0.025f;
            consumeItems(ItemStack.with(NuItems.Tcoal,6,Items.sand,6));
            health = 800;
            size=2;
            itemCapacity = 40;
            drawer = new DrawMulti(new DrawRegion("-bottom"),new DrawRegion("-top"),new DrawDefault(),
                    new DrawRegion("-rotator"){{
                        rotateSpeed = 3.6f;
                        spinSprite = true;
                    }},
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
        coinProducer = new HunfuBlock("coinProducer") {{
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
            plans = Seq.with(
                    new Plan(null,90f,with(NuItems.bigIron,100),null,null,0,15),
                    new Plan(null,120f,with(NuItems.pumice,100),null,null,0,80),
                    new Plan(null,90f,with(NuItems.frailPolyester,100),null,null,0,20)
            );
            consumePower(8f);
            researchCost = with(NuItems.rubber,320,
                    NuItems.alkSliver,480,
                    NuItems.bigIron,800,
                    NuItems.monoSiliCrystal,650,
                    NuItems.pumice,640
                    );
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
                    new Plan(with(NuItems.bigIron,100),60f*10,null,20),
                    new Plan(with(NuItems.monoSiliCrystal,100),60f*20,null,80),
                    new Plan(with(Items.graphite,100),60f*20,null,80),
                    new Plan(with(NuItems.sulFurFrag,100),60f*16,null,60),
                    new Plan(with(NuItems.magent,100),60f*30,null,120),
                    new Plan(with(NuItems.frailPolyester,100),60f*10,null,30),
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
        contaminationRoom = new GenericCrafter("contaminationRoom"){{
            requirements(Category.crafting, with(
                    NuItems.bigIron,240,
                    NuItems.graphite,145,
                    NuItems.monoSiliCrystal,140,
                    NuItems.magent,40
            ));
            outputLiquid = new LiquidStack(NuLiquid.dirtySolution,0.15f);
            hasItems = true;
            consumePower(4f);
            craftTime = 75f;
            health = 800;
            size = 2;
            consumeItems(ItemStack.with(NuItems.dirtyCoagulum,3));
            craftEffect = new WrapEffect(){{
                effect = Fx.lava;
                color = NuColor.HonorColor;
            }};
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
        crushingRoom = new GenericCrafter("crushingRoom"){{
            requirements(Category.crafting,with(
                    NuItems.frailPolyester,60,
                    NuItems.monoSiliCrystal,45
            ));
            health = 400;
            size = 1;
            researchCostMultiplier = 1.15f;
            craftEffect = NuFx.PaleSmoke;
            itemCapacity = 40;
            ambientSound = Sounds.loopSmelter;
            ambientSoundVolume = 0.24f;
            drawer = new DrawMulti(new DrawRegion("-rotator"){{
                rotateSpeed = 20f;
                spinSprite = true;
            }}, new DrawDefault(),new DrawRegion("-top"));
           consumeItem(NuItems.dirtyCoagulum,2);
           outputItem = new ItemStack(NuItems.Tcoal,4);
        }};
        seedCollector = new GenericCrafter("seedCollector") {{
            requirements(Category.crafting, with(Items.graphite,450,NuItems.pumice,250,NuItems.magent,50));
            hasItems = true;
            craftTime = 450f;
            health = 800;
            size = 2;
            buildTime = 40f;
            drawer = new DrawMulti(new DrawRegion("-rotator"){{
                rotateSpeed = 4f;
                spinSprite = true;
            }}, new DrawDefault(),new DrawRegion("-top"));
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
            craftTime = 600f;
            consumePower(7.5f);
            outputItem = new ItemStack(NuItems.rubber,3);
            drawer = new DrawMulti(new DrawHeatInput(){{
                heatColor = NuColor.HeatColor;
            }},new DrawDefault(),new DrawGlowRegion("-heat"){{
                blending = Blending.additive;
                color = NuColor.HeatConColor;
            }},new DrawDefault(), new DrawParticles(){{
                color = NuColor.HeatColor;
                alpha = 0.6f;
                particleSize = 4f;
                particles = 10;
                particleRad = 12f;
                particleLife = 140f;
            }});
            ambientSound = Sounds.loopSmelter;
            consumeItems(ItemStack.with(NuItems.rubberFrag,2));
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
        dirtyDecompositionRoom = new GenericCrafter("dirtyDecompositionRoom"){{
            requirements(Category.crafting, with(
                    NuItems.monoSiliCrystal,125,
                    Items.graphite,200,
                    NuItems.bigIron,240,
                    NuItems.pumice, 80));
            size = 3;
            researchCostMultiplier = 1.2f;
            craftTime = 75f;
            rotate = true;
            invertFlip = true;
            group = BlockGroup.liquids;
            itemCapacity = 20;
            liquidCapacity = 50f;
            consumeLiquid(NuLiquid.dirtySolution, 32f / 60f);
            consumePower(8f);
            drawer = new DrawMulti(
                    new DrawRegion("-bottom"),
                    new DrawLiquidTile(NuLiquid.dirtySolution, 2f),
                    new DrawBubbles(NuColor.HonorColor){{
                        sides = 10;
                        recurrence = 3f;
                        spread = 6;
                        radius = 1.5f;
                        amount = 20;
                    }},
                    new DrawRegion(),
                    new DrawLiquidOutputs(),
                    new DrawGlowRegion(){{
                        alpha = 0.7f;
                        color = Liquids.water.color;
                        glowIntensity = 0.3f;
                        glowScale = 6f;
                    }}
            );
            ambientSound = Sounds.loopElectricHum;
            ambientSoundVolume = 0.08f;
            regionRotated1 = 3;
            outputItem = new ItemStack(NuItems.dirtyCoagulum,1);
            outputLiquids = LiquidStack.with(Liquids.water,10f/60,NuLiquid.nuclearFluid,8f/60);
            liquidOutputDirections = new int[]{1, 3};
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
        sacredIronEngravingRoom = new GenericCrafter("sacredIronEngravingRoom"){{
            requirements(Category.crafting,with(
                    NuItems.pumice,300,
                    NuItems.magent,250,
                    NuItems.frailPolyester,125,
                    NuItems.alkSliver,200
            ));
            ambientSound = Sounds.loopGrind;
            ambientSoundVolume = 0.2f;
            size = 2;
            buildTime = 25f;
            health = 800;
            itemCapacity = 40;
            consumeItems(ItemStack.with(NuItems.bigIron,3));
            consumeLiquids(LiquidStack.with(NuLiquid.prismLiquid,45/60f));
            outputItem = new ItemStack(NuItems.sacredIron,3);
            craftTime = 100f;
            liquidCapacity = 500f;
            craftEffect = new ParticleEffect(){{
                particles = 8;
                cone = 180;
                lenFrom = 15f;
                lenTo = 0f;
                spin = 3f;
                sizeFrom = 4f;
                sizeTo = 0f;
                colorFrom = NuColor.PaleColor;
                colorTo = NuColor.PaleBackColor;
                lifetime = 60f;
                layer =110f;
            }};
            consumePower(12f);
        }};
        prismSmasher = new HeatCrafter("prismSmasher"){{
            requirements(Category.crafting,with(
                    NuItems.sacredIron,100,
                    NuItems.magent,210,
                    NuItems.pumice,300
            ));
            health = 1200;
            size = 3;
            craftTime = 600f;
            heatRequirement = 3;
            maxEfficiency = 6;
            ambientSound = Sounds.loopSmelter;
            ambientSoundVolume = 0.24f;
            researchCostMultiplier = 1.1f;
            consumePower(6f);
            outputLiquid = new LiquidStack(NuLiquid.prismLiquid,45/60f);
            consumeItems(ItemStack.with(NuItems.prismCrystal,4));
            craftEffect = Fx.smoke;
            itemCapacity = 40;
        }};
        backflowReversalRoom = new GenericCrafter("backflowReversalRoom"){{
            requirements(Category.crafting,with(
                    NuItems.pumice,300,
                    NuItems.magent,250,
                    NuItems.uranium,125,
                    NuItems.sacredIron,200
            ));
            ambientSound = Sounds.loopGrind;
            ambientSoundVolume = 0.2f;
            size = 3;
            buildTime = 25f;
            health = 1400;
            itemCapacity = 40;
            consumeItems(ItemStack.with(NuItems.sacredIron,4,NuItems.thallium,3,NuItems.prismCrystal,3));
            consumeLiquids(LiquidStack.with(NuLiquid.prismLiquid,1f));
            outputItem = new ItemStack(NuItems.remakeSource,5);
            craftTime = 300f;
            liquidCapacity = 500f;
            craftEffect = new ParticleEffect(){{
                particles = 8;
                cone = 180;
                lenFrom = 15f;
                lenTo = 0f;
                spin = 3f;
                sizeFrom = 4f;
                sizeTo = 0f;
                colorFrom = NuColor.PaleColor;
                colorTo = NuColor.PaleBackColor;
                lifetime = 60f;
                layer =110f;
            }};
            consumePower(18f);
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
            consumeItems(ItemStack.with(NuItems.thallium,6,NuItems.sulFurFrag,3));
            craftEffect = Fx.smoke;
            itemCapacity = 60;
        }};
        divineTearsEnrichRoom = new GenericCrafter("divinTearsEnrichRoom"){{
            requirements(Category.crafting,with(
                    NuItems.pumice,300,
                    NuItems.magent,250,
                    NuItems.uranium,125,
                    NuItems.sacredIron,200
            ));
            ambientSound = Sounds.loopGrind;
            ambientSoundVolume = 0.2f;
            size = 3;
            buildTime = 25f;
            health = 1400;
            itemCapacity = 40;
            consumeItems(ItemStack.with(NuItems.magent,1,NuItems.prismCrystal,2));
            consumeLiquids(LiquidStack.with(Liquids.water,35/60f));
            outputLiquid = new LiquidStack(NuLiquid.divineTears,0.4f);
            craftTime = 120f;
            liquidCapacity = 500f;
            craftEffect = new ParticleEffect(){{
                particles = 8;
                cone = 180;
                lenFrom = 15f;
                lenTo = 0f;
                spin = 3f;
                sizeFrom = 4f;
                sizeTo = 0f;
                colorFrom = NuColor.PaleColor;
                colorTo = NuColor.PaleBackColor;
                lifetime = 60f;
                layer =110f;
            }};
            consumePower(14f);
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
            size = 3;
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
            size = 2;
            hasItems = hasLiquids =true;
            drawer = new DrawMulti(new DrawRegion("-bottom"),
                    new DrawDefault(),
                    new DrawCultivator(){{
                        plantColor = NuLiquid.nuclearFluid.color;
                        plantColorLight = NuColor.SailBackColor;
                    }},new DrawRegion("-top")
            );
            consumePower(20f);
            consumeItems(ItemStack.with(NuItems.oriUranium,2));
            consumeLiquids(LiquidStack.with(Liquids.water,0.5f));
            outputItem = new ItemStack(NuItems.uranium,2);
            craftEffect = new ParticleEffect(){{
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
            }};
        }};
        MagenticStormStabiliser = new StormCrafterBlock("MagenticStormStabiliser") {{
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
        MagentEnergyStation = new HeatProducer("MagentEnergyStation") {{
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
            drawer = new DrawMulti(new DrawRegion("-bottom"),new DrawLiquidTile(Liquids.water,2){{
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
            itemCapacity = 500;
            scaledHealth = 500;
        }};
        FederalWarehouse = new StorageBlock("FederalWarehouse"){{
            requirements(Category.effect, with(NuItems.magent,300,NuItems.alkSliver,100));
            size = 3;
            itemCapacity = 1500;
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
            speedBoostPhase = 0.6f;
            ambientSoundVolume = 0.12f;
            phaseRangeBoost = 24f;
            hasBoost = true;
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
            phaseRangeBoost = 64f;
            speedBoost = 1.8f;
            speedBoostPhase = 1.7f;
            useTime = 300f;
            ambientSoundVolume = 0.12f;
            hasBoost = true;
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
            reload = 0.25f;
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
                health = 1600;
                armor = 10;
                size = 1;
                lightningChance = 0.02f;
            }};
        bigIronLargeWall = new Wall("bigIronLargeWall"){{
            requirements(Category.defense, with(NuItems.bigIron,24));
            health = 1600*4;
            armor = 10;
            size = 2;
            lightningChance = 0.025f;
        }};
        energyStorageWall = new EnergyShieldWall("energyStorageWall"){{
            requirements(Category.defense, with(NuItems.bigIron, 10, NuItems.monoSiliCrystal, 6));
            health = 1750;
            armor = 14;
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
            health = 1750 * 4;
            armor = 14;
            powerCapacity  = 2400f * 4;
            fullPowerRegenPerSec = 2f;
            shieldEnabled = false;
            shieldRadius  = 150f;
        }};
        IllusionGate = new AutoDoor("illusionGate"){{
            requirements(Category.defense, with(NuItems.bigIron, 5, NuItems.monoSiliCrystal,1));
            health = 1700;
            armor = 14;
            size = 1;
        }};
        IllusionLargeGate = new AutoDoor("illusionLargeGate"){{
            requirements(Category.defense, with(NuItems.bigIron, 20, NuItems.monoSiliCrystal,4));
            health = 1700*4;
            armor = 14;
            size = 2;
        }};
        frailPolyesterWall = new Wall("frailPolyesterWall"){{
            requirements(Category.defense, with(NuItems.frailPolyester, 6));
            health = 1600;
            armor  = 18;
            chanceDeflect = 1f;
            size = 1;
            flashHit = true;
        }};
        frailPolyesterLargeWall = new Wall("frailPolyesterLargeWall"){{
            requirements(Category.defense, with(NuItems.frailPolyester, 6));
            health = 1600*4;
            armor = 18;
            chanceDeflect = 1f;
            size = 2;
            flashHit = true;
        }};
        // ===================== 磁力墙（受击按概率吸引周围敌方单位）=====================
        magneticPullWall = new MagneticPullWall("magneticPullWall"){{
            requirements(Category.defense, with(NuItems.magent, 10, NuItems.bigIron, 8));
            health = 1900;
            armor = 24;
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
            health = 1900 * 4;
            armor = 24;
            lightningChance = 0.035f;
            pullChance   = 0.32f;
            pullRadius   = 220f;      // 27.5 格
            pullStrength = 5.0f;
            pullDuration = 28f;
            pullEffect   = Fx.steam;
        }};
        floatWall = new Wall("floatWall"){{
            requirements(Category.defense, with(NuItems.pumice, 6));
            health = 2400;
            armor = 28;
            size = 1;
        }};
        floatLargeWall = new Wall("floatLargeWall"){{
            requirements(Category.defense, with(NuItems.pumice,24));
            health = 2400*4;
            size = 2;
            armor = 28;
        }};
        rubberWall = new Wall("rubberWall"){{
            requirements(Category.defense, with(NuItems.rubber, 5, NuItems.frailPolyester, 2));
            health = 2750;
            armor = 32;
            size = 1;
            insulated = true;
            absorbLasers = true;
            schematicPriority = 10;
        }};
        rubberLargeWall = new Wall("rubberLargeWall"){{
            requirements(Category.defense,with(NuItems.rubber,20,NuItems.frailPolyester,8));
            health = 2750*4;
            armor = 32;
            size = 2;
            insulated = true;
            absorbLasers = true;
            schematicPriority = 10;
        }};
        alkSliverWall = new  PowerTurret("alkSliverWall"){{
            requirements(Category.defense,with(NuItems.alkSliver,6));
            health = 3000;
            armor = 36;
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
                despawnEffect = Fx.none;
                hitEffect = new RadialEffect(){{
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
            health = 3000*4;
            armor = 36;
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
                despawnEffect = Fx.none;
                hitEffect = new RadialEffect(){{
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
            health = 3500;
            armor = 45;
            size = 1;
        }};
        thallideLargeWall = new Wall("thallideLargeWall"){{
            requirements(Category.defense, with(NuItems.thallide,24));
            health = 3500*4;
            armor = 45;
            size = 2;
        }};
        uraniumWall = new ShieldWall("uraniumWall"){{
            requirements(Category.defense, with(NuItems.uranium,6));
            health = 4000;
            armor = 48;
            size = 1;
            shieldHealth = 1000;
            hasPower = true;
            consumePower(0.1f);
            chanceDeflect = 20f;
        }};
        uraniumLargeWall = new ShieldWall("uraniumLargeWall"){{
            requirements(Category.defense, with(NuItems.uranium,24));
            health = 4000*4;
            armor = 48;
            size = 2;
            shieldHealth = 1000*4;
            hasPower = true;
            consumePower(0.1f);
            chanceDeflect = 20f;
        }};
        energyShield = new BaseShield("energyShield"){{
            size = 2;
            armor = 40;
            requirements(Category.defense, with(NuItems.uranium,4, NuItems.thallide, 20));
            health = 3200*4;
            radius = 12f;
            consumePower(0.2f);
        }};
        Lotus = new Wall("Lotus"){{
            size = 2;
        }};


        bigIronRouter = new DuctRouter("bigIronRouter"){{
            requirements(Category.distribution, with(NuItems.bigIron,12));
            health = 200;
            speed = 3.5f;
            regionRotated1 = 1;
            solid = false;
            researchCost = with(NuItems.bigIron,64);
        }};
        bigIronOverFlow = new OverflowDuct("bigIronOverFlow"){{
            requirements(Category.distribution, with(Items.graphite,8,NuItems.bigIron,8));
            health = 200;
            speed = 3.5f;
            solid = false;
            researchCostMultiplier = 1.5f;
        }};
        bigIronUnderFlow = new OverflowDuct("bigIronUnderFlow"){{
            requirements(Category.distribution, with(Items.graphite,8,NuItems.bigIron,8));
            health = 200;
            speed = 3.5f;
            solid = false;
            researchCostMultiplier = 1.5f;
            invert = true;
        }};
        bigIronJunction = new Junction("bigIronJunction"){{
            requirements(Category.distribution, with(NuItems.bigIron, 3));
            speed = 40;
            capacity = 15;
            health = 300;
            buildCostMultiplier = 3f;
        }};
        bigIronBridge = new BufferedItemBridge("bigIronBridge"){{
            requirements(Category.distribution, with(Items.graphite,6,NuItems.bigIron, 6));
            fadeIn = moveArrows = false;
            range = 4;
            speed = 45f;
            health = 300;
            arrowSpacing = 6f;
            bufferCapacity = 14;
            crushFragile = true;
        }};
        bigIronDuct = new Duct("bigIronDuct"){{
            requirements(Category.distribution, with(NuItems.bigIron,3));
            health = 200;
            speed = 3.5f;
            researchCost = with(NuItems.bigIron,12);
            bridgeReplacement = bigIronBridge;
            junctionReplacement = bigIronJunction;
        }};
        basicUnloader = new DirectionalUnloader("basicUnloader"){{
            requirements(Category.distribution, with(Items.graphite, 20, NuItems.monoSiliCrystal,20, NuItems.bigIron, 10));
            health = 200;
            speed = 2f;
            solid = false;
            underBullets = true;
            allowCoreUnload = true;
            regionRotated1 = 1;
        }};
        ThermalConductor = new HeatConductor("ThermalConductor"){{
            requirements(Category.crafting, with(NuItems.pumice,40, Items.graphite,60));
            researchCostMultiplier = 10f;
            group = BlockGroup.heat;
            size = 2;
            drawer = new DrawMulti(new DrawDefault(), new DrawHeatOutput(){{
                heatColor = NuColor.HeatColor;
            }}, new DrawHeatInput("-heat"));
            regionRotated1 = 1;
        }};
        GaintThermalConductor = new HeatConductor("GaintThermalConductor"){{
            requirements(Category.crafting, with(NuItems.pumice,160, Items.graphite,100,NuItems.rubber,45));
            researchCostMultiplier = 10f;
            group = BlockGroup.heat;
            size = 3;
            drawer = new DrawMulti(new DrawDefault(), new DrawHeatOutput(){{
                heatColor = NuColor.HeatColor;
            }}, new DrawHeatInput("-heat"));
            regionRotated1 = 1;
        }};
        floatRouter = new DuctRouter("floatRouter"){{
            requirements(Category.distribution, with(NuItems.pumice,12));
            health = 700;
            speed = 1.5f;
            regionRotated1 = 1;
            solid = false;
        }};
        floatJunction = new Junction("floatJunction"){{
            requirements(Category.distribution, with(NuItems.pumice, 3));
            speed = 60;
            capacity = 30;
            health = 700;
            buildCostMultiplier = 3f;
        }};
        floatOverFlow = new OverflowDuct("floatOverFlow"){{
            requirements(Category.distribution, with(NuItems.frailPolyester,8,NuItems.pumice,8));
            health = 700;
            speed = 1.5f;
            solid = false;
            researchCostMultiplier = 1.5f;
        }};
        floatUnderFlow = new OverflowDuct("floatUnderFlow"){{
            requirements(Category.distribution, with(NuItems.frailPolyester,8,NuItems.pumice,8));
            health = 700;
            speed = 1.5f;
            solid = false;
            researchCostMultiplier = 1.5f;
            invert = true;
        }};
        floatBridge = new BufferedItemBridge("floatBridge"){{
            requirements(Category.distribution, with(NuItems.frailPolyester,10,NuItems.pumice, 6));
            fadeIn = moveArrows = false;
            range = 7;
            speed = 20f;
            health = 700;
            itemCapacity = 25;
            arrowSpacing = 6f;
            bufferCapacity = 14;
            crushFragile = true;
        }};
        floatDuct = new Duct("floatDuct"){{
            requirements(Category.distribution, with(NuItems.pumice,3));
            health = 700;
            speed = 1.5f;
            bridgeReplacement = floatBridge;
            junctionReplacement = floatJunction;
        }};
        UnitCarryingPoint = new UnitCargoLoader("UnitCarryingPoint"){{
            requirements(Category.distribution, with(NuItems.monoSiliCrystal,80, NuItems.rubber, 50,NuItems.alkSliver, 20));
            size = 2;
            health = 1000;
            unitBuildTime = 60f *8f;
            consumePower(8f);
            consumeLiquid(NuLiquid.strangeLiquid, 0.2f);
            itemCapacity = 500;
        }};
        UnitUnloadingContainer = new UnitCargoUnloadPoint("UnitUnloadingContainer"){{
            requirements(Category.distribution, with(NuItems.monoSiliCrystal,90, NuItems.pumice,130));
            size = 2;
            health = 1000;
            itemCapacity = 200;
            researchCost = with(Items.silicon, 3000, Items.oxide, 20);
        }};
        SwiftConveyor = new StackConveyor("SwiftConveyor"){{
            requirements(Category.distribution, with(NuItems.uranium,2,NuItems.pumice,5));
            health = 700;
            speed = 0.2f;
            itemCapacity = 50;
            outputRouter = false;
            hasPower = true;
            consumesPower = true;
            conductivePower = true;
            underBullets = true;
            baseEfficiency = 1f;
            consumePower(0.2f);
            researchCost = with(NuItems.uranium, 30, NuItems.pumice, 80);
        }};
        UnifiedDrive = new MassDriver("UnifiedDrive"){{
            requirements(Category.distribution, with(
                    NuItems.magent,30,
                    NuItems.alkSliver,25,
                    NuItems.monoSiliCrystal,30,
                    NuItems.frailPolyester,60
            ));
            size = 1;
            health = 500;
            itemCapacity = 160;
            reload = 1f;
            rotateSpeed = 200f;
            minDistribute = 15;
            baseExplosiveness = 7.5f;
            dumpTime = 1;
            shootEffect = new MultiEffect(new WaveEffect(){{
                colorFrom = NuColor.SurvivalColor;
                colorTo = NuColor.SurvivalBackColor;
                sizeFrom = 0f;
                sizeTo = 12f;
                strokeFrom =2.5f;
                strokeTo = 0f;
                sides = 4;
                interp = Interp.circleOut;
                lifetime = 60f;
            }},new WaveEffect(){{
                colorFrom = NuColor.SurvivalColor;
                colorTo = NuColor.SurvivalBackColor;
                sizeFrom = 0f;
                sizeTo = 8f;
                strokeFrom =2f;
                strokeTo = 2f;
                sides = 4;
                interp = Interp.circleOut;
                lifetime = 60f;
            }},new ParticleEffect(){{
                sizeFrom =2f;
                sizeTo = 0f;
                particles = 1;
                length = 0;
                baseLength = 0;
                colorFrom = NuColor.SurvivalColor;
                colorTo = NuColor.SurvivalBackColor;
                lifetime = 60f;
                interp = Interp.circleIn;
            }});
        }};


        bigIronPump = new Pump("bigIronPump"){{
            requirements(Category.liquid, with(NuItems.bigIron,40, NuItems.frailPolyester,15));
            pumpAmount = 10f / 60f;
            liquidCapacity = 50f;
            size = 1;
            health = 500;
        }};
        floatPump =new Pump("floatPump"){{
            requirements(Category.liquid, with(NuItems.pumice,40, NuItems.monoSiliCrystal,15,NuItems.frailPolyester,60));
            pumpAmount = 1.2f;
            liquidCapacity = 160f;
            consumePower(1f);
            size = 2;
            health = 1200;
        }};
        nuclearPowerPump =new Pump("nuclearPowerPump"){{
            requirements(Category.liquid, with(NuItems.rubber,40, NuItems.uranium,15,NuItems.frailPolyester,120));
            pumpAmount = 5f;
            liquidCapacity = 1000f;
            consumePower(2.5f);
            size = 3;
            health = 2000;
        }};
        OrganicJunction = new LiquidJunction("OrganicJunction"){{
            requirements(Category.liquid, with(Items.graphite, 4, NuItems.frailPolyester,8));
            solid = false;
        }};
        OrganicRouter = new  LiquidRouter("OrganicRouter"){{
            requirements(Category.liquid, with(Items.graphite, 8, NuItems.frailPolyester, 4));
            liquidCapacity = 300f;
            liquidPadding = 2f;
            researchCostMultiplier = 3;
            underBullets = true;
            solid = false;
            health = 600;
            explosivenessScale = flammabilityScale = 40f/15f;
        }};
        OrganicBridge = new LiquidBridge("OrganicBridge"){{
            requirements(Category.liquid, with(Items.graphite, 4, NuItems.frailPolyester, 8));
            floating = true;
            fadeIn = moveArrows = false;
            arrowSpacing = 6f;
            range = 4;
            hasPower = false;
            liquidCapacity = 100f;
            explosivenessScale = flammabilityScale = 20f/10f;
        }};
        OrganicConduit =new Conduit("OrganicConduit"){{
            requirements(Category.liquid, with(NuItems.frailPolyester,2));
            liquidCapacity =50f;
            liquidPressure = 1.6f;
            health = 200;
            explosivenessScale = flammabilityScale = 1f;
            junctionReplacement = OrganicJunction;
            bridgeReplacement = OrganicBridge;
        }};
        FloatJunction = new LiquidJunction("FloatJunction"){{
            requirements(Category.liquid, with(NuItems.pumice,6, NuItems.frailPolyester,12));
            solid = false;
        }};
        FloatRouter = new LiquidRouter("FloatRouter"){{
            requirements(Category.liquid, with(Items.graphite,14, NuItems.frailPolyester,20,NuItems.pumice,4));
            liquidCapacity = 600f;
            liquidPadding = 5f;
            researchCostMultiplier = 3;
            underBullets = true;
            solid = false;
            health = 1000;
            explosivenessScale = flammabilityScale = 4f/60f;
        }};
        FloatBridge = new LiquidBridge("FloatBridge"){{
            requirements(Category.liquid, with(Items.graphite,12, NuItems.frailPolyester,15,NuItems.pumice,8));
            floating = true;
            fadeIn = moveArrows = false;
            arrowSpacing = 6f;
            range = 7;
            hasPower = false;
            liquidCapacity = 1000f;
            explosivenessScale = flammabilityScale = 20f/240f;
        }};
        FloatConduic = new Conduit("FloatConduit"){{
            requirements(Category.liquid, with(NuItems.frailPolyester,4,NuItems.pumice,2));
            liquidCapacity =140f;
            liquidPressure = 4f;
            health = 800;
            explosivenessScale = flammabilityScale = 0.3f;
            junctionReplacement = FloatJunction;
            bridgeReplacement = FloatBridge;
        }};
        FloatTank = new LiquidRouter("FloatTank"){{
            requirements(Category.liquid, with(Items.graphite,14, NuItems.frailPolyester,20,NuItems.pumice,4));
            liquidCapacity = 4500f;
            liquidPadding = 10f;
            researchCostMultiplier = 3;
            underBullets = true;
            solid = false;
            size = 2;
            health = 2500;
            explosivenessScale = flammabilityScale = 20f/60f;
        }};


        ExperimentalMachineryUnitFactory = new UnitFactory("ExperimentalMachineryUnitFactory"){{
            requirements(Category.units, with(NuItems.bigIron,120, NuItems.monoSiliCrystal,120,Items.graphite,80));
            plans = Seq.with(
                    new UnitPlan(FederalUnitTypes.honor, 60f * 24, with(NuItems.monoSiliCrystal,45,NuItems.dirtyCoagulum,25)),
                    new UnitPlan(FederalUnitTypes.vile, 60f * 30, with(NuItems.monoSiliCrystal,50,NuItems.Tcoal,30)),
                    new UnitPlan(FederalUnitTypes.sailor, 60f * 36, with(NuItems.monoSiliCrystal,65))
            );
            size = 3;
            consumePower(4f);
            researchCostMultiplier = 0.5f;
        }};
        ExperimentalUnitReconstructionFactory = new Reconstructor("ExperimentalUnitReconstructionFactory"){{
            requirements(Category.units, with(NuItems.pumice, 200, NuItems.magent, 120, NuItems.monoSiliCrystal, 90));
            size = 3;
            consumePower(10f);
            consumeItems(with(NuItems.monoSiliCrystal,140,Items.graphite,90,NuItems.sulFurFrag,45));
            consumeLiquid(NuLiquid.strangeLiquid,0.1f);
            constructTime = 60f * 42f;
            upgrades.addAll(
                    new UnitType[]{FederalUnitTypes.honor, FederalUnitTypes.proud},
                    new UnitType[]{FederalUnitTypes.vile, FederalUnitTypes.shame},
                    new UnitType[]{FederalUnitTypes.sailor, FederalUnitTypes.cruise}
            );
        }};
        MechanicalAssemblyFactory = new UnitAssembler("MechanicalAssemblyFactory"){{
            requirements(Category.units, with(NuItems.pumice, 500, NuItems.magent,150, NuItems.alkSliver,80, NuItems.monoSiliCrystal,650));
            regionSuffix = "-dark";
            droneType = FederalUnitTypes.humorous;
            size = 5;
            plans.add(
                    new AssemblerUnitPlan(FederalUnitTypes.vanity, 60f * 50f, PayloadStack.list(FederalUnitTypes.honor,8, energyStorageLargeWall, 10)),
                    new AssemblerUnitPlan(FederalUnitTypes.overPraise, 60f * 60f * 3f, PayloadStack.list(FederalUnitTypes.proud,12, rubberLargeWall, 15))
            );
            areaSize = 13;
            researchCostMultiplier = 0.4f;
            consumePower(16f);
            consumeLiquid(NuLiquid.liquidOxygen, 0.2f);
        }};
        AirshipAssemblyFactory = new UnitAssembler("AirshipAssemblyFactory"){{
            requirements(Category.units, with(NuItems.pumice, 500, NuItems.magent,150, NuItems.alkSliver,80, NuItems.monoSiliCrystal,650));
            regionSuffix = "-dark";
            droneType = FederalUnitTypes.humorous;
            size = 5;
            plans.add(
                    new AssemblerUnitPlan(FederalUnitTypes.loss, 60f * 50f, PayloadStack.list(FederalUnitTypes.vile,8, energyStorageLargeWall, 8)),
                    new AssemblerUnitPlan(FederalUnitTypes.nonsense, 60f * 60f * 3f, PayloadStack.list(FederalUnitTypes.shame,12, alkSliverLargeWall, 15))
            );
            areaSize = 13;
            researchCostMultiplier = 0.4f;
            consumePower(16f);
            consumeLiquid(NuLiquid.liquidOxygen, 0.2f);
        }};
        ShipAssemblyFactory = new UnitAssembler("ShipAssemblyFactory"){{
            requirements(Category.units, with(NuItems.pumice, 500, NuItems.magent,150, NuItems.alkSliver,80, NuItems.monoSiliCrystal,650));
            regionSuffix = "-dark";
            droneType = FederalUnitTypes.humorous;
            size = 5;
            plans.add(
                    new AssemblerUnitPlan(FederalUnitTypes.wanderer, 60f * 50f, PayloadStack.list(FederalUnitTypes.sailor,8, energyStorageLargeWall, 8)),
                    new AssemblerUnitPlan(FederalUnitTypes.setsails, 60f * 60f * 3f, PayloadStack.list(FederalUnitTypes.cruise,12, alkSliverLargeWall, 15))
            );
            areaSize = 13;
            researchCostMultiplier = 0.4f;
            consumePower(16f);
            consumeLiquid(NuLiquid.liquidOxygen, 0.2f);
        }};
        NuclearAssemblyParts = new Wall("NuclearAssemblyParts"){{
            size = 3;
            requirements(Category.units, with(NuItems.uranium,40,NuItems.monoSiliCrystal,100,NuItems.frailPolyester,150));
            health = 10000;
            armor = 25;
        }};
        AbsurdAssemblyParts = new Wall("AbsurdAssemblyParts"){{
            size = 3;
            requirements(Category.units, with(NuItems.sacredIron,40,NuItems.monoSiliCrystal,100,NuItems.pumice,150));
            health = 10000;
            armor = 25;
        }};
        ThalliumAssemblyParts = new Wall("ThalliumAssemblyParts"){{
            size = 3;
            requirements(Category.units, with(NuItems.thallide,40,NuItems.monoSiliCrystal,100,Items.graphite,150));
            health = 10000;
            armor = 25;
        }};
        AssemblyPlantModule = new UnitAssemblerModule("AssemblyPlantModule"){{
            requirements(Category.units, with(
                    NuItems.rubber, 300,
                    NuItems.pumice, 500,
                    NuItems.magent, 250,
                    NuItems.monoSiliCrystal, 400));
            consumePower(6f);
            regionSuffix = "-dark";
            researchCostMultiplier = 0.75f;
            size = 3;
        }};
        ParadoxUnitAssemblyFactory = new ClickSwitchAssembler("ParadoxUnitAssemblyFactory"){{
            size = 6;
        health = 100000;
        areaSize = 18;                 // 装配区（格）
        droneType = FederalUnitTypes.humorous;
        dronesCreated = 4;
        plans.add(new AssemblerUnitPlan(FederalUnitTypes.blindLoyalty,
                                60f*60f*5, PayloadStack.list(FederalUnitTypes.vanity,4,ThalliumAssemblyParts,5)));
        plans.add(new AssemblerUnitPlan(FederalUnitTypes.cowardTraitor,
                                60*60f*5, PayloadStack.list(FederalUnitTypes.loss,4,AbsurdAssemblyParts,5)));
        plans.add(new AssemblerUnitPlan(FederalUnitTypes.captain,
                    60*60f*5, PayloadStack.list(FederalUnitTypes.wanderer,4,NuclearAssemblyParts,5)));
         consumePower(40f);
         consumeLiquid(NuLiquid.prismLiquid,0.4f);
         requirements(Category.units, with(NuItems.uranium,250,
                 NuItems.monoSiliCrystal,800,
                 NuItems.rubber,300,
                 NuItems.alkSliver,240,
                 NuItems.thallide,200
         ));
        }};
        TerminalUnitAssemblyFactory = new ClickSwitchAssembler("TerminalUnitAssemblyFactory"){{
            size = 8;
            health = 1000000;
            areaSize = 25;                 // 装配区（格）
            droneType = FederalUnitTypes.humorous;
            dronesCreated = 8;
            plans.add(new AssemblerUnitPlan(FederalUnitTypes.safeguardRights,
                    60f*60f*12, PayloadStack.list(FederalUnitTypes.overPraise,6,ThalliumAssemblyParts,12)));
            plans.add(new AssemblerUnitPlan(FederalUnitTypes.desperate,
                    60*60f*12, PayloadStack.list(FederalUnitTypes.nonsense,6,AbsurdAssemblyParts,12)));
            plans.add(new AssemblerUnitPlan(FederalUnitTypes.nemo,
                    60*60f*12, PayloadStack.list(FederalUnitTypes.setsails,6,NuclearAssemblyParts,12)));
            consumePower(75f);
            consumeLiquid(NuLiquid.divineTears,0.8f);
            requirements(Category.units, with(NuItems.uranium,2500,
                    NuItems.monoSiliCrystal,2000,
                    NuItems.rubber,1000,
                    NuItems.alkSliver,1300,
                    NuItems.thallide,1500,
                    NuItems.remakeSource,1000,
                    NuItems.sacredIron,900
            ));
        }};
        bigIronUnitConveyor = new PayloadConveyor("bigIronUnitConveyor"){{
            requirements(Category.units, with(Items.graphite,45, NuItems.bigIron,25));
            canOverdrive = true;
        }};
        floatUnitConvryor = new PayloadConveyor("floatUnitConvryor"){{
            requirements(Category.units, with(Items.graphite,90, NuItems.pumice,45));
            canOverdrive = true;
            payloadLimit = 6f;
            size = 5;
        }};
        floatUnitRouter = new PayloadRouter("floatUnitRouter"){{
            requirements(Category.units, with(Items.graphite, 90,NuItems.pumice,45));
            canOverdrive = true;
            payloadLimit = 6f;
            size = 5;
        }};
        ParadoxAssemblyModule = new Wall("ParadoxAssemblyModule"){{
            size = 5;
            requirements(Category.units, with(NuItems.prismCrystal,300,NuItems.monoSiliCrystal,500,NuItems.rubber,350));
            health = 50000;
            armor = 45;
        }};
        TerminalAssemblyModule = new Wall("TerminalAssemblyModule"){{
            size = 5;
            requirements(Category.units, with(NuItems.remakeSource,450,NuItems.monoSiliCrystal,900,NuItems.sacredIron,560));
            health = 50000;
            armor = 45;
        }};
        GodForsakenParts = new Wall("GodForsakenParts"){{
            size = 3;
            requirements(Category.units, with(NuItems.remakeSource,40,NuItems.monoSiliCrystal,100,NuItems.magent,150));
            health = 10000;
            armor = 25;
        }};
        FatedParts = new Wall("FatedParts"){{
            size = 3;
            requirements(Category.units, with(NuItems.remakeSource,40,NuItems.monoSiliCrystal,100,NuItems.prismCrystal,150));
            health = 10000;
            armor = 25;
        }};
        StandaloneParts = new Wall("StandaloneParts"){{
            size = 3;
            requirements(Category.units, with(NuItems.remakeSource,40,NuItems.monoSiliCrystal,100,NuItems.thallium,150));
            health = 10000;
            armor = 25;
        }};
        CalamityParts = new Wall("CalamityParts"){{
            size = 5;
            requirements(Category.units, with(NuItems.thallide,450,NuItems.monoSiliCrystal,900,NuItems.uranium,300));
            health = 50000;
            armor = 45;
        }};
        BuildingConstructor = new Constructor("BuildingConstructor"){{
            requirements(Category.units, with(NuItems.monoSiliCrystal,120, Items.graphite,75,NuItems.pumice,120));
            regionSuffix = "-dark";
            hasPower = true;
            buildSpeed = 1.2f;
            consumePower(4f);
            size = 3;
            filter = Seq.with(
                    bigIronWall,bigIronLargeWall,energyStorageWall,energyStorageLargeWall,IllusionGate,IllusionLargeGate,
                    frailPolyesterWall,frailPolyesterLargeWall,magneticPullWall,magneticPullLargeWall,
                    floatWall,floatLargeWall,rubberWall,rubberLargeWall,alkSliverWall,alkSliverLargeWall,
                    thallideWall,thallideLargeWall,uraniumWall,uraniumLargeWall,energyShield,Lotus,ParadoxAssemblyModule,TerminalAssemblyModule,NuclearAssemblyParts,AbsurdAssemblyParts,
                    ThalliumAssemblyParts,GodForsakenParts,FatedParts,StandaloneParts,CalamityParts
            );
        }};
        SpecialUnitFactory = new ClickSwitchAssembler("SpecialUnitFactory"){{
            consumePower(15f);
            consumeLiquid(NuLiquid.strangeLiquid,2f);
            requirements(Category.units, with(NuItems.alkSliver,50,
                    NuItems.monoSiliCrystal,200,
                    NuItems.rubber,100
            ));
            size = 7;
            health = 100000;
            areaSize = 50;                 // 装配区（格）
            droneType = FederalUnitTypes.humorous;
            dronesCreated = 4;
            plans.add(new AssemblerUnitPlan(FederalUnitTypes.hometown,
                    60f*30f, PayloadStack.list(FederalUnitTypes.honor,10)));
        }};
        PaleUnitFactory = new UnitFactory("PaleUnitFactory"){{
            requirements(Category.units, with(NuItems.prismCrystal,450, NuItems.monoSiliCrystal,240,NuItems.rubber,80));
            plans = Seq.with(
                    new UnitPlan(FederalUnitTypes.pale, 60f *36, with(NuItems.monoSiliCrystal,120,NuItems.pumice,75)),
                    new UnitPlan(FederalUnitTypes.mornLight, 60f *45, with(NuItems.monoSiliCrystal,100,NuItems.prismCrystal,30)),
                    new UnitPlan(FederalUnitTypes.pureJade, 60f*32, with(NuItems.magent,60,NuItems.monoSiliCrystal,90))
            );
            size = 3;
            consumePower(7f);
            researchCostMultiplier = 0.5f;
        }};
        PaleNumberReconstruction = new Reconstructor("PaleNumberReconstruction"){{
            requirements(Category.units, with(NuItems.pumice, 200, NuItems.prismCrystal, 120, NuItems.monoSiliCrystal, 90));
            size = 3;
            consumePower(10f);
            consumeItems(with(NuItems.monoSiliCrystal,210,Items.graphite,200,NuItems.sacredIron,85));
            consumeLiquid(NuLiquid.strangeLiquid,0.2f);
            constructTime = 60f * 54f;
            upgrades.addAll(
                    new UnitType[]{FederalUnitTypes.pale, FederalUnitTypes.ripple},
                    new UnitType[]{FederalUnitTypes.mornLight, FederalUnitTypes.sunsetGlow},
                    new UnitType[]{FederalUnitTypes.pureJade, FederalUnitTypes.darkMaple}
            );
        }};
        PaleMultiplyReconstruction = new Reconstructor("PaleMultiplyReconstruction"){{
            requirements(Category.units, with(
                    NuItems.pumice,450,
                    NuItems.prismCrystal,350,
                    NuItems.monoSiliCrystal,560,
                    NuItems.rubber,350
            ));
            size = 5;
            consumePower(15f);
            consumeItems(with(NuItems.monoSiliCrystal,450,NuItems.magent,300,NuItems.sacredIron,430));
            consumeLiquid(NuLiquid.strangeLiquid,0.5f);
            constructTime = 60f * 60f * 1.5f;
            upgrades.addAll(
                    new UnitType[]{FederalUnitTypes.ripple, FederalUnitTypes.greatPath},
                    new UnitType[]{FederalUnitTypes.sunsetGlow, FederalUnitTypes.dusk},
                    new UnitType[]{FederalUnitTypes.darkMaple, FederalUnitTypes.brightCrow}
            );
        }};
        PaleExponentReconstruction = new Reconstructor("PaleExponentReconstruction"){{
            requirements(Category.units, with(
                    NuItems.sacredIron,700,
                    NuItems.prismCrystal,600,
                    NuItems.monoSiliCrystal,1200,
                    NuItems.rubber,700,
                    NuItems.thallium,560
            ));
            size = 7;
            consumePower(21f);
            consumeItems(with(NuItems.monoSiliCrystal,900,NuItems.thallium,650,NuItems.rubber,350));
            consumeLiquid(NuLiquid.liquidOxygen,0.4f);
            constructTime = 60f * 60f * 1.5f;
            upgrades.addAll(
                    new UnitType[]{FederalUnitTypes.greatPath, FederalUnitTypes.loyalRequest},
                    new UnitType[]{FederalUnitTypes.dusk, FederalUnitTypes.swallowingDay},
                    new UnitType[]{FederalUnitTypes.brightCrow, FederalUnitTypes.saint}
            );
        }};
        PaleImmeasurableReconstruction = new ClickSwitchAssembler("PaleImmeasurableReconstruction"){{
            requirements(Category.units, with(
                    NuItems.sacredIron,1400,
                    NuItems.prismCrystal,1000,
                    NuItems.monoSiliCrystal,2500,
                    NuItems.rubber,1500,
                    NuItems.thallide,900,
                    NuItems.remakeSource,600
            ));
            size = 9;
            areaSize = 20;                 // 装配区（格）
            droneType = FederalUnitTypes.humorous;
            dronesCreated = 4;
            consumePower(27f);
            consumeLiquid(NuLiquid.divineTears,0.5f);
            plans.add(new AssemblerUnitPlan(FederalUnitTypes.paladin,
                    60f*60f*12, PayloadStack.list(FederalUnitTypes.greatPath,10,StandaloneParts,12)));
            plans.add(new AssemblerUnitPlan(FederalUnitTypes.moonLight,
                    60*60f*12, PayloadStack.list(FederalUnitTypes.dusk,10,FatedParts,12)));
            plans.add(new AssemblerUnitPlan(FederalUnitTypes.bloodLotus,
                    60*60f*12, PayloadStack.list(FederalUnitTypes.brightCrow,10,GodForsakenParts,12,Lotus,10)));
        }};


        multiTurret = new MultiPowerTurret("multiTurret"){{
        requirements(Category.turret, with(
            NuItems.bigIron, 120,
            NuItems.monoSiliCrystal, 80,
            Items.graphite, 60
                           ));
        size = 2;
        health = 2000;
        range = 160f;
        reload = 60f;
        consumePower(6f);
        // —— 注册多种攻击模式 ——
        modes = Seq.with(
                               new Mode("standard", new BasicBulletType(3f, 80f){{
                lifetime = 60f; width = 8f; height = 16f;
            }}, NuItems.bigIron),
            new Mode("laser", new LaserBulletType(){{
                damage = 200f; length = 200f; lifetime = 24f;
            }}, Items.graphite),
            new Mode("heal", new SweepBulletType(0f){{
                heal = 2f; scanRadius = 120f; fieldAngle = 90f;
            }}, NuItems.thallide)
                           );
    }};
        awnlessSpike = new PowerTurret("awnlessSpike"){{
           size = 2;
           health = 2000;
           armor = 22;
           shake = 1f;
           reload = 1200f;
           range = 160f;
           consumePower(5f);
            requirements(Category.turret, with(
                    NuItems.bigIron, 120,
                    NuItems.monoSiliCrystal, 80,
                    Items.graphite, 60
            ));
            shoot = new ShootPattern(){{
               shots = 12;shotDelay = 180f;
            }};
            for (int i = 0; i < shoot.shots; i++){{
               shoot.shotDelay = shoot.shotDelay-10f;
            shootType = new BasicBulletType(5f, 60f){{
                width = 20f;
                height = 20f;
           lifetime = 60f;
           pierce = true;
           pierceCap = 3;
           // — PointBulletType 拖尾参数（参考 NuBlocks / FederalUnitTypes 中其它 Point 弹的写法）
           trailInterval = 12f;              // 每 2 tick 触发一次
           trailEffect   = NuFx.DespSmokeTail;   // 现成能量烟尾，避免 Fx.trailPoint 未定义
           hitEffect = Fx.flakExplosion;
           shootEffect = Fx.shootBig;
                }};
            }};
        }};
        DefeatGod = new ItemTurret("DefeatGod"){{
            size = 1;
            health = 1000;
            armor = 12;
            shake = 2f;
            reload = 90f;
            ammoPerShot = 1;
            maxAmmo = 20;
            shootCone = 5f;
            rotateSpeed = 9f;
            range = 160f;
            targetGround = true;
            targetAir =true;
            requirements(Category.turret,with(NuItems.bigIron,25));
            shoot = new ShootBarrel(){{
                barrels = new float[]{
                        4f, 0f, 0f,
                        -4f,0f, 0f
                };
                shots = 12;
                shotDelay = 4.5f;
            }};
            researchCost = with(NuItems.bigIron,120);
            ammo(
              NuItems.bigIron,new BasicBulletType(5f,76f){{
                  frontColor = lightColor = trailColor = NuColor.PaleColor;
                  backColor = hitColor = NuItems.bigIron.color;
                  lifetime = 32f;
                  trailLength = 4;
                  trailWidth = 3f;
                  width = 18f;
                  height = 24f;
                  despawnEffect = hitEffect = new WaveEffect(){{
                      sizeFrom = 16f; sizeTo = 4f;
                      strokeFrom = 0.5f; strokeTo = 4f;   // 越收越粗
                      interp = Interp.reverse;
                      sides = 3; rotation = 15f;        //
                      lifetime = 12f;
                      colorFrom = NuColor.PaleColor;
                      colorTo = NuItems.bigIron.color;
                  }};
                    }},
             Items.graphite,new BasicBulletType(5f,130f){{
                        frontColor = lightColor = trailColor = Color.white;
                        backColor = hitColor = Items.graphite.color;
                        lifetime = 32f;
                        trailLength = 4;
                        trailWidth = 3f;
                        width = 18f;
                        height = 24f;
                        reloadMultiplier = 1.3f;
                        despawnEffect = hitEffect = new WaveEffect(){{
                            sizeFrom = 16f; sizeTo = 4f;
                            strokeFrom = 0.5f; strokeTo = 4f;   // 越收越粗
                            interp = Interp.reverse;
                            sides = 3; rotation = 15f;        //
                            lifetime = 16f;
                            colorFrom = Color.white;
                            colorTo = Items.graphite.color;
                        }};
                    }},
            NuItems.monoSiliCrystal,new BasicBulletType(5f,90f){{
                        frontColor = lightColor = trailColor = NuItems.monoSiliCrystal.color;
                        backColor = hitColor = Items.silicon.color;
                        lifetime = 40f;
                        trailLength = 6;
                        trailWidth = 3f;
                        width = 18f;
                        height = 24f;
                        reloadMultiplier = 0.75f;
                        homingPower = 1.5f;
                        homingRange = 100f;
                        rangeChange = 40f;
                        despawnEffect = hitEffect = new WaveEffect(){{
                            sizeFrom = 16f; sizeTo = 4f;
                            strokeFrom = 0.5f; strokeTo = 4f;   // 越收越粗
                            interp = Interp.reverse;
                            sides = 3; rotation = 15f;        //
                            lifetime = 16f;
                            colorFrom = Items.silicon.color;
                            colorTo = NuItems.monoSiliCrystal.color;
                        }};
                    }},
                    NuItems.magent,new BasicBulletType(5f,230f){{
                        frontColor = lightColor = trailColor = NuItems.magent.color;
                        backColor = hitColor = Color.white;
                        lifetime = 48f;
                        trailLength = 12;
                        trailWidth = 3f;
                        width = 18f;
                        height = 24f;
                        reloadMultiplier = 1.5f;
                        rangeChange = 80f;
                        knockback = 0.5f;
                        despawnEffect = hitEffect = new WaveEffect(){{
                            sizeFrom = 24f; sizeTo = 8f;
                            strokeFrom = 1f; strokeTo = 5.5f;   // 越收越粗
                            interp = Interp.reverse;
                            sides = 3; rotation = 60f;        //
                            lifetime = 25f;
                            colorFrom = Color.white;
                            colorTo = NuItems.magent.color;
                        }};
                        fragBullets = 3;
                        fragBullet = new ExplosionBulletType(45f,16f){{
                            killShooter = false;
                        }};
                    }}
            );
        }};
        StandingGround = new ItemTurret("StandingGround"){{
            size = 1;
            health = 1000;
            armor = 12;
            shake = 2.5f;
            reload = 60f;
            ammoPerShot = 1;
            maxAmmo = 15;
            shootCone = 5f;
            rotateSpeed = 9f;
            range = 200f;
            requirements(Category.turret,with(NuItems.bigIron,45,Items.graphite,25));
            targetAir = true;
            targetGround = false;
            shoot = new ShootPattern(){{
               shots = 12;
               shotDelay = 2f;
            }};
            researchCost = with(NuItems.bigIron,500,Items.graphite,150);
            ammo(
                    Items.sand,new FlakBulletType(5f,25f){{
                        lifetime = 40f;
                        knockback = 0.25f;
                        reloadMultiplier = 1.55f;
                        width = 16f;
                        height = 24f;
                        splashDamage = 10f;
                        splashDamageRadius = 24f;
                        trailLength = 5;
                        frontColor = lightColor = trailColor = Color.white;
                        backColor = lightColor = Items.sand.color;
                        despawnEffect = hitEffect = new MultiEffect(new ParticleEffect(){{
                            line = true;
                            strokeFrom = 1f; strokeTo = 3.5f;
                            lenFrom = 3f; lenTo = 16f;
                            cone = 90f;
                            colorFrom = Items.sand.color;
                            colorTo = Color.white;
                        }},new ParticleEffect(){{
                            strokeFrom = 1f; strokeTo = 3.5f;
                            sizeFrom = 2.5f; sizeTo = 6.5f;
                            interp = Interp.pow5Out;            // 先飞爆冲出去
                            cone = 360f;                          // 60° 左右散开
                            length = 40f;
                            sizeInterp = Interp.slope;           // 到中间再缩回去
                            particles = 3;
                            colorFrom = Color.white;
                            colorTo = Items.sand.color;
                        }});
                        trailLength = 5;
                    }},
                    NuItems.frailPolyester,new FlakBulletType(5f,48f){{
                        lifetime = 40f;
                        knockback = 0.75f;
                        width = 20f;
                        height = 28f;
                        splashDamage = 21f;
                        splashDamageRadius = 16f;
                        status = StatusEffects.burning;
                        statusDuration = 60*5.5f;
                        frontColor = lightColor = trailColor = Color.white;
                        backColor = lightColor = NuItems.frailPolyester.color;
                        trailLength = 6;
                        despawnEffect = hitEffect = new MultiEffect(new ParticleEffect(){{
                            line = true;
                            strokeFrom = 1f; strokeTo = 3.5f;
                            lenFrom = 3f; lenTo = 16f;
                            cone = 90f;
                            colorFrom = NuItems.frailPolyester.color;
                            colorTo = NuColor.SurvivalColor;
                        }},new ParticleEffect(){{
                            strokeFrom = 1f; strokeTo = 3.5f;
                            sizeFrom = 2.5f; sizeTo = 6.5f;
                            interp = Interp.pow5Out;            // 先飞爆冲出去
                            cone = 360f;                          // 60° 左右散开
                            length = 40f;
                            sizeInterp = Interp.slope;           // 到中间再缩回去
                            particles = 5;
                            colorFrom = NuItems.frailPolyester.color;
                            colorTo = NuColor.SurvivalColor;
                        }});
                    }},
                    NuItems.monoSiliCrystal,new FlakBulletType(5f,56f){{
                        width = 18f;
                        height = 25f;
                        lifetime = 40f;
                        knockback = 1.5f;
                        reloadMultiplier = 0.75f;
                        splashDamage = 25f;
                        splashDamageRadius = 30f;
                        homingPower = 0.6f;
                        homingRange = 50f;
                        frontColor = lightColor = trailColor = Items.silicon.color;
                        backColor = lightColor = NuItems.monoSiliCrystal.color;
                        despawnEffect = hitEffect = new MultiEffect(new ParticleEffect(){{
                            line = true;
                            strokeFrom = 1f; strokeTo = 3.5f;
                            lenFrom = 3f; lenTo = 8f;
                            lifetime = 12f;
                            cone = 90f;
                            colorFrom = NuItems.monoSiliCrystal.color;
                            colorTo = Items.silicon.color;
                        }}, new ParticleEffect(){{
                            strokeFrom = 1f; strokeTo = 3.5f;
                            sizeFrom = 2.5f; sizeTo = 6.5f;
                            interp = Interp.pow5Out;            // 先飞爆冲出去
                            cone = 360f;                          // 60° 左右散开
                            length = 40f;
                            sizeInterp = Interp.slope;           // 到中间再缩回去
                            particles = 5;
                            colorFrom = Items.silicon.color;
                            colorTo = NuItems.monoSiliCrystal.color;
                        }});
                        trailLength = 8;
                        fragBullets = 2;
                        fragRandomSpread = 20f;
                        fragBullet = new FlakBulletType(1f,75f){{
                            width = 16f;
                            height = 16f;
                            lifetime = 1f;
                            splashDamage = 16f;
                            splashDamageRadius = 16f;
                            homingPower = 0.1f;
                            homingRange = 50f;
                            frontColor = lightColor = trailColor = Items.silicon.color;
                            backColor = lightColor = NuItems.monoSiliCrystal.color;
                            despawnEffect = hitEffect = new WaveEffect(){{
                                sizeTo = 32f;
                                strokeFrom = 4f; strokeTo = 1.5f;
                                interp = Interp.pow2Out;
                                lightColor = NuColor.PaleColor;
                                lightInterp = Interp.reverse;   // 开始最亮，随半径扩大熄灭（最符合冲击波）
                                colorFrom = NuItems.monoSiliCrystal.color;
                                colorTo = Items.silicon.color;
                            }};
                        }};
                    }}
            );
        }};
        Wanuo = new ItemTurret("Wanuo"){{
            size = 1;
            health = 1000;
            armor = 12;
            shake = 2.5f;
            reload = 40f;
            ammoPerShot = 2;
            maxAmmo = 30;
            shootCone = 5f;
            rotateSpeed = 9f;
            range = 120f;
            requirements(Category.turret,with(NuItems.bigIron,150,NuItems.monoSiliCrystal,50));
            targetAir = false;
            targetGround = true;
            shoot = new ShootMulti(
                   new ShootSpread(8, 2.5f),new ShootPattern(){{
                       shots = 2;
                       shotDelay = 15f;
            }});
            ammo(
                    NuItems.bigIron,new BasicBulletType(8f,56f){{
                        width = 8f;
                        height = 24f;
                        lifetime = 15f;
                        frontColor = lightColor = trailColor = NuColor.PaleColor;
                        backColor = hitColor = NuItems.bigIron.color;
                        trailLength = 5;
                        trailInterval = 3f;
                        trailEffect = new  ParticleEffect(){{
                            particles = 2;
                            sizeFrom = 5f;
                            sizeTo = 1f;
                            lifetime = 15f;
                            colorFrom = NuItems.bigIron.color;
                            colorTo = NuColor.PaleColor;
                            sizeInterp = Interp.slope;
                        }};
                        despawnEffect = hitEffect = Fx.flakExplosionBig;
                    }},
                    Items.graphite,new BasicBulletType(8f,100f){{
                        width = 8f;
                        height = 24f;
                        lifetime = 15f;
                        frontColor = lightColor = trailColor = Color.white;
                        backColor = hitColor = Items.graphite.color;
                        trailLength = 5;
                        trailInterval = 3f;
                        trailEffect = new  ParticleEffect(){{
                            particles = 6;
                            sizeFrom = 5f;
                            sizeTo = 1f;
                            lifetime = 15f;
                            colorFrom = Items.graphite.color;
                            colorTo = NuColor.PaleColor;
                            sizeInterp = Interp.slope;
                        }};
                        despawnEffect = hitEffect = Fx.flakExplosionBig;
                    }},
                    NuItems.monoSiliCrystal,new BasicBulletType(8f,78f){{
                        width = 8f;
                        height = 24f;
                        lifetime = 15f;
                        homingPower = 0.6f;
                        homingRange = 60f;
                        homingDelay = 3f;
                        frontColor = lightColor = trailColor = Items.silicon.color;
                        backColor = hitColor = NuItems.monoSiliCrystal.color;
                        trailLength = 5;
                        trailInterval = 3f;
                        trailEffect = new  ParticleEffect(){{
                            particles = 6;
                            sizeFrom = 5f;
                            sizeTo = 1f;
                            lifetime = 15f;
                            colorFrom = NuItems.monoSiliCrystal.color;
                            colorTo = NuColor.PaleColor;
                            sizeInterp = Interp.slope;
                        }};
                        despawnEffect = hitEffect = Fx.flakExplosionBig;
                    }},
                    NuItems.magent,new BasicBulletType(8f,190f){{
                        width = 8f;
                        height = 24f;
                        lifetime = 15f;
                        knockback = -1f;
                        frontColor = lightColor = trailColor = Color.white;
                        backColor = hitColor = NuItems.magent.color;
                        trailLength = 5;
                        trailInterval = 3f;
                        trailEffect = new  ParticleEffect(){{
                            particles = 6;
                            sizeFrom = 5f;
                            sizeTo = 1f;
                            lifetime = 15f;
                            colorFrom = NuItems.magent.color;
                            colorTo = NuColor.PaleColor;
                            sizeInterp = Interp.slope;
                        }};
                        despawnEffect = hitEffect = Fx.flakExplosionBig;
                    }},
                    NuItems.sulFurFrag,new BasicBulletType(8f,86f){{
                        width = 8f;
                        height = 24f;
                        lifetime = 15f;
                        reloadMultiplier = 1.2f;
                        status = StatusEffects.burning;
                        statusDuration = reload*reloadMultiplier*10f;
                        frontColor = lightColor = trailColor = Items.sand.color;
                        backColor = hitColor = NuItems.sulFurFrag.color;
                        trailLength = 5;
                        trailInterval = 3f;
                        trailEffect = new  ParticleEffect(){{
                            particles = 6;
                            sizeFrom = 5f;
                            sizeTo = 1f;
                            lifetime = 15f;
                            colorFrom = Items.sand.color;
                            colorTo = NuItems.sulFurFrag.color;
                            sizeInterp = Interp.slope;
                        }};
                        despawnEffect = hitEffect = Fx.flakExplosionBig;
                    }}
            );
        }};
        Incinerate = new ItemTurret("Incinerate"){{
            requirements(Category.turret, with(NuItems.bigIron,50,NuItems.monoSiliCrystal,50,Items.graphite,40));
            size = 1;
            health = 1000;
            armor = 12;
            shake = 2.5f;
            reload = 10f;
            ammoPerShot = 2;
            maxAmmo = 30;
            shootCone = 5f;
            rotateSpeed = 9f;
            range = 120f;
            targetAir = false;
            targetGround = true;
            ammo(
              NuItems.Tcoal,new FireBulletType(5f,98f){{
                  colorFrom = NuItems.Tcoal.color;
                  colorMid = NuItems.monoSiliCrystal.color;
                  colorTo = Items.coal.color;
                  fireTrailChance = 0f;
                  radius = 5f;velMin=speed-0.5f;velMax =speed+1f;
                  lifetime = 2f;
                  drag = 0.01f;
                  fragBullets = 15;
                  fragRandomSpread = 5f;
                  fragBullet = new FireBulletType(5f,35f){{
                      colorFrom = NuItems.Tcoal.color;
                      colorMid = NuItems.monoSiliCrystal.color;
                      colorTo = Items.coal.color;
                      fireTrailChance = 0.2f;
                      radius = 5f;velMin = speed-0.5f;velMax = speed+1f;
                      lifetime = 18f;
                      drag = 0.01f;
                      collidesTiles = true;
                      collides = true;
                  }};
                }},
                    NuItems.sulFurFrag,new FireBulletType(5f,140f){{
                        colorFrom = NuItems.Tcoal.color;
                        colorMid = NuItems.monoSiliCrystal.color;
                        colorTo = Items.coal.color;
                        radius = 10f;velMin = 4.5f;velMax = 6f;
                        fireTrailChance = 0f;
                        lifetime = 5f;
                        drag = 0.01f;
                        fragBullets = 15;
                        fragRandomSpread = 30f;
                        fragBullet = new FireBulletType(5f,45f){{
                            colorFrom = NuItems.Tcoal.color;
                            colorMid = NuItems.monoSiliCrystal.color;
                            colorTo = Items.coal.color;
                            fireTrailChance = 0.5f;
                            radius = 5f;velMin = 4.5f;velMax = 6f;
                            lifetime = 20f;
                            drag = 0.01f;
                            collidesTiles = true;
                            collides = true;
                        }};
                    }},
                    NuItems.frailPolyester,new FireBulletType(5f,125f){{
                        colorFrom = NuItems.Tcoal.color;
                        colorMid = NuItems.monoSiliCrystal.color;
                        colorTo = Items.coal.color;
                        fireTrailChance = 0f;
                        radius = 10f;velMin = 4.5f;velMax = 6f;
                        lifetime = 5f;
                        drag = 0f;
                        reloadMultiplier = 1.5f;
                        fragBullets = 10;
                        fragRandomSpread = 20f;
                        fragBullet = new FireBulletType(5f,50f){{
                            colorFrom = NuItems.Tcoal.color;
                            colorMid = NuItems.monoSiliCrystal.color;
                            colorTo = Items.coal.color;
                            fireTrailChance = 0.6f;
                            radius = 5f;velMin = 4.5f;velMax = 6f;
                            collidesTiles = true;
                            collides = true;
                            lifetime = 20f;
                            drag = 0.01f;
                        }};
                    }},
                    NuItems.rubber,new FireBulletType(5f,350f){{
                        colorFrom = NuItems.Tcoal.color;
                        colorMid = NuItems.monoSiliCrystal.color;
                        colorTo = Items.coal.color;
                        fireTrailChance = 0f;
                        radius = 10f;velMin = 4.5f;velMax = 6f;
                        lifetime = 5f;
                        drag = 0f;
                        fragBullets = 25;
                        reloadMultiplier = 0.6f;
                        fragRandomSpread = 50f;
                        fragBullet = new FireBulletType(5f,80f){{
                            colorFrom = NuItems.Tcoal.color;
                            colorMid = NuItems.monoSiliCrystal.color;
                            colorTo = Items.coal.color;
                            fireTrailChance = 0.9f;
                            radius = 5f;velMin = 4.5f;velMax = 6f;
                            lifetime = 22f;
                            collidesTiles = true;
                            collides = true;
                            drag = 0.01f;
                        }};
                    }}
            );
        }};
        Joy = new LiquidTurret("Joy"){{
            requirements(Category.turret, with(NuItems.bigIron,50,NuItems.frailPolyester,80,Items.graphite,10));
            size = 1;
            health = 1000;
            armor = 12;
            shake = 0.15f;
            rotateSpeed = 9f;
            range = 200f;
            targetAir = true;
            targetGround = true;
            shootCone = 50f;
            liquidCapacity = 10f;
            shootEffect = Fx.shootLiquid;
            flags = EnumSet.of(BlockFlag.turret, BlockFlag.extinguisher);
            reload = 16f;
            shoot.shotDelay = 1f;
            shoot.shots = 10;
            ammo(
                    Liquids.water,new LiquidBulletType(Liquids.water){{
                        knockback = 0.7f;
                        layer = Layer.bullet - 2f;
                        speed = 5f;
                        lifetime = 40f;
                        damage = 1.5f;
                    }},
                    NuLiquid.strangeLiquid,new LiquidBulletType(NuLiquid.strangeLiquid){{
                        layer = Layer.bullet - 2f;
                        speed = 5f;
                        lifetime = 40f;
                        damage = 5f;
                    }},
                    NuLiquid.liquidOxygen,new LiquidBulletType(NuLiquid.liquidOxygen){{
                        layer = Layer.bullet - 2f;
                        speed = 5f;
                        lifetime = 40f;
                        damage = 6f;
                    }},
                    NuLiquid.dirtySolution,new LiquidBulletType(NuLiquid.dirtySolution){{
                        layer = Layer.bullet - 2f;
                        speed = 5f;
                        lifetime = 40f;
                        damage = 12f;
                    }},
                    NuLiquid.nuclearFluid,new LiquidBulletType(NuLiquid.nuclearFluid){{
                        layer = Layer.bullet - 2f;
                        speed = 5f;
                        lifetime = 40f;
                        damage = 15f;
                        status = NuStatus.radiation;
                        statusDuration = 60f*speed;
                    }},
                    NuLiquid.prismLiquid,new LiquidBulletType(NuLiquid.prismLiquid){{
                        layer = Layer.bullet - 2f;
                        speed = 8f;
                        lifetime = 25f;
                        damage = 10f;
                    }},
                    NuLiquid.divineTears,new LiquidBulletType(NuLiquid.divineTears){{
                        layer = Layer.bullet - 2f;
                        speed = 8f;
                        lifetime = 25f;
                        damage = 45f;
                        status = NuStatus.divineWrath;
                        statusDuration = 60f*speed*4.5f;
                    }}
            );
        }};
        TraceSource = new ItemTurret("TraceSource"){{
            requirements(Category.turret, with(NuItems.bigIron,50,NuItems.frailPolyester,80,Items.graphite,10));
            targetAir = false;
            size = 2;
            shoot.shots = 4;
            inaccuracy = 11f;
            reload = 40f;
            ammoEjectBack = 5f;
            ammoUseEffect = Fx.casing3Double;
            ammoPerShot = 2;
            maxAmmo = 40;
            velocityRnd = 0.2f;
            scaleLifetimeOffset = 1f / 9f;
            recoil = 6f;
            shake = 2f;
            range = 290f;
            minRange = 50f;
            coolant = consumeCoolant(0.3f);
            health = 2000;
            depositCooldown = 2.0f;
            shootSound = Sounds.shootRipple;
            ammo(
                    NuItems.monoSiliCrystal,new ArtilleryBulletType(4.5f,120f){{
                                width = 10f;
                                height = 13f;
                                lifetime = 63f;
                                splashDamage = 56f;
                                splashDamageRadius = 16f;
                                drag = 0f;
                                homingPower = 0.65f;
                                homingDelay = 5f;
                                homingRange = 120f;
                                collidesTiles = true;
                                collides = true;
                                collidesAir = false;
                                scaleLife = true;
                                lifeScaleRandMax = 1.08f;
                                lifeScaleRandMin = 0.95f;
                                frontColor = lightColor = trailColor = NuItems.monoSiliCrystal.color;
                                backColor = hitColor = Items.silicon.color;
                                trailLength = 5;
                                trailInterval = 6f;
                                trailEffect = new WrapEffect() {{
                                    effect = Fx.artilleryTrail;
                                    color = NuItems.monoSiliCrystal.color;
                                    rotation = 0f;
                                }};
                                despawnEffect = hitEffect = new ExplosionEffect() {{
                                    waveColor = Color.gray;
                                    sparkColor = Items.silicon.color;
                                    smokeColor = NuItems.monoSiliCrystal.color;
                                    waveRad = 12f;       // 改爆炸大小
                                    waveRadBase = 2f;
                                    waveLife = 10f;
                                    smokeRad = 24f;
                                    smokeSize = 6f;
                                    sparkRad = 18f;
                                    sparkStroke = 2f;
                                    sparkLen = 5f;
                                    smokes = 7;          // 更浓密
                                    sparks = 6;
                                }};
                    }},
                    NuItems.graphite,new ArtilleryBulletType(4.5f,135f){{
                        width = 10f;
                        height = 13f;
                        lifetime = 63f;
                        splashDamage = 100f;
                        splashDamageRadius = 25f;
                        drag = 0f;
                        collidesTiles = true;
                        collides = true;
                        collidesAir = false;
                        scaleLife = true;
                        lifeScaleRandMax = 1.28f;
                        lifeScaleRandMin = 1.01f;
                        frontColor = lightColor = trailColor = Color.white;
                        backColor = hitColor = NuItems.graphite.color;
                        trailLength = 8;
                        trailInterval = 6f;
                        trailEffect = new WrapEffect() {{
                            effect = Fx.artilleryTrail;
                            color = NuItems.graphite.color;
                            rotation = 0f;
                        }};
                        despawnEffect = hitEffect = new ExplosionEffect() {{
                            waveColor = Color.gray;
                            sparkColor = Color.white;
                            smokeColor = NuItems.graphite.color;
                            waveRad = 12f;       // 改爆炸大小
                            waveRadBase = 2f;
                            waveLife = 10f;
                            smokeRad = 24f;
                            smokeSize = 6f;
                            sparkRad = 18f;
                            sparkStroke = 2f;
                            sparkLen = 5f;
                            smokes = 7;          // 更浓密
                            sparks = 6;
                        }};
                    }},
                    NuItems.magent,new ArtilleryBulletType(4.5f,260f){{
                        width = 10f;
                        height = 13f;
                        lifetime = 63f;
                        reloadMultiplier = 1.2f;
                        splashDamage = 178f;
                        splashDamageRadius = 10f;
                        drag = 0f;
                        collidesTiles = true;
                        collides = true;
                        collidesAir = false;
                        scaleLife = true;
                        lifeScaleRandMax = 1.28f;
                        lifeScaleRandMin = 1.01f;
                        frontColor = lightColor = trailColor = Color.white;
                        backColor = hitColor = NuItems.magent.color;
                        trailLength = 8;
                        trailInterval = 6f;
                        trailEffect = new WrapEffect() {{
                            effect = Fx.artilleryTrail;
                            color = NuItems.magent.color;
                            rotation = 0f;
                        }};
                        despawnEffect = hitEffect = new ExplosionEffect() {{
                            waveColor = Color.gray;
                            sparkColor = Color.white;
                            smokeColor = NuItems.magent.color;
                            waveRad = 12f;       // 改爆炸大小
                            waveRadBase = 2f;
                            waveLife = 10f;
                            smokeRad = 24f;
                            smokeSize = 6f;
                            sparkRad = 18f;
                            sparkStroke = 2f;
                            sparkLen = 5f;
                            smokes = 7;          // 更浓密
                            sparks = 6;
                        }};
                    }},
                    NuItems.pumice,new ArtilleryBulletType(4.5f,325f){{
                        width = 10f;
                        height = 13f;
                        lifetime = 63f;
                        reloadMultiplier = 0.75f;
                        splashDamage = 236f;
                        splashDamageRadius = 24f;
                        drag = 0f;
                        collidesTiles = true;
                        collides = true;
                        collidesAir = false;
                        scaleLife = true;
                        lifeScaleRandMax = 1.28f;
                        lifeScaleRandMin = 1.01f;
                        frontColor = lightColor = trailColor = NuItems.bigIron.color;
                        backColor = hitColor = NuItems.pumice.color;
                        trailLength = 8;
                        trailInterval = 6f;
                        trailEffect = new WrapEffect() {{
                            effect = Fx.artilleryTrail;
                            color = NuItems.pumice.color;
                            rotation = 0f;
                        }};
                        despawnEffect = hitEffect = new ExplosionEffect() {{
                            waveColor = NuItems.monoSiliCrystal.color;
                            sparkColor = NuItems.bigIron.color;
                            smokeColor = NuItems.pumice.color;
                            waveRad = 18f;       // 改爆炸大小
                            waveRadBase = 5f;
                            waveLife = 15f;
                            smokeRad = 30f;
                            smokeSize = 12f;
                            sparkRad = 24f;
                            sparkStroke = 3f;
                            sparkLen = 8f;
                            smokes = 7;          // 更浓密
                            sparks = 6;
                        }};
                    }}
            );
        }};
        Kurao = new PowerTurret("Kurao"){{
            requirements(Category.turret, with(
                    NuItems.bigIron,150,
                    NuItems.monoSiliCrystal,100,
                    Items.graphite,80,
                    NuItems.magent,50
            ));
            range = 200f;
            shoot.firstShotDelay = 40f;
            recoil = 2f;
            reload = 60f;
            shake = 2f;
            shootEffect = new WrapEffect(){{
                  effect = Fx.lancerLaserShoot;
                  color = NuColor.SailColor;
            }};
            smokeEffect = Fx.none;
            heatColor = Color.red;
            size = 2;
            health = 2000;
            targetAir = false;
            moveWhileCharging = false;
            accurateDelay = false;
            shootSound = Sounds.shootLancer;
            coolant = consumeCoolant(0.2f);
            chargeSound = Sounds.chargeLancer;
            consumePower(8f);
            shootType = new LaserBulletType(){{
                damage = 345f;
                colors = new Color[]{NuColor.SailColor,NuColor.SailBackColor, Color.white};
                chargeEffect = new MultiEffect(
                        new WrapEffect(){{
                        effect = Fx.lancerLaserCharge;
                        color = NuColor.SailColor;
                        rotation = 0f;
                        }},new WrapEffect(){{
                        effect = Fx.lancerLaserChargeBegin;
                        color = NuColor.SailBackColor;
                        }});
                buildingDamageMultiplier = 0.6f;
                armorMultiplier = 5.5f;
                hitEffect = new WrapEffect(){{
                    effect = Fx.hitLancer;
                    color = NuColor.SailColor;
                }};
                hitSize = 4;
                lifetime = 24f;
                drawSize = 400f;
                reloadMultiplier = 1.5f;
                collidesAir = false;
                length = 210f;
                pierceCap = 4;
            }};
        }};
        MPI = new PowerTurret("MPI"){{
            requirements(Category.turret, with(
                    NuItems.bigIron,150,
                    NuItems.monoSiliCrystal,100,
                    Items.graphite,80,
                    NuItems.magent,50
            ));
            range = 160f;
            recoil = 2f;
            reload = 120f;
            shake = 2f;
            shootEffect = new WrapEffect(){{
                   effect = Fx.lancerLaserShoot;
                   color = NuColor.EnergyColor;
                }};
            smokeEffect = Fx.none;
            heatColor = Color.red;
            size = 2;
            health = 2000;
            targetAir = true;
            moveWhileCharging = false;
            accurateDelay = false;
            shootSound = Sounds.shootLancer;
            coolant = consumeCoolant(0.2f);
            chargeSound = Sounds.chargeLancer;
            consumePower(8f);
            shoot = new ShootAlternate(){{
                shots = 9;
                shotDelay =10f;
                spread = 3f;
                barrels =3;
            }};
            shootType = new SpeedDamageBulletType(2f,80f){{
                damageIncrease = 1.5f;
                accel = 0.15f;
                lifetime = 40f;
                width = 6f;
                height = 21f;
                trailLength = 6;
                trailWidth = 1.6f;
                pierce = true;
                pierceCap = 10;
                frontColor = lightColor = trailColor = NuColor.EnergyLiColor;
                backColor = hitColor = NuColor.EnergyBackColor;
                despawnEffect = hitEffect = new WaveEffect(){{
                    sizeFrom = 32f; sizeTo = 16f;
                    strokeFrom = 0.1f; strokeTo = 4.5f;   // 越收越粗
                    interp = Interp.reverse;
                    sides = 4; rotation = 45f;        //
                    lifetime = 25f;
                    colorFrom = Color.white;
                    colorTo = NuColor.EnergyColor;
                }};
                intervalBullets = 2;
                bulletInterval = 2f;
                intervalRandomSpread = 180f;
                intervalBullet = new LightningBulletType(){{
                    damage = 24f;
                    lifetime = 16f;
                    status = StatusEffects.shocked;
                    statusDuration = 60f*5f;
                    lightningLength = 6;
                    lightningLengthRand =5;
                    lightningColor = NuColor.EnergyLiColor;
                }};
                fragBullets = 3;
                fragRandomSpread = 0f;
                fragBullet = new ExplosionBulletType(0f,78f){{
                    killShooter = false;
                    despawnEffect = hitEffect = Fx.none;
                }};
            }};
        }};
        NonSeen = new ItemTurret("NonSeen"){{
            requirements(Category.turret, with(
                    NuItems.graphite,150,
                    NuItems.monoSiliCrystal,150,
                    NuItems.magent,40));
            size = 2;
            health = 2000;
            armor = 20;
            shake = 2.8f;
            reload = 35f;
            ammoPerShot = 3;
            maxAmmo = 45;
            shootCone = 80f;
            rotateSpeed = 12f;
            range = 220f;
            targetAir = true;
            targetGround = true;
            ammo(
                    NuItems.magent,new MissileBulletType(6f,250f){{
                        width = 10f;
                        height = 32f;
                        lifetime =17.5f;
                        homingPower = 0.5f;
                        weaveScale = 12f;
                        frontColor = lightColor = trailColor = NuItems.magent.color;
                        backColor = hitColor = Color.white;
                        trailLength = 8;
                        fragBullets = 20;
                        fragVelocityMin = 0.8f;
                        fragVelocityMax = 1.25f;
                        fragLifeMin = 0.5f;
                        fragBullet = new BasicBulletType(3f,75f){{
                            width = 10f;
                            height = 18f;
                            lifetime = 28f;
                            pierce = true;
                            pierceCap = 6;
                            pierceBuilding = true;
                            frontColor = lightColor = trailColor = NuItems.magent.color;
                            backColor = hitColor = Color.white;
                            trailWidth = 2.5f;
                            trailLength = 6;
                            hitEffect = despawnEffect = new WaveEffect(){{
                               colorFrom = NuItems.magent.color;
                               colorTo = Color.white;
                               sizeFrom = 4f;sizeTo = 2f;
                               strokeFrom = 1f;strokeTo = 4.5f;
                               lifetime = 16f;
                            }};
                            homingPower = 0.1f;
                            buildingDamageMultiplier = 0.5f;
                        }};
                        bulletInterval = 2f;
                        intervalRandomSpread = 20f;
                        intervalBullets = 2;
                        intervalAngle = 180f;
                        intervalSpread = 300f;
                        intervalBullet = new BasicBulletType(3f,75f){{
                            width = 10f;
                            height = 18f;
                            lifetime = 28f;
                            pierce = true;
                            pierceCap = 6;
                            pierceBuilding = true;
                            frontColor = lightColor = trailColor = NuItems.magent.color;
                            backColor = hitColor = Color.white;
                            trailWidth = 2.5f;
                            trailLength = 6;
                            hitEffect = despawnEffect = new WaveEffect(){{
                                colorFrom = NuItems.magent.color;
                                colorTo = Color.white;
                                sizeFrom = 4f;sizeTo = 2f;
                                strokeFrom = 1f;strokeTo = 4.5f;
                                lifetime = 16f;
                            }};
                            homingPower = 0.1f;
                            buildingDamageMultiplier = 0.5f;
                        }};
                        fragBullets = 1;
                        fragRandomSpread = 0f;
                        fragBullet = new MissileBulletType(6f,250f){{
                            width = 10f;
                            height = 32f;
                            lifetime =17.5f;
                            homingPower = 0.5f;
                            weaveScale = 12f;
                            frontColor = lightColor = trailColor = NuItems.magent.color;
                            backColor = hitColor = Color.white;
                            trailLength = 8;
                            fragBullets = 20;
                            fragVelocityMin = 0.8f;
                            fragVelocityMax = 1.25f;
                            fragLifeMin = 0.5f;
                            fragBullet = new BasicBulletType(3f,75f){{
                                width = 10f;
                                height = 18f;
                                lifetime = 28f;
                                pierce = true;
                                pierceCap = 6;
                                pierceBuilding = true;
                                frontColor = lightColor = trailColor = NuItems.magent.color;
                                backColor = hitColor = Color.white;
                                trailWidth = 2.5f;
                                trailLength = 6;
                                hitEffect = despawnEffect = new WaveEffect(){{
                                    colorFrom = NuItems.magent.color;
                                    colorTo = Color.white;
                                    sizeFrom = 4f;sizeTo = 2f;
                                    strokeFrom = 1f;strokeTo = 4.5f;
                                    lifetime = 16f;
                                }};
                                homingPower = 0.1f;
                                buildingDamageMultiplier = 0.5f;
                            }};
                            bulletInterval = 2f;
                            intervalRandomSpread = 20f;
                            intervalBullets = 2;
                            intervalAngle = 180f;
                            intervalSpread = 300f;
                            intervalBullet = new BasicBulletType(3f,75){{
                                width = 10f;
                                height = 18f;
                                lifetime = 28f;
                                pierce = true;
                                pierceCap = 6;
                                pierceBuilding = true;
                                frontColor = lightColor = trailColor = NuItems.magent.color;
                                backColor = hitColor = Color.white;
                                trailWidth = 2.5f;
                                trailLength = 6;
                                hitEffect = despawnEffect = new WaveEffect(){{
                                    colorFrom = NuItems.magent.color;
                                    colorTo = Color.white;
                                    sizeFrom = 4f;sizeTo = 2f;
                                    strokeFrom = 1f;strokeTo = 4.5f;
                                    lifetime = 16f;
                                }};
                                homingPower = 0.1f;
                                buildingDamageMultiplier = 0.5f;
                            }};
                        }};
                    }},
                    NuItems.monoSiliCrystal,new MissileBulletType(6f,175f){{
                        width = 10f;
                        height = 32f;
                        lifetime =37f;
                        homingPower = 1f;
                        weaveScale = 12f;
                        reloadMultiplier = 0.8f;
                        frontColor = lightColor = trailColor = NuItems.monoSiliCrystal.color;
                        backColor = hitColor = Items.silicon.color;
                        trailLength = 8;
                        fragBullets = 20;
                        fragVelocityMin = 0.8f;
                        fragVelocityMax = 1.25f;
                        fragLifeMin = 0.5f;
                        fragBullet = new BasicBulletType(3f,57.5f){{
                            width = 10f;
                            height = 18f;
                            lifetime = 28f;
                            pierce = true;
                            pierceCap = 6;
                            pierceBuilding = true;
                            frontColor = lightColor = trailColor = NuItems.monoSiliCrystal.color;
                            backColor = hitColor = Items.silicon.color;
                            trailWidth = 2.5f;
                            trailLength = 6;
                            hitEffect = despawnEffect = new WaveEffect(){{
                                colorFrom = NuItems.monoSiliCrystal.color;
                                colorTo = Items.silicon.color;
                                sizeFrom = 4f;sizeTo = 2f;
                                strokeFrom = 1f;strokeTo = 4.5f;
                                lifetime = 16f;
                            }};
                            homingPower = 1f;
                            buildingDamageMultiplier = 0.5f;
                        }};
                        bulletInterval = 2f;
                        intervalRandomSpread = 20f;
                        intervalBullets = 2;
                        intervalAngle = 180f;
                        intervalSpread = 300f;
                        intervalBullet = new BasicBulletType(3f,57.5f){{
                            width = 10f;
                            height = 18f;
                            lifetime = 28f;
                            pierce = true;
                            pierceCap = 6;
                            pierceBuilding = true;
                            frontColor = lightColor = trailColor = NuItems.monoSiliCrystal.color;
                            backColor = hitColor =Items.silicon.color;
                            trailWidth = 2.5f;
                            trailLength = 6;
                            hitEffect = despawnEffect = new WaveEffect(){{
                                colorFrom = NuItems.monoSiliCrystal.color;
                                colorTo = Items.silicon.color;
                                sizeFrom = 4f;sizeTo = 2f;
                                strokeFrom = 1f;strokeTo = 4.5f;
                                lifetime = 16f;
                            }};
                            homingPower = 1f;
                            buildingDamageMultiplier = 0.75f;
                        }};
                    }},
                    NuItems.frailPolyester,new MissileBulletType(6f,120f){{
                        width = 10f;
                        height = 32f;
                        lifetime =37f;
                        homingPower = 0.3f;
                        weaveScale = 12f;
                        frontColor = lightColor = trailColor = NuItems.frailPolyester.color;
                        backColor = hitColor = NuColor.SurvivalColor;
                        trailLength = 8;
                        fragBullets = 20;
                        fragVelocityMin = 0.8f;
                        fragVelocityMax = 1.25f;
                        fragLifeMin = 0.5f;
                        status = StatusEffects.burning;
                        statusDuration = 60f*5.5f;
                        fragBullet = new BasicBulletType(3f,40f){{
                            width = 10f;
                            height = 18f;
                            lifetime = 28f;
                            pierce = true;
                            pierceCap = 6;
                            pierceBuilding = true;
                            frontColor = lightColor = trailColor = NuItems.frailPolyester.color;
                            backColor = hitColor = NuColor.SurvivalColor;
                            trailWidth = 2.5f;
                            trailLength = 6;
                            hitEffect = despawnEffect = new WaveEffect(){{
                                colorFrom = NuItems.frailPolyester.color;
                                colorTo = NuColor.SurvivalColor;
                                sizeFrom = 4f;sizeTo = 2f;
                                strokeFrom = 1f;strokeTo = 4.5f;
                                lifetime = 16f;
                            }};
                            homingPower = 0.45f;
                            buildingDamageMultiplier = 0.6f;
                        }};
                        bulletInterval = 2f;
                        intervalRandomSpread = 20f;
                        intervalBullets = 2;
                        intervalAngle = 180f;
                        intervalSpread = 300f;
                        intervalBullet = new BasicBulletType(3f,40f){{
                            width = 10f;
                            height = 18f;
                            lifetime = 28f;
                            pierce = true;
                            pierceCap = 6;
                            pierceBuilding = true;
                            frontColor = lightColor = trailColor = NuItems.frailPolyester.color;
                            backColor = hitColor = NuColor.SurvivalColor;
                            trailWidth = 2.5f;
                            trailLength = 6;
                            hitEffect = despawnEffect = new WaveEffect(){{
                                colorFrom = NuItems.frailPolyester.color;
                                colorTo = NuColor.SurvivalColor;
                                sizeFrom = 4f;sizeTo = 2f;
                                strokeFrom = 1f;strokeTo = 4.5f;
                                lifetime = 16f;
                            }};
                            homingPower = 0.45f;
                            buildingDamageMultiplier = 0.6f;
                        }};
                    }},
                    NuItems.rubberFrag,new MissileBulletType(6f,215f){{
                        width = 10f;
                        height = 32f;
                        lifetime =37f;
                        homingPower = 0.3f;
                        weaveScale = 12f;
                        frontColor = lightColor = trailColor = NuItems.rubber.color;
                        backColor = hitColor = NuItems.rubberFrag.color;
                        trailLength = 8;
                        fragBullets = 20;
                        fragVelocityMin = 0.8f;
                        fragVelocityMax = 1.25f;
                        fragLifeMin = 0.5f;
                        status = StatusEffects.burning;
                        statusDuration = 60f*5.5f;
                        fragBullet = new BasicBulletType(6f,78f){{
                            width = 10f;
                            height = 18f;
                            lifetime = 14f;
                            pierce = true;
                            pierceCap = 6;
                            pierceBuilding = true;
                            frontColor = lightColor = trailColor = NuItems.rubber.color;
                            backColor = hitColor =NuItems.rubberFrag.color;
                            trailWidth = 2.5f;
                            trailLength = 6;
                            hitEffect = despawnEffect = new WaveEffect(){{
                                colorFrom = NuItems.rubber.color;
                                colorTo = NuItems.rubberFrag.color;
                                sizeFrom = 4f;sizeTo = 2f;
                                strokeFrom = 1f;strokeTo = 4.5f;
                                lifetime = 16f;
                            }};
                            homingPower = 0.45f;
                            buildingDamageMultiplier = 0.6f;
                        }};
                        bulletInterval = 2f;
                        intervalRandomSpread = 20f;
                        intervalBullets = 2;
                        intervalAngle = 180f;
                        intervalSpread = 300f;
                        intervalBullet = new BasicBulletType(6f,78f){{
                            width = 10f;
                            height = 18f;
                            lifetime = 14f;
                            pierce = true;
                            pierceCap = 6;
                            pierceBuilding = true;
                            frontColor = lightColor = trailColor = NuItems.rubber.color;
                            backColor = hitColor = NuItems.rubberFrag.color;
                            trailWidth = 2.5f;
                            trailLength = 6;
                            hitEffect = despawnEffect = new WaveEffect(){{
                                colorFrom = NuItems.rubber.color;
                                colorTo =NuItems.rubberFrag.color;
                                sizeFrom = 4f;sizeTo = 2f;
                                strokeFrom = 1f;strokeTo = 4.5f;
                                lifetime = 16f;
                            }};
                            homingPower = 0.45f;
                            buildingDamageMultiplier = 0.6f;
                        }};
                    }},
                    NuItems.dirtyCoagulum,new MissileBulletType(6f,115f){{
                        width = 10f;
                        height = 32f;
                        lifetime =37f;
                        homingPower = 0.3f;
                        weaveScale = 12f;
                        frontColor = lightColor = trailColor = NuItems.dirtyCoagulum.color;
                        backColor = hitColor =NuColor.HonorColor;
                        trailLength = 8;
                        fragBullets = 20;
                        fragVelocityMin = 0.8f;
                        fragVelocityMax = 1.25f;
                        fragLifeMin = 0.5f;
                        reloadMultiplier = 1.45f;
                        fragBullet = new BasicBulletType(6f,54f){{
                            width = 10f;
                            height = 18f;
                            lifetime = 14f;
                            pierce = true;
                            pierceCap = 6;
                            pierceBuilding = true;
                            frontColor = lightColor = trailColor = NuItems.dirtyCoagulum.color;
                            backColor = hitColor=NuColor.HonorColor;
                            trailWidth = 2.5f;
                            trailLength = 6;
                            hitEffect = despawnEffect = new WaveEffect(){{
                                colorFrom =NuItems.dirtyCoagulum.color;
                                colorTo = NuColor.HonorColor;
                                sizeFrom = 4f;sizeTo = 2f;
                                strokeFrom = 1f;strokeTo = 4.5f;
                                lifetime = 16f;
                            }};
                            homingPower = 0.45f;
                            buildingDamageMultiplier = 0.6f;
                        }};
                        bulletInterval = 2f;
                        intervalRandomSpread = 20f;
                        intervalBullets = 2;
                        intervalAngle = 180f;
                        intervalSpread = 300f;
                        intervalBullet = new BasicBulletType(6f,32f){{
                            width = 10f;
                            height = 18f;
                            lifetime = 14f;
                            pierce = true;
                            pierceCap = 6;
                            pierceBuilding = true;
                            frontColor = lightColor = trailColor = NuItems.dirtyCoagulum.color;
                            backColor = hitColor = NuColor.HonorColor;
                            trailWidth = 2.5f;
                            trailLength = 6;
                            hitEffect = despawnEffect = new WaveEffect(){{
                                colorFrom =NuItems.dirtyCoagulum.color;
                                colorTo =NuColor.HonorColor;
                                sizeFrom = 4f;sizeTo = 2f;
                                strokeFrom = 1f;strokeTo = 4.5f;
                                lifetime = 16f;
                            }};
                            homingPower = 0.45f;
                            buildingDamageMultiplier = 0.6f;
                        }};
                    }}
            );
        }};
        Traction = new TractorBeamTurret("Traction"){{
            requirements(Category.turret, with(
                    NuItems.bigIron,250,
                    NuItems.monoSiliCrystal,250,
                    NuItems.pumice,40,
                    NuItems.magent,100));
            size = 2;
            health = 2000;
            armor = 20;
            shootCone = 360f;
            rotateSpeed = 40f;
            scaledForce = 9f;
            damage = 2f;
            force = 20f;
            range = 300f;
            hasPower = true;
            targetAir = true;
            targetGround = true;
            consumePower(10f);
        }};
        BreathSoil = new ShadeConTurret("BreathSoil"){{
       // —— 基础属性 ——
       requirements(Category.turret, with(
               NuItems.bigIron, 500,
               NuItems.monoSiliCrystal, 360,
               Items.graphite, 360,
               NuItems.magent,80
                              ));
       size = 2;
       health = 1800;
       range = 140f;            // 射程 = 扫描半径（init() 会自动把 scanRadius 抬到 ≥ range）
       reload = 30f;            // 每发扫描脉冲的间隔（tick）
       shake = 0f;
       recoil = 0f;
       shootSound = Sounds.none;
       consumePower(15f);        // 耗电
       // —— 自定义扫描子弹参数（覆盖默认）——
       shootType = new SweepBulletType(100f){{
           heal = 10f;                    // 受伤友方每次触发治疗量
           shield = 4f;                // 满血友方每次触发护盾增加量
           maxShieldRatio = 2f;        // 护盾上限 = 60% 最大血量
           scanRadius = 240f;            // 扫描半径（一般 ≥ 炮塔 range）
           fieldAngle = 60f;            // 扇形角度
           damageInterval = 5f;          // 伤害/治疗/护盾触发间隔（tick）
           lifetime = 12f;               // 单发扫描脉冲持续时长（tick）
           scanColor = NuColor.EnergyColor;
           healColor  = NuColor.EnergyLiColor;
           healThreshold = 0.75f;
           pdDamage = 25f;
           autoHealTarget = true;
       }};
       // —— 可选：关闭对满血友方的护盾瞄准（只治疗受伤友方 + 攻击敌方）——
            targetShielding = true;
        }};
        BurnInjured = new ContinuousLiquidTurret("BurnInjured"){{
            requirements(Category.turret, with(
                    NuItems.bigIron,250,
                    NuItems.frailPolyester,180,
                    Items.graphite,150,
                    NuItems.pumice,90
            ));
            size = 2;
            health = 2000;
            armor = 20;
            shake = 0f;
            rotateSpeed = 6f;
            range = 160f;reload = 5f;
            targetAir = true;
            targetGround = true;
            shootCone = 3f;
            liquidCapacity = 250f;
            liquidConsumed = 0.2f;
            cooldownTime = 100f;
            shootWarmupSpeed = 0.75f;
            researchCostMultiplier = 1.2f;
            inaccuracy =5f;
            velocityRnd = 0.15f;
            ammo(
                    NuLiquid.strangeLiquid,new ContinuousFlameBulletType(350f){{
                        collides = collidesTiles = collidesAir = true;
                        pierceArmor = pierce = true;
                        length = 180f;width = 2.4f;
                        continuous = true;
                        flareColor = NuLiquid.strangeLiquid.color;
                        colors = new Color[]{NuColor.HonorColor,NuLiquid.strangeLiquid.color,
                                NuColor.HonorBackColor,NuColor.PaleColor,Color.white.cpy()};
                        drawFlare = true;pierceCap = 6;damageInterval = 12f;
                        buildingDamageMultiplier = 0.8f; timescaleDamage = true;
                    }},
                    NuLiquid.liquidOxygen,new ContinuousFlameBulletType(500f){{
                        collides = collidesTiles = collidesAir = true;
                        pierceArmor = pierce = true;
                        length = 210f;width = 2.4f;
                        continuous = true;
                        flareColor = NuLiquid.liquidOxygen.color;
                        colors = new Color[]{NuColor.EnergyColor,NuLiquid.liquidOxygen.color,
                                NuColor.EnergyBackColor,NuColor.PaleColor,Color.white.cpy()};
                        drawFlare = true;pierceCap = 10;damageInterval = 10f;
                        buildingDamageMultiplier = 0.9f;
                        rangeChange = 40f; timescaleDamage = true;
                    }},
                    NuLiquid.nuclearFluid,new ContinuousFlameBulletType(85f){{
                        collides = collidesTiles = collidesAir = true;
                        pierceArmor = pierce = true;
                        length = 140f;width = 2.4f;
                        continuous = true;
                        flareColor = NuLiquid.nuclearFluid.color;
                        colors = new Color[]{NuColor.SailColor,NuLiquid.nuclearFluid.color,
                                NuColor.SailBackColor,NuColor.PaleColor,Color.white.cpy()};
                        drawFlare = true;pierceCap = 3;damageInterval = 15f;
                        buildingDamageMultiplier = 0.9f;
                        rangeChange = -20f; timescaleDamage = true;
                        status = NuStatus.radiation;
                        statusDuration = 60f*6;
                    }},
                    NuLiquid.dirtySolution,new ContinuousFlameBulletType(54f){{
                        collides = collidesTiles = collidesAir = true;
                        pierceArmor = pierce = true;
                        length = 160f;width = 2.4f;
                        continuous = true;
                        flareColor = NuLiquid.dirtySolution.color;
                        colors = new Color[]{NuColor.HonorColor,NuLiquid.dirtySolution.color,
                                NuColor.HonorBackColor,NuColor.PaleColor,Color.white.cpy()};
                        drawFlare = true;pierceCap = 6;damageInterval = 15f;
                        buildingDamageMultiplier = 0.9f; timescaleDamage = true;
                    }},
                    NuLiquid.divineTears,new ContinuousFlameBulletType(600f){{
                        collides = collidesTiles = collidesAir = true;
                        pierceArmor = pierce = true;
                        length = 245f;width = 2.4f;
                        continuous = true;
                        flareColor = NuLiquid.liquidOxygen.color;
                        colors = new Color[]{NuColor.EnergyColor,NuLiquid.liquidOxygen.color,
                                NuColor.EnergyBackColor,NuColor.PaleColor,Color.white.cpy()};
                        drawFlare = true;pierceCap = 12;damageInterval = 6f;
                        buildingDamageMultiplier = 0.9f;
                        rangeChange = 80f; timescaleDamage = true;
                        status = NuStatus.divineWrath;statusDuration = 60f*10;
                    }},
                    NuLiquid.prismLiquid,new ContinuousFlameBulletType(360f){{
                        collides = collidesTiles = collidesAir = true;
                        pierceArmor = pierce = true;
                        length = 285f;width = 2.4f;
                        continuous = true;
                        flareColor = NuLiquid.liquidOxygen.color;
                        colors = new Color[]{NuColor.EnergyColor,NuLiquid.liquidOxygen.color,
                                NuColor.EnergyBackColor,NuColor.PaleColor,Color.white.cpy()};
                        drawFlare = true;pierceCap = 12;damageInterval = 12f;
                        buildingDamageMultiplier = 0.9f;
                        rangeChange = 120f; timescaleDamage = true;
                        status = NuStatus.pulse;statusDuration = 60f*10;
                        fragBullets = 4;fragRandomSpread = 360f;
                        fragBullet = new LightningBulletType(){{
                            damage = 65f;lightningLength =22;lightningLengthRand = 10;
                            lightningColor = NuColor.PaleColor;
                        }};
                    }}
            );
        }};
        TurbidAir = new ContinuousTurret("TurbidAir"){{
            requirements(Category.turret, with(
                    NuItems.pumice,250,
                    NuItems.magent,180,
                    Items.graphite,150,
                    NuItems.rubber,90
            ));
            size = 3;
            health = 3000;
            armor = 28;
            shake = 0f;
            rotateSpeed = 10f;
            range = 240f;targetAir = targetGround = true;
            unitSort = UnitSorts.strongest;
            consumeLiquid(NuLiquid.strangeLiquid, 0.15f);
            consumePower(18f);
            shootCone = 360f;
            liquidCapacity = 400f;
            cooldownTime = 100f;
            shootWarmupSpeed = 0.08f;
            aimChangeSpeed = 0.9f;
            rotateSpeed = 0.9f;
            researchCostMultiplier = 1.25f;
            inaccuracy =5f;
            velocityRnd = 0.15f;
            shootType = new PointLaserBulletType(){{
                damage = 600f;
                buildingDamageMultiplier = 0.9f;
                hitColor = NuColor.SailBackColor;
                color = NuColor.SailColor;
            }};
        }};
        CrossTractor = new ItemTurret("CrossTractor"){{
            requirements(Category.turret, with(NuItems.bigIron,500,NuItems.monoSiliCrystal,350,NuItems.alkSliver,100,NuItems.pumice,250));
            ammo(
                    NuItems.magent,new SpeedDamageBulletType(3.2f, 50f){{
                        damageIncrease = 50f;
                        width = 20f;
                        height = 20f;
                        lifetime = 80f;
                        ammoMultiplier = 2;
                        hitEffect = despawnEffect = Fx.hitBulletColor;
                        hitColor = backColor = trailColor = Pal.copperAmmoBack;
                        frontColor = Pal.copperAmmoFront;
                    }}
            );
            shoot = new ShootAlternate(){{
                shots = 6;
                shotDelay = 7.5f;
                spread = 5f;
                barrels =3;
            }};
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
            recoil = 2.5f;
            shootY = 3f;
            reload = 75f;
            range = 320f;
            shootCone = 15f;
            ammoUseEffect = Fx.casing1;
            health = 3000;
            size = 3;
            ammoPerShot = 5;
            maxAmmo = 50;
            inaccuracy = 2f;
            rotateSpeed = 5f;
            coolant = consumeCoolant(0.25f);
            coolantMultiplier = 4f;
            researchCostMultiplier = 1.5f;
            depositCooldown = 2.0f;
        }};
        Mountains = new ItemTurret("Mountains"){{
            size = 3;
            health = 3000;
            armor = 28;
            shake = 2f;
            reload = 150f;
            ammoPerShot =3;
            maxAmmo = 60;
            shootCone = 20f;
            rotateSpeed = 9f;
            range = 320f;
            targetGround = true;
            targetAir =true;
            shootY = 4f;
            requirements(Category.turret,with(
                    NuItems.alkSliver,250,
                    NuItems.magent,125,
                    NuItems.pumice,350,
                    NuItems.prismCrystal,200
            ));
            shoot = new ShootBarrel(){{
                barrels = new float[]{
                        6f, 0f, 0f,
                        0f, 0f ,0f,
                        -6f,0f, 0f
                };
                shots = 12;
                shotDelay = 8f;
            }};
            ammo(
                    NuItems.bigIron,new BulletType(0f,0f){{
                        shootEffect = Fx.shootBig;
                        smokeEffect = Fx.shootSmokeMissileColor;
                        hitColor = Pal.redLight;
                        ammoMultiplier = 1f;
                        spawnUnit = FederalUnitTypes.HolyWhite;
                    }},
                    NuItems.magent,new BulletType(0f,0f){{
                        shootEffect = Fx.shootBig;
                        smokeEffect = Fx.shootSmokeMissileColor;
                        hitColor = Pal.redLight;
                        ammoMultiplier = 1f;
                        spawnUnit = FederalUnitTypes.HolyDay;
                    }},
                    NuItems.alkSliver,new BulletType(0f,0f){{
                        shootEffect = Fx.shootBig;
                        smokeEffect = Fx.shootSmokeMissileColor;
                        hitColor = Pal.redLight;
                        ammoMultiplier = 1f;
                        spawnUnit = FederalUnitTypes.HolyVoid;
                    }},
                    NuItems.uranium,new BulletType(0f,0f){{
                        shootEffect = Fx.shootBig;
                        smokeEffect = Fx.shootSmokeMissileColor;
                        hitColor = Pal.redLight;
                        ammoMultiplier = 1f;
                        spawnUnit = FederalUnitTypes.HolyCore;
                    }},
                    NuItems.thallide,new BulletType(0f,0f){{
                        shootEffect = Fx.shootBig;
                        smokeEffect = Fx.shootSmokeMissileColor;
                        hitColor = Pal.redLight;
                        ammoMultiplier = 1f;
                        spawnUnit = FederalUnitTypes.HolyProclamation;
                    }},
                    NuItems.pumice,new BulletType(0f,0f){{
                        shootEffect = Fx.shootBig;
                        smokeEffect = Fx.shootSmokeMissileColor;
                        hitColor = Pal.redLight;
                        ammoMultiplier = 1f;
                        spawnUnit = FederalUnitTypes.HolyFloat;
                    }}
            );
        }};
        MistRosy = new ItemTurret("MistRosy"){{
            coolantMultiplier = 1.5f;
            liquidCapacity = 600f;
            coolant = consumeCoolant(0.2f);
            depositCooldown = 2.0f;
            size = 3;
            health = 3000;
            armor = 28;
            shake = 2f;
            reload = 60f;
            ammoPerShot =3;
            maxAmmo = 60;
            shootCone = 45f;
            rotateSpeed = 9f;
            range = 160f;
            targetGround = true;
            targetAir =true;
            shootY = 4f;
            inaccuracy = 5f;
            velocityRnd = 0.12f;
            requirements(Category.turret,with(
                    NuItems.alkSliver,250,
                    NuItems.magent,125,
                    NuItems.pumice,350,
                    NuItems.prismCrystal,200
            ));
            shoot = new ShootSpread(45,1f);
            ammo(
                    NuItems.bigIron,new BasicBulletType(6.5f,42f){{
                        width = 5f;
                        height = 15f;
                        shootEffect = Fx.shootBigColor;
                        smokeEffect = Fx.shootSmokeSquareSparse;
                        hitEffect = despawnEffect = Fx.hitSquaresColor;
                        lifetime = 25f;
                        knockback = 0.1f;
                        reloadMultiplier = 1.25f;
                        trailLength = 10;
                        trailWidth = 1.6f;
                        frontColor = lightColor =trailColor =Color.white;
                        backColor = hitColor = NuItems.bigIron.color;
                    }},
                    NuItems.monoSiliCrystal,new BasicBulletType(6.5f,57f){{
                        width = 5f;
                        height = 15f;
                        shootEffect = Fx.shootBigColor;
                        smokeEffect = Fx.shootSmokeSquareSparse;
                        hitEffect = despawnEffect = Fx.hitSquaresColor;
                        lifetime = 25f;
                        knockback = 0.1f;
                        reloadMultiplier = 0.85f;
                        trailLength = 10;
                        trailWidth = 1.6f;
                        homingPower = 0.75f;
                        homingRange = 50f;
                        frontColor = lightColor =trailColor =Items.silicon.color;
                        backColor = hitColor = NuItems.monoSiliCrystal.color;
                    }},
                    NuItems.magent,new BasicBulletType(6.5f,90f){{
                        width = 5f;
                        height = 15f;
                        shootEffect = Fx.shootBigColor;
                        smokeEffect = Fx.shootSmokeSquareSparse;
                        hitEffect = despawnEffect = Fx.hitSquaresColor;
                        lifetime = 25f;
                        knockback = 0.1f;
                        trailLength = 10;
                        trailWidth = 1.6f;
                        frontColor = lightColor =trailColor =NuItems.magent.color;
                        backColor = hitColor = Color.white;
                    }},
                    NuItems.sulFurFrag,new BasicBulletType(6.5f,65f){{
                        width = 5f;
                        height = 15f;
                        shootEffect = Fx.shootBigColor;
                        smokeEffect = Fx.shootSmokeSquareSparse;
                        hitEffect = despawnEffect = Fx.hitSquaresColor;
                        lifetime = 25f;
                        knockback = 0.1f;
                        reloadMultiplier = 0.9f;
                        status = StatusEffects.burning;
                        statusDuration = 60f*4.5f;
                        trailLength = 10;
                        trailWidth = 1.6f;
                        frontColor = lightColor =trailColor =Items.sand.color;
                        backColor = hitColor = NuItems.sulFurFrag.color;
                    }},
                    NuItems.pumice,new BasicBulletType(6.5f,110f){{
                        width = 5f;
                        height = 15f;
                        shootEffect = Fx.shootBigColor;
                        smokeEffect = Fx.shootSmokeSquareSparse;
                        hitEffect = despawnEffect = Fx.hitSquaresColor;
                        lifetime = 25f;
                        knockback = 0.1f;
                        reloadMultiplier = 0.9f;
                        status = StatusEffects.freezing;
                        statusDuration = 60f*4.5f;
                        trailLength = 10;
                        trailWidth = 1.6f;
                        frontColor = lightColor =trailColor =Color.white;
                        backColor = hitColor = NuItems.pumice.color;
                    }},
                    NuItems.sacredIron,new BasicBulletType(6.5f,190f){{
                        width = 5f;
                        height = 15f;
                        shootEffect = Fx.shootBigColor;
                        smokeEffect = Fx.shootSmokeSquareSparse;
                        hitEffect = despawnEffect = Fx.hitSquaresColor;
                        lifetime = 25f;
                        knockback = 0.1f;
                        reloadMultiplier = 0.9f;
                        status = NuStatus.enrich;
                        statusDuration = 60f*4.5f;
                        trailLength = 10;
                        trailWidth = 1.6f;
                        frontColor = lightColor =trailColor =NuColor.EnergyColor;
                        backColor = hitColor = NuItems.sacredIron.color;
                    }}
            );
        }};
        CircuitBreak = new LaserTurret("CircuitBreak"){{
            requirements(Category.turret,with(
                    NuItems.pumice,560,
                    NuItems.magent,260,
                    NuItems.monoSiliCrystal,500,
                    NuItems.alkSliver,70
            ));
           size = 3;
           shootEffect = Fx.shootBigSmoke2;shootCone = 40f;
           health = 3000;
           range = 240f;
           reload = 200f;
           targetGround = true;
           targetAir = true;
           liquidCapacity = 800f;
           recoil = 2.65f;
           shoot.shotDelay = 180f;
           inaccuracy = 0f;
           recoilTime = 50f;
           shake = 5f;
           rotateSpeed = 10f;
           coolEffect = Fx.steam;
           minWarmup = 0.86f;
           shootDuration = 240f;
           shootSound = Sounds.shootMeltdown;
           loopSound = Sounds.beamMeltdown;
           loopSoundVolume = 2f;
           liquidCapacity = 60f;
           coolant = consumeCoolant(0.5f);
           consumePower(18f);
           shootType = new ContinuousLaserBulletType(300f){{
                damageInterval = 10f;
                length = 245f;
                buildingDamageMultiplier = 0.75f;
                width = 2.5f;
                pierceArmor = true;
                knockback = 0.85f;
                timescaleDamage = true;
                colors = new Color[]{NuColor.SailColor,NuColor.SailConColor,NuColor.SailBackColor,Color.white};
                shootEffect = Fx.shootBigSmoke2;
                strokeFrom = 5.5f;strokeTo = 0.75f;pointyScaling = 1.5f;
                shootCone = 40f;
                intervalBullets = 4;
                bulletInterval = 10f;
                intervalRandomSpread = 30f;
                intervalBullet = new BulletType(0f,0f){{
                   instantDisappear = true;
                   fragBullets = 2;
                   fragRandomSpread = 10f;
                   fragBullet = new LaserBulletType(70f){{
                       length = 180f;
                       colors = new Color[]{NuColor.SailColor,NuColor.SailBackColor,Color.white};
                       width = 7f;sideAngle = 45f; sideLength =32f;sideWidth = 3f;
                       pierce = true;pierceCap = 3;
                       fragBullets = 2;fragRandomSpread = 0f;
                       fragBullet = new LightningBulletType(){{
                          damage = 28f;lightningLength =22;lightningLengthRand = 10;
                          lightningColor = NuColor.SailColor;
                          status = NuStatus.radiation;
                          statusDuration = 60f*6f;
                       }};
                   }};
                }};
            }};
        }};
        IntermittentPressure = new PowerTurret("IntermittentPressure"){{
            requirements(Category.turret, with(
                    NuItems.pumice,850,
                    NuItems.monoSiliCrystal,950,
                    NuItems.rubber,450,
                    Items.graphite,800,
                    NuItems.thallide,450
            ));
            range = 400f;
            recoil = 2f;
            reload = 210f;
            shake = 2f;
            shootEffect = new WrapEffect(){{
                effect = Fx.lancerLaserShoot;
                color = NuColor.CoreColor;
            }};
            smokeEffect = Fx.none;
            heatColor = Color.red;
            size = 4;
            shootCone = 30f;
            health = 4000;
            targetAir = true;
            shootSound = Sounds.shootLancer;
            coolant = consumeCoolant(0.5f);
            consumePower(20f);
            unitSort = UnitSorts.strongest;
            velocityRnd = 0.15f;
            warmupMaintainTime = 120f;
            minWarmup = 0.96f;
            shootWarmupSpeed = 0.08f;
            shoot = new ShootMulti(new ShootPattern(){{
               shots = 3; shotDelay=30f;
            }},new ShootSpread(3,20f));
            shootType =new FlakBulletType(5f,750f){{
                    collides = collidesAir = collidesGround = collidesTiles = true;
                    lifetime = 65f;
                    width = 8f;
                    trailLength = 20;
                    height = 18f;
                    frontColor = lightColor = trailColor = Color.white;
                    backColor = hitColor = NuColor.CoreColor;
                    despawnEffect = hitEffect = new WaveEffect(){{
                        sizeFrom = 40f; sizeTo = 4f;
                        strokeFrom = 1f; strokeTo = 7.5f;   // 越收越粗
                        interp = Interp.reverse;
                        sides = 6; rotation = 60f;        //
                        lifetime = 24f;
                        colorFrom = NuColor.PaleColor;
                        colorTo = NuColor.CoreColor;
                    }};
                    trailLength = 18;
                    trailWidth = 4.2f;
                    trailInterval = 3f;
                    trailEffect = new MultiEffect(NuFx.sniperGlowTail,new WaveEffect(){{
                        sizeFrom = 32f; sizeTo = 8f;
                        strokeFrom = 1f; strokeTo = 6f;   // 越收越粗
                        interp = Interp.reverse;
                        sides = 4; rotation = 30f;        //
                        lifetime = 20f;
                        colorFrom = NuColor.EnergyColor;
                        colorTo = NuColor.EnergyBackColor;
                    }});
                    trailEffect = NuFx.sniperGlowTail;
                    fragBullets = 10;
                    fragRandomSpread = 90f;
                    fragBullet = new LaserBulletType(75f){{
                        length = 95f;
                        width = 1f;
                        colors = new Color[]{NuColor.DespColor, NuColor.BombColor, NuColor.BombBackColor, NuColor.DespBackColor};
                        hitEffect = Fx.hitLancer;
                        sideAngle = 175f;
                        sideWidth = 1f;
                        sideLength = 40f;
                        lifetime = 22f;
                        pierceCap = 2;
                        optimalLifeFract = 1f;
                        status = NuStatus.pulse;
                        statusDuration = 60f * 2f;
                    }};
            }};
        }};
        DivineCreation = new ItemTurret("DivineCreation"){{
            coolantMultiplier = 1.5f;
            liquidCapacity = 600f;
            coolant = consumeCoolant(0.2f);
            consumePower(20f);
            depositCooldown = 2.0f;
            size = 4;
            health = 4000;
            armor = 28;
            shake = 2f;
            reload = 330f;
            ammoPerShot =3;
            maxAmmo = 60;
            shootCone = 45f;
            rotateSpeed = 9f;
            range = 480f;
            targetGround = true;
            targetAir =true;
            shootY = 4f;
            inaccuracy = 5f;
            velocityRnd = 0.12f;
            requirements(Category.turret,with(
                    NuItems.alkSliver,600,
                    NuItems.uranium,350,
                    NuItems.pumice,650,
                    NuItems.prismCrystal,1000,
                    NuItems.remakeSource,150
            ));
            shoot = new ShootPattern(){{
                shots = 2;
                shotDelay = 110f;
            }};
            ammo(
                    NuItems.bottledMagenticStorm,new BasicBulletType(4.5f,800f){{
                        lifetime = 89f;
                        frontColor = lightColor = trailColor = NuColor.EnergyColor;
                        backColor = hitColor = NuColor.EnergyBackColor;
                        trailWidth = 3f;
                        trailLength = 15;
                        width = 24f;
                        height = 32f;
                        trailInterval = 3f;
                        trailEffect = new MultiEffect(new ParticleEffect(){{
                            line = true;
                            strokeFrom = 0.5f; strokeTo = 7f;
                            lenFrom = 6f; lenTo = 32f;
                            cone = 180f;
                            colorFrom = NuColor.EnergyColor;
                            colorTo = Color.white;
                        }},new WaveEffect(){{
                            interp = Interp.circleOut;
                            lifetime = 20f;sizeFrom = 4f;sizeTo=24f;strokeFrom=4f;strokeTo=0.5f;
                            colorFrom=NuColor.EnergyColor;colorTo=NuColor.EnergyBackColor;
                        }});
                        pierce = true; pierceCap = 10;
                        fragBullets = 1;fragRandomSpread = 0f;
                        fragBullet = new BasicBulletType(0f,800f){{
                            pierce = true; pierceCap = 100;
                            width = 24f;
                            height = 32f;
                            frontColor = lightColor = trailColor = NuColor.EnergyColor;
                            backColor = hitColor = NuColor.EnergyBackColor;
                            lifetime = 100f;
                            intervalBullets = 6;
                            intervalRandomSpread = 360f;
                            bulletInterval = 2f;
                            intervalBullet = new MultiBulletType(new BasicBulletType(5f,80f){{
                                lifetime = 16f;
                                width = 8f;
                                height = 21f;
                                frontColor = lightColor = trailColor = NuColor.EnergyColor;
                                backColor = hitColor = NuColor.EnergyBackColor;
                                trailLength = 15;trailWidth = 8f;
                                despawnEffect = hitEffect = new WaveEffect(){{
                                    sizeFrom = 32f; sizeTo = 8f;
                                    strokeFrom = 1f; strokeTo = 6f;   // 越收越粗
                                    interp = Interp.reverse;
                                    sides = 8; rotation = 30f;        //
                                    lifetime = 20f;
                                    colorFrom = NuColor.EnergyColor;
                                    colorTo = NuColor.EnergyBackColor;
                                }};
                            }},new LightningBulletType(){{
                                damage = 65f;
                                lifetime = 45f;
                                status = StatusEffects.shocked;
                                lightningLength = 50;
                                lightningLengthRand = 4;
                                lightningColor = NuColor.EnergyColor;
                            }},new LightningBulletType(){{
                                damage = 65f;
                                lifetime = 45f;
                                status = StatusEffects.shocked;
                                lightningLength = 20;
                                lightningLengthRand = 4;
                                lightningColor = NuColor.EnergyColor;
                            }});
                        }};
                    }},
                    NuItems.thallide,new BasicBulletType(4.5f,700f){{
                        lifetime = 89f;
                        frontColor = lightColor = trailColor = NuColor.HonorColor;
                        backColor = hitColor = NuColor.HonorBackColor;
                        trailWidth = 3f;
                        trailLength = 15;
                        width = 24f;
                        height = 32f;
                        trailInterval = 3f;
                        trailEffect = new MultiEffect(new ParticleEffect(){{
                            line = true;
                            strokeFrom = 0.5f; strokeTo = 7f;
                            lenFrom = 6f; lenTo = 32f;
                            cone = 180f;
                            colorFrom = NuColor.HonorColor;
                            colorTo = Color.white;
                        }},new WaveEffect(){{
                            interp = Interp.circleOut;
                            lifetime = 20f;sizeFrom = 4f;sizeTo=24f;strokeFrom=4f;strokeTo=0.5f;
                            colorFrom=NuColor.HonorColor;colorTo=NuColor.HonorBackColor;
                        }});
                        pierce = true; pierceCap = 10;
                        fragBullets = 1;fragRandomSpread = 0f;
                        fragBullet = new BasicBulletType(0f,800f){{
                            pierce = true; pierceCap = 100;
                            width = 24f;
                            height = 32f;
                            frontColor = lightColor = trailColor = NuColor.HonorColor;
                            backColor = hitColor = NuColor.HonorBackColor;
                            lifetime = 80f;
                            intervalBullets = 6;
                            intervalRandomSpread = 360f;
                            bulletInterval = 2f;
                            intervalBullet = new BasicBulletType(5f,80f){{
                                lifetime = 24f;
                                width = 8f;
                                height = 21f;
                                frontColor = lightColor = trailColor = NuColor.HonorColor;
                                backColor = hitColor = NuColor.HonorBackColor;
                                trailLength = 15;trailWidth = 8f;
                                despawnEffect = hitEffect = new WaveEffect(){{
                                    sizeFrom = 32f; sizeTo = 8f;
                                    strokeFrom = 1f; strokeTo = 6f;   // 越收越粗
                                    interp = Interp.reverse;
                                    sides = 8; rotation = 30f;        //
                                    lifetime = 20f;
                                    colorFrom = NuColor.HonorColor;
                                    colorTo = NuColor.HonorBackColor;
                                }};
                            }};
                        }};
                    }},
                    NuItems.uranium,new BasicBulletType(4.5f,1000f){{
                        lifetime = 89f;
                        frontColor = lightColor = trailColor = NuColor.SailColor;
                        backColor = hitColor = NuColor.SailBackColor;
                        trailWidth = 3f;
                        trailLength = 15;
                        width = 24f;
                        height = 32f;
                        trailInterval = 3f;
                        status = NuStatus.radiation;statusDuration = 60f*10f;
                        trailEffect = new MultiEffect(new ParticleEffect(){{
                            line = true;
                            strokeFrom = 0.5f; strokeTo = 7f;
                            lenFrom = 6f; lenTo = 32f;
                            cone = 180f;
                            colorFrom = NuColor.SailColor;
                            colorTo = Color.white;
                        }},new WaveEffect(){{
                            interp = Interp.circleOut;
                            lifetime = 20f;sizeFrom = 4f;sizeTo=24f;strokeFrom=4f;strokeTo=0.5f;
                            colorFrom=NuColor.SailColor;colorTo=NuColor.SailBackColor;
                        }});
                        pierce = true; pierceCap = 10;
                        fragBullets = 1;fragRandomSpread = 0f;
                        fragBullet = new BasicBulletType(0f,1000f){{
                            pierce = true; pierceCap = 100;
                            width = 24f;
                            height = 32f;
                            status = NuStatus.radiation;statusDuration = 60f*10f;
                            frontColor = lightColor = trailColor = NuColor.SailColor;
                            backColor = hitColor = NuColor.SailBackColor;
                            lifetime = 80f;
                            intervalBullets = 6;
                            intervalRandomSpread = 360f;
                            bulletInterval = 2f;
                            intervalBullet = new BasicBulletType(5f,100f){{
                                lifetime = 24f;
                                width = 8f;
                                height = 21f;
                                frontColor = lightColor = trailColor = NuColor.SailColor;
                                backColor = hitColor = NuColor.SailBackColor;
                                trailLength = 15;trailWidth = 8f;
                                status = NuStatus.radiation;statusDuration = 60f*10f;
                                despawnEffect = hitEffect = new WaveEffect(){{
                                    sizeFrom = 32f; sizeTo = 8f;
                                    strokeFrom = 1f; strokeTo = 6f;   // 越收越粗
                                    interp = Interp.reverse;
                                    sides = 8; rotation = 30f;        //
                                    lifetime = 20f;
                                    colorFrom = NuColor.SailColor;
                                    colorTo = NuColor.SailBackColor;
                                }};
                            }};
                        }};
                    }},
                    NuItems.pyratite,new BasicBulletType(4.5f,450f){
                        {
                            lifetime = 89f;
                            frontColor = lightColor = trailColor = NuItems.sand.color;
                            backColor = hitColor = NuItems.pyratite.color;
                            trailWidth = 3f;
                            trailLength = 15;
                            width = 24f;
                            height = 32f;
                            status = StatusEffects.burning;
                            statusDuration = 60f * 10f;
                            trailInterval = 3f;
                            trailEffect = new MultiEffect(new ParticleEffect() {{
                                line = true;
                                strokeFrom = 0.5f;
                                strokeTo = 7f;
                                lenFrom = 6f;
                                lenTo = 32f;
                                cone = 180f;
                                colorFrom = NuItems.pyratite.color;
                                colorTo = Color.white;
                            }}, new WaveEffect() {{
                                interp = Interp.circleOut;
                                lifetime = 20f;
                                sizeFrom = 4f;
                                sizeTo = 24f;
                                strokeFrom = 4f;
                                strokeTo = 0.5f;
                                colorFrom = NuItems.pyratite.color;
                                colorTo = NuItems.sand.color;
                            }});
                            pierce = true;
                            pierceCap = 10;
                            fragBullets = 1;
                            fragRandomSpread = 0f;
                            fragBullet = new BasicBulletType(0f, 450f) {{
                                pierce = true;
                                pierceCap = 100;
                                width = 24f;
                                height = 32f;
                                frontColor = lightColor = trailColor = NuItems.sand.color;
                                backColor = hitColor = NuItems.pyratite.color;
                                lifetime = 80f;
                                intervalBullets = 6;
                                status = StatusEffects.burning;
                                statusDuration = 60f * 10f;
                                intervalRandomSpread = 360f;
                                bulletInterval = 2f;
                                intervalBullet = new MultiBulletType(new BasicBulletType(5f, 32f) {{
                                    lifetime = 24f;
                                    width = 8f;
                                    height = 21f;
                                    status = StatusEffects.burning;
                                    statusDuration = 60f * 10f;
                                    frontColor = lightColor = trailColor = NuItems.sand.color;
                                    backColor = hitColor = NuItems.pyratite.color;
                                    trailLength = 15;
                                    trailWidth = 8f;
                                    despawnEffect = hitEffect = new WaveEffect() {{
                                        sizeFrom = 32f;
                                        sizeTo = 8f;
                                        strokeFrom = 1f;
                                        strokeTo = 6f;   // 越收越粗
                                        interp = Interp.reverse;
                                        sides = 8;
                                        rotation = 30f;        //
                                        lifetime = 20f;
                                        colorFrom = NuItems.pyratite.color;
                                        colorTo = NuItems.sand.color;
                                    }};
                                }}, new FireBulletType(5f, 98f) {
                                    {
                                        colorFrom = NuItems.Tcoal.color;
                                        colorMid = NuItems.monoSiliCrystal.color;
                                        colorTo = Items.coal.color;
                                        fireTrailChance = 0f;
                                        radius = 5f;
                                        velMin = speed - 0.5f;
                                        velMax = speed + 1f;
                                        lifetime = 2f;
                                        drag = 0.01f;
                                        fragBullets = 15;
                                        fragRandomSpread = 5f;
                                        fragBullet = new FireBulletType(5f, 35f) {{
                                            colorFrom = NuItems.Tcoal.color;
                                            colorMid = NuItems.monoSiliCrystal.color;
                                            colorTo = Items.coal.color;
                                            fireTrailChance = 0.2f;
                                            radius = 5f;
                                            velMin = speed - 0.5f;
                                            velMax = speed + 1f;
                                            lifetime = 18f;
                                            drag = 0.01f;
                                            collidesTiles = true;
                                            collides = true;
                                        }};
                                    }
                                });
                            }};
                        }},
                            NuItems.remakeSource,new BasicBulletType(4.5f,1200f){{
                            lifetime = 89f;
                            frontColor = lightColor = trailColor = NuColor.PaleColor;
                            backColor = hitColor = NuItems.remakeSource.color;
                            trailWidth = 3f;
                            trailLength = 15;
                            width = 24f;
                            height = 32f;
                            status = NuStatus.enrich;
                            statusDuration = 60f*3f;
                            trailInterval = 3f;
                            trailEffect = new MultiEffect(new ParticleEffect(){{
                                line = true;
                                strokeFrom = 0.5f; strokeTo = 7f;
                                lenFrom = 6f; lenTo = 32f;
                                cone = 180f;
                                colorFrom = NuItems.remakeSource.color;
                                colorTo = Color.white;
                            }},new WaveEffect(){{
                                interp = Interp.circleOut;
                                lifetime = 20f;sizeFrom = 4f;sizeTo=24f;strokeFrom=4f;strokeTo=0.5f;
                                colorFrom=NuColor.PaleColor;colorTo=NuItems.remakeSource.color;
                            }});
                            pierce = true; pierceCap = 10;
                            fragBullets = 1;fragRandomSpread = 0f;
                            fragBullet = new BasicBulletType(0f,1200f){{
                                pierce = true; pierceCap = 100;
                                width = 24f;
                                height = 32f;
                                frontColor = lightColor = trailColor =NuColor.PaleColor;
                                backColor = hitColor =NuItems.remakeSource.color;
                                lifetime = 80f;
                                intervalBullets = 6;
                                intervalRandomSpread = 360f;
                                bulletInterval = 2f;
                                intervalBullet = new MultiBulletType(new BasicBulletType(5f,120f){{
                                    lifetime = 24f;
                                    width = 8f;
                                    height = 21f;
                                    frontColor = lightColor = trailColor =NuColor.PaleColor;
                                    backColor = hitColor = NuItems.remakeSource.color;
                                    trailLength = 15;trailWidth = 8f;
                                    despawnEffect = hitEffect = new WaveEffect(){{
                                        sizeFrom = 32f; sizeTo = 8f;
                                        strokeFrom = 1f; strokeTo = 6f;   // 越收越粗
                                        interp = Interp.reverse;
                                        sides = 8; rotation = 30f;        //
                                        lifetime = 20f;
                                        colorFrom =NuColor.PaleColor;
                                        colorTo = NuItems.remakeSource.color;
                                    }};
                                }},new LaserBulletType(65f){{
                                    colors = new Color[]{NuColor.PaleColor,NuItems.remakeSource.color,Color.white};
                                    length = 145f;width = 3f; sideLength = 32f;sideWidth = 1.5f;
                                    hitSize = 16;lifetime = 20f;
                                    impact = true; collides = true;pierce = true;pierceCap=12;
                                }});
                            }};
                        }}
            );
        }};
        FilthySin = new ItemTurret("FilthySin"){{
            coolantMultiplier = 0.65f;
            liquidCapacity = 600f;
            coolant = consumeCoolant(0.2f);
            consumePower(25f);
            depositCooldown = 2.0f;
            size = 4;
            health = 4000;
            armor = 28;
            shake = 2f;
            reload = 330f;
            ammoPerShot =5;
            maxAmmo = 50;
            shootCone = 10f;
            rotateSpeed = 9f;
            range = 800f;
            targetGround = true;
            targetAir =true;
            shootY = 4f;
            inaccuracy = 5f;
            velocityRnd = 0.12f;
            requirements(Category.turret,with(
                    NuItems.alkSliver,600,
                    NuItems.uranium,600,
                    NuItems.pumice,650,
                    NuItems.sacredIron,350,
                    NuItems.prismCrystal,1000,
                    NuItems.remakeSource,250
            ));
            ammo(
                    NuItems.thallide, new PointBulletType(){{
                        lifetime = 600f;
                        damage = 1000f;
                        speed = 3f;
                        shootEffect = NuFx.HonorInstShoot;
                        smokeEffect = Fx.smokeCloud;
                        hitEffect = new MultiEffect(NuFx.HonorInstHit,
                                new ExplosionEffect(){{
                                    waveColor    = NuColor.HonorColor;
                                    smokeColor   = NuColor.PaleColor;
                                    sparkColor   = NuColor.HonorBackColor;
                                    waveLife     = 8f;
                                    waveStroke   = 3f;
                                    waveRad      = 128f;
                                    waveRadBase  = 0f;
                                    sparkStroke  = 3f;
                                    sparkRad     = 128f;
                                    sparkLen     = 14f;
                                    smokeSize    = 32f;
                                    smokeSizeBase= 1f;
                                    smokeRad     = 160f;
                                    smokes       = 26;
                                    sparks       = 24;
                                    lifetime     = 180f;
                                }}
                        );
                        hitSound = Sounds.explosion;
                        despawnSound = Sounds.explosion;
                        despawnEffect = new MultiEffect(NuFx.HonorInstBomb,
                                new ExplosionEffect(){{
                                    waveColor    = NuColor.HonorColor;
                                    smokeColor   = NuColor.PaleColor;
                                    sparkColor   = NuColor.HonorBackColor;
                                    waveLife     = 8f;
                                    waveStroke   = 3f;
                                    waveRad      = 128f;
                                    waveRadBase  = 0f;
                                    sparkStroke  = 3f;
                                    sparkRad     = 128f;
                                    sparkLen     = 14f;
                                    smokeSize    = 32f;
                                    smokeSizeBase= 1f;
                                    smokeRad     = 160f;
                                    smokes       = 26;
                                    sparks       = 24;
                                    lifetime     = 180f;
                                }}
                        );
                        trailEffect = new MultiEffect(NuFx.HonorInstTrail,
                                new ParticleEffect(){{
                                    particles    = 1;
                                    length       = 20f;
                                    interp       = Interp.circleOut;
                                    sizeInterp   = Interp.circleIn;
                                    colorFrom    = NuColor.HonorColor.a(0.53f);
                                    colorTo      = NuColor.HonorColor;
                                    sizeFrom     = 4f;
                                    sizeTo       = 0f;
                                    lifetime     = 60f;
                                    layer        = 100f;
                                }}
                        );
                        trailInterval = 20f;
                        buildingDamageMultiplier = 0.5f;
                        status = NuStatus.paralysis;
                        statusDuration = 600f;
                        splashDamage = 2500f;
                        scaledSplashDamage = true;
                        splashDamageRadius = 80f;
                        hitShake = 10f;
                        despawnHit = false;
                        makeFire = true;
                        fragBullets = 2;fragRandomSpread = 0f;
                        fragBullet = new ExplosionBulletType(600f,80f){{
                            killShooter = false;
                            despawnEffect = Fx.none;
                        }};
                    }},
                    NuItems.uranium, new RailBulletType(){{
                        lifetime = 600f;
                        damage = 5800f;
                        speed = 3f;
                        shootEffect = NuFx.SailInstShoot;
                        hitEffect = NuFx.SailInstHit;
                        pierceEffect = Fx.railHit;
                        smokeEffect = Fx.smokeCloud;
                        pointEffect = NuFx.SailInstTrail;
                        despawnEffect = NuFx.SailExplosion;
                        hitSound = Sounds.explosion;
                        despawnSound = Sounds.explosion;
                        buildingDamageMultiplier = 0.6f;
                        status = NuStatus.radiation;
                        statusDuration = 150f;
                        scaledSplashDamage = true;
                        hitShake = 10f;
                        length = 880f;
                        rangeChange = 80f;
                        despawnHit = false;
                        makeFire = true;
                    }},
                    NuItems.bottledMagenticStorm, new RailBulletType(){{
                        lifetime = 600f;
                        damage = 4500f;
                        speed = 3f;
                        shootEffect = NuFx.EnergyInstShoot;
                        hitEffect = NuFx.EnergyInstHit;
                        pierceEffect = Fx.railHit;
                        smokeEffect = Fx.smokeCloud;
                        pointEffect = NuFx.EnergyInstTrail;
                        despawnEffect = NuFx.EnergyExplosion;
                        hitSound = Sounds.explosion;
                        despawnSound = Sounds.explosion;
                        length = 800f;
                        buildingDamageMultiplier = 0.8f;
                        status = NuStatus.paralysis;
                        statusDuration = 600f;
                        splashDamage = 3280f;
                        scaledSplashDamage = true;
                        splashDamageRadius = 128f;
                        hitShake = 10f;
                        despawnHit = false;
                        makeFire = true;
                        fragBullets = 9;fragRandomSpread = 360f;
                        fragBullet = new BasicBulletType(6f,80f){{
                           width = 20f;height = 20f;sprite = "circle-bullet";
                           lifetime = 5f;
                           frontColor=lightColor=hitColor= NuColor.EnergyColor;
                           backColor=hitColor=NuColor.EnergyBackColor;
                           fragBullets = 1;fragRandomSpread = 0f;
                           fragBullet = new BasicBulletType(0f,80f){{
                               width = 20f;height = 20f;sprite = "circle-bullet";
                               lifetime = 120f;pierce = true;pierceCap = 100;
                               frontColor=lightColor=hitColor= NuColor.EnergyColor;
                               backColor=hitColor=NuColor.EnergyBackColor;
                               intervalBullets = 6;intervalRandomSpread=360f;
                               bulletInterval = 2f;intervalBullet = new LightningBulletType(){{
                                   damage = 65f;
                                   lifetime = 20f;
                                   status = StatusEffects.shocked;statusDuration=60f*lifetime;
                                   lightningLength = 10;
                                   lightningLengthRand = 2;
                                   lightningColor = NuColor.EnergyColor;
                               }};
                           }};
                        }};
                    }}
            );
        }};
        EverVictorious = new ItemTurret("EverVictorious"){{
            consumeLiquid(NuLiquid.strangeLiquid,0.5f);
            size = 4;
            health = 4000;
            armor = 28;
            shake = 2f;
            reload = 400f;
            ammoPerShot =5;
            maxAmmo = 50;
            shootCone = 10f;
            rotateSpeed = 9f;
            range = 800f;
            targetGround = true;
            targetAir =true;
            shootY = 4f;
            inaccuracy = 5f;
            velocityRnd = 0.12f;
            shoot = new ShootPattern(){{
               shots = 2;
               shotDelay = 150f;
            }};
            requirements(Category.turret,with(
                    NuItems.alkSliver,750,
                    NuItems.thallide,600,
                    NuItems.pumice,650,
                    NuItems.sacredIron,350,
                    NuItems.prismCrystal,1000
            ));
            ammo(
                    NuItems.thallide,new BulletType(0f,0f){{
                            shootEffect = Fx.shootBig;
                            smokeEffect = Fx.shootSmokeMissileColor;
                            hitColor = Pal.redLight;
                            ammoMultiplier = 1f;
                            spawnUnit = FederalUnitTypes.smokeLeaf;
                        }},
                    NuItems.uranium,new BulletType(0f,0f){{
                        shootEffect = Fx.shootBig;
                        smokeEffect = Fx.shootSmokeMissileColor;
                        hitColor = Pal.redLight;
                        ammoMultiplier = 1f;
                        spawnUnit = FederalUnitTypes.greyLeaf;
                    }},
                    NuItems.sacredIron,new BulletType(0f,0f){{
                        shootEffect = Fx.shootBig;
                        smokeEffect = Fx.shootSmokeMissileColor;
                        hitColor = Pal.redLight;
                        ammoMultiplier = 1f;
                        spawnUnit = FederalUnitTypes.despLeaf;
                        rangeChange =-32f;
                        reloadMultiplier = 0.75f;
                    }},
                    NuItems.remakeSource,new BulletType(0f,0f){{
                        shootEffect = Fx.shootBig;
                        smokeEffect = Fx.shootSmokeMissileColor;
                        hitColor = Pal.redLight;
                        ammoMultiplier = 1f;
                        spawnUnit = FederalUnitTypes.despLeaf;
                        rangeChange =56f;
                        reloadMultiplier = 1.5f;
                    }}
            );
        }};
        SoarDragon = new ItemTurret("SoarDragon"){{
            size = 5;
            health = 5000;
            armor = 45;
            shake = 2f;
            reload = 90f;
            ammoPerShot =5;
            maxAmmo = 75;
            shootCone = 10f;
            rotateSpeed = 9f;
            range = 800f;
            targetGround = true;
            targetAir =true;
            shootY = 4f;
            inaccuracy = 5f;
            velocityRnd = 0.12f;
        }};


        bigIronDrill = new Drill("bigIronDrill"){{
            requirements(Category.production, with(NuItems.bigIron, 10));
            drillTime = 60*4.5f;
            size = 2;
            health = 500;
            tier = 3;
            hardnessDrillMultiplier = 200f;
            itemCapacity = 20;
            hasItems = hasLiquids = true;
            consumeLiquid(Liquids.water, 2/60f).boost();
            liquidBoostIntensity = 1.2f;
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
        floatBaseDrill =new Drill("floatBaseDrill"){{
            requirements(Category.production, with(
                    NuItems.pumice,20,
                    Items.graphite,15
            ));
            drillTime = 60f * 3f;
            size = 2;
            hasPower = true;
            tier = 3;
            health = 1500;
            drillEffect = new MultiEffect(Fx.mineImpact,Fx.mineImpactWave.wrap(NuItems.pumice.color, 40f));
            itemCapacity = 100;
            researchCostMultiplier = 1f;
            drillMultipliers.put(NuItems.rubberFrag, 2f);
            liquidBoostIntensity = 1.4f;
            fogRadius = 10;
            consumeLiquid(Liquids.water, 4/60f).boost();
        }};
        floatDrill = new BurstDrill("floatDrill"){{
            requirements(Category.production, with(
                    NuItems.pumice,50,
                    NuItems.monoSiliCrystal,40,
                    Items.graphite,45
            ));
            drillTime = 60f * 5f;
            size = 3;
            hasPower = true;
            tier = 4;
            health = 1500;
            drillEffect = new MultiEffect(Fx.mineImpact,Fx.drillSteam, Fx.mineImpactWave.wrap(NuItems.pumice.color, 40f));
            shake = 4f;
            arrows = 2;arrowSpacing = 2.5f;arrowOffset = 0f;
            //arrowColor = NuColor.PaleColor;glowColor = Color.white;
            //baseArrowColor = NuItems.pumice.color;
            itemCapacity = 100;
            researchCostMultiplier = 1f;
            drillMultipliers.put(NuItems.rubberFrag, 2f);
            liquidBoostIntensity = 2f;
            fogRadius = 10;
            consumePower(5f);
            consumeLiquid(NuLiquid.strangeLiquid, 3/60f).boost();
            // — 修正 arrow 贴图像素位置：
            //   BurstDrill 内部硬编码把 arrowRegion 的 PNG 几何中心对齐 rotator 中心，
            //   但 floatDrill-arrow.png 的箭头像素画在 PNG 右上，所以通过 FloatDrill 的
            //   arrowOffsetX / arrowOffsetY 把它往左下拖回中心（单位=像素，1 tile = 32 像素）。
            //   可按实际视觉再微调。
        }};
        sliverDrill = new Drill("sliverDrill"){{
            requirements(Category.production, with(
                    NuItems.pumice, 60,
                    NuItems.magent,25,
                    NuItems.alkSliver,35
            ));
            drillTime = 60*2.5f;
            size = 3;
            health = 2000;
            tier = 4;
            hardnessDrillMultiplier = 4f;
            liquidBoostIntensity = 1.6f;
            itemCapacity = 200;
            hasItems = hasLiquids = true;
            drillEffect =Fx.mineHuge;
            consumePower(7f);
            consumeLiquid(NuLiquid.strangeLiquid, 4/60f).boost();
        }};
        nuclearDrill = new Drill("nuclearDrill"){{
            requirements(Category.production, with(
                    NuItems.pumice, 100,
                    NuItems.magent,80,
                    NuItems.uranium,10,
                    NuItems.monoSiliCrystal,90
            ));
            drillTime = 80f;
            size = 4;
            health = 2000;
            tier = 6;
            hardnessDrillMultiplier = 8f;
            liquidBoostIntensity = 1.8f;
            itemCapacity = 200;
            hasItems = hasLiquids = true;
            drillEffect = Fx.mineHuge;
            consumePower(16f);
            consumeLiquid(NuLiquid.liquidOxygen, 2.45f/60f).boost();
        }};
        prismDrill = new BeamDrill("prismDrill"){{
            requirements(Category.production, with(
                    NuItems.pumice, 40,
                    NuItems.rubber,25,
                    NuItems.prismCrystal,50
            ));
            consumePower(1.25f);
            drillTime = 45f;
            itemCapacity = 40;
            heatColor = glowColor = sparkColor = NuColor.HeatColor;boostHeatColor = NuItems.prismCrystal.color;
            tier = 5;
            size = 3;
            range = 10;
            fogRadius = 12;
            consumeLiquid(NuLiquid.prismLiquid, 10f / 60f).boost();
        }};
        hotMeltDrill = new BeamDrill("hotMeltDrill"){{
            requirements(Category.production, with(
                    Items.graphite, 40,
                    NuItems.monoSiliCrystal,25,
                    NuItems.pumice,50
            ));
            consumePower(0.5f);
            drillTime = 100f;
            tier = 4;
            heatColor = NuColor.HeatColor;
            boostHeatColor = NuItems.monoSiliCrystal.color;
            size = 2;
            range = 6;
            fogRadius = 8;
            consumeLiquid(NuLiquid.strangeLiquid, 0.05f / 60f).boost();
        }};
        waterSamplingDevice = new SolidPump("waterSamplingDevice"){{
            requirements(Category.production, with(
                    NuItems.frailPolyester,30,
                    Items.graphite, 30,
                    NuItems.bigIron,45
            ));
            result = Liquids.water;
            pumpAmount = 11/60f;
            size = 1;
            liquidCapacity = 100f;
            rotateSpeed = 2f;
            attribute = Attribute.water;
            envRequired |= Env.groundWater;
            consumePower(1.25f);
        }};
        wallCrusher = new WallCrafter("wallCrusher"){{
            requirements(Category.production, with(
                    Items.graphite,65,
                    NuItems.monoSiliCrystal,100,
                    NuItems.bigIron,75
            ));
            consumePower(3/60f);
            drillTime = 60f;
            size = 2;
            attribute = Attribute.sand;
            output = NuItems.sand;
            fogRadius = 3;
            ambientSound = Sounds.loopDrill;
            ambientSoundVolume = 0.04f;
        }};
        droneDrill = new DroneHarvester("droneDrill"){{
            size = 3;
            health = 1200;
            mineCooldown = 40f;
            areaSize = 7;              // 组装区域 7×7 格
            dronesCreated = 8;         // 4 架无人机
            drillTier = 4;             // 可采硬度 ≤1 的矿物（铜/铅）
            mineSpeed = 0.4f;         // 每帧采挖进度（约 1.5s 采满 1 单位）
            droneSpeed = 0.45f;        // 无人机飞行速度（插值系数）
            droneBuildTime = 60f*3f; // 造满 4 架 drone 需要 5 秒
            blacklist.add(Items.thorium);  // 钍矿不采
            consumePower(2f);
         requirements(Category.production, with(NuItems.bigIron, 200,NuItems.monoSiliCrystal, 100));
        }};
        rubberCrusher = new WallCrafter("rubberCrusher"){{
            requirements(Category.production, with(
                    NuItems.pumice,90,
                    NuItems.monoSiliCrystal,135,
                    NuItems.magent,90,
                    Items.graphite,60
            ));
            consumePower(45/60f);
            drillTime = 210f;
            size = 3;
            attribute = NuAttribute.oriRubber;
            output = NuItems.oriRubber;
            fogRadius = 6;
            ambientSound = Sounds.loopDrill;
            ambientSoundVolume = 0.04f;
        }};
        uranCrystalCrusher = new WallCrafter("uranCrystalCrusher"){{
            requirements(Category.production, with(
                    NuItems.pumice,80,
                    NuItems.frailPolyester,100,
                    NuItems.monoSiliCrystal,150,
                    NuItems.rubber,60
            ));
            consumePower(1f);
            drillTime = 180f;
            size = 3;
            attribute = NuAttribute.uranCrystal;
            output = NuItems.uranCrystal;
            fogRadius = 4;
            ambientSound = Sounds.loopDrill;
            ambientSoundVolume = 0.04f;
        }};
        fifthCoagulator = new AttributeCrafter("fifthCoagulator"){{
            requirements(Category.production, with(
                    NuItems.bigIron, 25,
                    NuItems.monoSiliCrystal, 25,
                    Items.graphite,10));
            outputItem = new ItemStack(NuItems.dirtyCoagulum,2);
            craftTime = 60;
            size = 2;
            hasLiquids = true;
            hasPower = true;
            hasItems = true;
            liquidCapacity = 80f;
            craftEffect = Fx.none;
            attribute = NuAttribute.dirty;
            ambientSound = Sounds.loopCultivator;
            ambientSoundVolume = 0.075f;
            baseEfficiency = 0.6f;
            legacyReadWarmup = true;
            drawer = new DrawMulti(
                    new DrawRegion("-bottom"),
                    new DrawLiquidTile(Liquids.water,1),
                    new DrawDefault(),
                    new DrawCultivator(){{
                        plantColor = NuItems.dirtyCoagulum.color;
                        plantColorLight = NuLiquid.dirtySolution.color;
                    }},
                    new DrawRegion("-top")
            );
            maxBoost = 12f;
            consumePower(125f/60f);
            consumeLiquid(Liquids.water, 25f/60f);
        }};
        prismVenter = new AttributeCrafter("prismVenter"){{
            requirements(Category.production, with(Items.graphite,100,NuItems.rubber,75,NuItems.alkSliver,30));
            attribute = Attribute.steam;
            group = BlockGroup.liquids;
            minEfficiency = 9f - 0.0001f;
            baseEfficiency = 0f;
            displayEfficiency = false;
            craftEffect = Fx.turbinegenerate;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawBlurSpin("-rotator", 6f), new DrawRegion("-mid"), new DrawLiquidTile(Liquids.water, 38f / 4f), new DrawDefault());
            craftTime = 120f;
            size = 3;
            ambientSound = Sounds.loopHum;
            ambientSoundVolume = 0.06f;
            hasLiquids = true;
            boostScale = 1f / 9f;
            itemCapacity = 0;
            outputLiquid = new LiquidStack(NuLiquid.prismLiquid, 100/60f);
            consumePower(4f);
            liquidCapacity = 60f;
        }};
    }
}