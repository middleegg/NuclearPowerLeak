package Npl.newSth;

import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.*;
import arc.util.io.Writes;
import Npl.newSth.Type.*;
import arc.util.io.Reads;
import arc.util.Log;

public class DevourAbility extends Ability {
    // 配置参数
    public float spawnInterval = 60f * 2f; // 每2秒生成一个
    public UnitType childType;             // 子单位类型
    public float evolveTime = 60f * 15f;   // 15秒后进化
    public UnitType evolveUnitType;        // 进化后的单位类型

    private float spawnTimer = 0f;
    private float evolveTimer = 0f;

    private float timer = 0f;
    private int absorbedCount = 0;
    private boolean hasEvolved = false;

    public float healthBonusPerAbsorb = 0.01f;   // 每个吸收+1%血量
    public float damageBonusPerAbsorb = 0.01f;   // 每个吸收+1%伤害
    public float armorBonusPerAbsorb = 0.01f;    // 每个吸收+1%护甲
    public float regenBonusPerAbsorb = 0.01f;    // 每个吸收+1%回复
    /** 可回归的子单位类型名称列表，ReturnAi 自动识别并钻入 */
    public arc.struct.Seq<String> returnChildTypes = new arc.struct.Seq<>();

    @Override
    public void update(Unit unit) {
        // 1. 生成子单位逻辑
        spawnTimer += Time.delta;
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f;
            spawnChild(unit);
        }

        // 2. 进化计时逻辑
        evolveTimer += Time.delta;
        if (evolveTimer >= evolveTime) {
            evolveTimer = 0f;
            evolve(unit);
        }
        if (Time.time % 60f < 0.1f) { // 每秒打印一次
            Log.info("Absorbed count: " + absorbedCount);
        }
    }
    public void onAbsorb(Unit unit) {
        absorbedCount++;
    }

    // 在每个CoreBlock范围内生成子单位
    public void spawnChild(Unit parent) {
        float coreRange = 240f;
        for (var build : mindustry.gen.Groups.build) {
            if (build.block instanceof mindustry.world.blocks.storage.CoreBlock && build.team == parent.team) {
                float angle = Mathf.random(0f, Mathf.PI2);
                float dist = Mathf.random(0f, coreRange);
                float childX = build.x + Mathf.cos(angle) * dist;
                float childY = build.y + Mathf.sin(angle) * dist;
                Unit child = childType.create(parent.team);
                child.set(childX, childY);
                child.add();
                Fx.spawn.at(childX, childY);
            }
        }
    }

    // 单位进化的方法
    public void evolve(Unit unit) {
        Log.info("Evolving! absorbedCount:" + absorbedCount);
        // 安全检查：进化目标为null时跳过
        if (evolveUnitType == null) {
            Log.err("DevourAbility: evolveUnitType is null, cannot evolve!");
            return;
        }
        // 创建进化后的单位
        Unit newUnit = evolveUnitType.create(unit.team);
        newUnit.set(unit.x, unit.y);
        newUnit.rotation = unit.rotation;
        // 应用加成
        applyBonuses(newUnit);
        // 添加到世界
        newUnit.add();
        // 移除旧单位
        unit.remove();
        // 播放进化特效
        Fx.unitAssemble.at(newUnit.x, newUnit.y, newUnit.rotation);
    }
    public void unlockNewMechanic(Unit unit) {
        // 这里写解锁逻辑，比如添加护盾、增加武器等
        Fx.spawn.at(unit.x, unit.y);
    }
    public void applyBonuses(Unit unit) {
        float healthMultiplier = 1f + absorbedCount * healthBonusPerAbsorb;
        float damageMultiplier = 1f + absorbedCount * damageBonusPerAbsorb;
        float armorMultiplier = 1f + absorbedCount * armorBonusPerAbsorb;
        float regenMultiplier = absorbedCount * regenBonusPerAbsorb;
        // 应用加成到单位
        float newMax = unit.type.health * healthMultiplier;
// 保留当前血量的比例
        float ratio = unit.maxHealth > 0 ? unit.health / unit.maxHealth : 1f;
        unit.maxHealth = newMax;
        unit.health = newMax * ratio; // 血量和上限同步提升，比例不变
        float maxShield = unit.type.health*regenMultiplier;
        unit.shield = maxShield;
        float maxArmor = unit.type.armor*armorMultiplier;
        unit.armor = maxArmor;
        unit.reloadMultiplier = 1f + absorbedCount * 0.01f;

        // 如果你有自定义单位类，可以这样传递加成
        if (unit instanceof EvolveEntity) {
            EvolveEntity custom = (EvolveEntity) unit;
            custom.damageMultiplier = damageMultiplier;
            custom.armorMultiplier = armorMultiplier;
            custom.regenMultiplier = regenMultiplier;
        }
        Log.info("Applying bonuses: healthMultiplier=" + healthMultiplier);
        // 检查是否解锁新机制（比如吸收50个后获得额外武器）
    }

    // 在 TimedSpawnAbility 中添加
    public void write(Writes write) {
        write.i(absorbedCount);
    }

    public void read(Reads read) {
        absorbedCount = read.i();
    }
}