package Npl.Rouge;

import arc.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.type.*;
import mindustry.world.modules.*;

import java.io.*;

import static mindustry.Vars.*;

/**
 * RougeSave —— Rouge 战役模式的核心数据库
 * ==============================================================
 * 存档独立：保存在用户数据目录下的 rouge_save.bin，
 *           和原版战役存档（campaign save slots）完全隔离，互不影响。
 *
 *   ① completedSectors：已完成（撤离成功）的 sector 名称集合
 *   ② storedItems：跨 sector 携带的资源（用 ObjectMap 避免 ItemModule 数组越界）
 *   ③ currentSectorName：当前正在玩的 sector 名称
 */
public class RougeSave {

    /** 已完成的 sector 名称集合（key = SectorPreset.name） */
    public static ObjectSet<String> completedSectors = new ObjectSet<>();

    /** 跨 sector 携带的资源（Item → 数量）。用 ObjectMap 而非 ItemModule，避免数组越界 */
    public static ObjectMap<Item, Integer> storedItems = new ObjectMap<>();

    /** 当前正在玩的 sector 名称（null 表示不在 Rouge 战役中） */
    public static String currentSectorName = null;

    /** 当前 sector 的撤离波次阈值（由难度决定，5/10/15/20/25） */
    public static int currentEvacWave = 0;

    /** 备份的原版 sector 存档（启动 Rouge 前保存，结束后恢复，保证不污染原版战役） */
    public static Object backupSave = null;
    /** 备份的原版 sector 信息 */
    public static Object backupInfo = null;

    // ===== 树杈状关卡进度 =====

    /** 树随机种子（用种子重建同一棵树） */
    public static long treeSeed = 0;

    /** 已完成的树节点 ID 列表 */
    public static IntSeq completedNodeIds = new IntSeq();

    /** 分叉点的选择记录：key=节点ID，value=0选左/1选中/2选右 */
    public static IntMap<Integer> branchChoices = new IntMap<>();

    /** 科技树等级：key=科技节点ID hashCode，value=等级 */
    public static IntMap<Integer> techLevels = new IntMap<>();

    /** 短期记忆容器：用于临时存放游戏过程中的数据（如节点状态、事件标记等） */
    public static ObjectMap<String, Object> shortTermMemory = new ObjectMap<>();

    /** 当前是否处于 Rouge 模式（用于控制 UI 显示等） */
    public static boolean isInRougeMode = false;

    /** 存档文件名 */
    private static final String SAVE_FILE = "rouge_save.bin";

    /**
     * 根据 sector 难度计算撤离波次阈值（v104.6 玩法）
     */
    public static int evacWaveFor(float difficulty){
        if(difficulty <= 1f) return 5;
        if(difficulty <= 2f) return 10;
        if(difficulty <= 3f) return 15;
        if(difficulty <= 4f) return 20;
        return 25;
    }

    /** 难度对应的中文名 */
    public static String difficultyName(float difficulty){
        if(difficulty <= 1f) return "简单";
        if(difficulty <= 2f) return "普通";
        if(difficulty <= 3f) return "困难";
        if(difficulty <= 4f) return "疯狂";
        return "毁灭";
    }

    /** 判断一个 sector 是否完成 */
    public static boolean isCompleted(SectorPreset preset){
        return preset != null && completedSectors.contains(preset.name);
    }

    /** 标记一个 sector 为已完成 */
    public static void markCompleted(SectorPreset preset){
        if(preset != null) completedSectors.add(preset.name);
    }

    /** 获取存储的某物品数量 */
    public static int getStored(Item item){
        return storedItems.get(item, 0);
    }

    /** 获取存储资源总数量 */
    public static int storedTotal(){
        int sum = 0;
        for(var e : storedItems.entries()) sum += e.value;
        return sum;
    }

    /**
     * 从核心数据库里取出存储的资源（开局时调用，把资源加到核心里）
     * 直接遍历 storedItems，不用 ItemModule 中转
     */
    public static void applyStoredToCore(ItemModule coreItems){
        for(var e : storedItems.entries()){
            if(e.value > 0) coreItems.add(e.key, e.value);
        }
    }

    /**
     * 撤离时把核心里的资源存进数据库（跨 sector 携带）
     * @param coreItems 核心的 ItemModule
     * @param ratio 携带比例（0~1），默认 1.0 = 全部带走
     */
    public static void storeFromCore(ItemModule coreItems, float ratio){
        storedItems.clear();
        for(Item item : content.items()){
            int amount = coreItems.get(item);
            if(amount > 0){
                storedItems.put(item, (int)(amount * ratio));
            }
        }
    }

    /**
     * 保存到文件。
     * 联机客户端不落盘：客户端只镜像房主进度，自己的 rouge_save.bin 不应被别人的战役覆盖。
     */
    public static void save(){
        if(net.client()){
            return;
        }

        try{
            File file = saveFile();
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            Writes write = new Writes(out);
            writeTo(write);
            write.close();
            out.close();
        }catch(Exception e){
            Log.err("RougeSave save failed", e);
        }

        // 房主：进度变化后同步给联机客户端（SP 下是空操作）
        RougeNet.onSave();
    }

    /** 把全部状态写入给定的 Writes（文件存档与联机同步共用同一套字段顺序） */
    public static void writeTo(Writes write){
        // ① 已完成 sector
        write.i(completedSectors.size);
        for(String name : completedScoresToArray()){
            write.str(name);
        }
        // ② 携带资源
        write.i(storedItems.size);
        for(var e : storedItems.entries()){
            write.s(e.key.id);
            write.i(e.value);
        }
        // ③ 树种子
        write.l(treeSeed);
        // ④ 已完成节点 ID
        write.i(completedNodeIds.size);
        for(int i = 0; i < completedNodeIds.size; i++){
            write.i(completedNodeIds.get(i));
        }
        // ⑤ 分叉选择
        write.i(branchChoices.size);
        for(var entry : branchChoices.entries()){
            write.i(entry.key);
            write.i(entry.value);
        }
        // ⑥ 科技树等级
        write.i(techLevels.size);
        for(var entry : techLevels.entries()){
            write.i(entry.key);
            write.i(entry.value);
        }
        // ⑦ 短期记忆容器
        write.i(shortTermMemory.size);
        for(var entry : shortTermMemory.entries()){
            write.str(entry.key);
            write.str(entry.value != null ? entry.value.toString() : "");
        }
    }

    /** 从文件加载 */
    public static void load(){
        try{
            File file = saveFile();
            if(!file.exists()){
                // 文件不存在 = 没有任何进度。必须清空内存态，
                // 否则刚从别人房间退出来时会把房主/上一局的状态留在内存里。
                clearProgress();
                return;
            }

            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            Reads read = new Reads(in);
            readFrom(read, in);
            read.close();
            in.close();
        }catch(Exception e){
            Log.err("RougeSave load failed", e);
            // 读失败就当没有存档，避免内存里残留上一局 / 房主房间里的数据
            clearProgress();
        }
    }

    /** 清空内存中的进度（不写盘） */
    public static void clearProgress(){
        completedSectors.clear();
        storedItems.clear();
        currentSectorName = null;
        currentEvacWave = 0;
        treeSeed = 0;
        completedNodeIds.clear();
        branchChoices.clear();
        techLevels.clear();
        shortTermMemory.clear();
        isInRougeMode = false;
    }

    /**
     * 从给定的 Reads 读取状态（文件存档与联机同步共用）。
     * @param probe 旧存档兼容用的输入流（据此判断后半段是否存在）；联机 payload 传 null（段落总是齐全）
     */
    public static void readFrom(Reads read, InputStream probe){
        completedSectors.clear();
        storedItems.clear();
        completedNodeIds.clear();
        branchChoices.clear();
        techLevels.clear();
        shortTermMemory.clear();

        int count = readCount(read);
        for(int i = 0; i < count; i++){
            completedSectors.add(read.str());
        }

        int itemCount = readCount(read);
        for(int i = 0; i < itemCount; i++){
            short itemId = read.s();
            int amount = read.i();
            Item item = content.item(itemId);
            if(item != null && amount > 0){
                storedItems.put(item, amount);
            }
        }

        // ③ 树种子
        treeSeed = read.l();

        // ④ 已完成节点 ID
        int nodeCount = readCount(read);
        for(int i = 0; i < nodeCount; i++){
            completedNodeIds.add(read.i());
        }

        // ⑤ 分叉选择
        int branchCount = readCount(read);
        for(int i = 0; i < branchCount; i++){
            int nodeId = read.i();
            int branch = read.i();
            branchChoices.put(nodeId, branch);
        }

        // ⑥ 科技树等级（旧存档可能没有这一段）
        if(probe == null || hasMore(probe)){
            int techCount = readCount(read);
            for(int i = 0; i < techCount; i++){
                int techId = read.i();
                int level = read.i();
                techLevels.put(techId, level);
            }
        }

        // ⑦ 短期记忆容器（旧存档可能没有这一段）
        if(probe == null || hasMore(probe)){
            int memoryCount = readCount(read);
            for(int i = 0; i < memoryCount; i++){
                String key = read.str();
                String value = read.str();
                shortTermMemory.put(key, value);
            }
        }
    }

    /**
     * 读取一个“计数”字段并做合理性检查。
     * 联机状态下这些字节直接来自网络，损坏/截断的包会让后面的循环疯狂分配，
     * 所以这里直接拒绝明显越界的值（上层按读取失败处理）。
     */
    private static int readCount(Reads read){
        int count = read.i();
        if(count < 0 || count > 65536){
            throw new RuntimeException("RougeSave: bogus section count " + count);
        }
        return count;
    }

    private static boolean hasMore(InputStream in){
        try{
            return in.available() > 0;
        }catch(Exception e){
            return false;
        }
    }

    /** 获取存档文件路径（用户数据目录下，和原版战役存档隔离） */
    private static File saveFile(){
        return Core.files.local("rouge_save.bin").file();
    }

    /** 重置所有进度（保留物资和科技等级，重新随机地图） */
    public static void reset(){
        completedSectors.clear();
        // 注意：不清空 storedItems，重置只重置关卡进度，物资保留
        // 注意：不清空 techLevels，外场演绎的研究进度是永久增益，不应重置
        currentSectorName = null;
        currentEvacWave = 0;
        treeSeed = System.nanoTime();
        completedNodeIds.clear();
        branchChoices.clear();
        save();
    }

    /** ObjectSet 没有直接的 toArray，手动转一下 */
    private static String[] completedScoresToArray(){
        String[] arr = new String[completedSectors.size];
        int i = 0;
        for(String s : completedSectors) arr[i++] = s;
        return arr;
    }
}
