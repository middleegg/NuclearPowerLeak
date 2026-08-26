package Npl.content;

import arc.graphics.Color;
import arc.util.Time;
import mindustry.content.Planets;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.graphics.g3d.*;
import mindustry.type.ItemStack;
import mindustry.type.Planet;
import mindustry.world.meta.Env;
import Npl.content.NuItems;  // 你的物品
import Npl.newSth.*;
import mindustry.maps.planet.SerpuloPlanetGenerator;
import static mindustry.graphics.g3d.PlanetRenderer.outlineColor;
import static mindustry.graphics.g3d.PlanetRenderer.outlineRad;

public class Azer {
    // 声明你的星球
    public static Planet Azer;

    public static void load() {
        // 创建星球
        Azer = new Planet(
                "Azer",          // 名称（对应图片文件名）
                Planets.sun,          // 绕哪个恒星转（或 null）
                1.3f,                 // 大小（相对于地球）
                3                     // 生成器种子
        ){{
            // ===== 基础属性 =====
            visible = true;               // 在星图中可见
            accessible = true;            // 可点击进入
            alwaysUnlocked = true;        // 始终解锁（不需要科技树）
            iconColor = Color.valueOf("E2FF6D");  // 星图上的颜色
            defaultCore = NuBlocks.FederalJuniorCore;
            launchCapacityMultiplier = 0.6F;
            // ===== 地形生成 =====
            // 使用六边形网格（标准行星）
            meshLoader = () -> new HexMesh(this, 6);
            sectorApproxRadius = 0.4f;
            // 使用自定义地形生成器（如果不需要特殊地形，用默认的）
            // generator = new MyPlanetGenerator();  // 需要自定义的话

            // ===== 规则设置 =====
            ruleSetter = r -> {
                r.waves = true;                           // 启用波次
                r.waveTeam = Team.green;                   // 波次属于哪个队伍
                r.placeRangeCheck = false;                // 不限制建造范围
                r.hideSpawns = true;                     // 显示敌人出生点
                r.waveSpacing = 76 * Time.toSeconds;      // 波次间隔 60 秒
                r.initialWaveSpacing = 5f * Time.toMinutes; // 第一波前准备 5 分钟
                r.hideBannedBlocks = true;                // 隐藏被禁用的方块
                // 初始物资（开局给的物品）
                r.loadout = ItemStack.list(
                        NuItems.bigIron, 200   // 生铁 100 个
                );
                // 队伍规则
                Rules.TeamRule teamRule = r.teams.get(r.defaultTeam);
                teamRule.rtsAi = true;                   //  RTS AI
                teamRule.unitBuildSpeedMultiplier = 1f;   // 单位建造速度  倍率
                teamRule.buildSpeedMultiplier = 1f;       // 建筑建造速度  倍率
            };
            // ===== 大气和环境 =====
            atmosphereColor = Color.valueOf("5BFFAD");    // 大气颜色
            landCloudColor = Color.valueOf("CDBFA3");     // 陆地云层颜色
            atmosphereRadIn = 0.2f;                      // 大气内半径
            atmosphereRadOut = 0.45f;                     // 大气外半径
            bloom = true;
            generator= new AzerPlanetGenerator();
            // ===== 轨道环（可选） =====
            // 如果要做环，需要创建 cloudMeshLoader
            meshLoader = () -> new NoiseMesh(
                    Azer,           // 星球对象
                    123,              // 种子
                    6,                  // 网格细分
                    1.24f,                // 半径
                    12, 0.3f, 1.2f, 1.2f,  // 地形噪声参数
                    Color.valueOf("CDBFA3"),        // 颜色1：绿色（陆地）
                    Color.valueOf("C5FFE2"),         // 颜色2：蓝色（海洋）
                    1,                  // 颜色噪声八度
                    0.25f,               // 颜色噪声持久性
                    0.6f,               // 颜色噪声频率
                    0.55f                // 颜色阈值：噪声>0.4显示蓝色，否则显示绿色
            );
            cloudMeshLoader = () -> new MultiMesh(
                    // 第一层云
                    new HexSkyMesh(this, 11, 0.15f, 0.14f, 5, Color.valueOf("C5FFE2").a(0.75f), 2, 0.45f, 0.9f, 0.38f),
                    // 第二层云
                    new HexSkyMesh(this, 3, 0.6f, 0.15f, 5, Color.valueOf("C5FFE2").a(0.75f), 2, 0.45f, 1f, 0.41f)
            );
        }};
    }
}