package Npl.content.UI;

import arc.struct.Seq;
import mindustry.content.StatusEffects;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

/**
 * Boss血条管理器
 * - 支持多Boss同时显示多条血条
 * - Boss判定：手动指定UnitType / boss状态效果 / 血量超过阈值
 * - Boss死亡后血条显示"已击败"数秒后回收
 */
public class BossBarManager {
    private final Seq<NewBossHUD> barPool = new Seq<>();
    public int maxBars = 5;
    public float bossHpThreshold = 8000f;

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

        // 分配血条：跳过处于击败倒计时的血条（指针正常推进，不堵塞后续分配）
        int barIndex = 0;
        for (Unit boss : bosses) {
            while (barIndex < barPool.size && barPool.get(barIndex).isDefeated()) {
                barIndex++;
            }
            if (barIndex >= barPool.size) break;
            NewBossHUD bar = barPool.get(barIndex);
            if (bar.getTarget() != boss) {
                arc.util.Log.info("[BossBar] 分配血条 -> " + boss.type.name + " (hp=" + (int)boss.health + ")");
            }
            bar.setTarget(boss);
            barIndex++;
        }

        // 处理剩余血条（跳过击败倒计时中的，保留其显示）
        for (int i = barIndex; i < barPool.size; i++) {
            NewBossHUD bar = barPool.get(i);
            if (bar.isDefeated()) continue;
            Unit t = bar.getTarget();
            if (t != null && t.dead) {
                bar.markDefeated();
            } else {
                bar.setTarget(null);
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
        // 1. 在手动指定列表中
        if (bossTypes.contains(unit.type)) return true;
        // 2. 有boss状态效果
        if (unit.hasEffect(StatusEffects.boss)) return true;
        // 3. 血量超过阈值
        if (unit.maxHealth >= bossHpThreshold) return true;
        return false;
    }
}
