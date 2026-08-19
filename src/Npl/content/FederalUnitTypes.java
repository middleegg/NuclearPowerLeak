package Npl.content;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.struct.IntMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.ai.types.*;
import mindustry.ai.*;
import mindustry.audio.SoundLoop;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.part.DrawPart;
import mindustry.entities.part.HaloPart;
import mindustry.entities.part.RegionPart;
import mindustry.entities.part.ShapePart;
import mindustry.entities.pattern.*;
import mindustry.entities.units.WeaponMount;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.MultiPacker;
import mindustry.graphics.Pal;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.type.unit.*;
import mindustry.type.weapons.PointDefenseWeapon;
import mindustry.type.weapons.RepairBeamWeapon;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.Env;
import Npl.content.*;
import Npl.newSth.Type.*;
import Npl.newSth.AI.*;
import Npl.newSth.*;
import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;
import static mindustry.Vars.*;

public class FederalUnitTypes{
    public static  UnitType
   //core's,存活，复生
   survive,resurrection,
   //desp's，卑劣，耻辱，迷失，妄言，懦叛，绝望
    vile,shame,loss,nonsense,cowardTraitor,desperate,
   //honor's，荣耀，高傲，虚荣，谬赞，愚忠，守权
    honor,proud,vanity,overPraise,blindLoyalty,safeguardRights,
   //sail's，水手，游弋，浪客，扬帆，船长，尼莫
    sailor,cruise,wanderer,setsails,captain,nemo,
   //pale's Ground，苍白，涟漪，禄途，忠挽，圣骑
    pale,ripple,greatPath,loyalRequest,paladin,
   //pale's Flying，晨光，日霞，黄昏，吞昼，月华
    mornLight,sunsetGlow,dusk,swallowingDay,moonLight,
   //pale's Hovering，纯玉，暗枫，辉鸦，圣徒，血莲
    pureJade,darkMaple,brightCrow,saint,bloodLotus,
   //special
    hometown,amicable,confucianScholar,subjects,doc,jargon,humorous;
    public static Weapon truly;
    public static void load(){

        survive = new UnitType("survive"){{
            constructor =  UnitEntity::create;
            EntityMapping.nameMap.put(name,constructor);
            flying = true;
            drawCell = true;
            circleTarget = true;
            itemCapacity = 45;
            health = 1200;
            speed = 4f;
            trailLength = 8;
            researchCostMultiplier = 0.50f;
            flyingLayer = Layer.flyingUnit;
            mineItems = Seq.with(NuItems.bigIron,NuItems.frailPolyester,NuItems.sulFurFrag);
            armor = 14;
            hitSize = 12f;
            mineTier = 3;
            buildRange = 480f;
            drag = 0.1f;
            buildSpeed = 3.5f;
            mineSpeed = 4.5f;
            alwaysUnlocked = true;
            isEnemy = true;
            abilities.add(new ShieldRegenFieldAbility(20f,1000f,120f,50f));
            weapons.add(new Weapon("breakBullet-l"){{
                x = -6.5f;
                y = -1f;
                reload = 120f;
                shootCone = 20f;
                range = 125f;
                velocityRnd = 0.2f;
                inaccuracy = 1f;
                mirror = false;
                shoot = new ShootSpread(3,4f);
                shootY = 1f;
                shootX = -1f;
                recoil = 2.5f;
                bullet = new MissileBulletType(5f,80f){{
                    maxRange = 120f;
                    collides = true;
                    collidesAir = true;
                    collidesGround = true;
                    collidesTeam = true;
                    height = 16f;
                    width = 15f;
                    lifetime = 25f;
                    buildingDamageMultiplier = 0.01f;
                    hitEffect = despawnEffect = new WaveEffect(){{
                        strokeFrom = 2.5f;
                        strokeTo = 0f;
                        colorFrom = NuColor.SurvivalColor;
                        colorTo = NuColor.SurvivalBackColor;
                        sizeFrom = 12f;
                        sizeTo = 1f;
                        lifetime =12f;
                        sides = 12;
                    }};
                    healAmount = 25f;
                    lightColor = frontColor = trailColor = NuColor.SurvivalColor;
                    backColor = hitColor = NuColor.SurvivalBackColor;
                    trailInterval = 2.5f;
                    trailEffect = NuFx.survivalEnergyTail;
                }};
            }});
            weapons.add(new Weapon("breakBullet-r"){{
                x = 6.5f;
                y = -1f;
                reload = 120f;
                shootCone = 20f;
                range = 125f;
                mirror = false;
                velocityRnd = 0.2f;
                inaccuracy = 1f;
                shoot = new ShootSpread(3,4f){{
                    firstShotDelay = 60f;
                }};
                shootY = 1f;
                shootX = -1f;
                recoil = 2.5f;
                bullet = new MissileBulletType(5f,80f){{
                    maxRange = 120f;
                    collides = true;
                    collidesAir = true;
                    collidesGround = true;
                    collidesTeam = true;
                    height = 16f;
                    width = 15f;
                    lifetime = 25f;
                    buildingDamageMultiplier = 0.01f;
                    hitEffect = despawnEffect = new WaveEffect(){{
                        strokeFrom = 2.5f;
                        strokeTo = 0f;
                        colorFrom = NuColor.BloodColor;
                        colorTo = NuColor.BloodBackColor;
                        sizeFrom = 12f;
                        sizeTo = 1f;
                        lifetime =12f;
                        sides = 12;
                    }};
                    healAmount = 25f;
                    lightColor = frontColor = trailColor = NuColor.BloodColor;
                    backColor = hitColor = NuColor.BloodBackColor;
                    trailInterval = 2.5f;
                    trailEffect = NuFx.bloodEnergyTail;
                }};
            }});
        }};
        resurrection = new UnitType("resurrection"){{
            constructor =  UnitEntity::create;
            EntityMapping.nameMap.put(name,constructor);
            flying = true;
            drawCell = true;
            circleTarget = true;
            itemCapacity = 70;
            health = 2000;
            speed = 4.5f;
            flyingLayer = Layer.flyingUnit;
            armor = 16;
            hitSize = 16f;
            targetAir = true;
            mineWalls = true;
            mineTier = 4;
            buildRange = 560f;
            trailLength = 12;
            drag = 0.1f;
            buildSpeed = 4f;
            mineSpeed = 5f;
            isEnemy = true;
            mineItems = Seq.with(NuItems.bigIron,NuItems.frailPolyester,NuItems.sulFurFrag,NuItems.pumice);
            faceTarget = true;
            abilities.add(new RepairFieldAbility(120f,120f,64f,0.5f));
            abilities.add(new EnergyFieldAbility(30f,60f,80f){{
                healEffect = Fx.heal;
                hitEffect = Fx.hitLaserBlast;
                damageEffect = Fx.chainLightning;
                status = StatusEffects.shocked;
                statusDuration = 60f*10f;
                x = 0f;
                y = 0f;
                targetAir = true;
                targetGround = true;
                maxTargets = 80;
                healPercent = 0.5f;
                displayHeal = true;
                effectRadius = 4f;
                color = NuColor.CoreConColor;
                rotateSpeed = 10f;
                hitBuildings = false;
            }});
            weapons.add(new Weapon("laserOzne"){{
                 x = 0f;
                 y = 3f;
                 top = true;
                 range = 160f;
                 reload = NuFx.LaserChargeSmallCore.lifetime * 2.1f;
                 shootCone = 10f;
                shootStatusDuration = NuFx.LaserChargeSmallCore.lifetime + 30f;
                shootStatus = StatusEffects.unmoving;
                shoot.firstShotDelay = NuFx.LaserChargeSmallCore.lifetime;
                parentizeEffects = true;
                bullet = new LaserBulletType(300f){{
                    chargeEffect = NuFx.LaserChargeSmallCore;
                    length = 170f;
                    width = 15f;
                    lifetime = 65f;
                    largeHit = true;
                    lightColor = lightningColor = NuColor.DespColor;
                    healAmount = 60f;
                    collidesTeam = true;
                    sideAngle = 15f;
                    sideWidth = 0f;
                    sideLength = 0f;
                    buildingDamageMultiplier = 0.01f;
                    colors = new Color[]{NuColor.CoreColor,NuColor.CoreBackColor,Color.white};
                }};
            }});
            weapons.add(new Weapon("hardShoot-l"){{
                x = -9f;
                y = 1.5f;
                reload = NuFx.chargeRing.lifetime*1.5f;
                shootCone = 120f;
                shoot = new ShootPattern(){{
                   shots = 4;
                   shotDelay = NuFx.chargeRing.lifetime/6;
                }};
                mirror = false;
                rotate = true;
                range = 140f;
                bullet = new BasicBulletType(4f,60f){{
                    frontColor = lightColor = trailColor = NuColor.SurvivalColor;
                    backColor = hitColor = NuColor.SurvivalBackColor;
                    width = 12f;
                    height = 12f;
                    sprite = "circle-bullet";
                    homingPower = 0.4f;
                    homingRange = 80f;
                    homingDelay = 10f;
                    trailLength = 10;
                    trailWidth = 4f;
                    trailInterval = 4f;
                    trailEffect  = NuFx.survivalEnergyTail;
                    healAmount = damage*0.4f;
                    hitEffect = despawnEffect = new WaveEffect(){{
                        strokeFrom = 2.5f;
                        strokeTo = 0f;
                        colorFrom = NuColor.SurvivalColor;
                        colorTo = NuColor.SurvivalBackColor;
                        sizeFrom = 12f;
                        sizeTo = 1f;
                        lifetime =12f;
                        sides = 12;
                    }};
                }};
            }});
            weapons.add(new Weapon("hardShoot-r"){{
                x = 9f;
                y = 1.5f;
                reload = NuFx.chargeRing.lifetime*1.5f;
                shootCone = 120f;
                shoot = new ShootPattern(){{
                    shots = 4;
                    shotDelay = NuFx.chargeRing.lifetime/6;
                }};
                mirror = false;
                rotate = true;
                range = 140f;
                bullet = new BasicBulletType(4f,60f){{
                    frontColor = lightColor = trailColor = NuColor.BloodColor;
                    backColor = hitColor = NuColor.BloodBackColor;
                    width = 12f;
                    height = 12f;
                    sprite = "circle-bullet";
                    homingPower = 0.4f;
                    homingRange = 80f;
                    homingDelay = 10f;
                    trailLength = 10;
                    trailWidth = 4f;
                    trailInterval = 4f;
                    trailEffect  = NuFx.bloodEnergyTail;
                    healAmount = damage*0.4f;
                    hitEffect = despawnEffect = new WaveEffect(){{
                        strokeFrom = 2.5f;
                        strokeTo = 0f;
                        colorFrom = NuColor.BloodColor;
                        colorTo = NuColor.BloodBackColor;
                        sizeFrom = 12f;
                        sizeTo = 1f;
                        lifetime =12f;
                        sides = 12;
                    }};
                }};
            }});
        }};
        vile = new UnitType("vile"){{
            constructor =  UnitEntity::create;
            EntityMapping.nameMap.put(name,constructor);
            flying = true;
            drawCell = true;
            circleTarget = true;
            itemCapacity = 1;
            health = 1000;
            speed = 3.5f;
            researchCostMultiplier = 0.50f;
            flyingLayer = Layer.flyingUnit;
            armor = 7;
            hitSize = 7f;
            alwaysUnlocked = false;
            targetAir = false;
            maxRange = 40f;
            // 【最简写法】和你写 InterceptFieldAbility 一样，用双大括号（匿名子类）最顺手
            // ⬇️⬇️⬇️ FederalUnitTypes.java 里，和 InterceptFieldAbility 写在一起 ⬇️⬇️⬇️
            abilities.add(new InvisibleAbility(){{
                fullyInvisible       = true;     // ★ 开启完全隐身（单位本体/腿/底座/阴影都看不到）
                visibleToAllies      = true;     // 队友还能正常看到（便于协同），false = 队友也完全看不见
                stealthDotSize       = 0f;       // 0 = 真·完全隐身 1 个像素点都不留；1.2f = 留个针尖绿点提示位置
                revealDamageDuration = 60f * 4;  // 被打中显形 4 秒，想长点就调大
            }});
            targetFlags = new BlockFlag[]{BlockFlag.battery,BlockFlag.drill,BlockFlag.shield};
            weapons.add(new Weapon("evil"){{
                x = 0f;
                y = 0f;
                top = false;
                display = false;
                shootY = 4f;
                mirror = true;
                reload = 75f;
                ignoreRotation = true;
                shootCone = 360f;
                shootSound = Sounds.shootHorizon;
                shoot = new ShootPattern(){{
                   shots = 4;
                   shotDelay = 8f;
                }};
                bullet = new BombBulletType(50f, 4f){{
                    width = 45f;
                    height = 45f;
                    maxRange = 4f;
                    sprite = "nu-bigDan";
                    hitEffect= despawnEffect = Fx.flakExplosion;
                    shootEffect = Fx.none;
                    smokeEffect = NuFx.PaleSmoke;
                    status = StatusEffects.blasted;
                    statusDuration = 75f;
                    damage = splashDamage * 0.8f;
                    frontColor = lightColor = trailColor = NuColor.BombColor;
                    backColor = NuColor.BombBackColor;
                    fragBullets = 4;
                    fragRandomSpread = 360f;
                    fragBullet = new LightningBulletType(){{
                        damage = 10;
                        collidesAir = true;
                        ammoMultiplier = 1.3f;
                        lightningColor = NuColor.BombColor;
                        lightningLength = 6;
                        lightningLengthRand = 3;
                    }};
                }};
            }});
        }};
        shame = new UnitType("shame"){{
            constructor =  UnitEntity::create;
            EntityMapping.nameMap.put(name,constructor);
            flying = true;
            drawCell = true;
            circleTarget = false;
            health = 4600;
            speed = 2.2f;
            accel = 0.1f;
            drag = 0.12f;
            itemCapacity = 45;
            researchCostMultiplier = 0.75f;
            flyingLayer = Layer.flyingUnit;
            armor = 12;
            hitSize = 13f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 180f;
            weapons.add(new Weapon("sword of shame"){{
                x = 0f;
                y = 8f;
                shootY = 10f;
                shootCone = 15f;
                top = true;
                reload = 45f;
                range = 180f;
                mirror = true;
                alternate = false;
                shootSound = Sounds.shootTank;
                shoot = new ShootPattern(){{
                   shots = 3;
                   shotDelay = 12f;
                }};
                bullet = new BasicBulletType(4f,80f){{
                    lifetime = 35f;
                    height = 24f;
                    width = 18f;
                    frontColor = lightColor = trailColor = NuColor.DespColor;
                    backColor = NuColor.DespBackColor;
                    maxRange = 180f;
                    trailLength = 12;
                    trailWidth = 2.8f;
                    trailInterval = 4f;
                    trailEffect = NuFx.despEnergyTail;      // ★ 蓝紫能量尾（发光强、粒子多、够华丽）
                    despawnEffect = hitEffect = new expEffect(12f, 32f){{
                        lightColor = NuColor.DespColor;
                        particles = 16;
                        sizeFrom = 0f;
                        sizeTo = 32f;
                    }};
                    fragBullets = 4;
                    fragRandomSpread = 360f;
                    fragBullet = new BasicBulletType(5f,32f){{
                        lifetime = 10f;
                        frontColor = lightColor = trailColor = NuColor.DespColor;
                        backColor = NuColor.DespBackColor;
                        height = 18f;
                        width = 12f;
                        trailLength = 9;
                        trailWidth = 1.8f;
                        despawnEffect = hitEffect = NuFx.bulletHitSmall;
                        homingPower = 1f;
                        homingDelay = 1f;
                        homingRange = 80f;
                    }};
                }};
            }});
        }};
        loss = new UnitType("loss"){{
            constructor =  UnitEntity::create;
            EntityMapping.nameMap.put(name,constructor);
            flying = true;
            drawCell = true;
            circleTarget = false;
            health = 9000;
            speed = 1.65f;
            accel = 0.21f;
            drag = 0.2f;
            itemCapacity = 90;
            researchCostMultiplier = 1f;
            flyingLayer = Layer.flyingUnit;
            armor = 18;
            hitSize = 20f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 210f;
            weapons.add(new Weapon("shadow of loss"){{
                x = 4f;
                y = 15f;
                shootY = 8f;
                shootCone = 15f;
                mirror = true;
                range = 145f;
                targetAir = true;
                targetGround = true;
                maxRange = 150f;
                reload = 60f;
                alternate = false;
                shootSound = Sounds.shootFuse;
                bullet = new ShrapnelBulletType(){{
                    length = 150f;
                    width = 10f;
                    fromColor= NuColor.DespColor;
                    toColor = NuColor.DespBackColor;
                    lifetime = 24f;
                }};
            }});
            weapons.add(new Weapon("inpast olfriend"){{
                x = 2f;
                y = 0f;
                shootCone = 30f;
                range = 200f;
                top = true;
                reload = 60f;
                mirror = true;
                alternate = true;
                shootSound = Sounds.shootArc;
                shoot = new ShootPattern(){{
                    shots = 3;
                    shotDelay = 9f;
                }};
                bullet = new BasicBulletType(4f,45f){{
                    pierce = true;
                    pierceArmor = true;
                    pierceCap =4;
                    width = 20f;
                    height = 20f;
                    sprite = "circle-bullet";
                    lifetime = 50f;
                    frontColor = lightColor = trailColor = NuColor.DespColor;
                    backColor = NuColor.DespBackColor;
                    despawnEffect = hitEffect = NuFx.bulletHitSmall;
                    intervalBullets = 2;
                    bulletInterval = 2f;
                    intervalBullet = new LightningBulletType(){{
                        damage = 24;
                        collidesAir = true;
                        ammoMultiplier = 1.3f;
                        lightningColor = NuColor.BombColor;
                        lightningLength = 9;
                        lightningLengthRand = 3;
                    }};
                }};
            }});
            weapons.add(new Weapon(){{
                x =  4f;
                y = -8f;
                mirror = true;
                rotate = true;
                range = 220f;
                top = true;
                reload = 40f;
                alternate = false;
                shootSound = Sounds.shoot;
                shootCone = 30f;
                shoot = new ShootPattern(){{
                    shots = 3;
                    shotDelay = 10f;
                }};
                bullet = new BasicBulletType(5f,65f){{
                    lifetime = 44f;
                    width = 14f;
                    height = 20f;
                    frontColor = lightColor = trailColor = NuColor.DespColor;
                    backColor = NuColor.DespBackColor;
                    despawnEffect = hitEffect = NuFx.bulletHitSmall;
                    homingPower = 1f;
                    homingDelay = 4f;
                    weaveMag = 10f;
                    weaveScale = 5f;
                    homingRange = 200f;
                }};
            }});
        }};
        nonsense = new UnitType("nonsense"){{
            constructor =  UnitEntity::create;
            EntityMapping.nameMap.put(name,constructor);
            flying = true;
            drawCell = true;
            circleTarget = false;
            health = 19000;
            speed = 1.3f;
            accel = 0.25f;
            drag = 0.2f;
            itemCapacity = 145;
            researchCostMultiplier = 1.25f;
            flyingLayer = Layer.flyingUnit;
            armor = 23;
            hitSize = 29f;
            alwaysUnlocked = false;
            targetAir = true;
            fogRadius = 240f;
            immunities.addAll(StatusEffects.burning, StatusEffects.wet);
            targetFlags = new BlockFlag[]{BlockFlag.factory, BlockFlag.storage, BlockFlag.battery, null};
            maxRange = 480f;
            abilities.add(new EnergyFieldAbility(200f,90f,240f){{
                status = StatusEffects.burning;
                statusDuration = 60f*12f;
                x = 0f;
                y = 0f;
                targetAir = true;
                targetGround = true;
                hitBuildings = false;
                hitUnits = true;
                healPercent = 0.05f;
                maxTargets = 240;
                color = NuColor.DespColor;
            }});
            abilities.add(new RegenAbility(){{
                percentAmount = 0f;
                amount = 2f;
            }});
                weapons.add(new Weapon("something for nothing") {{
                    reload = 60;
                    x = 10f;
                    y = 8f;
                    mirror = true;
                    rotate = true;
                    top = true;
                    reload = 60f;
                    recoil = 0.5f;
                    shake = 0.3f;
                    range = 400f;
                    shootSound = Sounds.shootAvert;
                    shoot = new ShootSpread(3, 5f);
                    bullet = new BasicBulletType(3f, 800f) {{
                        lifetime = 70f;
                        maxRange = 400f;
                        width = 20f;
                        height = 28f;
                        shootEffect = Fx.shootSmokeSquareBig;
                        smokeEffect = Fx.shootSmokeDisperse;
                        frontColor = lightColor = trailColor = NuColor.DespColor;
                        backColor = hitColor = NuColor.DespBackColor;
                        trailInterval = 2f;
                        trailLength = 24;
                        trailWidth = 7.5f;
                        trailEffect = new MultiEffect(NuFx.sniperGlowTail, new SeqEffect(new WaveEffect() {{
                            interp = Interp.circleOut;
                            lifetime = 16f;
                            sizeFrom = 1f;
                            sizeTo = 8f;
                            strokeFrom = 2f;
                            strokeTo = 0f;
                            colorFrom = NuColor.BombColor;
                            colorTo = NuColor.BombBackColor;
                        }}, new ParticleEffect() {{
                            particles = 4;
                            sizeFrom = 5f;
                            sizeTo = 1f;
                            length = 7f;
                            baseLength = 3f;
                            lifetime = 13f;
                            colorFrom = NuColor.BombColor;
                            colorTo = NuColor.BombBackColor;
                        }}));
                        accel = 2f;
                        drag = 0.3f;
                        despawnEffect = hitEffect = new MultiEffect(NuFx.PaleSmoke, Fx.flakExplosion, Fx.hitBulletColor);
                        collidesAir = true;
                        collidesGround = true;
                        fragBullets = 10;
                        fragRandomSpread = 90f;
                        fragBullet = new LaserBulletType(80f) {{
                            colors = new Color[]{NuColor.DespColor, NuColor.BombColor, NuColor.BombBackColor, NuColor.DespBackColor};
                            length = 80f;
                            hitEffect = Fx.hitLancer;
                            sideAngle = 175f;
                            sideWidth = 1f;
                            sideLength = 40f;
                            lifetime = 22f;
                            drawSize = 400f;
                            pierceCap = 2;
                            optimalLifeFract = 1f;
                            status = NuStatus.paralysis;
                            statusDuration = 60f * 5f;
                        }};
                    }};
                }});
            weapons.add(new Weapon("why say"){{
                    reload = 75f;
                    x = 2f;
                    y = 1f;
                    range = 480f;
                    mirror = false;
                    shake = 4f;
                    recoil = 0.3f;
                    reload = 75f;
                    maxRange = 500f;
                    shootSound = Sounds.shootMissileLarge;
                    bullet = new BasicBulletType(0f,0f){{
                        instantDisappear = true;
                        shootEffect = new ExplosionEffect(){{
                            waveColor = smokeColor =sparkColor = NuColor.DespColor;
                            sparks = 12;
                            smokes = 10;
                            lifetime = 45f;
                        }};
                        shake = 2f;
                        speed = 0f;
                        keepVelocity = false;
                        inaccuracy = 2f;
                        smokeEffect = NuFx.PaleSmoke;
                        spawnUnit =new MissileUnitType("own"){{
                            constructor = TimedKillUnit::create;
                            EntityMapping.nameMap.put(name, constructor);
                            deathShake = 1f;
                            accel = 0.3f;
                            missileAccelTime = 10f;
                            health = 2000;
                            range = 2f;
                            speed = 5f;
                            crashDamageMultiplier = 3.4f;
                            flyingLayer = Layer.flyingUnit;
                            engineSize = 5f;
                            engineLayer = 3;
                            hidden = true;
                            hitSize = 8f;
                            trailLength = 12;
                            lowAltitude = true;
                            lifetime = 108f;
                            maxRange = 2f;
                            abilities.add(new EnergyFieldAbility(200f,10f,160f){{
                                status =  StatusEffects.shocked;
                                statusDuration = 1200f;
                                x = 0f;
                                y = 0f;
                                targetAir = true;
                                targetGround = true;
                                hitBuildings = true;
                                maxTargets = 160;
                                color = NuColor.DespColor;
                            }});
                            weapons.add(new Weapon(){{
                                display = false;
                                x = 0f;
                                y = 0f;
                                targetAir = true;
                                targetGround = true;
                                shootCone = 360f;
                                reload = 1f;
                                range = 2f;
                                shake = 10f;
                                shootOnDeath = true;
                                bullet = new ExplosionBulletType(20f,160f){{
                                    damage = 150f;
                                    despawnSound = hitSound = Sounds.explosionMissile;
                                    status = StatusEffects.shocked;
                                    statusDuration = 1200f;
                                    knockback = -1f;
                                    lightColor = NuColor.DespColor;
                                    shootEffect = new MultiEffect(Fx.titanSmoke,Fx.massiveExplosion,Fx.scatheLight,new SeqEffect(new WaveEffect(){{
                                        lightColor = colorTo = NuColor.DespColor;
                                        sizeTo = 128f;
                                        sides = 24;
                                        strokeFrom = 4f;
                                        lifetime = 10f;
                                    }},new WaveEffect(){{
                                        lightColor = colorTo = NuColor.DespColor;
                                        sizeTo = 114f;
                                        sides = 24;
                                        strokeFrom = 4f;
                                        lifetime = 10f;
                                    }},new WaveEffect(){{
                                        lightColor = colorTo = NuColor.DespColor;
                                        sizeTo = 100f;
                                        sides = 24;
                                        strokeFrom = 4f;
                                        lifetime = 10f;
                                    }},new WaveEffect(){{
                                        lightColor = colorTo = NuColor.DespColor;
                                        sizeTo = 86f;
                                        sides = 24;
                                        strokeFrom = 4f;
                                        lifetime = 10f;
                                    }},new WaveEffect(){{
                                        lightColor = colorTo = NuColor.DespColor;
                                        sizeTo = 72f;
                                        sides = 24;
                                        strokeFrom = 4f;
                                        lifetime = 10f;
                                    }}));
                                    fragBullets = 6;
                                    fragRandomSpread = 360f;
                                    fragBullet = new FlakBulletType(8f,160f){{
                                        lifetime = 8f;
                                        collidesAir = true;
                                        collidesGround = true;
                                        width = 20f;
                                        height = 20f;
                                        sprite="large-bomb";
                                        trailLength = 10;
                                        frontColor = lightColor = trailColor = NuColor.BombColor;
                                        backColor = NuColor.BombBackColor;
                                        despawnEffect = hitEffect = new MultiEffect(NuFx.bulletHitSmall,Fx.titanExplosionFrag,new WaveEffect(){{
                                            sizeFrom = 5f;
                                            sizeTo = 20f;
                                            strokeFrom = 2f;
                                            strokeTo = 0.2f;
                                            colorFrom = NuColor.BombColor;
                                            colorTo = NuColor.BombBackColor;
                                            lifetime = 30f;
                                        }});
                                    }};
                                }};
                            }});
                        }};;
                    }};
                }});
        }};
        cowardTraitor = new UnitType("cowardTraitor"){{
            constructor =  UnitEntity::create;
            EntityMapping.nameMap.put(name,constructor);
            flying = true;
            drawCell = true;
            circleTarget = false;
            health = 48000;
            speed = 1.1f;
            accel = 0.4f;
            drag = 0.35f;
            itemCapacity = 200;
            researchCostMultiplier = 1.25f;
            flyingLayer = Layer.flyingUnit;
            armor = 35;
            hitSize = 38f;
            alwaysUnlocked = false;
            targetAir = true;
            fogRadius = 240f;
            immunities.addAll(StatusEffects.burning, StatusEffects.wet);
            targetFlags = new BlockFlag[]{BlockFlag.factory, BlockFlag.storage, BlockFlag.battery, null};
        }};
        desperate = new UnitType("desperate"){{
            constructor =  UnitEntity::create;
            EntityMapping.nameMap.put(name,constructor);
            flying = true;
            drawCell = true;
            circleTarget = false;
            health = 110000;
            speed = 0.9f;
            accel = 0.6f;
            drag = 0.5f;
            itemCapacity = 345;
            researchCostMultiplier = 1.25f;
            flyingLayer = Layer.flyingUnit;
            armor = 50;
            hitSize = 45f;
            alwaysUnlocked = false;
            targetAir = true;
            fogRadius = 240f;
            immunities.addAll(StatusEffects.burning, StatusEffects.wet);
            targetFlags = new BlockFlag[]{BlockFlag.factory, BlockFlag.storage, BlockFlag.battery, null};
        }};
        honor = new UnitType("honor"){{
            constructor = MechUnit::create;
            EntityMapping.nameMap.put(name,constructor);
            researchCostMultiplier = 0.5f;
            speed = 1.05f;
            hitSize = 7f;
            armor = 7f;
            health = 1300;
            stepSoundVolume = 0.4f;
            weapons.add(new Weapon("nu-Cannon of Glory"){{
                reload = 72f;
                x = -6.1f;
                y = 1.25f;
                top = true;
                range = 200f;
                mirror = true;
                shoot = new ShootPattern(){{
                    shots = 3;
                    shotDelay = 18;
                }};
                shootSound = Sounds.shootMissile;
                ejectEffect = Fx.casing1;
                bullet = new MissileBulletType(4f, 42){{
                    width = 10f;
                    height = 12f;
                    lifetime = 48f;
                    frontColor = lightColor = trailColor = NuColor.HonorColor;
                    backColor = NuColor.HonorBackColor;
                    weaveScale=8f;
                    trailLength= 8;
                    homingPower = 0.5f;
                    fragBullets = 4;
                    fragRandomSpread = 60;
                    fragVelocityMin=0.6f;
                    fragVelocityMax=1.6f;
                    fragBullet = new ExplosionBulletType(20f,8f){{
                        width = 8f;
                        height = 8f;
                        sprite = "circle-bullet";
                        killShooter = false;
                        splashDamagePierce = false;
                        lifetime = 7.5f;
                    }};
                }};
            }});
        }};
        proud = new UnitType("proud"){{
            constructor = MechUnit::create;
            EntityMapping.nameMap.put(name,constructor);
            researchCostMultiplier = 0.75f;
            speed = 0.75f;
            hitSize = 14f;
            armor = 12f;
            health = 6000;
            alwaysUnlocked=false;
            targetAir = false;
            abilities.add(new ShieldRegenFieldAbility(18f, 400f, 45f, 60f));
            stepSoundVolume = 0.4f;
            weapons.add(new Weapon("nu-Proud Salute"){{
                top = true;
                x = 10f;
                y = 2f;
                mirror = true;
                recoil = 0.2f;
                shootCone= 45;
                reload = 120f;
                range = 260f;
                rotate = true;
                shootSound = Sounds.shootArtillery;
                shoot = new ShootPattern(){{
                    shots = 4;
                    shotDelay=20f;
                }};

                bullet = new BasicBulletType(4f,230f){{
                    width = 20f;
                    height = 20f;
                    drag = 0.1f;
                    maxRange = 260;
                    lifetime = 18f;
                    shootEffect = new WaveEffect(){{
                       sizeFrom = 3f;
                       sizeTo = 14f;
                       strokeFrom = 2f;
                       strokeTo = 0.2f;
                       colorFrom = NuColor.HonorColor;
                       colorTo = NuColor.HonorBackColor;
                       lifetime = 12f;
                    }};
                    hitEffect = Fx.blastExplosion;
                    collidesAir = false;
                    frontColor = lightColor = trailColor = NuColor.HonorColor;
                    backColor = NuColor.HonorBackColor;
                    fragBullets = 3;
                    fragRandomSpread = 30;
                    fragBullet = new ArtilleryBulletType(2.2f,40f,"circle-bullet"){{
                        width = 12f;
                        height = 15f;
                        trailEffect = Fx.artilleryTrail;
                        weaveScale = 12f;
                        frontColor = lightColor = trailColor = NuColor.HonorColor;
                        backColor = NuColor.HonorBackColor;
                        homingPower = 1f;
                        homingRange = 200f;
                        accel = 0.5f;
                        drag = -0.02f;
                        collidesAir = false;
                        lifetime =25;
                        splashDamagePierce = false;
                        splashDamageRadius = 16f;
                        splashDamage=90f;
                        despawnEffect= new MultiEffect(Fx.hitSquaresColor, new ParticleEffect(){{
                            particles = 3;
                            sizeFrom = 6.5f;
                            sizeTo = 0f;
                            length =3f;
                            baseLength = 1f;
                            lifetime = 26f;
                            colorFrom = NuColor.HonorColor;
                            colorTo = NuColor.HonorBackColor;
                        }});
                    }};
                }};
            }});
        }};
        vanity = new UnitType("vanity"){{
            constructor = MechUnit::create;
            EntityMapping.nameMap.put(name,constructor);
            researchCostMultiplier = 1f;
            speed = 0.6f;
            hitSize = 19f;
            armor = 23f;
            health = 11000;
            alwaysUnlocked = false;
            targetAir = true;
            weapons.add(new Weapon("nu-vanities"){{
                x=18f;
                y=0f;
                mirror = true;
                recoil = 3.5f;
                reload = 90f;
                shootSound = Sounds.shootDiffuse;
                rotate = false;
                range = 180f;
                shoot.firstShotDelay = 20f;
                shoot = new ShootPattern(){{
                    shots = 3;
                    shotDelay =25f;
                }};
                bullet = new BasicBulletType(4f,24f){{
                    width = 5f;
                    height = 24f;
                    collides=true;
                    shootEffect = new ParticleEffect(){{
                        particles = 4;
                        sizeFrom = 5f;
                        sizeTo = 1f;
                        length =7f;
                        baseLength = 3f;
                        lifetime = 13f;
                        colorFrom = NuColor.HonorColor;
                        colorTo = NuColor.HonorBackColor;
                    }};
                    maxRange = 320f;
                    lifetime =10f;
                    instantDisappear = true;
                    fragBullets = 16;
                    fragRandomSpread = 20f;
                    fragVelocityMin = 0.9f;
                    fragVelocityMax = 1.1f;
                    fragLifeMin =0.9f;
                    fragLifeMax =1.2f;
                    fragBullet = new BasicBulletType(2.5f,42f){{
                        width = 5f;
                        height = 24f;
                        accel = 0.5f;
                        drag = -0.01f;
                        inaccuracy = 3f;
                        lifetime = 25f;
                        splashDamage=18f;
                        splashDamageRadius = 16f;
                        frontColor=lightColor=trailColor= NuColor.HonorColor;
                        backColor = NuColor.HonorBackColor;
                        lightning = 8;
                        lightningLength = 3;
                        lightningLengthRand = 4;
                        lightningDamage = 5f;
                        lightningColor = NuColor.HonorColor;
                    }};

                }};
            }});
            weapons.add(new Weapon("move"){{
                x=0f;
                y=0f;
                top=false;
                display = false;
                rotate = true;
                mirror = false;
                reload = 140f;
                range = 320f;
                bullet = new BasicBulletType(30f,40f){{
                    despawnEffect = Fx.smoke;
                    hitEffect = Fx.blastExplosion;
                    recoil = -62.5f;
                    status = StatusEffects.shocked;
                    width = 0f;
                    height = 0f;
                    lifetime = 10f;
                    homingPower = 1f;
                    homingRange = 80;
                }};
            }});
        }};
        overPraise = new UnitType("overpraise"){{
            constructor = MechUnit::create;
            EntityMapping.nameMap.put(name,constructor);
            researchCostMultiplier = 1.25f;
            speed = 0.5f;
            hitSize = 25f;
            armor = 32f;
            health = 25000;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 400;
            canBoost = true;
            boostMultiplier = 1.5f;
            abilities.add(new LiquidExplodeAbility(){{
                liquid = NuLiquid.dirtySolution;
                amount = 1000f;
            }});
            abilities.add(new LiquidRegenAbility(){{
                liquid = NuLiquid.dirtySolution;
                slurpEffect = Fx.neoplasmHeal;
            }});
            abilities.add(new ArmorPlateAbility(){{
                healthMultiplier = 0.3f;
            }});
            mechStepParticles = true;
            stepShake = 0.75f;
            drownTimeMultiplier = 1.6f;
            mechFrontSway = 1.9f;
            mechSideSway = 0.6f;
            stepSound = Sounds.mechStepHeavy;
            stepSoundPitch = 0.9f;
            stepSoundVolume = 0.45f;
            immunities.addAll(StatusEffects.burning, StatusEffects.melting);
            weapons.add(new Weapon("nu-truly"){{
                x = 20f;
                y = 0f;
                mirror = true;
                continuous = true;
                top = true;
                alwaysContinuous = true;
                shootSound = Sounds.shootSublimate;
                shootY=1;
                range = 280f;
                maxRange = 280f;
                bullet = new ContinuousFlameBulletType(110f){{
                    optimalLifeFract = 0.5f;
                    length = 280f;
                    hitEffect = Fx.hitFlameBeam;
                    lifetime = 16f;
                    lightColor = hitColor= NuColor.HonorColor;
                    colors = new Color[]{NuColor.HonorColor,NuColor.HonorConColor,NuColor.HonorElseColor,NuColor.HonorBackColor};
                    lightOpacity = 0.7f;
                    laserAbsorb = false;
                    ammoMultiplier = 1f;
                    drawFlare = true;
                    flareColor = NuColor.HonorConColor;
                    pierceArmor = true;
                    pierceCap =3;
                    knockback = 1f;
                    timescaleDamage = true;
                }};
            }});
            weapons.add(new Weapon("nu-scattering60"){{
                top = true;
                reload=90f;
                recoil = -1f;
                x = 15f;
                y = -10f;
                mirror = true;
                rotate =  true;
                range = 400;
                maxRange = 400;
                shoot = new ShootSpread(8, 90f);
                bullet = new BasicBulletType(5f,0f){{
                    lifetime = 4f;
                    width = 12f;
                    height = 15f;
                    maxRange = 400f;
                    frontColor = Color.valueOf("ffffff");
                    trailColor = lightColor = NuColor.HonorElseColor;
                    backColor=hitColor=NuColor.HonorBackColor;
                    trailLength = 25;
                    shootEffect = smokeEffect = Fx.sparkShoot;
                    fragBullets = 4;
                    fragRandomSpread =60f;
                    fragBullet = new BasicBulletType(5.25f,400f){{
                        hitColor = NuColor.HonorConColor;
                        lifetime = 80f;
                        width = 12f;
                        height = 15f;
                        frontColor = Color.valueOf("ffffff");
                        trailColor = lightColor = NuColor.HonorElseColor;
                        backColor=hitColor=NuColor.HonorBackColor;
                        trailLength = 25;
                        splashDamage = 60f;
                        splashDamageRadius = 24f;
                        homingPower = 1f;
                        homingRange = 280f;
                        homingDelay = 10f;
                        despawnHit =true;
                        despawnEffect = hitEffect =new MultiEffect(Fx.hitBulletColor, new SeqEffect(new WaveEffect(){{
                            lightColor=colorTo=NuColor.HonorConColor;
                            sizeTo = 16f;
                            sizeFrom =24f;
                            strokeFrom = 4f;
                            lifetime = 16f;
                        }},new WaveEffect(){{
                            lightColor=colorTo=NuColor.HonorConColor;
                            sizeTo = 12f;
                            sizeFrom =24f;
                            strokeFrom = 4f;
                            lifetime = 16f;
                        }}));
                    }};
                }};
            }});
            weapons.add(new Weapon("nu-Sound Of Overpraise"){{
                x=10f;
                y=8f;
                mirror = true;
                rotate =true;
                top = false;
                shootCone = 10;
                reload = 60f;
                range = 400f;
                shootY=1f;
                recoil = 5f;
                shake = 2f;
                maxRange = 400f;
                ejectEffect = Fx.casing4;
                shootSound = Sounds.shootReign;
                shoot = new ShootPattern(){{
                    shots = 4;
                    shotDelay = 8f;
                }};
                bullet = new BasicBulletType(5f,260f){{
                    lifetime = 80f;
                    status = StatusEffects.shocked;
                    shootStatusDuration = 20f;
                    sprite = "missile";
                    width = 15f;
                    height = 35f;
                    homingPower=0.12f;
                    homingRange =32f;
                    shootEffect = Fx.shootBig;
                    hitEffect = Fx.blastExplosion;
                    despawnSound = Sounds.explosion;
                    fragBullets=14;
                    frontColor=lightColor=trailColor=NuColor.HonorColor;
                    backColor=NuColor.HonorBackColor;
                    fragRandomSpread=21f;
                    fragLifeMin=0.7f;
                    fragLifeMax=1.3f;
                    fragVelocityMin=0.9f;
                    fragVelocityMax=1.1f;
                    fragBullet = new BasicBulletType(3f, 42){{
                        width = 10f;
                        height = 10f;
                        pierce = true;
                        pierceBuilding = true;
                        pierceCap = 3;
                        lifetime = 5f;
                        despawnEffect=hitEffect = Fx.flakExplosion;
                        splashDamage = 15f;
                        splashDamageRadius = 24f;
                    }};
                }};
            }});
        }};
        blindLoyalty = new UnitType("blindLoyalty"){{
            constructor = MechUnit::create;
            EntityMapping.nameMap.put(name,constructor);
            researchCostMultiplier = 1.75f;
            speed = 0.7f;
            hitSize = 40f;
            armor = 44f;
            health = 53000;
            alwaysUnlocked = false;
            targetAir = true;
            canBoost = true;
            boostMultiplier = 1f;
            mechStepParticles = true;
            stepShake = 1.5f;
            drownTimeMultiplier = 4f;
            mechFrontSway = 4f;
            mechSideSway = 1.6f;
            stepSound = Sounds.mechStepHeavy;
            stepSoundPitch = 1.9f;
            stepSoundVolume = 0.9f;
            immunities.addAll(StatusEffects.burning, StatusEffects.melting);
            abilities.add(new RegenAbility(){{
                percentAmount = 1f;
                amount = 4f;
            }});
            abilities.add(new ShieldRegenFieldAbility(1250f, 15000f, 120f, 120f));
            weapons.add(new Weapon("momentRain"){{
                x = 0f;
                y = -5f;
                mirror = false;
                rotate =  true;
                range = 400f;
                reload = 150f;
                shake = 2f;
                recoil = 3.5f;
                shoot = new ShootAlternate(){{
                    shots = 6;
                    spread = 4f;
                    barrels = 5;
                }};
                bullet = new  FlakBulletType(5f,900F){{
                    width = 20f;
                    height = 20f;
                    maxRange = 400f;
                    collidesAir = true;
                    collidesGround = true;
                    collides = true;
                    sprite="large-bomb";
                    lifetime = 10f;
                    trailColor = lightColor = NuColor.HonorElseColor;
                    backColor=hitColor=NuColor.HonorBackColor;
                    trailLength = 3;
                    despawnEffect=hitEffect =new MultiEffect(Fx.magmasmoke,new ExplosionEffect(){{
                        waveColor=smokeColor=sparkColor= NuColor.HonorElseColor;
                        waveLife = 6f;
                        waveStroke = 3f;
                        waveRad = 15f;
                        waveRadBase = 2f;
                        sparkStroke = 1f;
                        sparkRad = 23f;
                        sparkLen = 3f;
                        smokeSize = 4f;
                        smokeSizeBase = 0.5f ;
                        smokeRad = 23f;
                        smokes = 5 ;
                        sparks = 4;
                    }});
                    fragBullets = 5;
                    fragRandomSpread = 40f;
                    fragBullet = new FlakBulletType(5f,180f){{
                        collidesAir = true;
                        collidesGround = true;
                        collides = true;
                        width = 12f;
                        height = 12f;
                        sprite = "large-orb";
                        lifetime = 10f;
                        trailColor = lightColor = NuColor.HonorElseColor;
                        backColor=hitColor=NuColor.HonorBackColor;
                        despawnEffect=hitEffect = Fx.flakExplosion;
                        trailLength=10;
                        fragBullets = 5;
                        fragRandomSpread = 40f;
                        fragBullet = new FlakBulletType(5f,36f){{
                            collidesAir = true;
                            collidesGround = true;
                            collides = true;
                            accel = 3f;
                            drag = 0.3f;
                            width = 12f;
                            height = 12f;
                            homingPower = 0.4f;
                            homingRange = 300f;
                            sprite = "large-orb";
                            lifetime = 45f;
                            trailColor = lightColor = NuColor.HonorElseColor;
                            backColor=hitColor=NuColor.HonorBackColor;
                            despawnEffect=hitEffect = Fx.flakExplosion;
                            trailLength=10;
                        }};
                    }};
                }};
            }});
            weapons.add(new Weapon("genius"){{
                x = 6f;
                y = -5f;
                mirror = true;
                rotate = true;
                range = 240f;
                reload = 10f;
                shake = 3.5f;
                recoil = 2.5f;
                bullet = new BulletType(5f, 1240f){{
                    hitSize = 24f;
                    lifetime = 48f;
                    pierce = true;
                    pierceBuilding = true;
                    pierceCap = 90;
                    statusDuration = 60f * 5;
                    shootEffect = NuFx.HonorFlame;
                    hitEffect = NuFx.hitFlameHonor;
                    despawnEffect = Fx.none;
                    status = StatusEffects.burning;
                    keepVelocity = false;
                    hittable = false;
                }};
            }});
            weapons.add(new Weapon("fake"){{
                x = 14f;
                y = 5f;
                mirror = true;
                rotate = true;
                range = 560f;
                reload = 20f;
                shake = 4.5f;
                recoil = 2.5f;
                shoot.shots = 2;
                shoot.shotDelay = 8f;
                bullet = new PointBulletType(){{
                    trailSpacing = 3f;
                    trailInterval = 2f;
                    trailEffect = NuFx.HonorEnergyTail;
                    buildingDamageMultiplier = 0.7f;
                    damage = 200f;
                    maxRange = 560f;
                    lifetime = 40f;
                    speed = 14f;
                    healPercent = 0.01f;
                    healAmount = 30f;
                    splashDamageRadius = 24f;
                    splashDamage = 56f;
                    status = NuStatus.paralysis;
                    statusDuration = 60f*3;
                    chargeEffect = new ParticleEffect() {{
                        particles = 1;
                        sizeFrom = 0f;
                        sizeTo = 4f;
                        length = -35;
                        baseLength = 35;
                        lifetime = 55f;
                        colorFrom = NuColor.HonorConColor;
                        colorTo = NuColor.HonorElseColor;
                    }};
                    shootEffect = new ExplosionEffect(){{
                        waveColor=smokeColor= NuColor.HonorColor;
                        sparkColor= NuColor.PaleColor;
                        waveLife = 12f;
                        waveStroke = 3.5f;
                        waveRad = 15f;
                        waveRadBase = 2f;
                        sparkStroke = 1f;
                        sparkRad = 23f;
                        sparkLen = 3f;
                        smokeSize = 12f;
                        smokeSizeBase = 3.5f ;
                        smokeRad = 23f;
                        smokes = 5;
                        sparks = 4;
                    }};
                    despawnEffect = hitEffect = Fx.hitLaserBlast;
                }};
            }});
        }};
        safeguardRights = new UnitType("safeguardRights"){{
            constructor = MechUnit::create;
            EntityMapping.nameMap.put(name,constructor);
            researchCostMultiplier = 1f;
            speed = 0.6f;
            hitSize = 48f;
            armor = 68f;
            health = 130000;
            alwaysUnlocked = false;
            targetAir = true;
            abilities.add(new RegenAbility(){{
                percentAmount = 2f;
                amount = 10f;
            }});
            abilities.add(new ShieldRegenFieldAbility(2000f, 20000f, 180f, 180f));
            weapons.add(new Weapon("Rights"){{
                x = 10f;
                y = 0f;
                mirror = true;
                rotate = true;
                reload = 140f;
                shake = 5.5f;
                range = 560f;
                recoil = 5f;
                rotateSpeed = 10f;
                cooldownTime = reload;
                shootCone = 2f;
                shootSound = Sounds.shootForeshadow;
                bullet = new RailBulletType(){{
                    maxRange = 560f;
                    shootEffect = Fx.instShoot;
                    hitEffect = Fx.instHit;
                    pierceEffect = Fx.railHit;
                    smokeEffect = Fx.smokeCloud;
                    pointEffect = Fx.instTrail;
                    despawnEffect = NuFx.RightsExplosion;
                    pointEffectSpace = 20f;
                    damage = 4500;
                    length = 560;
                    buildingDamageMultiplier = 0.7f;
                    pierceDamageFactor = 1f;
                    hitShake = 6f;
                    fragBullets = 6;
                    fragRandomSpread = 360f;
                    lightningColor = lightColor = hitColor = NuColor.HonorColor;
                    splashDamage = 1200f;
                    splashDamageRadius = 40f;
                    scaledSplashDamage = true;
                    makeFire = true;
                    despawnShake = 8f;
                    hitShake = 6.5f;
                    status = NuStatus.radiation;
                    statusDuration = 60f*4;
                    fragBullet = new LightningBulletType(){{
                        lifetime = 25f;
                        damage = 250;
                        status = StatusEffects.shocked;
                        lightningLength = 36;
                        lightningLengthRand = 2;
                        lightningColor = NuColor.HonorConColor;
                    }};
                }};
            }});
            weapons.add(new Weapon("gurad"){{
                x = 3f;
                y = 4f;
                mirror = true;
                rotate = true;
                range = 560f;
                reload = NuFx.LaserChargeHonor.lifetime * 2.4f;
                shootCone = 10f;
                shootStatusDuration = NuFx.LaserChargeHonor.lifetime + 30f;
                shootStatus = StatusEffects.unmoving;
                shoot.firstShotDelay = NuFx.LaserChargeHonor.lifetime;
                parentizeEffects = true;
                cooldownTime =  NuFx.LaserChargeHonor.lifetime * 0.5f;
                bullet = new LaserBulletType(4000f){{
                    chargeEffect = NuFx.LaserChargeHonor;
                    length = 560f;
                    width = 50f;
                    lifetime = 80f;
                    largeHit = true;
                    lightColor = lightningColor = NuColor.HonorColor;
                    collidesTeam = true;
                    sideAngle = 15f;
                    sideWidth = 0f;
                    sideLength = 0f;
                    colors = new Color[]{NuColor.HonorColor,NuColor.HonorBackColor,NuColor.PaleColor};
                }};
            }});
            weapons.add(new Weapon("loyalty"){{
                x =  13f;
                y =  0f;
                reload = 40f;
                range = 480f;
                mirror = false;
                rotate = true;
                recoil = 2f;
                shootCone = 120f;
                shoot = new ShootAlternate(){{
                    shots = 6;
                    shotDelay = 6f;
                    spread = 8f;
                    barrels = 5;
                }};
                bullet = new BasicBulletType(2f,1000f){{
                    maxRange = 480f;
                    width = 36f;
                    height = 36f;
                    accel = 1f;
                    lifetime = 22.5f;
                    drag = 0.2f;
                    frontColor = trailColor = lightColor = NuColor.HonorColor;
                    backColor=hitColor=NuColor.HonorBackColor;
                    trailLength = 3;
                    shootEffect = Fx.shootBig;
                    hitEffect = despawnEffect= Fx.blastExplosion;
                    fragBullets = 2;
                    fragRandomSpread = 0f;
                    fragBullet = new BasicBulletType(0f,1500f){{
                        width = 28f;
                        height = 28f;
                        lifetime = 90f;
                        pierce = true;
                        pierceArmor = true;
                        pierceCap =15;
                        frontColor = trailColor = lightColor = NuColor.HonorColor;
                        backColor=hitColor=NuColor.HonorBackColor;
                        hitEffect = despawnEffect= Fx.blastExplosion;
                        fragBullets = 3;
                        fragRandomSpread = 60f;
                        fragBullet = new BasicBulletType(4f,750f){{
                            width = 21f;
                            height = 21f;
                            trailEffect = NuFx.HonorEnergyTail;
                            trailInterval = 2.5f;
                            lifetime = 57.5f;
                            accel = 0.1f;
                            drag = -0.001f;
                            frontColor = trailColor = lightColor = NuColor.HonorColor;
                            backColor=hitColor=NuColor.HonorBackColor;
                            hitEffect = despawnEffect = new WaveEffect(){{
                                strokeFrom = 2.5f;
                                strokeTo = 0f;
                                colorFrom = NuColor.HonorColor;
                                colorTo = NuColor.HonorBackColor;
                                sizeFrom = 20f;
                                sizeTo = 1f;
                                lifetime =24f;
                                sides = 12;
                            }};
                            intervalBullets = 2;
                            intervalRandomSpread = 30f;
                            bulletInterval = 5f;
                            intervalBullet = new LightningBulletType(){{
                                lifetime = 25f;
                                damage = 250;
                                status = StatusEffects.shocked;
                                lightningLength = 11;
                                lightningLengthRand = 2;
                                lightningColor = NuColor.HonorConColor;
                            }};
                        }};
                    }};
                }};
            }});
        }};
        sailor = new UnitType("sailor"){{
            constructor =  UnitWaterMove::create;
            EntityMapping.nameMap.put(name,constructor);
            speed = 1.7f;
            rotateSpeed = 20f;
            health = 960;
            hitSize = 8f;
            researchCostMultiplier = 0.5f;
            trailLength = 10;
            waveTrailX = 5f;
            waveTrailY = -4f;
            trailScl = 2f;
            armor = 10f;
            faceTarget = true;
            abilities.add(new ForceFieldAbility(30f,0.3f,450f,120f,8,20f));
            weapons.add(new Weapon("flag of sailor"){{
                x = 5f;
                y = 3f;
                reload = 60f;
                mirror = true;
                rotate =true;
                top = true;
                alternate = true;
                minShootVelocity = 0f;
                targetAir = true;
                targetGround = true;
                shootSound = Sounds.shoot;
                shoot = new ShootPattern(){{
                   shots=4;
                   shotDelay = 8f;
                }};
                bullet = new BasicBulletType(8f,60f){{
                    lifetime = 25.625f;
                    width = 10f;
                    height = 18f;
                    frontColor = lightColor = trailColor = NuColor.SailColor;
                    backColor = NuColor.SailBackColor;
                    status = StatusEffects.wet;
                    statusDuration = 1200f;
                    trailLength = 4;
                    trailWidth = 2.4f;
                    trailInterval = 3f;
                    trailEffect = NuFx.sailEnergyTail;
                    despawnEffect = hitEffect =NuFx.sailArcHit;
                    collides = true;
                    collidesAir = true;
                    collidesGround = true;
                    pierce = true;
                    pierceArmor = true;
                    pierceCap =4;
                    splashDamage = 15f;
                    splashDamageRadius = 24f;
                    intervalBullets = 4;
                    bulletInterval = 4f;
                    intervalBullet = new LightningBulletType(){{
                        damage = 12;
                        lifetime = 10f;
                        collidesAir = true;
                        ammoMultiplier = 1.3f;
                        lightningColor = NuColor.SailBackColor;
                        lightningLength = 3;
                        lightningLengthRand = 6;
                        status =  StatusEffects.shocked;
                        statusDuration = 120f;
                    }};
                }};
            }});
        }};
        cruise = new UnitType("cruise"){{
            constructor =  UnitWaterMove::create;
            EntityMapping.nameMap.put(name,constructor);
            speed = 1.3f;
            rotateSpeed = 15f;
            health = 4600;
            hitSize = 14f;
            researchCostMultiplier = 0.75f;
            trailLength = 24;
            waveTrailX = 3.75f;
            waveTrailY = -7.5f;
            trailScl = 2f;
            armor = 16f;
            fogRadius = 42f;
            lightRadius = 42f;
            faceTarget = true;
            abilities.add(new ForceFieldAbility(42f,0.4f,1000f,720f,4,20f));
            weapons.add(new Weapon("cruise of shadow"){{
                x = 6f;
                y = 6f;
                top = true;
                mirror = true;
                rotate =true;
                reload = 90f;
                range = 180f;
                alternate = true;
                shootSound = Sounds.shoot;
                shoot = new ShootPattern(){{
                   shots = 6;
                   shotDelay = 10f;
                }};
                bullet = new MissileBulletType(4f,87f){{
                    width = 10f;
                    height = 18f;
                    homingPower = 0.6f;
                    lifetime = 45f;
                    maxRange = 180f;
                    collides = true;
                    collidesAir = true;
                    collidesGround = true;
                    frontColor = lightColor = trailColor = NuColor.SailColor;
                    backColor = NuColor.SailBackColor;
                    status = NuStatus.lack;
                    statusDuration = 1200f;
                    trailEffect = NuFx.sailEnergyTail;
                    despawnEffect = hitEffect =NuFx.sailArcHit;
                    sprite = "circle-bullet";
                }};
            }});
            weapons.add(new Weapon("Great Cocoon"){{
                x = 0f;
                y = 0f;
                top = false;
                mirror = true;
                reload = 20f;
                range = 220f;
                alternate = true;
                rotate =true;
                shootSound = Sounds.shoot;
                shoot = new ShootPattern(){{
                   shots = 2;
                   shotDelay = 10f;
                }};
                bullet = new PointBulletType(){{
                    trailSpacing = 3f;
                    trailEffect = new MultiEffect(new ParticleEffect() {{
                        particles = 1;
                        length = 0;
                        baseLength = 1;
                        lifetime = 7.5f;
                        line = true;
                        randLength = false;
                        lenFrom = 3.3f;
                        lenTo = 3.3f;
                        strokeFrom = 1.2f;
                        strokeTo = 0f;
                        colorFrom = NuColor.SailColor;
                        colorTo = NuColor.SailBackColor;
                        cone = 0f;
                    }},new ParticleEffect() {{
                        particles = 1;
                        sizeFrom = 1f;
                        sizeTo = 0f;
                        length = 0;
                        baseLength = 3;
                        lifetime = 15f;
                        colorFrom = NuColor.PaleColor;
                        colorTo = NuColor.PaleBackColor;
                    }});
                    buildingDamageMultiplier = 0.7f;
                    damage = 40;
                    maxRange = 220f;
                    lifetime = 13.75f;
                    speed = 16f;
                    healPercent = 0.3f;
                    healAmount = 30f;
                    splashDamageRadius = 32f;
                    splashDamage = 17.6f;
                    status = NuStatus.paralysis;
                    statusDuration = 60f;
                    chargeEffect = new ParticleEffect() {{
                        particles = 2;
                        sizeFrom = 0f;
                        sizeTo = 2f;
                        length = -35;
                        baseLength = 35;
                        lifetime = 55f;
                        colorFrom = NuColor.PaleConColor;
                        colorTo = NuColor.PaleSilverColor;
                    }};
                    shootEffect = new ExplosionEffect(){{
                        waveColor=smokeColor=sparkColor= NuColor.PaleColor;
                        waveLife = 6f;
                        waveStroke = 3f;
                        waveRad = 15f;
                        waveRadBase = 2f;
                        sparkStroke = 1f;
                        sparkRad = 23f;
                        sparkLen = 3f;
                        smokeSize = 4f;
                        smokeSizeBase = 0.5f ;
                        smokeRad = 23f;
                        smokes = 5 ;
                        sparks = 4;
                    }};
                    despawnEffect = hitEffect = NuFx.sailArcHit;

                }};
            }});
        }};
        wanderer = new UnitType("wanderer"){{
            constructor =  UnitWaterMove::create;
            EntityMapping.nameMap.put(name,constructor);
            speed = 0.8f;
            rotateSpeed = 5f;
            health = 9600;
            hitSize = 24f;
            trailLength = 40;
            waveTrailX = 8f;
            waveTrailY = -8f;
            researchCostMultiplier = 1f;
            trailScl = 2.5f;
            armor = 23f;
            fogRadius = 42f;
            lightRadius = 42f;
            faceTarget = true;
            abilities.add(new ShieldArcAbility(){{
                max=4000f;
                cooldown = 240f;
                regen = 1f;
                angle = 180f;
                whenShooting = true;
                width = 10f;
                x = 0f;
                y = -22f;
                radius = 60f;
            }});
            weapons.add(new Weapon("death of wanderer"){{
                x = 6f;
                y = 8f;
                top = true;
                reload = 75f;
                range = 280f;
                shootCone = 180f;
                alternate = true;
                rotate = true;
                shootSound = Sounds.shoot;
                shoot = new ShootPattern(){{
                    shots = 4;
                    shotDelay = 12f;
                }};
                bullet = new MissileBulletType(8f,164f){{
                    width =25f;
                    height = 25f;
                    maxRange = 280f;
                    status = NuStatus.radiation;
                    homingPower = 0.75f;
                    weaveMag = 5f;
                    weaveScale = 5f;
                    buildingDamageMultiplier = 1.2f;
                    lifetime = 30f;
                    healAmount = 100f;
                    healPercent = 0.35f;
                    shootEffect = Fx.shootBig2;
                    smokeEffect = Fx.shootSmokeTitan;
                    splashDamage = 100f;
                    splashDamageRadius = 80f;
                    maxRange = 280f;
                    collides = true;
                    collidesAir = true;
                    collidesGround = true;
                    frontColor = lightColor = trailColor = NuColor.SailColor;
                    backColor = NuColor.SailBackColor;
                    trailInterval = 3f;
                    trailWidth = 3f;
                    trailLength =12;
                    trailChance =1f;
                    trailEffect = NuFx.sailEnergyTail;
                    hitEffect = despawnEffect = NuFx.plasmaHit;
                    lightRadius = 40f;
                    lightOpacity = 0.7f;
                    fragBullets = 4;
                    fragRandomSpread = 45f;
                    fragVelocityMin = 0.9f;
                    fragVelocityMax = 1.2f;
                    fragBullet = new MissileBulletType(4f,42f){{
                        lifetime = 10f;
                        splashDamage = 30f;
                        splashDamageRadius = 12f;
                        frontColor = lightColor = trailColor = NuColor.SailColor;
                        backColor = NuColor.SailBackColor;
                        width = 14f;
                        height = 14f;
                        trailLength = 6;
                        trailWidth = 1.2f;
                        homingPower = 1f;
                        homingRange = 60f;
                        status = NuStatus.lack;
                        hitEffect = despawnEffect = NuFx.sailArcHit;
                    }};
                }};
            }});
            weapons.add(new RepairBeamWeapon("the wanderer"){{
                x = 0f;
                y = -8f;
                mirror = true;
                shootCone = 90f;
                repairSpeed = 0.8f;
                targetBuildings = true;
                targetUnits = true;
                controllable = false;
                aiControllable = true;
                autoTarget = true;
                rotate = true;
                bullet.maxRange = 280f;
            }});
        }};
        setsails = new UnitType("setsails"){{
            constructor =  UnitWaterMove::create;
            EntityMapping.nameMap.put(name,constructor);
            speed = 0.65f;
            rotateSpeed = 3f;
            health = 22000;
            hitSize = 31f;
            trailLength = 54;
            waveTrailX = 16f;
            waveTrailY = -16f;
            researchCostMultiplier = 1.25f;
            trailScl = 5f;
            armor = 42f;
            fogRadius = 120f;
            lightRadius = 200f;
            faceTarget = true;
            abilities.add(new SuppressionFieldAbility(){{
                reload = 100f;
                maxDelay = 60f;
                range = 240f;
                orbRadius = 5f;
                orbMidScl = 0.4f;
                orbSinScl = 10f;
                orbSinMag = 1f;
                color = NuColor.PaleColor;
                layer =110f;
                x = 0f;
                y = 0f;
                particles = 7;
                particleSize = 4f;
                particleLen = 7f;
                rotateScl = 3f;
                particleLife = 110f;
                active = true;
                particleColor = NuColor.PaleSilverColor;
                effectColor = NuColor.SailColor;
                applyParticleChance = 16f;
                timer = 10f;
            }});
            abilities.add(new ShieldArcAbility(){{
                max = 10000f;
                cooldown = 300f;
                regen = 1.5f;
                angle =90f;
                whenShooting =true;
                width = 24f;
                x = 0f;
                y = 0f;
                radius = 64f;
            }});
            weapons.add(new Weapon("bulletEMP"){{
                rotate = true;
                mirror = true;
                x = 17.5f;
                y = -6.5f;
                reload = 90f;
                shake = 3f;
                rotateSpeed = 2f;
                shadow = 30f;
                shootY = 7f;
                recoil = 4f;
                range = 400f;
                cooldownTime = 15;
                shootSound = Sounds.shootNavanax;
                shoot = new ShootPattern(){{
                   shots = 2;
                   shotDelay = 25f;
                }};
                bullet = new EmpBulletType(){{
                   scaleLife = true;
                   lightOpacity = 0.7f;
                   maxRange = 400f;
                   unitDamageScl = 1.2f;
                   healPercent = 25f;
                   timeIncrease = 3f;
                   timeDuration = 60f*4f;
                   powerDamageScl = 4f;
                   powerSclDecrease = 2f;
                   unitDamageScl = 1f;
                   damage = 600f;
                    frontColor = lightColor = trailColor = NuColor.SailColor;
                    backColor = hitColor = NuColor.SailBackColor;
                    lightRadius = 80f;
                    clipSize = 80f;
                    collidesAir = true;
                    shootEffect = Fx.hitEmpSpark;
                    smokeEffect = Fx.shootBigSmoke2;
                    lifetime = 60f;
                    speed = 6f;
                    trailLength = 30;
                    trailWidth = 10f;
                    width = 25f;
                    height = 25f;
                    sprite = "circle-bullet";
                    despawnEffect = hitEffect = new MultiEffect(new WaveEffect(){{
                        lifetime = 60f;
                        sizeFrom = 0f;
                        sizeTo= 80f;
                        strokeFrom = 1f;
                        strokeTo = 3f;
                        colorFrom = NuColor.SailColor;
                        colorTo = NuColor.SailBackColor;
                    }},new WaveEffect(){{
                        startDelay = 60f;
                        lifetime = 80f;
                        sizeFrom = 80f;
                        sizeTo = 80f;
                        interp = Interp.circleOut;
                        strokeFrom = 3f;
                        strokeTo = 0f;
                        colorFrom = NuColor.SailColor;
                        colorTo = NuColor.SailBackColor;
                    }});
                    trailChance = 0f;
                    trailInterval = 2f;
                    trailEffect = NuFx.sailEnergyTail;
                    splashDamage = 450f;
                    splashDamageRadius = 80f;
                    hitShake = 4f;
                    trailRotation = true;
                    status = NuStatus.paralysis;
                    statusDuration = 60f*1f;
                    hitSound = Sounds.explosionNavanax;
                    fragBullets = 10;
                    fragRandomSpread = 360f;
                    fragLifeMin = 0.75f;
                    fragLifeMax = 1.45f;
                    fragBullet = new FlakBulletType(4f,100f){{
                        width = 10f;
                        height = 12f;
                        lifetime = 15f;
                        frontColor = lightColor = trailColor = NuColor.SailColor;
                        backColor = hitColor = NuColor.SailBackColor;
                        hitEffect = despawnEffect = new MultiEffect(Fx.titanExplosionFrag,Fx.titanLightSmall,NuFx.sailArcHit,new WaveEffect(){{
                            lifetime = 8f;
                            strokeFrom = 1f;
                            sizeTo = 8f;
                        }},new ParticleEffect(){{
                            particles = 1;
                            sizeFrom =8f;
                            sizeTo = 0f;
                            sizeInterp = interp.pow5In;
                            interp = interp.pow10Out;
                            layer = 120f;
                            length =75;
                            baseLength = 5f;
                            lifetime =60f;
                            colorFrom = NuColor.PaleColor;
                            colorTo = NuColor.PaleBackColor;
                        }});
                    }};
                }};
            }});
            weapons.add(new Weapon("lightning"){{
                 x = 0f;
                 y = -3f;
                 reload = 240f;
                 top = true;
                 mirror = false;
                 rotate = true;
                 rotateSpeed = 8f;
                 range = 400f;
                 shadow = 30f;
                 shootY = 7f;
                 recoil = 4f;
                 alwaysShooting = false;
                 shootSound = Sounds.shootCollaris;
                 shoot = new ShootPattern(){{
                     shots = 3;
                     shotDelay = 40f;
                 }};
                 bullet = new BasicBulletType(3f,700f){{
                     pierce = true;
                     pierceArmor = true;
                     pierceBuilding = true;
                     pierceCap = 120;
                     lifetime = 300f;
                     width = 30f;
                     height = 30f;
                     rangeOverride = 400f;
                     collides = true;
                     circleShooter = true;
                     circleShooterRotateSpeed = 6f;
                     circleShooterRadius = 160f;
                     frontColor = lightColor = trailColor = NuColor.SailConColor;
                     backColor = hitColor = NuColor.SailElseColor;
                     trailLength = 12;
                     trailWidth = 6f;
                     intervalBullets = 6;
                     bulletInterval = 30f;
                     intervalRandomSpread = 360f;
                     intervalBullet = new BasicBulletType(5f,80f){{
                         lifetime = 90f;
                         collides = true;
                         width = 18f;
                         height = 24f;
                         pierceCap = 2;
                         pierceArmor = true;
                         pierce = true;
                         frontColor = lightColor = trailColor = NuColor.SailColor;
                         backColor = hitColor = NuColor.SailBackColor;
                         trailLength = 20;
                         trailWidth = 8f;
                         homingPower = 1f;
                         homingRange = 400f;
                         homingDelay = 10f;
                         hitEffect = despawnEffect = NuFx.sailHitSmall;
                         buildingDamageMultiplier = 0.8f;
                     }};
                 }};
            }});
        }};
        captain = new UnitType("captain"){{
            constructor =  UnitWaterMove::create;
            EntityMapping.nameMap.put(name,constructor);
            speed = 0.6f;
            rotateSpeed = 3f;
            health = 50000;
            hitSize = 36f;
            trailLength = 90;
            waveTrailX = 30f;
            waveTrailY = -30f;
            researchCostMultiplier = 1.25f;
            trailScl = 8f;
            armor = 56f;
            fogRadius = 240f;
            lightRadius = 320f;
            faceTarget = true;
        }};
        nemo = new UnitType("nemo"){{
            constructor =  UnitWaterMove::create;
            EntityMapping.nameMap.put(name,constructor);
            speed = 0.65f;
            rotateSpeed = 3f;
            health = 120000;
            hitSize = 43f;
            trailLength = 120;
            waveTrailX = 54f;
            waveTrailY = -54f;
            researchCostMultiplier = 1.25f;
            trailScl = 12f;
            armor = 75f;
            fogRadius = 320f;
            lightRadius = 400f;
            faceTarget = true;
        }};
        pale = new UnitType("pale"){{
            constructor = MechUnit::create;
            EntityMapping.nameMap.put(name,constructor);
            health = 2000;
            speed = 1.3f;
            researchCostMultiplier = 0.50f;
            // 拦截场：3 种简洁写法任选其一（都能正确变色，不再冗长）
            armor = 15;
            hitSize = 7f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 128;
            // —— 【写法 A：链式 setter（强烈推荐）】—— 一行到底，和 Weapon(){{}} 差不多，最顺
            abilities.add(new InterceptFieldAbility(24f, 5.5f, 20f, 0.95f, 5, 60f*2, 60f*10)
                    .color(NuColor.PaleColor)   // ★ 调用 .color() 会自动把 useShieldColor=false，不用再写开关！
                    .sides(32)
                    .rotation(0f));
            // —— 【写法 B：双大括号匿名子类（现在也能用了）】——
            //    因为 copy() 里我改成了 new InterceptFieldAbility() + 手工全量字段拷贝，
            //    不再依赖 super.copy()/ClassMap 反射识别匿名子类，所以你最爱的 Weapon 风格也稳了。
            // abilities.add(new InterceptFieldAbility(56f,5f,90f,0.8f,8,60f*2,60f*10){{
            //     color = NuColor.PaleColor;   // 双保险：copy 里只要 color!=Pal.shield 会自动关 useShieldColor
            //     sides = 32;
            // }});

            // —— 【写法 C：Color 版构造（最短）】—— 专门带 Color 的构造，一行 new 完什么都不用写
            // abilities.add(new InterceptFieldAbility(NuColor.PaleColor, 56f, 5f, 90f, 0.8f, 8, 60f*2, 60f*10));
            weapons.add(new Weapon("whites"){{
                x=0f;
                y=0f;
                reload=45f;
                range = 128f;
                shake = 0f;
                recoil = 0f;
                rotate=true;
                mirror=false;
                bullet=new BasicBulletType(4f,90f){{
                   width = 18f;
                   height = 24f;
                   lifetime = 32f;
                   lightColor = frontColor = trailColor = NuColor.PaleColor;
                   backColor = NuColor.PaleBackColor;
                   trailLength = 5;
                   trailInterval =2f;
                   trailEffect = new SeqEffect(new WaveEffect(){{
                       interp = Interp.circleOut;
                       lifetime = 16f;
                       sizeFrom = 1f;
                       sizeTo = 8f;
                       strokeFrom=2f;
                       strokeTo=0f;
                       colorFrom = NuColor.PaleColor;
                       colorTo = NuColor.PaleBackColor;
                   }}, new ParticleEffect(){{
                       particles = 4;
                       sizeFrom = 5f;
                       sizeTo = 1f;
                       length =7f;
                       baseLength = 3f;
                       lifetime = 13f;
                       colorFrom = NuColor.PaleColor;
                       colorTo = NuColor.PaleBackColor;
                   }});
                    status = StatusEffects.corroded;
                    shootStatusDuration = 20f;
                    despawnEffect=hitEffect =new MultiEffect(Fx.magmasmoke,new ExplosionEffect(){{
                        waveColor=smokeColor=sparkColor= NuColor.PaleColor;
                        waveLife = 6f;
                        waveStroke = 3f;
                        waveRad = 15f;
                        waveRadBase = 2f;
                        sparkStroke = 1f;
                        sparkRad = 23f;
                        sparkLen = 3f;
                        smokeSize = 4f;
                        smokeSizeBase = 0.5f ;
                        smokeRad = 23f;
                        smokes = 5 ;
                        sparks = 4;
                    }});
                    intervalBullets = 2;
                    intervalRandomSpread = 30f;
                    bulletInterval = 20f;
                    intervalBullet = new BasicBulletType(4.5f,30f){{
                        lifetime = 16f;
                        width = 12f;
                        height = 12f;
                        lightColor = frontColor = trailColor = NuColor.PaleColor;
                        backColor = NuColor.PaleBackColor;
                        trailLength = 5;
                        trailInterval =2f;
                        trailEffect = new ParticleEffect(){{
                            particles = 4;
                            sizeFrom = 5f;
                            sizeTo = 1f;
                            length =7f;
                            baseLength = 3f;
                            lifetime = 13f;
                            colorFrom = NuColor.PaleColor;
                            colorTo = NuColor.PaleBackColor;
                        }};
                        status = StatusEffects.corroded;
                        shootStatusDuration = 20f;
                        despawnEffect=hitEffect =new ExplosionEffect(){{
                            waveColor=smokeColor=sparkColor= NuColor.PaleColor;
                            waveLife = 6f;
                            waveStroke = 3f;
                            waveRad = 15f;
                            waveRadBase = 2f;
                            sparkStroke = 1f;
                            sparkRad = 23f;
                            sparkLen = 3f;
                            smokeSize = 4f;
                            smokeSizeBase = 0.5f ;
                            smokeRad = 23f;
                            smokes = 5 ;
                            sparks = 7;
                        }};
                        fragBullets=1;
                        fragBullet = new LightningBulletType(){{
                            damage = 16;
                            collidesAir = true;
                            ammoMultiplier = 1.3f;
                            lightningColor = NuColor.PaleBackColor;
                            lightningLength = 3;
                            lightningLengthRand = 6;
                        }};
                    }};
                }};
            }});
        }};
        ripple = new UnitType("ripple"){{
            constructor = MechUnit::create;
            EntityMapping.nameMap.put(name,constructor);
            health = 5600;
            speed = 1.2f;
            researchCostMultiplier = 0.75f;
            armor = 25;
            hitSize = 14f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 200;
            abilities.add(new InterceptFieldAbility(40f, 6f, 30f, 1.2f, 8, 60f*2, 60f*10)
                    .color(NuColor.PaleColor)   // ★ 调用 .color() 会自动把 useShieldColor=false，不用再写开关！
                    .sides(32)
                    .rotation(0f));
        }};
        greatPath = new UnitType("greatPath"){{
            constructor = MechUnit::create;
            EntityMapping.nameMap.put(name,constructor);
            health = 12600;
            speed = 1.1f;
            researchCostMultiplier = 1f;
            armor = 32;
            hitSize = 21f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 200;
            abilities.add(new InterceptFieldAbility(48f, 6.5f, 38f, 1.4f, 10, 60f*2, 60f*10)
                    .color(NuColor.PaleColor)   // ★ 调用 .color() 会自动把 useShieldColor=false，不用再写开关！
                    .sides(32)
                    .rotation(0f));
        }};
        loyalRequest = new UnitType("loyalRequest"){{
            constructor = MechUnit::create;
            EntityMapping.nameMap.put(name,constructor);
            health = 25000;
            speed = 1f;
            researchCostMultiplier = 1.25f;
            armor = 52;
            hitSize = 30f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 200;
            abilities.add(new InterceptFieldAbility(64f, 5f, 80f, 1.6f, 12, 60f*2, 60f*10)
                    .color(NuColor.PaleColor)   // ★ 调用 .color() 会自动把 useShieldColor=false，不用再写开关！
                    .sides(32)
                    .rotation(0f));
        }};
        paladin = new UnitType("paladin"){{
            constructor = MechUnit::create;
            EntityMapping.nameMap.put(name,constructor);
            health = 56000;
            speed = 0.78f;
            researchCostMultiplier = 1.50f;
            armor = 86;
            hitSize = 36f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 200;
            abilities.add(new InterceptFieldAbility(80f, 6f, 106f, 2f, 15, 60f*2, 60f*10)
                    .color(NuColor.PaleColor)   // ★ 调用 .color() 会自动把 useShieldColor=false，不用再写开关！
                    .sides(32)
                    .rotation(0f));
        }};
        mornLight = new UnitType("mornLight"){{
            constructor = PayloadUnit::create;
            aiController = DefenderAI::new;
            flying = true;
            EntityMapping.nameMap.put(name,constructor);
            health = 1550;
            speed = 1.45f;
            researchCostMultiplier = 0.50f;
            // 拦截场：3 种简洁写法任选其一（都能正确变色，不再冗长）
            armor = 8;
            hitSize = 5f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 100f;
            payloadCapacity = 256f;
            abilities.add(new InterceptFieldAbility(16f, 5.5f, 20f, 0.85f, 7, 60f*4, 60f*15)
                    .color(NuColor.PaleColor)   // ★ 调用 .color() 会自动把 useShieldColor=false，不用再写开关！
                    .sides(32)
                    .rotation(0f));
            abilities.add(new StatusFieldAbility(StatusEffects.overclock,75f,120f,32f));
            // 最简洁（使用默认值：回 20% 血 + 5 秒无敌 + 「复活！」提示）
            abilities.add(new ResurrectionAbility(){{
                healPercent         = 0.1f;                         // 回血
                invincibleDuration  = 60f * 5;                       // 5 秒无敌
                resurrectText       = "神啊，再给我一次机会吧";          // 自拟文本
                resurrectColor      = NuColor.PaleColor;             // 和 Pale 单位同色
                resurrectEffect     = NuFx.explosion2;               // 小爆炸 10 像素那个
                floatTextDuration   = 60f * 2.5f;                    // 浮字 2.5 秒
                showBottomHint      = true;                         // 玩家的 HUD 也弹大字
            }});
            abilities.add(new RepairFieldAbility(65f, 60f * 2, 24f,0.05f));
            weapons.add(new Weapon("church"){{
                x = 4f;
                y = 0f;
                mirror = true;
                rotate = true;
                reload = 60f;
                recoil = 0.5f;
                shake = 0.3f;
                range = 100f;
                bullet = new BasicBulletType(5f,95f){{
                    lifetime = 12f;
                    width = 24f;
                    height = 24f;
                    maxRange = 100f;
                    splashDamage = 28f;
                    splashDamageRadius = 32f;
                    lightColor = frontColor = trailColor = NuColor.PaleColor;
                    backColor = NuColor.PaleBackColor;
                    trailLength = 10;
                    collidesGround = true;
                    trailInterval =2f;
                    trailEffect = new ParticleEffect(){{
                        particles = 4;
                        sizeFrom = 5f;
                        sizeTo = 1f;
                        length =7f;
                        baseLength = 3f;
                        lifetime = 13f;
                        colorFrom = NuColor.PaleColor;
                        colorTo = NuColor.PaleBackColor;
                    }};
                    fragBullets = 4;
                    fragRandomSpread = 40f;
                    fragBullet = new FlakBulletType(4f,40f){{
                        collidesAir = true;
                        lifetime = 10f;
                        ammoMultiplier = 1.3f;
                        splashDamage = 18f;
                        splashDamageRadius = 11f;
                        collidesGround = true;
                        lightColor = frontColor = trailColor = NuColor.PaleColor;
                        backColor = NuColor.PaleBackColor;
                        lifetime = 20f;
                    }};
                }};
            }});
        }};
        sunsetGlow = new UnitType("sunsetGlow"){{
            constructor = PayloadUnit::create;
            flying = true;
            EntityMapping.nameMap.put(name,constructor);
            health = 5600;
            speed = 1.25f;
            researchCostMultiplier = 0.75f;
            armor = 15;
            hitSize = 12f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 100f;
            payloadCapacity = 576f;
            abilities.add(new InterceptFieldAbility(20f, 3.5f, 34f, 0.7f, 10, 60f*4, 60f*15)
                    .color(NuColor.PaleColor)   // ★ 调用 .color() 会自动把 useShieldColor=false，不用再写开关！
                    .sides(32)
                    .rotation(0f));
        }};
        dusk = new  UnitType("dusk"){{
            constructor = PayloadUnit::create;
            flying = true;
            EntityMapping.nameMap.put(name,constructor);
            health = 10800;
            speed = 1.15f;
            researchCostMultiplier = 1.00f;
            armor = 21;
            hitSize = 20f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 100f;
            payloadCapacity = 1024f;
            abilities.add(new InterceptFieldAbility(24f, 4f, 45f, 0.85f, 12, 60f*4, 60f*15)
                    .color(NuColor.PaleColor)   // ★ 调用 .color() 会自动把 useShieldColor=false，不用再写开关！
                    .sides(32)
                    .rotation(0f));
        }};
        swallowingDay = new UnitType("swallowingDay"){{
            constructor = PayloadUnit::create;
            flying = true;
            EntityMapping.nameMap.put(name,constructor);
            health = 23600;
            speed = 1.0f;
            researchCostMultiplier = 1.25f;
            armor = 30;
            hitSize = 28f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 100f;
            payloadCapacity = 1600f;
            abilities.add(new InterceptFieldAbility(32f, 6f, 75f, 0.9f, 18, 60f*4, 60f*15)
                    .color(NuColor.PaleColor)   // ★ 调用 .color() 会自动把 useShieldColor=false，不用再写开关！
                    .sides(32)
                    .rotation(0f));
        }};
        moonLight = new UnitType("moonLight"){{
            constructor = PayloadUnit::create;
            flying = true;
            EntityMapping.nameMap.put(name,constructor);
            health = 59000;
            speed = 0.9f;
            researchCostMultiplier = 1.5f;
            armor = 40;
            hitSize = 36f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 100f;
            payloadCapacity = 2304f;
            abilities.add(new InterceptFieldAbility(40f, 7f,200f, 0.7f, 20, 60f*4, 60f*15)
                    .color(NuColor.PaleColor)   // ★ 调用 .color() 会自动把 useShieldColor=false，不用再写开关！
                    .sides(32)
                    .rotation(0f));
            abilities.add(new ResurrectionAbility(){{
                healPercent         = 0.75f;                         // 回血
                invincibleDuration  = 60f * 15;                       // 5 秒无敌
                resurrectText       = "我不后悔";          // 自拟文本
                resurrectColor      = NuColor.PaleColor;             // 和 Pale 单位同色
                resurrectEffect     = NuFx.explosion2;               // 小爆炸 10 像素那个
                floatTextDuration   = 60f * 4f;                    // 浮字 2.5 秒
                showBottomHint      = true;                         // 玩家的 HUD 也弹大字
            }});
        }};
        pureJade = new UnitType("PureJade"){{
                constructor = ElevationMoveUnit::create;
                EntityMapping.nameMap.put(name, constructor);
                health = 1550;
                speed = 1.55f;
                researchCostMultiplier = 0.50f;
                armor = 12;
                hitSize = 8f;
                alwaysUnlocked = false;
                targetAir = true;
                maxRange = 160f;
                mineRange = 120f;
                mineSpeed = 1f;
                mineTier = 2;
                mineWalls = true;
                mineFloor = true;
                mineHardnessScaling = true;
                healColor = lightColor = NuColor.PaleColor;
                buildRange = 120f;
                buildSpeed = 1f;
                engineOffset = -3;
                engineSize = 4f;
                lightRadius = 64f;
                fogRadius = 120f;
                targetAir = true;
                targetGround = true;
                lowAltitude = true;
                hovering = true;
                rotateMoveFirst = true;
                healFlash = true;
                forceMultiTarget = true;
                canDrown = false;
                shadowElevation = 0.1f;
                moveSound = Sounds.loopExtract;
                moveSoundVolume = 0.25f;
                moveSoundPitchMin = 0.7f;
                moveSoundPitchMax = 1.5f;
                abilities.add(new InterceptFieldAbility(20f, 5.5f, 20f, 1.4f, 12, 60f * 2, 60f * 4)
                        .color(NuColor.PaleColor)   // ★ 调用 .color() 会自动把 useShieldColor=false，不用再写开关！
                        .sides(32)
                        .rotation(0f));
                abilities.add(new ForceFieldAbility(16f, 0.2f, 300f, 60f * 10, 12, 36f));
                abilities.add(new MoveEffectAbility(0f, -7f, NuColor.PaleColor, Fx.missileTrailShort, 4f) {{
                    teamColor = true;
                }});
                weapons.add(new Weapon() {{
                    x = 4f;
                    y = 0f;
                    mirror = true;
                    rotate = true;
                    reload = 45f;
                    recoil = 0.5f;
                    shake = 0.3f;
                    range = 160f;
                    bullet = new FlakBulletType(4f, 65f) {{
                        lifetime = 25f;
                        width = 15f;
                        height = 24f;
                        splashDamage = 56f;
                        splashDamageRadius = 32f;
                        lightColor = frontColor = trailColor = NuColor.PaleColor;
                        backColor = NuColor.PaleBackColor;
                        trailLength = 10;
                        collidesGround = true;
                        trailInterval =2f;
                        trailEffect = new ParticleEffect(){{
                            particles = 2;
                            sizeFrom = 8f;
                            sizeTo = 1f;
                            length =7f;
                            baseLength = 3f;
                            lifetime = 10f;
                            colorFrom = NuColor.PaleColor;
                            colorTo = NuColor.PaleBackColor;
                        }};
                        despawnEffect=hitEffect = new MultiEffect( NuFx.PaleSmoke,NuFx.explosion2);
                        fragBullets = 4;
                        fragRandomSpread = 90f;
                        fragBullet = new BasicBulletType(5f,47f){{
                            lifetime = 12f;
                            width = 10f;
                            height = 18f;
                            lightColor = frontColor = trailColor = NuColor.PaleColor;
                            backColor = NuColor.PaleBackColor;
                            trailLength = 10;
                            despawnEffect = hitEffect = NuFx.explosion2;
                            homingPower = 1f;
                            homingDelay = 2f;
                            homingRange = 50f;
                        }};
                    }};
                }});
            }};
        darkMaple = new UnitType("darkMaple"){{
            constructor = ElevationMoveUnit::create;
            EntityMapping.nameMap.put(name, constructor);
            health = 4550;
            speed = 1.45f;
            researchCostMultiplier = 0.75f;
            armor = 24;
            hitSize = 16f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 160f;
            healColor = lightColor = NuColor.PaleColor;
            engineOffset = -3;
            engineSize = 8f;
            lightRadius = 128f;
            fogRadius = 120f;
            targetAir = true;
            targetGround = true;
            lowAltitude = true;
            hovering = true;
            rotateMoveFirst = true;
            healFlash = true;
            forceMultiTarget = true;
            canDrown = false;
            shadowElevation = 0.1f;
            moveSound = Sounds.loopExtract;
            moveSoundVolume = 0.25f;
            moveSoundPitchMin = 0.7f;
            moveSoundPitchMax = 1.5f;
            abilities.add(new InterceptFieldAbility(24f, 3.5f, 25f, 1.4f, 12, 60f * 2, 60f * 4)
                    .color(NuColor.PaleColor)   // ★ 调用 .color() 会自动把 useShieldColor=false，不用再写开关！
                    .sides(32)
                    .rotation(0f));
            abilities.add(new ForceFieldAbility(20f, 0.2f, 1200f, 60f * 10, 12, 36f));
            abilities.add(new MoveEffectAbility(0f, -7f, NuColor.PaleColor, Fx.missileTrailShort, 4f) {{
                teamColor = true;
            }});
        }};
        brightCrow = new UnitType("brightCrow"){{
            constructor = ElevationMoveUnit::create;
            EntityMapping.nameMap.put(name, constructor);
            health = 9500;
            speed = 1.35f;
            researchCostMultiplier = 1f;
            armor = 32;
            hitSize = 24f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 160f;
            healColor = lightColor = NuColor.PaleColor;
            engineOffset = -3;
            engineSize = 16f;
            lightRadius = 128f;
            fogRadius = 120f;
            targetAir = true;
            targetGround = true;
            lowAltitude = true;
            hovering = true;
            rotateMoveFirst = true;
            healFlash = true;
            forceMultiTarget = true;
            canDrown = false;
            shadowElevation = 0.1f;
            moveSound = Sounds.loopExtract;
            moveSoundVolume = 0.25f;
            moveSoundPitchMin = 0.7f;
            moveSoundPitchMax = 1.5f;
            abilities.add(new InterceptFieldAbility(32f, 2f, 25f, 1.7f, 20, 60f * 2, 60f * 4)
                    .color(NuColor.PaleColor)   // ★ 调用 .color() 会自动把 useShieldColor=false，不用再写开关！
                    .sides(32)
                    .rotation(0f));
            abilities.add(new ForceFieldAbility(24f, 0.2f, 4500f, 60f * 10, 12, 36f));
            abilities.add(new MoveEffectAbility(0f, -7f, NuColor.PaleColor, Fx.missileTrailShort, 4f) {{
                teamColor = true;
            }});
        }};
        saint = new  UnitType("saint"){{
            constructor = ElevationMoveUnit::create;
            EntityMapping.nameMap.put(name, constructor);
            health = 25500;
            speed = 1.05f;
            researchCostMultiplier = 1.25f;
            armor = 40;
            hitSize = 32f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 160f;
            healColor = lightColor = NuColor.PaleColor;
            engineOffset = -3;
            engineSize = 24f;
            lightRadius = 320f;
            fogRadius = 400f;
            targetAir = true;
            targetGround = true;
            lowAltitude = true;
            hovering = true;
            rotateMoveFirst = true;
            healFlash = true;
            forceMultiTarget = true;
            canDrown = false;
            shadowElevation = 0.1f;
            moveSound = Sounds.loopExtract;
            moveSoundVolume = 0.25f;
            moveSoundPitchMin = 0.7f;
            moveSoundPitchMax = 1.5f;
            abilities.add(new InterceptFieldAbility(48f, 7f, 98f, 2f, 24, 60f * 2, 60f * 4)
                    .color(NuColor.PaleColor)   // ★ 调用 .color() 会自动把 useShieldColor=false，不用再写开关！
                    .sides(32)
                    .rotation(0f));
            abilities.add(new ForceFieldAbility(48f, 0.2f, 10000f, 60f * 10, 12, 36f));
            abilities.add(new MoveEffectAbility(0f, -7f, NuColor.PaleColor, Fx.missileTrailShort, 4f) {{
                teamColor = true;
            }});
        }};
        bloodLotus = new  UnitType("bloodLotus"){{
            constructor = ElevationMoveUnit::create;
            EntityMapping.nameMap.put(name, constructor);
            health = 58000;
            speed = 0.85f;
            researchCostMultiplier = 1.5f;
            armor = 48;
            hitSize = 40f;
            alwaysUnlocked = false;
            targetAir = true;
            maxRange = 160f;
            healColor = lightColor = NuColor.PaleColor;
            engineOffset = -3;
            engineSize = 32f;
            lightRadius = 128f;
            fogRadius = 120f;
            targetAir = true;
            targetGround = true;
            lowAltitude = true;
            hovering = true;
            rotateMoveFirst = true;
            healFlash = true;
            forceMultiTarget = true;
            canDrown = false;
            shadowElevation = 0.1f;
            moveSound = Sounds.loopExtract;
            moveSoundVolume = 0.25f;
            moveSoundPitchMin = 0.7f;
            moveSoundPitchMax = 1.5f;
            abilities.add(new InterceptFieldAbility(48f, 7f, 230f, 2.3f, 30, 60f * 2, 60f * 4)
                    .color(NuColor.PaleColor)   // ★ 调用 .color() 会自动把 useShieldColor=false，不用再写开关！
                    .sides(32)
                    .rotation(0f));
            abilities.add(new ForceFieldAbility(80f, 0.2f, 24000f, 60f * 10, 12, 36f));
            abilities.add(new MoveEffectAbility(0f, -7f, NuColor.PaleColor, Fx.missileTrailShort, 4f) {{
                teamColor = true;
            }});
        }};
        hometown = new UnitType("hometown"){{
            constructor=LegsUnit::create;
            EntityMapping.nameMap.put(name, constructor);
            health = 12000;
            speed = 0.85f;
            researchCostMultiplier = 1f;
            armor = 48;
            hitSize = 24f;
            legCount = 6;
            legMoveSpace = 1.45f;
            lockLegBase = true;
            legContinuousMove = true;
            legGroupSize = 3;
            legPairOffset = 5f;
            legLength = 40;
            legExtension = -6f;
            legBaseOffset = 15f;
            legSpeed = 3f;
            legLengthScl = 1.5f;
            stepShake = 1.5f;
            rippleScale = 1.25f;
            legSplashDamage = 85f;
            legSplashRange  = 24f;
            hovering = true;
            legPhysicsLayer = true;
            rotateMoveFirst = true;
            healFlash = true;
            shadowElevation = 0.7f;
            drownTimeMultiplier = 2f;
            targetAir = true;
            targetGround = true;
            weapons.add(new Weapon("real-knife"){{
                x = -18f;
                y = 0f;
                reload = 135f;
                shoot = new ShootAlternate(){{
                    shots = 18;
                    shotDelay =5f;
                    spread = 3f;
                    barrels =3;
                }};
                rotate = false;
                range = 175f;
                mirror = true;
                shake = 5f;
                bullet = new BasicBulletType(0f,0f){{
                   maxRange = 160f;
                   instantDisappear = true;
                   fragBullets = 2;
                   fragRandomSpread = 40f;
                   fragLifeMax = 1.5f;
                   fragLifeMin = 0.8f;
                   fragBullet = new FlakBulletType(6f,50f){{
                       lifetime = 30f;
                       width = 9f;
                       height = 60f;
                       pierceBuilding = true;
                       pierce = true;
                       pierceCap = 5;
                       frontColor = lightColor = trailColor = NuColor.SpecialPurpleColor;
                       backColor = hitColor = NuColor.SpecialPurpleBackColor;
                       homingPower = 2f;
                       homingRange = 160f;
                       homingDelay = 2f;
                       splashDamage = 12f;
                       splashDamageRadius = 24f;
                       intervalBullets = 2;
                       bulletInterval = 4f;
                       intervalRandomSpread = 45f;
                       intervalBullet = new LightningBulletType(){{
                           lifetime = 12f;
                           damage = 9f;
                           status = StatusEffects.shocked;
                           lightningLength = 10;
                           lightningLengthRand = 2;
                           lightningColor = NuColor.SpecialPurpleColor;
                       }};
                   }};
                }};
            }});
            weapons.add(new Weapon("knife1"){{
                reload = 10f;
                x = 8f;
                y = 0f;
                shootSound = Sounds.shootSap;
                range = 120f;
                mirror = true;
                shake = 5f;
                bullet = new SapBulletType(){{
                    damage = 400f;
                    length = 135;
                    width = 0.6f;
                    lifetime = 30f;
                    knockback = -1f;
                    lightColor = trailColor = NuColor.SpecialPurpleColor;
                    hitColor = NuColor.SpecialPurpleBackColor;
                    shootEffect = new WaveEffect(){{
                        strokeFrom = 2.5f;
                        strokeTo = 0f;
                        colorFrom = NuColor.SpecialPurpleColor;
                        colorTo = NuColor.SpecialPurpleBackColor;
                        sizeFrom = 8f;
                        sizeTo = 1f;
                        lifetime =20f;
                        sides = 12;
                    }};
                }};
            }});
            weapons.add(new Weapon("knife2"){{
                reload = 5f;
                x = 8f;
                y = 5f;
                shootSound = Sounds.shootSap;
                range = 100f;
                mirror = true;
                shake = 5f;
                bullet = new SapBulletType(){{
                    damage = 340f;
                    length = 115;
                    width = 0.6f;
                    lifetime = 10f;
                    knockback = -1f;
                    lightColor = trailColor = NuColor.SpecialPurpleColor;
                    hitColor = NuColor.SpecialPurpleBackColor;
                    shootEffect = new WaveEffect(){{
                        strokeFrom = 2.5f;
                        strokeTo = 0f;
                        colorFrom = NuColor.SpecialPurpleColor;
                        colorTo = NuColor.SpecialPurpleBackColor;
                        sizeFrom = 8f;
                        sizeTo = 1f;
                        lifetime =20f;
                        sides = 12;
                    }};
                }};
            }});
        }};
        humorous = new ErekirUnitType("humorous"){{
            constructor = BuildingTetherPayloadUnit::create;
            EntityMapping.nameMap.put(name, constructor);
            health = 2000;
            armor = 1000;
            speed = 4f;
            flying = true;
            targetable = true;
            engineSize = 2f;
            hitSize = 4f;
            buildSpeed = 5f;
            drag = 0.058f;
            accel = 0.1f;
            itemCapacity = 45;
            alwaysUnlocked = true;
            useUnitCap = false;
            logicControllable = false;
            playerControllable = false;
            allowedInPayloads = false;
            buildBeamOffset = 10f;
            createWreck = false;
            controller = u -> new AssemblerAI();
            hidden = true;
            isEnemy = false;
            killable = false;
        }};
    }
}