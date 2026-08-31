package Npl.Rouge;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.maps.*;
import mindustry.type.*;

/**
 * RougeMaps —— Rouge 模式关卡配置中心
 * ==============================================================
 * 集中管理所有 Rouge 模式的关卡配置：
 *   - 地图难度与波次
 *   - 地图描述与大小
 *   - 终点地图定义
 *   - 可用地图列表
 *   - 敌人波次配置
 */
public class RougeMaps {

    // ==================== 地图配置数据类 ====================

    /** 单个地图的完整配置 */
    public static class MapConfig {
        public String mapName;
        public String displayName;
        public String description;
        public int width;
        public int height;
        public float difficulty;
        public int baseWaves;
        public boolean isEndMap;

        public MapConfig(String mapName, String displayName, String description, int width, int height, float difficulty, int baseWaves, boolean isEndMap){
            this.mapName = mapName;
            this.displayName = displayName;
            this.description = description;
            this.width = width;
            this.height = height;
            this.difficulty = difficulty;
            this.baseWaves = baseWaves;
            this.isEndMap = isEndMap;
        }
    }

    // ==================== 终点地图定义 ====================

    /** 三条分支的终点地图 */
    public static final String[] END_MAPS = {
        "planetaryTerminal",    // 左线终点
        "geothermalStronghold", // 中线终点
        "testingGrounds"        // 右线终点
    };

    /** 终点地图显示名称 */
    private static final String[] END_MAP_NAMES = {
        "行星终端",
        "地热要塞",
        "试验场"
    };

    // ==================== 所有可用地图配置 ====================

    private static final ObjectMap<String, MapConfig> MAP_CONFIGS = new ObjectMap<>();

    static{
        // 起点地图
        MAP_CONFIGS.put("frozenForest", new MapConfig("frozenForest", "冰封森林",
            "初始关卡，适合新手熟悉操作", 480, 480, 1.0f, 5, false));

        // 终点地图
        MAP_CONFIGS.put("planetaryTerminal", new MapConfig("planetaryTerminal", "行星终端",
            "通往深空的中转站，最终决战之地", 300, 300, 3.0f, 15, true));
        MAP_CONFIGS.put("geothermalStronghold", new MapConfig("geothermalStronghold", "地热要塞",
            "利用地热能源的地下堡垒", 280, 280, 3.5f, 12, true));
        MAP_CONFIGS.put("testingGrounds", new MapConfig("testingGrounds", "试验场",
            "武器测试与战术演练区域", 320, 320, 4.0f, 12, true));

        // 普通关卡
        MAP_CONFIGS.put("atolls", new MapConfig("atolls", "环礁",
            "水域环绕的岛屿群", 256, 256, 1.5f, 8, false));
        MAP_CONFIGS.put("biomassFacility", new MapConfig("biomassFacility", "生物质设施",
            "研究生物质能的废弃工厂", 256, 256, 1.8f, 8, false));
        MAP_CONFIGS.put("coastline", new MapConfig("coastline", "海岸线",
            "沿海防御阵地", 300, 200, 1.6f, 8, false));
        MAP_CONFIGS.put("crateredBattleground", new MapConfig("crateredBattleground", "陨石坑战场",
            "经历过激烈交战的撞击坑区域", 280, 280, 2.0f, 10, false));
        MAP_CONFIGS.put("cruxscape", new MapConfig("cruxscape", "赤红地貌",
            "被赤红砂岩覆盖的荒芜之地", 256, 256, 2.2f, 10, false));
        MAP_CONFIGS.put("desolateRift", new MapConfig("desolateRift", "荒凉裂谷",
            "深不见底的裂谷地带", 256, 300, 2.5f, 10, false));
        MAP_CONFIGS.put("extractionOutpost", new MapConfig("extractionOutpost", "采集前哨",
            "资源采集前哨站", 200, 200, 1.8f, 8, false));
        MAP_CONFIGS.put("facility32m", new MapConfig("facility32m", "32号设施",
            "编号32的秘密研究设施", 300, 300, 2.3f, 10, false));
        MAP_CONFIGS.put("fallenVessel", new MapConfig("fallenVessel", "坠落飞船",
            "坠毁的星际飞船残骸", 256, 256, 2.0f, 10, false));
        MAP_CONFIGS.put("frontier", new MapConfig("frontier", "边境",
            "文明边缘的哨站", 256, 256, 1.7f, 8, false));
        MAP_CONFIGS.put("fungalPass", new MapConfig("fungalPass", "真菌通道",
            "被真菌覆盖的地下通道", 280, 280, 2.1f, 10, false));
        MAP_CONFIGS.put("impact0078", new MapConfig("impact0078", "0078号撞击点",
            "0078号小行星撞击点", 256, 256, 2.4f, 10, false));
        MAP_CONFIGS.put("infestedCanyons", new MapConfig("infestedCanyons", "感染峡谷",
            "被虫群感染的深邃峡谷", 300, 250, 2.6f, 12, false));
        MAP_CONFIGS.put("littoralShipyard", new MapConfig("littoralShipyard", "滨海船坞",
            "沿海的船坞设施", 280, 200, 2.0f, 8, false));
        MAP_CONFIGS.put("mycelialBastion", new MapConfig("mycelialBastion", "菌丝堡垒",
            "菌丝构成的奇异堡垒", 256, 256, 2.3f, 10, false));
        MAP_CONFIGS.put("navalFortress", new MapConfig("navalFortress", "海军要塞",
            "海上军事要塞", 300, 250, 2.5f, 12, false));
        MAP_CONFIGS.put("nuclearComplex", new MapConfig("nuclearComplex", "核能综合设施",
            "大型核能发电设施", 350, 350, 2.8f, 12, false));
        MAP_CONFIGS.put("overgrowth", new MapConfig("overgrowth", "过度生长",
            "植物过度生长的区域", 256, 256, 1.9f, 8, false));
        MAP_CONFIGS.put("perilousHarbor", new MapConfig("perilousHarbor", "危险港",
            "危险的港口区域", 250, 200, 2.2f, 10, false));
        MAP_CONFIGS.put("ruinousShores", new MapConfig("ruinousShores", "废墟海岸",
            "满是废墟的海岸线", 300, 200, 2.1f, 10, false));
        MAP_CONFIGS.put("saltFlats", new MapConfig("saltFlats", "盐滩",
            "广阔的盐田地带", 400, 400, 2.0f, 8, false));
        MAP_CONFIGS.put("stainedMountains", new MapConfig("stainedMountains", "染血山脉",
            "被战争染红的山脉", 300, 300, 2.7f, 12, false));
        MAP_CONFIGS.put("sunkenPier", new MapConfig("sunkenPier", "沉没码头",
            "沉入水下的旧码头", 200, 200, 1.8f, 8, false));
        MAP_CONFIGS.put("taintedWoods", new MapConfig("taintedWoods", "腐化树林",
            "被腐化污染的森林", 256, 256, 2.0f, 10, false));
        MAP_CONFIGS.put("tarFields", new MapConfig("tarFields", "焦油油田",
            "焦油资源丰富的油田", 300, 300, 2.4f, 10, false));
        MAP_CONFIGS.put("weatheredChannels", new MapConfig("weatheredChannels", "风蚀水道",
            "被风蚀作用塑造的水道", 256, 300, 2.1f, 10, false));
        MAP_CONFIGS.put("windsweptIslands", new MapConfig("windsweptIslands", "风蚀群岛",
            "常年被风吹拂的群岛", 350, 200, 2.3f, 10, false));
    }

    // ==================== 查询方法 ====================

    /** 获取地图配置 */
    public static MapConfig getMapConfig(String mapName){
        return MAP_CONFIGS.get(mapName);
    }

    /** 获取地图显示名称 */
    public static String getMapDisplayName(String mapName){
        MapConfig cfg = MAP_CONFIGS.get(mapName);
        return cfg != null ? cfg.displayName : mapName;
    }

    /** 获取地图描述 */
    public static String getMapDescription(String mapName){
        MapConfig cfg = MAP_CONFIGS.get(mapName);
        return cfg != null ? cfg.description : "未知区域，需要探索";
    }

    /** 获取地图大小字符串 */
    public static String getMapSize(String mapName){
        MapConfig cfg = MAP_CONFIGS.get(mapName);
        return cfg != null ? cfg.width + "×" + cfg.height : "256×256";
    }

    /** 获取地图难度 */
    public static float getMapDifficulty(String mapName){
        MapConfig cfg = MAP_CONFIGS.get(mapName);
        return cfg != null ? cfg.difficulty : 1.0f;
    }

    /** 获取地图基础波次 */
    public static int getMapBaseWaves(String mapName){
        MapConfig cfg = MAP_CONFIGS.get(mapName);
        return cfg != null ? cfg.baseWaves : 5;
    }

    /** 检查是否是终点地图 */
    public static boolean isEndMap(String mapName){
        for(String end : END_MAPS){
            if(end.equals(mapName)) return true;
        }
        return false;
    }

    /** 获取终点地图显示名称 */
    public static String getEndMapDisplayName(int branchIndex){
        if(branchIndex >= 0 && branchIndex < END_MAP_NAMES.length){
            return END_MAP_NAMES[branchIndex];
        }
        return END_MAPS[branchIndex];
    }

    /** 获取指定分支的终点地图名称 */
    public static String getEndMapName(int branchIndex){
        if(branchIndex >= 0 && branchIndex < END_MAPS.length){
            return END_MAPS[branchIndex];
        }
        return null;
    }

    /** 获取所有终点地图名称 */
    public static String[] getEndMaps(){
        return END_MAPS;
    }

    // ==================== 可用地图列表 ====================

    /** 加载 rougeMaps 文件夹里的可用地图列表（排除起点和终点） */
    public static Seq<String> loadAvailableMaps(){
        Seq<String> maps = new Seq<>();
        try{
            Fi dir = Core.files.absolute("G:\\NuclearPowerLeak\\assets\\rougeMaps");
            if(!dir.exists()){
                dir = getModAssets().child("rougeMaps");
            }
            if(dir.exists()){
                for(Fi f : dir.list()){
                    String name = f.name();
                    if(name.endsWith(".msav")){
                        String baseName = name.substring(0, name.length() - 5);
                        if(!baseName.equals("frozenForest") &&
                           !baseName.equals(END_MAPS[0]) &&
                           !baseName.equals(END_MAPS[1]) &&
                           !baseName.equals(END_MAPS[2])){
                            maps.add(baseName);
                        }
                    }
                }
            }
        }catch(Exception e){
            Log.err("RougeMaps: failed to load map list", e);
        }
        // 注意：dir.list() 的顺序取决于文件系统，所以同一 seed 在不同机器上可能生成不同的树。
        // 联机时树是由房主整体同步过去的（RougeTree.writeTo/readFrom），不依赖这个顺序。
        return maps;
    }

    /**
     * 解析 rougeMaps 下的地图文件：开发机绝对路径优先，其次 mod 的 assets。
     * @return 不存在的文件返回 null
     */
    public static Fi getMapFile(String mapName){
        try{
            Fi file = Core.files.absolute("G:\\NuclearPowerLeak\\assets\\rougeMaps\\" + mapName + ".msav");
            if(file != null && file.exists()) return file;

            Fi alt = getModAssets().child("rougeMaps/" + mapName + ".msav");
            if(alt != null && alt.exists()) return alt;
        }catch(Exception e){
            Log.err("RougeMaps: failed to resolve map file " + mapName, e);
        }
        return null;
    }

    // ==================== 敌人波次配置 ====================

    /** 根据地图难度生成敌人波次配置 */
    public static Seq<SpawnGroup> generateSpawnGroups(String mapName){
        float difficulty = getMapDifficulty(mapName);
        int baseWaves = getMapBaseWaves(mapName);

        Seq<SpawnGroup> groups = new Seq<>();

        // 基础敌人 - 从第1波开始
        SpawnGroup dagger = new SpawnGroup(UnitTypes.dagger);
        dagger.unitAmount = (int)(3 + difficulty);
        dagger.begin = 1;
        dagger.spacing = Math.max(1, (int)(3 - difficulty * 0.5f));
        dagger.max = (int)(12 + difficulty * 4);
        dagger.unitScaling = 1.0f + difficulty * 0.3f;
        groups.add(dagger);

        // 中级敌人 - 从第3波开始
        SpawnGroup crawler = new SpawnGroup(UnitTypes.crawler);
        crawler.unitAmount = (int)(2 + difficulty * 0.5f);
        crawler.begin = 3;
        crawler.spacing = Math.max(1, (int)(4 - difficulty * 0.3f));
        crawler.max = (int)(8 + difficulty * 2);
        crawler.unitScaling = 0.8f + difficulty * 0.2f;
        groups.add(crawler);

        // 高级敌人 - 从第5波或第8波开始
        if(difficulty >= 2.0f){
            SpawnGroup vela = new SpawnGroup(UnitTypes.vela);
            vela.unitAmount = (int)(1 + difficulty * 0.3f);
            vela.begin = Math.min(baseWaves / 3, 8);
            vela.spacing = Math.max(2, (int)(5 - difficulty * 0.2f));
            vela.max = (int)(5 + difficulty);
            vela.unitScaling = 0.7f + difficulty * 0.15f;
            groups.add(vela);
        }

        // 精英敌人 - 高难度地图
        if(difficulty >= 3.0f){
            SpawnGroup reign = new SpawnGroup(UnitTypes.reign);
            reign.unitAmount = 1;
            reign.begin = Math.min(baseWaves / 2, 10);
            reign.spacing = 4;
            reign.max = (int)(3 + difficulty);
            reign.unitScaling = 0.5f + difficulty * 0.1f;
            groups.add(reign);
        }

        return groups;
    }

    // ==================== 地图图标 ====================

    /** 获取节点的图标贴图 */
    public static TextureRegion getNodeIcon(String mapName){
        try{
            // 优先从 atlas 查找（mod资源需要加 nu- 前缀）
            TextureRegion region = Core.atlas.find("nu-" + mapName);
            if(region != null && region.texture != null && region.found()) return region;

            // 尝试从文件加载
            Fi iconFile = getModAssets().child("sprites/ui/sectors/" + mapName + ".png");
            if(!iconFile.exists()){
                iconFile = Core.files.absolute("G:\\NuclearPowerLeak\\assets\\sprites\\ui\\sectors\\" + mapName + ".png");
            }
            if(iconFile.exists()){
                Pixmap pix = new Pixmap(iconFile);
                // 验证 Pixmap 尺寸有效
                if(pix.width > 0 && pix.height > 0){
                    Texture tex = new Texture(pix);
                    pix.dispose();
                    return new TextureRegion(tex);
                }else{
                    pix.dispose();
                }
            }
        }catch(Exception e){
            // 静默失败
        }
        return Core.atlas.find("white");
    }

    // ==================== 工具方法 ====================

    /** 获取 mod assets 目录 */
    private static Fi getModAssets(){
        try{
            return mindustry.Vars.mods.getMod("nu").root;
        }catch(Exception e){
            return Core.files.local("assets");
        }
    }
}
