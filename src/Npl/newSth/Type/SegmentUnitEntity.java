package Npl.newSth.Type;

import  Npl.newSth.Type.FedUnitType;
import  Npl.newSth.effects.WormDecal;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.layout.Table;
import mindustry.gen.Hitboxc;
import mindustry.gen.UnitEntity;
import mindustry.entities.Units;
import mindustry.type.Weapon;
import mindustry.entities.units.WeaponMount;

/**
 * ★★★ 分段虫子单位的"段身" Entity（多节单位系统的肢体）★★★
 * <p>
 * 【这个类是干什么的】
 *   SegmentUnitEntity 是虫子身上的一节"身子"（非头部）。一条虫子由：
 *   - 1 个 SegmentWormEntity（头部，负责思考和控制）
 *   - N 个 SegmentUnitEntity（段身，负责显示和承受伤害）
 *   段身只是一个"贴图载体"，自己不会思考、不会移动，位置完全由头部控制。
 * <p>
 * 【在多节单位系统中的角色】
 *   - 段身的位置/朝向由头部 SegmentWormEntity 通过 syncToHead() 每帧设置
 *   - 段身不参与 AI（update() 不调用 super.update() 的 AI/移动部分）
 *   - 段身不参与存档（serialize() 返回 false）
 *   - 段身死亡时通知头部（head.onSegmentDied），由头部决定是否分裂
 *   - 段身被施加状态效果时转给头部（updateStatus）
 *   - 段身被玩家选中时，控制权转给头部（controller 重写）
 * <p>
 * 【核心设计原则】
 *   1. 段身 vel 每帧清零：防止 physics=true 时段身推动头部移动
 *   2. 段身位置由头部控制：syncToHead(x, y, rotation) 直接设置位置
 *   3. 段身碰撞过滤：不与同头部的相邻段身碰撞（避免抖动）
 *   4. 段身绘制由头部统一调用：draw() 为空，drawBody() 由头部调用
 * <p>
 * 【设计来源】
 *   借鉴 PU132 mod 的 WormSegmentUnit，适配 Mindustry v154.3 原生 API。
 * <p>
 * 【v154.3 适配要点】
 *   - classId 注册到 EntityMapping.idMap（见 ZEntityRegister），避免 ClassCastException
 *   - 段身 UnitType 必须设 hidden=true（不出现在数据库/Spawner）、useUnitCap=false（不计入单位上限）
 *   - 段身 flying=true 自动有影子，physics=true 有碰撞体积
 * <p>
 * 【注意】
 *   触手系统（TentacleAbility/TentaclesBase/VoidPortalBulletType）已移除，本文件无相关引用。
 *   液压装饰（WormDecal）保留，仅 oppression 压迫者有，其他单位为 null。
 */
public class SegmentUnitEntity extends UnitEntity {

    /** 工厂方法（UnitType.constructor 用这个创建实例） */
    public static SegmentUnitEntity create() {
        return new SegmentUnitEntity();
    }

    /**
     * 返回注册的 classId（绕过 v154.3 的 checkEntityMapping 检查）
     * ★ 为什么需要这个：v154.3 要求每个自定义 Entity class 有唯一 classId，
     *   不注册会导致 UnitType.init() 抛 ClassCastException。
     */
    @Override
    public int classId() {
        return FedUnitEntity.classId(SegmentUnitEntity.class);
    }

    /** 引用头部（用于死亡通知、状态转移、控制权转移） */
    public SegmentWormEntity head = null;

    /** 段身在头部段身数组中的索引（0=第一节，count-1=尾节），用于 z 层级控制和武器分组 */
    public int segmentIndex = 0;

    /** 是否为尾部（最后一节段身，用 tail 贴图而不是 segment 贴图） */
    public boolean isTail = false;

    /**
     * 贴图前缀（由头部 createSegments 时设置，等于头部 type.name + "-"）
     * - arcnelidia 头部 → "arcnelidia-"
     * - toxobyte 头部 → "toxobyte-"
     * 用于查找段身贴图（segment/tail/cell/outline）
     */
    public String texturePrefix = "arcnelidia-";

    /**
     * ★ 段身每帧更新（核心：不调用 super.update() 的 AI/移动部分）
     *
     * 【为什么不调用 super.update()】
     *   段身不需要 AI/移动/drag，位置完全由头部 syncToHead(x,y,rot) 控制。
     *   如果调用 super.update()，段身会被物理系统推动，导致位置混乱。
     *
     * 【执行顺序】
     *   1. vel 清零（防止段身推动头部）
     *   2. 状态效果时间更新
     *   3. hitTime 衰减（受伤闪烁）
     *   4. 状态效果转移给头部
     *   5. 段身武器 + 同步头部状态
     *   6. 非分裂模式：段身血量同步为头部血量
     *   7. 检查头部是否还活着，死了段身也跟着死
     */
    @Override
    public void update() {
        // ★ 不调用 super.update() 的移动/AI 部分
        // 只保留必要的更新：状态效果、武器、血量

        // ★ 关键：段身 vel 必须每帧清零
        // 【为什么】physics=true 会让段身根据 vel 移动，如果 vel 有残留，
        //   段身会推动头部移动（出现"待机时单位自己向前走"的问题）
        vel.setZero();

        // 更新状态效果时间（让 buff 仍然生效）
        if (statuses.size > 0) {
            statuses.each(s -> s.time = Math.max(s.time - arc.util.Time.delta, 0f));
        }

        // hitTime 衰减（受伤闪烁）
        hitTime = Math.max(0f, hitTime - arc.util.Time.delta / 10f);

        // ★ 状态效果转移给头部（借鉴 PU132 WormSegmentUnit.updateStatus L261-265）
        updateStatus();

        // ★ 段身武器 + 同步头部状态（借鉴 PU132 WormSegmentUnit.wormSegmentUpdate L192-214）
        if (head != null && head.isAdded() && !head.dead) {
            // 同步头部 hitTime（让段身受伤闪烁与头部一致）
            hitTime = head.hitTime;

            // ★ 段身武器：参照 PU132 WormSegmentUnit.updateWeapon 重写索敌部分
            // 发射/冷却/旋转交给 v154.3 原版 Weapon.update() 处理
            if (mounts != null && mounts.length > 0) {
                // ★ PU132 弹幕同步机制（WormAI.updateWeapons L48-63）：
                // 当头部被玩家控制且正在射击，段身在 barrageRange 内时，
                // 段身复制头部的 aimX/aimY，跟随玩家瞄准方向齐射
                boolean barrageSync = head.isPlayer() && head.isShooting
                        && within(head, head.barrageRange + hitSize / 2f);

                for (WeaponMount mount : mounts) {
                    Weapon weapon = mount.weapon;

                    if (barrageSync && weapon.controllable) {
                        // ★ 弹幕模式：复制头部瞄准目标，跟随玩家射击
                        mount.aimX = head.aimX;
                        mount.aimY = head.aimY;
                        mount.shoot = true;
                        mount.rotate = true;
                    } else {
                        // ★ 自动索敌：搜索自己射程内的目标（单位 + 建筑）
                        // 之前只搜 Units.closestEnemy（只找单位），导致段身不打建筑
                        // 改用 Units.closestTarget（返回 Teamc，包含单位+建筑）
                        mindustry.gen.Teamc tgt = Units.closestTarget(team, x, y, weapon.range(),
                                u -> !u.dead,
                                t -> true);
                        if (tgt != null) {
                            mount.aimX = tgt.getX();
                            mount.aimY = tgt.getY();
                            mount.shoot = true;
                            mount.rotate = true;
                        } else {
                            mount.shoot = false;
                            mount.rotate = false;
                        }
                    }

                    // ★ 交给原版 Weapon.update 处理冷却、旋转、发射
                    weapon.update(this, mount);
                }
            }
        }

        // ★ 非分裂模式：段身血量同步为头部血量（参考 PU132 WormComp.update L249-250）
        // 【为什么】这样鼠标悬停任意段身时，显示的血量都与头部相同，
        //   所有节段"共用一个血条"。
        if (head != null && head.isAdded() && !head.splittable) {
            health = head.health;
            maxHealth = head.maxHealth;
        }

        // 检查头部是否还活着
        if (head == null || head.dead || !head.isAdded()) {
            // 头部死了，段身也跟着死
            health = 0f;
            dead = true;
        }
    }

    /**
     * ★ 段身状态效果转移给头部（借鉴 PU132 WormSegmentUnit.updateStatus L261-265）
     *
     * 【为什么要转移】
     *   段身不持有 buff，被施加的状态（如燃烧、减速）全部转给头部。
     *   这样整条虫子共用一个状态系统，不会出现"段身被减速但头部还在跑"的问题。
     */
    protected void updateStatus() {
        if (head == null || head.dead || !head.isAdded()) return;
        if (!statuses.isEmpty()) {
            statuses.each(s -> head.apply(s.effect, s.time));
            statuses.clear();
        }
    }

    /**
     * ★ 段身不参与存档（借鉴 PU132 WormSegmentUnit.serialize L176-178）
     *
     * 【为什么返回 false】
     *   段身位置由头部算法计算，不需要保存。
     *   读档时头部根据 savedSegmentCount 重建段身。
     *   如果段身参与存档，会导致读档后段身重复创建。
     */
    @Override
    public boolean serialize() {
        return false;
    }

    /**
     * ★ 受到伤害
     *
     * 【两种模式】
     *   - 分裂模式（splittable=true）：段身有独立血量，自己承受伤害，死亡时触发分裂
     *   - 非分裂模式（splittable=false）：伤害转移给头部（PU132 WormComp.damage L176-178）
     *
     * 【借鉴 PU132 WormSegmentUnit.damage】
     *   if(wormType.splittable) segmentHealth -= amount * wormType.segmentDamageScl;
     *   trueParentUnit.damage(amount);
     *
     * 【segmentDamageScl：段身伤害缩放】
     *   越大 = 段身越脆，越容易死亡分裂
     *   toxobyte 原版 8f，catenapede 原版 12f
     */
    @Override
    public void damage(float amount) {
        if (head != null && head.isAdded() && !head.splittable) {
            // 非分裂模式：伤害转移给头部
            head.damage(amount);
            return;
        }
        // 分裂模式：自己承受伤害（应用 segmentDamageScl 缩放）
        float scl = (head != null) ? head.segmentDamageScl : 1f;
        super.damage(amount * scl);
        // 头部也受到原始伤害（PU132 原版行为：段身受击时头部也掉血）
        if (head != null && head.isAdded()) {
            head.damage(amount);
        }
    }

    /**
     * ★ 段身死亡
     * 通知头部"我死了"，由头部决定是否分裂（onSegmentDied）。
     *
     * 【为什么不直接 super.kill()】
     *   段身死亡需要触发头部的分裂逻辑（后半段创建为新虫子），
     *   所以先通知头部，再 remove() 自己。
     */
    @Override
    public void kill() {
        if (dead) return;
        dead = true;
        // 通知头部：我死了，请重新分配段身列表
        if (head != null && head.isAdded()) {
            head.onSegmentDied(this);
        }
        remove();
    }

    // isCounted 在 v150.1 中不存在，移除（改用 hidden=true 隐藏段身）

    /**
     * 段身的 AI 状态跟随头部（借鉴 PU132 WormSegmentUnit.isAI L124-127）
     * 段身自己没有 AI，是否算 AI 状态看头部。
     */
    @Override
    public boolean isAI() {
        if (head == null) return false;
        return head.controller() instanceof mindustry.entities.units.AIController;
    }

    /**
     * 段身是否被玩家控制 = 头部是否被玩家控制（借鉴 PU132 WormSegmentUnit.isPlayer L118-121）
     */
    @Override
    public boolean isPlayer() {
        if (head == null) return false;
        return head.isPlayer();
    }

    /**
     * 段身治疗时同步治疗头部（借鉴 PU132 WormSegmentUnit.heal L146-151）
     */
    @Override
    public void heal(float amount) {
        if (head != null && head.isAdded()) {
            head.heal(amount);
        }
        super.heal(amount);
    }

    /**
     * 段身的玩家 = 头部的玩家（借鉴 PU132 WormSegmentUnit.getPlayer L135-138）
     */
    @Override
    public mindustry.gen.Player getPlayer() {
        if (head == null) return null;
        return isPlayer() ? (mindustry.gen.Player) head.controller() : null;
    }

    /**
     * ★★★ 关键1：段身重写 controller() 无参方法，返回头部的 controller ★★★
     *
     * 【为什么要这样做】
     *   v154.3 指挥模式（CommandAI）下令时（InputHandler L314）：
     *     if(unit.controller() instanceof CommandAI ai) ai.commandPosition(pos)
     *   如果段身返回自己的 controller，targetPos 会设在段身上，段身不移动。
     *   重写后返回头部的 controller，下令时设置头部的 targetPos，头部移动 ✓
     *
     * ★ 这是"选中段身也能控制整体移动"的核心实现。
     */
    @Override
    public mindustry.entities.units.UnitController controller() {
        if (head != null && head.isAdded()) {
            return head.controller();
        }
        return super.controller();
    }

    /**
     * ★★★ 关键2：段身重写 controller(UnitController next)，把玩家控制转给头部 ★★★
     * （借鉴 PU132 WormSegmentUnit.controller L107-115）
     *
     * 【为什么要这样做】
     *   玩家进入段身（controller(player)）时，把 Player 转给头部。
     *   段身自己不持有 Player controller（避免段身独立移动）。
     *
     * 【注意】不要重写 controller() 返回 null，否则 remove() 调用 controller.removed() 会 NPE。
     *   段身不需要 AI：我们在 update() 中不调用 super.update() 的 AI 部分。
     */
    @Override
    public void controller(mindustry.entities.units.UnitController next) {
        if (!(next instanceof mindustry.gen.Player)) {
            // AI controller：调用父类方法设置自己的 controller
            super.controller(next);
        } else if (head != null && head.isAdded()) {
            // Player controller：转给头部（用 head.controller(next) 设置头部 controller）
            head.controller(next);
        }
    }

    /**
     * ★ 让头部直接设置位置和朝向（跳过物理）
     * 由 SegmentWormEntity.update() 每帧调用。
     *
     * 【为什么需要这个】
     *   段身位置由头部 PU132 算法计算，不能让物理系统移动段身。
     *   syncToHead 直接设置 x/y/rotation，跳过物理系统。
     *
     * 【注】不重置 vel，因为 updateSegmentVLocal 会把段身速度同步到 segments[i].vel。
     *   段身 vel 用于碰撞/受击效果，头部 update() 会重新覆盖。
     */
    public void syncToHead(float x, float y, float rotation) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        // vel 不清零，由头部 updateSegmentVLocal 每帧覆盖
    }

    /**
     * ★ 鼠标悬停时，段身显示头部的贴图和名称（参考 PU132 WormComp.icon + 自定义 display）
     *
     * 【为什么要重写】
     *   v154.3 UnitType.display(unit, table) 内部：
     *   - 用 unit.type.uiIcon 显示贴图
     *   - 用 unit.type.localizedName 显示名称
     *   - 用 unit::healthf 显示血量
     *   重写后，让段身调用头部 type.display(head, table)，
     *   这样贴图/名称/血量都跟头部一致。
     */
    @Override
    public void display(Table table) {
        if (head != null && head.isAdded() && head.type != null) {
            // 调用头部的 type.display，用头部的贴图/名称，血量也跟头部一致
            head.type.display(head, table);
        } else {
            // 兜底：没有头部时，调用默认显示
            super.display(table);
        }
    }

    /**
     * ★ 段身被添加到世界时调用
     *
     * 【head==null 时自爆】
     *   段身只能由头部 createSegments 创建，单独生成（如 Spawner 召唤或作弊）没有意义。
     *   如果 head==null，直接自爆（health=0, dead=true, remove()）。
     */
    @Override
    public void add() {
        super.add();
        // ★ 如果 head==null（单独生成，如 Spawner 召唤或作弊）：直接自爆
        if (head == null) {
            health = 0f;
            dead = true;
            remove();
            return;
        }
    }

    /**
     * ★★★ 段身 draw() 为空（还原 PU132 WormSegmentUnit.draw L397-399 空方法）★★★
     *
     * 【PU132 原版架构】
     *   - WormSegmentUnit.add() 不加入 Groups.draw（L61 注释掉）
     *   - WormSegmentUnit.draw() 为空方法
     *   - 段身渲染由头部 UnityUnitType.drawBody() 驱动：
     *     遍历 segmentUnits[]，设 Draw.z(z - (i+1)/10000)，调 segment.drawBody()
     *
     * 【为什么不能让段身在 Groups.draw 中自己绘制】
     *   Mindustry 的 Draw 批处理在不同 entity 的 draw() 之间会 flush，
     *   z-sorting 只在同一 flush（同一 entity 的 draw 调用）内生效，
     *   跨 entity 的 z 值不会被正确排序，导致：
     *   1) 渲染顺序错误（高 index 段身后 flush 覆盖低 index 段身）
     *   2) 段身可能被方块覆盖（flush 顺序问题）
     *
     * 【正确做法】
     *   段身 draw() 为空，由头部 SegmentWormEntity.draw() 统一绘制所有段身。
     */
    @Override
    public void draw() {
        // 空：段身由头部 SegmentWormEntity.draw() 统一绘制
    }

    /**
     * ★★★ 段身实际绘制（由头部 SegmentWormEntity.draw() 调用）★★★
     *
     * 【借鉴 PU132 WormSegmentUnit.drawBody（L366-381）】
     *   type.applyColor(this);
     *   Draw.rect(region, this, rotation - 90);
     *   if(segmentType == 0 && cellRegion != error) drawCell(cellRegion);
     *   Draw.rect(outline, this, rotation - 90);
     *
     * 【z 层级由头部设置】
     *   head.draw() 中 Draw.z(baseZ - (i+1)/10000) 后调用本方法：
     *   - 段身 0 z = 头部z - 1/10000（头部覆盖第1节）
     *   - 段身 1 z = 头部z - 2/10000（第1节覆盖第2节）
     *   - ...
     *   - 段身 n-1 z = 头部z - n/10000（最低，尾部）
     *
     * 【绘制步骤】
     *   1. 保存 type 原本的字段（region/outlineRegion/cellRegion/drawCell）
     *   2. 尾部：查找 tail 贴图替换；非尾部：直接用段身贴图
     *   3. 手动绘制 body + cell + outline
     *   4. 绘制段身武器（按 segmentIndex 过滤）
     *   5. 绘制液压装饰（WormDecal，仅 oppression 有）
     *   6. 恢复 type 字段
     */
    public void drawBody() {
        mindustry.type.UnitType t = type;

        // 保存 type 原本的字段
        TextureRegion oldRegion = t.region;
        TextureRegion oldOutline = t.outlineRegion;
        TextureRegion oldCell = t.cellRegion;
        boolean oldDrawCell = t.drawCell;

        // ★ 尾部：查找 tail 贴图替换；非尾部：直接用 UnitType.load() 加载的段身贴图
        if (isTail) {
            String p = texturePrefix;
            String modP = "nu-" + p;
            TextureRegion tailR = findRegion(p + "tail", modP + "tail");
            if (tailR.found()) t.region = tailR;
            TextureRegion tailO = findRegion(p + "tail-outline", modP + "tail-outline");
            if (tailO.found()) t.outlineRegion = tailO;
            t.drawCell = false;
        }

        // 手动绘制（参考 PU132 WormSegmentUnit.drawBody L366-381）
        t.applyColor(this);
        Draw.rect(t.region, x, y, rotation - 90);

        // cell（非尾部，PU132 WormSegmentUnit.drawBody L372：segmentType == 0 时绘制）
        if (!isTail && t.drawCell && t.cellRegion.found()) {
            Draw.color(t.cellColor(this));
            Draw.rect(t.cellRegion, x, y, rotation - 90);
        }

        // outline（PU132 WormSegmentUnit.drawBody L373-378）
        if (t.outlineRegion.found()) {
            Draw.color(Color.white);
            Draw.rect(t.outlineRegion, x, y, rotation - 90);
        }

        // 段身武器（PU132 UnityUnitType.drawBody L678：drawWeapons(segment)）
        // 按 segmentIndex 过滤武器组（oppression 6武器分3组，尾部空组）
        mindustry.entities.units.WeaponMount[] oldMounts = mounts;
        mindustry.entities.units.WeaponMount[] filteredMounts = filterMountsForSegment(oldMounts);
        mounts = filteredMounts;
        t.drawWeapons(this);
        mounts = oldMounts;

        // 恢复 type 字段
        t.region = oldRegion;
        t.outlineRegion = oldOutline;
        t.cellRegion = oldCell;
        t.drawCell = oldDrawCell;

        // 液压杆（WormDecal，PU132 UnityUnitType.drawBody L361）
        // 仅 oppression 压迫者有，其他单位为 null
        if (head != null && head.isAdded() && head.type != null) {
            WormDecal decal = SegmentWormEntity.wormDecals.get(head.type.name);
            if (decal != null) {
                mindustry.gen.Unit parent = getParentSegment();
                if (parent != null) {
                    decal.draw(this, parent);
                }
            }
        }

        Draw.reset();
    }

    /**
     * 获取当前段身的父段（PU132 unit.parent()）
     * - segmentIndex == 0：父段是头部（head）
     * - segmentIndex > 0：父段是前一段身（head.segments[segmentIndex - 1]）
     *
     * 用于 WormDecal.draw(this, parent) 绘制段身到父段的液压杆。
     */
    private mindustry.gen.Unit getParentSegment() {
        if (head == null || !head.isAdded() || head.segments == null) return null;
        if (segmentIndex == 0) return head;
        int parentIdx = segmentIndex - 1;
        if (parentIdx < head.segments.length) {
            return head.segments[parentIdx];
        }
        return null;
    }

    /**
     * ★ 按 segmentIndex 过滤段身武器（PU132 weaponIdx 机制简化版）
     *
     * 【PU132 原版机制】
     *   segmentWeapons = Seq<Weapon>[] {组0, 组1, 组2, 空组}
     *   段 i 的武器组 idx = i >= length-1 ? 组数-1 : i % max(1, 组数-1)
     *
     * 【当前项目实现】
     *   段身 type.weapons 包含所有武器，按 groupSize 分组：
     *   - 段 i 的组 idx = i % groupCount（非尾部）
     *   - 尾部 = 空组（不画武器）
     *
     * 【配置示例】
     *   - oppression：6 个段身武器，分 3 组（每组 2 个），尾部空组
     *   - devourer：3 个段身武器，1 组（所有武器），尾部空组
     *   - arcnelidia/toxobyte/catenapede：1 个段身武器，1 组
     */
    private mindustry.entities.units.WeaponMount[] filterMountsForSegment(mindustry.entities.units.WeaponMount[] allMounts) {
        if (allMounts == null || allMounts.length == 0) return allMounts;
        // 查头部 SegmentConfig 获取武器组配置
        if (head == null) return allMounts;
        SegmentWormEntity.SegmentConfig cfg = SegmentWormEntity.configs.get(head.type.name);
        if (cfg == null) return allMounts;
        int groupSize = cfg.segmentWeaponGroupSize;
        int totalWeapons = allMounts.length;
        if (groupSize <= 0 || groupSize >= totalWeapons) return allMounts;

        // 计算组数（最后一个组是空组，用于尾部）
        int groupCount = (int) Math.ceil((float) totalWeapons / groupSize);
        if (groupCount <= 1) return allMounts;

        // PU132：idx = i >= segmentLength - 1 ? groupCount : i % max(1, groupCount - 1)
        // 但我们不知道 segmentLength，用 isTail 判断
        int idx;
        if (isTail) {
            // 尾部：空组（不画武器）
            return new mindustry.entities.units.WeaponMount[0];
        } else {
            // 非尾部：按 segmentIndex 分组（mod groupCount-1，因为最后一组是尾部空组）
            int effectiveGroups = groupCount - 1;
            if (effectiveGroups <= 0) return allMounts;
            idx = segmentIndex % effectiveGroups;
        }

        // 提取对应组的 mounts
        int start = idx * groupSize;
        int end = Math.min(start + groupSize, totalWeapons);
        mindustry.entities.units.WeaponMount[] result = new mindustry.entities.units.WeaponMount[end - start];
        System.arraycopy(allMounts, start, result, 0, end - start);
        return result;
    }

    /**
     * ★ 查找贴图：先试 name，找不到再试 prefixedName（带 mod 前缀）
     *
     * 【为什么需要双名字兼容】
     *   Mindustry 给 mod 贴图加 modname- 前缀，但 UnitType.load 用不带前缀的名字查找。
     *   所以查找时要两种都试：先查不带前缀的，找不到再查带前缀的。
     */
    private static TextureRegion findRegion(String name, String prefixedName) {
        TextureRegion r = arc.Core.atlas.find(name);
        if (r.found()) return r;
        return arc.Core.atlas.find(prefixedName);
    }

    /**
     * ★★★ 段身之间的碰撞过滤 ★★★
     *
     * 【为什么要碰撞过滤】
     *   如果段身之间互相碰撞，移动时会推挤抖动。
     *   过滤掉相邻段身的碰撞，让虫子移动顺畅。
     *
     * 【碰撞规则】
     *   - 段身 vs 自己的头部 → 不碰撞（避免头部和段身推挤）
     *   - 段身 vs 同头部的相邻段身（index 差 ≤ 2）→ 不碰撞（避免移动时抖动）
     *   - 段身 vs 同头部的非相邻段身（index 差 > 2）→ 碰撞（生成时重叠会弹开）
     *   - 段身 vs 其他单位 → 正常碰撞（其他单位不能穿过段身）
     *
     * 【为什么允许非相邻段身碰撞】
     *   生成很多段时，段身会重叠，允许非相邻段身碰撞可以形成自然散开效果。
     *
     * 【借鉴】PU132 WormSegmentUnit.collides + 原版碰撞挤压弹开效果
     */
    @Override
    public boolean collides(Hitboxc other) {
        // 段身 vs 自己的头部 → 不碰撞
        if (other == head) return false;
        // 段身 vs 同头部的其他段身 → 相邻的不碰撞，非相邻的碰撞
        if (head != null && other instanceof SegmentUnitEntity) {
            SegmentUnitEntity o = (SegmentUnitEntity) other;
            if (o.head == head) {
                // ★ 扩大不碰撞范围：index 差 ≤ 2 的段身都不碰撞
                //   之前只排除差1的相邻段，导致快速转向时非相邻段互相挤压抖动
                int indexDiff = Math.abs(segmentIndex - o.segmentIndex);
                return indexDiff > 2;
            }
        }
        // 段身 vs 其他单位 → 正常碰撞（其他单位不能穿过段身）
        return super.collides(other);
    }
}
