package Npl.newSth.AI;

import arc.math.Angles;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Predict;
import mindustry.entities.units.AIController;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Building;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.type.Weapon;
import mindustry.world.Tile;
import Npl.newSth.DragonClawWeapon;

/**
 * 列阵·士兵专用 AI（继承自 {@link AIController}）。
 * <p>
 * 士兵是陆地单位：平时贴地行走、跟随长官周围的阵位；一旦前方遇到墙体/建筑，
 * 立刻切换为助推状态翻越障碍，通过后再自动落地。
 * <p>
 * 长官（母体）一旦攻击，士兵统一瞄准长官正在攻击的那个目标——长官是官，士兵是兵。
 * <p>
 * 士兵是临时召唤物：存活 {@link #life} 帧后自行死亡。
 * <p>
 * 失控惩罚：长官阵亡，或长官停火超过 {@link #loseDelay} 帧，视为「脱离控制」；
 * 此后进入 {@link #bufferTime} 帧缓冲期，缓冲期内不掉血；缓冲期一过，士兵每秒
 * 损失 {@link #decayPerSecond} 的最大生命值（默认 25%/秒），很快自行溃散。
 * 一旦长官重新开始攻击，则取消失控状态、缓冲期重置。
 */
public class LingZhenGuardAI extends AIController{
    /** 阵位所跟随的长官。 */
    public Unit leader;
    /** 阵位序号（由 FormUpAbility 分配，越小越靠内）。 */
    public int slot;
    /** 相对长官的阵位偏移（世界坐标轴对齐）。 */
    public float offX, offY;
    /** 存活时间（帧），由 FormUpAbility 生成时注入；到点单位自行死亡。 */
    public float life = 60f * 30f;

    /** 长官停火多久算「脱离控制」（帧）。 */
    public float loseDelay = 60f * 3f;
    /** 脱离控制后的缓冲期（帧），缓冲期内不掉血。 */
    public float bufferTime = 60f * 5f;
    /** 缓冲期结束后每秒扣除的最大生命值比例。 */
    public float decayPerSecond = 0.25f;

    /** 复用的目标点，避免每帧分配。 */
    protected final Vec2 dest = new Vec2();
    /** 剩余存活时间（帧），首次更新时懒初始化为 {@link #life}。 */
    protected float lifeTimer = -1f;

    /** 长官持续停火的时长（帧）。 */
    protected float idleTimer = 0f;
    /** 失控缓冲期计时（帧）。 */
    protected float bufferTimer = 0f;

    /** 长官的龙爪武器（缓存，用于读取共享目标）。 */
    protected DragonClawWeapon sharedWeapon;
    protected Unit sharedOwner;

    @Override
    public void updateMovement(){
        if(unit == null || unit.dead || !unit.isAdded()) return;

        // 存活计时：到点自行死亡
        if(lifeTimer < 0f) lifeTimer = life;
        if(lifeTimer > 0f){
            lifeTimer -= Time.delta;
            if(lifeTimer <= 0f){
                unit.kill();
                return;
            }
        }

        // 失控判定 + 缓冲期 + 掉血
        updateControl();

        if(leader == null || leader.dead || !leader.isAdded()){
            unit.vel.setZero();
            faceTarget();
            return;
        }

        dest.set(leader.x + offX, leader.y + offY);
        float d = unit.dst(dest);
        float arrive = Math.max(3f, unit.hitSize);

        // 陆地行走：平时贴地；遇墙或正卡在实体格上 → 助推翻越
        if(unit.type.canBoost){
            boolean needBoost = onSolidTile() || (d > arrive && wallAhead());
            unit.updateBoosting(needBoost, false);
        }

        if(d > arrive){
            moveTo(dest, arrive);
        }else{
            unit.vel.setZero();
        }

        faceTarget();
    }

    /** 长官是否仍在有效掌控之下（处于战斗状态）。 */
    protected boolean underControl(){
        return inCombat(leader);
    }

    /**
     * 长官是否处于「战斗状态」。处于战斗状态时，士兵不进入失控、不掉血、不消失。
     * <ul>
     *   <li>玩家控制的单位：按下攻击键且未松开（{@code unit.isShooting} 或任一挂点
     *       {@code mount.shoot}）即算战斗，即使当前没有目标（对着空地开火也算）；</li>
     *   <li>非玩家控制的单位：单位攻击范围内存在可攻击目标（挂点已锁定
     *       {@code mount.target} 或正在开火）即算战斗。</li>
     * </ul>
     */
    public static boolean inCombat(Unit unit){
        if(unit == null || unit.dead || !unit.isAdded()) return false;

        boolean player = unit.isPlayer();
        if(unit.mounts != null){
            for(WeaponMount mount : unit.mounts){
                if(mount == null) continue;
                // 正在开火：玩家按住攻击键 / AI 决定开火 都会置位
                if(mount.shoot) return true;
                // AI 控制：射程内已锁定可攻击目标
                if(!player && mount.target != null) return true;
            }
        }
        // 玩家控制：按住攻击键即算战斗（当前无目标也算）
        return player && unit.isShooting;
    }

    /** 失控判定：长官阵亡立即失控；长官停火需超过 loseDelay。失控后缓冲期内不掉血，之后按比例掉血。 */
    protected void updateControl(){
        if(underControl()){
            idleTimer = 0f;
            bufferTimer = 0f;
            return;
        }

        boolean leaderGone = leader == null || leader.dead || !leader.isAdded();
        if(leaderGone){
            idleTimer = loseDelay;
        }else{
            idleTimer += Time.delta;
        }

        if(idleTimer < loseDelay) return;

        bufferTimer += Time.delta;
        if(bufferTimer < bufferTime) return;

        float amount = unit.maxHealth * decayPerSecond * (Time.delta / 60f);
        unit.health -= amount;
        if(unit.health <= 0f){
            unit.health = 0f;
            unit.kill();
        }
    }

    /** 当前所站格是否为实体方块（如出生点就在墙体里）。 */
    protected boolean onSolidTile(){
        Tile tile = unit.tileOn();
        return tile != null && tile.solid();
    }

    /** 前方一个身位是否被实体方块挡住。 */
    protected boolean wallAhead(){
        float ang = unit.angleTo(dest);
        float px = unit.x + Angles.trnsx(ang, unit.hitSize + 8f);
        float py = unit.y + Angles.trnsy(ang, unit.hitSize + 8f);
        Tile tile = Vars.world == null ? null : Vars.world.tileWorld(px, py);
        return tile != null && tile.solid();
    }

    /** 长官当前锁定的目标；士兵统一瞄准它。 */
    protected Teamc sharedTarget(){
        DragonClawWeapon w = sharedWeaponOf();
        return w == null ? null : w.currentTarget(leader);
    }

    protected DragonClawWeapon sharedWeaponOf(){
        if(leader == null || leader.type == null || leader.type.weapons == null) return null;
        if(sharedWeapon != null && sharedOwner == leader) return sharedWeapon;

        sharedWeapon = null;
        sharedOwner = null;
        for(Weapon w : leader.type.weapons){
            if(w instanceof DragonClawWeapon dc){
                sharedWeapon = dc;
                sharedOwner = leader;
                break;
            }
        }
        return sharedWeapon;
    }

    @Override
    public void updateTargeting(){
        Teamc shared = sharedTarget();

        // 长官没有明确目标时，退回常规索敌
        if(shared == null || invalid(shared)){
            super.updateTargeting();
            return;
        }

        target = shared;
        unit.isShooting = false;

        float rotation = unit.rotation - 90f;
        for(WeaponMount mount : unit.mounts){
            Weapon weapon = mount.weapon;
            if(!weapon.controllable || weapon.noAttack || !weapon.aiControllable) continue;

            // 瞄准长官锁定的同一个目标
            mount.target = shared;

            float mountX = unit.x + Angles.trnsx(rotation, weapon.x, weapon.y);
            float mountY = unit.y + Angles.trnsy(rotation, weapon.x, weapon.y);
            float wrange = weapon.range();
            float extra = 0f;
            if(shared instanceof Unit su) extra = su.hitSize / 2f;
            else if(shared instanceof Building sb) extra = sb.block.size * Vars.tilesize / 2f;

            boolean shoot = shared.within(mountX, mountY, wrange + extra) && shouldShoot() && shouldFire();
            Vec2 to = Predict.intercept(unit, shared, weapon.bullet);
            mount.aimX = to.x;
            mount.aimY = to.y;
            mount.shoot = mount.rotate = shoot;
            unit.isShooting |= mount.shoot;

            if(mount.shoot){
                unit.aimX = mount.aimX;
                unit.aimY = mount.aimY;
            }
        }
    }
}
