package Npl.content.UI;

import arc.struct.Seq;
import mindustry.content.StatusEffects;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

/**
 * Boss血条管理器
 * - 支持多Boss同时显示多条血条
 * - Boss判定：显式登记的UnitType / boss状态效果 / （可选）血量阈值
 * - 默认只认敌方阵营的Boss，避免我方和友军的高血量单位也弹血条挡视野
 * - Boss死亡后血条显示"已击败"数秒后回收
 */
public class BossBarManager {
    private final Seq<NewBossHUD> barPool = new Seq<>();
    public int maxBars = 5;

    /**
     * 血量阈值：额外把 maxHealth 达到该值的单位当作Boss。
     * 设为 0 表示关闭该判据（默认）。
     * 原实现固定为 8000f，会把 FederalUnitTypes 里 9000~130000 血的普通单位全部认成Boss，
     * 5 条血条被占满并每帧换目标，糊在屏幕中间。需要按血量筛Boss时再自行调高，例如 50000f。
     */
    public float bossHpThreshold = 0f;

    /**
     * 只显示敌方阵营的Boss（默认开启）。
     * 关闭后我方/友方阵营的Boss也会显示血条。
     */
    public boolean bossesOnly = true;

    /** 手动指定的Boss单位类型列表 */
    private final Seq<UnitType> bossTypes = new Seq<>();

    private Unit manualTarget;
    private boolean autoDetect = true;

    public BossBarManager() {
        for (int i = 0; i < maxBars; i++) {
            barPool.add(new NewBossHUD());
        }
    }

    public Seq<NewBossHUD> getBars() {
        return barPool;
    }

    /** 指定某个UnitType为Boss */
    public void setBossType(UnitType type) {
        if (type != null && !bossTypes.contains(type)) {
            bossTypes.add(type);
        }
    }

    /** 清除所有指定的Boss类型 */
    public void clearBossTypes() {
        bossTypes.clear();
    }

    public void setManualTarget(Unit unit) {
        this.manualTarget = unit;
    }

    public void clearManualTarget() {
        this.manualTarget = null;
    }

    public void setAutoDetect(boolean enabled) {
        this.autoDetect = enabled;
    }

    public void update() {
        // 击败倒计时由管理器驱动（Trigger.update），不依赖场景树，
        // 避免切换存档时血条被移出场景树导致倒计时冻结
        for (NewBossHUD bar : barPool) {
            bar.tickDefeat(arc.util.Time.delta);
        }

        Seq<Unit> bosses = new Seq<>();

        // 手动指定优先
        if (manualTarget != null && isValid(manualTarget)) {
            bosses.add(manualTarget);
        }

        // 自动检测
        if (autoDetect) {
            for (Unit unit : Groups.unit) {
                if (!isValid(unit)) continue;
                if (bosses.contains(unit)) continue;
                if (isBossUnit(unit)) {
                    bosses.add(unit);
                }
            }
        }

        // 按血量降序
        bosses.sort(u -> -u.health);

        // 分配血条：先填空闲槽位，处于"已击败"倒计时的槽位算被占用、保留其提示不被复用
        // （旧实现只按池序连续跳过击败态，某个槽位一旦被后面的活Boss复用，
        //   assignTarget 会把 defeatedTimer 清 0，"已击败"提示一闪就没）
        int barIndex = 0;
        for (Unit boss : bosses) {
            while (barIndex < barPool.size && !barPool.get(barIndex).isFree()) {
                barIndex++;
            }
            if (barIndex >= barPool.size) break;
            NewBossHUD bar = barPool.get(barIndex);
            if (bar.getTarget() != boss) {
                arc.util.Log.info("[BossBar] 分配血条 -> " + boss.type.name + " (hp=" + (int)boss.health + ")");
                bar.assignTarget(boss);
            }
            barIndex++;
        }

        // 处理剩余槽位：目标已死的转入击败提示，其余清空
        for (int i = barIndex; i < barPool.size; i++) {
            NewBossHUD bar = barPool.get(i);
            if (bar.isDefeated()) continue;
            Unit t = bar.getTarget();
            if (t != null && t.dead) {
                bar.markDefeated();
            } else {
                bar.assignTarget(null);
            }
        }
    }

    /** 世界重载时重置所有血条（击败状态不跨存档保留） */
    public void resetAll() {
        for (NewBossHUD bar : barPool) {
            bar.reset();
        }
    }

    private boolean isValid(Unit unit) {
        return unit != null && !unit.dead && unit.isAdded();
    }

    private boolean isBossUnit(Unit unit) {
        if (unit == null || unit.type == null) return false;
        // 0. 只认敌方阵营（我方/友方阵营的血条会挡视野）
        if (bossesOnly && !isHostile(unit)) return false;
        // 1. 在手动指定列表中
        if (bossTypes.contains(unit.type)) return true;
        // 2. 有boss状态效果（Mindustry 没有 UnitType.boss 字段，官方剧情Boss靠这个状态效果标记）
        if (unit.hasEffect(StatusEffects.boss)) return true;
        // 3. 血量超过阈值（阈值 <= 0 表示关闭该判据）
        if (bossHpThreshold > 0f && unit.maxHealth >= bossHpThreshold) return true;
        return false;
    }

    /** 是否属于敌方阵营（排除我方、友方阵营、废墟） */
    private boolean isHostile(Unit unit) {
        if (unit.team == null) return false;
        if (mindustry.Vars.player == null || mindustry.Vars.player.team() == null) {
            return unit.team != mindustry.game.Team.derelict;
        }
        return unit.team != mindustry.Vars.player.team() && unit.team != mindustry.game.Team.derelict;
    }
}
