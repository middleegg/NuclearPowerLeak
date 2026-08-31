package Npl.newSth;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.IntMap;
import arc.struct.IntSeq;
import arc.util.Align;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.units.WeaponMount;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Posc;
import mindustry.gen.Sounds;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.Weapon;
import mindustry.ui.Fonts;

/**
 * DragonClawWeapon extends Weapon —— 单位专用的「龙爪近战」武器。
 *
 * 设计定位：这是一件【近战】武器，龙爪只略微抬起，然后向目标方向【挥击/拍击】砸下，
 * 而不是从高空垂直坠落。整体节奏偏“沉重、慢蓄力、快落、长余波”。
 *
 * ============== 表现流程（一次完整的"拍击"） ==============
 *   阶段 0 蓄力(raiseTime)：龙爪向身后略微收、同时上抬 raiseHeight 高度，蓄势。
 *   阶段 1 预警(warnTime)：龙爪前半程探向目标方向并悬停；地面落点处亮起【范围圈】预警
 *                           （脉动外圈 + 收缩内圈 + 旋转刻度），期间落点持续跟踪目标。
 *   阶段 2 挥落(fallTime)：龙爪由悬停位高速向落点挥砸而下，带残影。
 *   阶段 3 余波(recoilTime)：冲击波扩散 + 尘土/火星迸发 + 龙爪回抽淡出。
 *
 * ============== 伤害结算（纯伤害，不秒杀） ==============
 *   落点半径内一律按 falloff 衰减造成伤害，不再对建筑做代码秒杀：
 *     · 单位    → unitDamage   * falloff(d)
 *     · 建筑    → edgeDamage   * falloff(d)
 *
 * ============== 与 TianFaWeapon 的差异 ==============
 *   · 不做弹药存放/齐射，改为"每次冷却打一记单发拍击"（reloadInterval）。
 *   · 落点伤害是"近战挥击 + 范围圈预警"，而不是远程齐射的区块预警。
 *
 * ============== 使用范例（在 UnitType 里） ==============
 * <pre>{@code
 * myUnit = new UnitType("myUnit"){{
 *     weapons.add(new DragonClawWeapon("my-claw"){{
 *         x = 0f; y = 4f;
 *         mirror = false;
 *
 *         clawRange     = 260f;   // 索敌 / 作战距离
 *         reloadInterval = 360f;  // 两记拍击之间的间隔（帧）
 *
 *         raiseTime = 1.1f;       // 蓄力上抬
 *         warnTime  = 1.6f;       // 范围圈预警
 *         fallTime  = 0.22f;      // 挥落
 *         recoilTime = 0.85f;     // 余波
 *         raiseHeight = 32f;      // 抬起高度（略微抬起）
 *
 *         impactRadius = 56f;     // 总冲击半径
 *         edgeDamage   = 900f;    // 建筑伤害（纯伤害，按 falloff 衰减）
 *         unitDamage   = 900f;    // 单位伤害（纯伤害，按 falloff 衰减）
 *         falloffInner = 0.4f;    // 内圈满伤比例
 *
 *         clawSize = 22f;         // 龙爪尺寸
 *     }});
 * }};
 * }</pre>
 */
public class DragonClawWeapon extends Weapon{

    // ============= 索敌 / 节奏 =============
    /** 索敌 / 作战范围（同时是 range() 返回值，决定载具 AI 交战距离） */
    public float clawRange = 260f;
    /** 两记拍击之间的间隔（帧） */
    public float reloadInterval = 360f;
    /** 能否打空中单位 */
    public boolean targetAir = false;
    /** 能否打地面单位 */
    public boolean targetGround = true;
    /** 能否打敌方建筑 */
    public boolean targetBlocks = true;
    /** 挂点转向速度（度/帧） */
    public float turnSpeed = 6f;

    // ============= 动作时长（秒）—— 沉重：慢蓄力 / 快落 / 长余波 =============
    /** 阶段 0：蓄力上抬时长 */
    public float raiseTime = 1.1f;
    /** 阶段 1：范围圈预警时长 */
    public float warnTime = 1.6f;
    /** 阶段 2：挥落时长（越短越有冲击力） */
    public float fallTime = 0.22f;
    /** 阶段 3：余波时长 */
    public float recoilTime = 0.85f;
    /** 抬起高度（像素）—— 近战武器只略微抬起，不必从高空坠落 */
    public float raiseHeight = 32f;

    // ============= 落点 / 伤害（纯伤害，不秒杀） =============
    /** 总冲击半径（像素） */
    public float impactRadius = 56f;
    /** 建筑伤害（按 falloff 衰减，不再秒杀） */
    public float edgeDamage = 900f;
    /** 单位伤害（按 falloff 衰减，不秒杀） */
    public float unitDamage = 900f;
    /** 内圈满伤比例：内 falloffInner*impactRadius 满伤，外圈线性衰减到 0 */
    public float falloffInner = 0.4f;

    // ============= 外观 =============
    /** 龙爪尺寸 */
    public float clawSize = 22f;
    /** 龙爪颜色 */
    public Color clawColor = Color.valueOf("c9d4e3");
    /** 预警主色（描边 / 外圈） */
    public Color warnColor = Color.valueOf("ffb545");
    /** 中心重击区颜色 */
    public Color warnFill = Color.valueOf("ff3b2f");
    /** 尘土颜色 */
    public Color dustColor = Color.valueOf("6b6153");
    /** 火星颜色 */
    public Color sparkColor = Color.valueOf("ffd27a");

    // ============= 可选附效（默认全部关闭） =============
    /** 点燃落点附近单位 burnTime 秒 */
    public boolean applyBurn = false;
    public float burnTime = 1.5f;
    /** 冲击波击退单位 */
    public boolean applyKnockback = false;
    public float knockbackForce = 2.4f;

    private static TextureRegion clawRegion;

    public DragonClawWeapon(String name){
        super(name);
        // 父类构造已把 bullet 设为 Bullets.placeholder（非 null），保证 range() 等父类方法不会 NPE
        shootSound = Sounds.none;
    }

    public DragonClawWeapon(){
        this("");
    }

    @Override
    public void load(){
        super.load();
        // 可选：若存在 "<name>-claw" 贴图就用贴图当龙爪，否则用程序化图形
        if(name != null && !name.isEmpty()){
            clawRegion = Core.atlas.find(name + "-claw");
        }
    }

    /** 覆写射程：本武器不发射弹丸，直接用配置的作战范围 */
    @Override
    public float range(){
        return clawRange;
    }

    // ============================================================
    //  per-unit + per-mount 运行时状态
    // ============================================================
    /** 单个武器挂点的运行时状态 */
    public static class WState{
        /** 距离下一记拍击的剩余帧数 */
        public float cooldown = 0f;
        /** 当前锁定的目标 */
        public Teamc target = null;
        /** 进行中的拍击（null 表示空闲） */
        public Strike strike = null;
    }

    /** 一次"拍击"的完整状态机 */
    public static class Strike{
        /** 归属阵营（决定伤害判定） */
        final Team team;
        /** 发起拍击时武器挂点的世界坐标（挥击起点） */
        final float sx, sy;
        /** 锁定的目标（预警期间持续跟踪其位置） */
        Posc target;
        /** 当前冲击点（世界坐标） */
        float tx, ty;
        /** 0=蓄力 1=预警 2=挥落 3=余波 */
        int phase = 0;
        float t = 0f;
        /** 尘土 / 火星的随机相位，避免每次一模一样 */
        float seed = Mathf.random(360f);

        Strike(Team team, float sx, float sy, Teamc target){
            this.team = team;
            this.sx = sx;
            this.sy = sy;
            this.target = target;
            this.tx = target != null ? target.getX() : sx;
            this.ty = target != null ? target.getY() : sy;
        }
    }

    protected final IntMap<WState[]> stateMap = new IntMap<>();
    protected float cleanCounter = 0f;

    // ============================================================
    //  主循环
    // ============================================================
    @Override
    public void update(Unit unit, WeaponMount mount){
        if(unit == null || unit.dead || mount == null || !unit.isAdded()){
            return;
        }

        WState st = acquireState(unit, mount);

        // ---------- 索敌 ----------
        if(st.target == null || !validTarget(st.target, unit)){
            st.target = findNearestTarget(unit);
        }
        boolean hasTarget = st.target != null && validTarget(st.target, unit);

        // ---------- 开火意图 ----------
        // 只要挂点在开火（玩家按住攻击键 / AI 决定开火）或单位处于开火状态，就算「要挥爪」；
        // 这样玩家对着空地、墙体按住攻击键也能挥出一记拍击，而不再是「没自动目标就没反应」。
        boolean manual = mount.shoot || unit.isShooting;

        // 落点：有自动目标就砸目标；否则砸向瞄准点（玩家准星 / AI 意图点）
        float fireX = hasTarget ? st.target.getX() : unit.aimX;
        float fireY = hasTarget ? st.target.getY() : unit.aimY;

        // 射程夹取：落点若超出 range()，沿「单位 -> 落点」方向拉回最大射程圆上。
        // 这样玩家把准星指到射程外按开火，龙爪仍然挥出，但只能砸在射程边缘，
        // 不会隔空敲到远处的空地 / 目标。
        float clampDist = Mathf.dst(unit.x, unit.y, fireX, fireY);
        if(clampDist > range() && clampDist > 0.0001f){
            float k = range() / clampDist;
            fireX = unit.x + (fireX - unit.x) * k;
            fireY = unit.y + (fireY - unit.y) * k;
        }

        boolean canSwing = hasTarget || manual;

        // ---------- 挂点朝向 / 预热（可视化瞄准） ----------
        if(canSwing){
            float ang = Angles.angle(mountWorldX(unit), mountWorldY(unit), fireX, fireY);
            mount.rotation = Angles.moveToward(mount.rotation, ang, turnSpeed * Time.delta);
            mount.warmup = Mathf.lerpDelta(mount.warmup, 1f, shootWarmupSpeed);
        }else{
            mount.warmup = Mathf.lerpDelta(mount.warmup, 0f, shootWarmupSpeed);
        }

        // ---------- 冷却 ----------
        if(st.cooldown > 0f) st.cooldown -= Time.delta;
        // 把冷却同步给 mount.reload，让原版 AI 能正确判断"武器是否就绪"
        mount.reload = Math.max(0f, st.cooldown);
        mount.smoothReload = reloadInterval > 0f ? Mathf.clamp(st.cooldown / reloadInterval) : 0f;

        // ---------- 发起一记拍击 ----------
        if(st.strike == null && st.cooldown <= 0f && canSwing){
            float sx = mountWorldX(unit);
            float sy = mountWorldY(unit);
            Strike strike = new Strike(unit.team, sx, sy, hasTarget ? st.target : null);
            // 无论手动还是自动，落点统一使用夹取后的射程内坐标
            strike.tx = fireX;
            strike.ty = fireY;
            st.strike = strike;
            st.cooldown = reloadInterval;
            // 起手：脚下扬起一圈尘土
            Fx.smoke.at(sx, sy);
            Effect.shake(2f, 4f, unit);
        }

        // ---------- 推进当前拍击 ----------
        if(st.strike != null){
            updateStrike(unit, st.strike);
            if(st.strike.phase > 3) st.strike = null;
        }

        sweepDeadUnits();
    }

    // ============================================================
    //  坐标 / 索敌工具
    // ============================================================
    /** 武器挂点的世界 X 坐标（与父类 draw 的换算一致） */
    protected float mountWorldX(Unit unit){
        return unit.x + Angles.trnsx(unit.rotation - 90f, x, y);
    }

    /** 武器挂点的世界 Y 坐标 */
    protected float mountWorldY(Unit unit){
        return unit.y + Angles.trnsy(unit.rotation - 90f, x, y);
    }

    /** 目标是否仍然有效（存活 + 仍在射程内） */
    protected boolean validTarget(Teamc t, Unit unit){
        if(t == null) return false;
        if(t instanceof Unit u){
            return !u.dead() && u.isAdded() && unit.within(u, range() + 4f);
        }
        if(t instanceof Building b){
            return !b.dead && b.isValid() && unit.within(b, range() + 4f);
        }
        return false;
    }

    /** 把冲击点拉回以单位为中心的射程圆内（目标跑远 / 瞄准超程时使用） */
    protected void clampImpact(Unit unit, Strike s){
        float d = Mathf.dst(unit.x, unit.y, s.tx, s.ty);
        if(d > range() && d > 0.0001f){
            float k = range() / d;
            s.tx = unit.x + (s.tx - unit.x) * k;
            s.ty = unit.y + (s.ty - unit.y) * k;
        }
    }

    /** 就近索敌：单位与建筑一起比较距离，取最近者（不再优先单位而屏蔽建筑） */
    protected Teamc findNearestTarget(Unit unit){
        float[] bestD = {Float.MAX_VALUE};
        Teamc[] best = {null};

        Units.nearbyEnemies(unit.team, unit.x, unit.y, range(), u -> {
            if(u.dead() || !u.isAdded()) return;
            if(!targetAir && !u.isGrounded()) return;
            if(!targetGround && u.isGrounded()) return;
            float d = Mathf.dst2(unit.x, unit.y, u.x, u.y);
            if(d < bestD[0]){
                bestD[0] = d;
                best[0] = u;
            }
        });

        if(targetGround && targetBlocks){
            try{
                Groups.build.intersect(unit.x - range(), unit.y - range(), range() * 2f, range() * 2f, b -> {
                    if(b == null || b.dead || b.block == null) return;
                    // 只排除己方建筑；自然墙体(derelict)、中立方块一律可打
                    if(b.team == unit.team) return;
                    float d = Mathf.dst2(unit.x, unit.y, b.x, b.y);
                    // 方形包围盒会带进四个角落的建筑（最远可达 range*1.414），
                    // 所以这里必须再做一次真正的圆形射程剔除，超程直接跳过。
                    if(d > range() * range()) return;
                    if(d < bestD[0]){
                        bestD[0] = d;
                        best[0] = b;
                    }
                });
            }catch(NullPointerException ignored){}
        }
        return best[0];
    }

    // ============================================================
    //  阶段推进
    // ============================================================
    protected void updateStrike(Unit unit, Strike s){
        s.t += Time.delta;

        switch(s.phase){
            case 0: { // 蓄力上抬
                if(s.t >= raiseTime * 60f){
                    s.phase = 1;
                    s.t = 0f;
                }
                break;
            }
            case 1: { // 预警：持续跟踪目标，刷新落点
                if(s.target != null){
                    boolean valid = (s.target instanceof Unit u) ? !u.dead() : (s.target instanceof Building b && !b.dead);
                    if(valid){
                        s.tx = s.target.getX();
                        s.ty = s.target.getY();
                    }
                }
                // 目标可能在预警期间跑出射程，这里持续把冲击点夹取回射程圆内
                clampImpact(unit, s);
                if(s.t >= warnTime * 60f){
                    s.phase = 2;
                    s.t = 0f;
                }
                break;
            }
            case 2: { // 挥落
                if(s.t >= fallTime * 60f){
                    s.phase = 3;
                    s.t = 0f;
                    landStrike(s);
                }
                break;
            }
            case 3: { // 余波
                if(s.t >= recoilTime * 60f){
                    s.phase = 4;
                }
                break;
            }
        }
    }

    // ============================================================
    //  落地结算：统一纯伤害（按 falloff 衰减，不秒杀）
    // ============================================================
    protected void landStrike(Strike s){
        // —— 敌方单位 ——
        Units.nearbyEnemies(s.team, s.tx, s.ty, impactRadius, u -> {
            if(u.dead() || !u.isAdded()) return;
            float d = Mathf.dst(u.x, u.y, s.tx, s.ty);
            if(d > impactRadius) return;
            float f = falloff(d);
            u.damage(unitDamage * f);
            if(applyBurn) u.apply(StatusEffects.burning, burnTime * 60f);
            if(applyKnockback && f > 0f){
                float ang = Angles.angle(s.tx, s.ty, u.x, u.y);
                u.vel.add(Mathf.cos(ang) * knockbackForce * f * 10f, Mathf.sin(ang) * knockbackForce * f * 10f);
            }
        });

        // —— 敌方建筑：同样纯伤害，不再代码杀 ——
        if(targetBlocks){
            try{
                Groups.build.intersect(s.tx - impactRadius, s.ty - impactRadius, impactRadius * 2f, impactRadius * 2f, b -> {
                    if(b == null || b.dead || b.team == s.team) return;
                    float d = Mathf.dst(b.x, b.y, s.tx, s.ty);
                    if(d > impactRadius) return;
                    b.damage(edgeDamage * falloff(d));
                });
            }catch(NullPointerException ignored){}
        }

        // —— 冲击表现：爆炸 + 冲击波 + 环状扬尘 + 屏幕震动 ——
        Fx.explosion.at(s.tx, s.ty);
        Fx.shockwave.at(s.tx, s.ty);
        for(int i = 0; i < 10; i++){
            float a = s.seed + i * 36f;
            float r = impactRadius * (0.4f + 0.6f * Mathf.random());
            Fx.smoke.at(s.tx + Angles.trnsx(a, r), s.ty + Angles.trnsy(a, r));
        }
        Effect.shake(9f, 16f, new Vec2(s.tx, s.ty));
    }

    /** 平台式衰减：内圈满伤，外圈线性衰减到 0 */
    protected float falloff(float d){
        float inner = impactRadius * falloffInner;
        if(d <= inner) return 1f;
        if(d >= impactRadius) return 0f;
        return Mathf.clamp(1f - (d - inner) / (impactRadius - inner));
    }

    // ============================================================
    //  绘制：先画父类挂点贴图，再叠加本次拍击的龙爪 / 范围圈预警
    // ============================================================
    @Override
    public void draw(Unit unit, WeaponMount mount){
        super.draw(unit, mount);
        if(unit == null || unit.dead || mount == null) return;

        WState[] arr = stateMap.get(unit.id());
        if(arr == null) return;
        int idx = mountIndex(unit, mount);
        if(idx < 0 || idx >= arr.length || arr[idx] == null) return;
        Strike s = arr[idx].strike;
        if(s == null) return;

        float oldZ = Draw.z();
        switch(s.phase){
            case 0: drawRaise(s); break;
            case 1: drawWarn(s); break;
            case 2: drawFall(s); break;
            case 3: drawRecoil(s); break;
        }
        // 复位：颜色/描边 + 还原图层，避免污染同一单位的后续挂点绘制
        Draw.reset();
        Draw.z(oldZ);
    }

    /** 阶段 0：龙爪向身后略微收，同时上抬蓄力 */
    protected void drawRaise(Strike s){
        float p = Mathf.clamp(s.t / (raiseTime * 60f));
        float ease = p * p; // 加速

        // 从挂点指向落点的方向
        float ang = Angles.angle(s.sx, s.sy, s.tx, s.ty);
        // 向身后收一点 + 略微抬起
        float back = clawSize * 0.9f * ease;
        float cx = s.sx - Angles.trnsx(ang, back);
        float cy = s.sy - Angles.trnsy(ang, back) + ease * raiseHeight;

        Draw.z(Layer.effect);
        // 地面阴影（随高度收缩变淡）
        Draw.color(Color.black, 0.30f * (1f - 0.4f * ease));
        Fill.circle(s.sx, s.sy, clawSize * (0.7f - 0.25f * ease));
        // 蓄力尘环
        Draw.color(warnColor, 0.45f * ease);
        Lines.stroke(1.5f);
        Lines.circle(s.sx, s.sy, 10f + ease * 20f);
        // 朝目标方向的蓄力弧线
        Draw.color(warnColor, 0.35f * ease);
        Lines.stroke(2f);
        Lines.arc(s.sx, s.sy, clawSize * 1.6f, 0.5f, ang);

        // 龙爪本体（朝向目标，不自旋）
        Draw.z(Layer.effect + 1f);
        drawClaw(cx, cy, clawSize, aimRot(s), 1f);
        Draw.color();
    }

    /** 阶段 1：龙爪探向目标方向并悬停，地面亮起范围圈预警 */
    protected void drawWarn(Strike s){
        float p = Mathf.clamp(s.t / (warnTime * 60f));
        float moveP = Mathf.clamp(p * 1.5f); // 前 2/3 时间探出到位
        // 悬停位：由挂点向落点方向探出约 30%，并略微抬起
        float holdX = Mathf.lerp(s.sx, s.tx, 0.30f);
        float holdY = Mathf.lerp(s.sy, s.ty, 0.30f) + raiseHeight;
        float hx = Mathf.lerp(s.sx, holdX, moveP);
        float hy = Mathf.lerp(s.sy + raiseHeight, holdY, moveP);

        Draw.z(Layer.effect);
        drawWarnCircle(s, p);

        // 悬停的龙爪（带轻微待发浮动，朝向目标，不自旋）
        Draw.z(Layer.effect + 1f);
        float bob = Mathf.sin(Time.time * 0.15f) * 2f;
        drawClaw(hx, hy + bob, clawSize, aimRot(s), 1f);

        // 落点百分比倒计时
        Font font = Fonts.outline != null ? Fonts.outline : Fonts.def;
        if(font != null){
            font.setColor(warnColor.r, warnColor.g, warnColor.b, 0.95f);
            font.draw((int)(p * 100f) + "%", s.tx, s.ty + font.getLineHeight() / 2f, Align.center);
        }
        Draw.color();
    }

    /** 阶段 2：龙爪由悬停位高速向落点挥砸而下 */
    protected void drawFall(Strike s){
        float p = Mathf.clamp(s.t / (fallTime * 60f));
        float ease = p * p;

        float holdX = Mathf.lerp(s.sx, s.tx, 0.30f);
        float holdY = Mathf.lerp(s.sy, s.ty, 0.30f) + raiseHeight;
        float cx = Mathf.lerp(holdX, s.tx, ease);
        float cy = Mathf.lerp(holdY, s.ty, ease);

        Draw.z(Layer.effect);
        drawShadow(s.tx, s.ty, 1f);
        drawWarnCircle(s, 1f);

        // 挥落残影：从悬停位到当前位置的拖尾
        Draw.color(warnColor, 0.28f * (1f - p));
        Lines.stroke(clawSize * 0.5f * (1f - p) + 2f);
        Lines.line(holdX, holdY, cx, cy);

        // 龙爪（朝向目标，不自旋）
        Draw.z(Layer.effect + 1f);
        drawClaw(cx, cy, clawSize, aimRot(s), 1f);
        Draw.color();
    }

    /** 阶段 3：冲击波扩散 + 尘土/火星迸发 + 龙爪回抽淡出 */
    protected void drawRecoil(Strike s){
        float after = recoilTime * 60f;
        float p = Mathf.clamp(s.t / after);
        float fade = 1f - p;

        Draw.z(Layer.effect);
        // 冲击圈
        if(fade > 0f){
            Draw.color(warnFill, 0.55f * fade);
            Fill.circle(s.tx, s.ty, impactRadius * (0.30f + 0.70f * p));
            Draw.color(Color.white, 0.85f * fade);
            Lines.stroke(6f * fade);
            Lines.circle(s.tx, s.ty, impactRadius * (0.5f + 1.0f * p));
        }

        // 尘土：向外扩散的土色尘团
        if(fade > 0f){
            Draw.color(dustColor, 0.45f * fade);
            for(int i = 0; i < 12; i++){
                float a = s.seed + i * 30f;
                float r = impactRadius * (0.5f + 1.1f * p);
                Fill.circle(s.tx + Angles.trnsx(a, r), s.ty + Angles.trnsy(a, r), (6f + 8f * p) * fade);
            }
        }

        // 火星：向外飞溅的亮色短线
        if(fade > 0f){
            Draw.color(sparkColor, fade);
            Lines.stroke(2f * fade);
            for(int i = 0; i < 16; i++){
                float a = s.seed * 1.7f + i * 22.5f;
                float r = impactRadius * (0.3f + 1.6f * p);
                float x1 = s.tx + Angles.trnsx(a, r * 0.6f);
                float y1 = s.ty + Angles.trnsy(a, r * 0.6f);
                float x2 = s.tx + Angles.trnsx(a, r);
                float y2 = s.ty + Angles.trnsy(a, r);
                Lines.line(x1, y1, x2, y2);
            }
        }

        // 龙爪回抽淡出
        if(fade > 0f){
            Draw.z(Layer.effect + 1f);
            float rx = Mathf.lerp(s.tx, s.sx, p);
            float ry = Mathf.lerp(s.ty, s.sy, p);
            drawClaw(rx, ry, clawSize * (1f + 0.25f * fade), aimRot(s), fade);
        }
        Draw.color();
    }

    /** 落点阴影：随预警推进放大 */
    protected void drawShadow(float x, float y, float p){
        float r = impactRadius * (0.5f + 0.5f * p);
        Draw.color(Color.black, 0.35f * (0.4f + 0.6f * p));
        Fill.circle(x, y, r * 0.9f);
    }

    /** 范围圈预警：脉动外圈 + 收缩内圈 + 旋转刻度 + 十字准星 */
    protected void drawWarnCircle(Strike s, float p){
        float pulse = 0.5f + 0.5f * Mathf.sin(Time.time * 0.25f);

        // 中心淡填充
        Draw.color(warnFill, 0.10f + 0.16f * p);
        Fill.circle(s.tx, s.ty, impactRadius);

        // 外圈（脉动）
        Draw.color(warnColor, 0.35f + 0.35f * pulse);
        Lines.stroke(3f);
        Lines.circle(s.tx, s.ty, impactRadius);

        // 内圈（收缩倒计时）
        Draw.color(warnFill, 0.55f);
        Lines.stroke(3f);
        Lines.circle(s.tx, s.ty, impactRadius * Mathf.clamp(1f - 0.6f * p));

        // 旋转刻度
        Draw.color(warnColor, 0.7f);
        Lines.stroke(2.5f);
        int segs = 12;
        for(int i = 0; i < segs; i++){
            float a = i * (360f / segs) + Time.time * 0.6f;
            float x1 = s.tx + Angles.trnsx(a, impactRadius * 0.86f);
            float y1 = s.ty + Angles.trnsy(a, impactRadius * 0.86f);
            float x2 = s.tx + Angles.trnsx(a, impactRadius);
            float y2 = s.ty + Angles.trnsy(a, impactRadius);
            Lines.line(x1, y1, x2, y2);
        }

        // 十字准星
        Draw.color(warnColor, 0.55f);
        Lines.stroke(2f);
        Lines.line(s.tx - impactRadius, s.ty, s.tx + impactRadius, s.ty);
        Lines.line(s.tx, s.ty - impactRadius, s.tx, s.ty + impactRadius);

        Draw.color();
    }

    // ============================================================
    //  龙爪图形
    // ============================================================
    /** 龙爪朝向：让三根利爪指向锁定的目标点（不自旋） */
    protected float aimRot(Strike s){
        // drawClaw 里利爪的基础角是 rot + 270°，令利爪指向 aimAng，则传入 aimAng - 270°
        return Angles.angle(s.sx, s.sy, s.tx, s.ty) - 270f;
    }

    /** 画一只朝下拍击的龙爪：掌心 + 三根利爪；若有 "<name>-claw" 贴图则优先用贴图 */
    protected void drawClaw(float cx, float cy, float size, float rot, float alpha){
        TextureRegion tr = claw();
        if(tr != null && tr.found()){
            Draw.color(Color.white, alpha);
            Draw.rect(tr, cx, cy, size * 2f, size * 2f, rot);
            Draw.color();
            return;
        }

        // 掌心
        Draw.color(Color.black, 0.45f * alpha);
        Fill.circle(cx, cy + size * 0.12f, size * 0.52f);
        Draw.color(clawColor, alpha);
        Fill.circle(cx, cy, size * 0.46f);

        // 三根利爪：指向下方（270°）呈扇形
        for(int i = -1; i <= 1; i++){
            float a = rot + 270f + i * 38f;
            float dx = Angles.trnsx(a, 1f);
            float dy = Angles.trnsy(a, 1f);
            float nx = -dy, ny = dx; // 垂直方向，用于加宽基部
            float bx = cx + dx * size * 0.25f;
            float by = cy + dy * size * 0.25f;
            float ex = cx + dx * size * 1.15f;
            float ey = cy + dy * size * 1.15f;
            float w = size * 0.16f;

            Draw.color(clawColor, alpha);
            Fill.tri(bx + nx * w, by + ny * w, bx - nx * w, by - ny * w, ex, ey);
            Draw.color(Color.white, alpha);
            Fill.circle(ex, ey, w * 0.5f);
        }
        Draw.color();
    }

    /** 懒加载龙爪贴图 */
    protected static TextureRegion claw(){
        return clawRegion;
    }

    // ============================================================
    //  state 存取 / GC
    // ============================================================
    protected WState acquireState(Unit unit, WeaponMount mount){
        int uid = unit.id();
        int idx = mountIndex(unit, mount);
        if(idx < 0) idx = 0;
        WState[] arr = stateMap.get(uid);
        int mountCnt = (unit.mounts != null) ? unit.mounts.length : Math.max(1, idx + 1);
        if(arr == null || arr.length != mountCnt){
            arr = new WState[mountCnt];
            for(int i = 0; i < mountCnt; i++) arr[i] = new WState();
            stateMap.put(uid, arr);
        }
        if(arr[idx] == null) arr[idx] = new WState();
        return arr[idx];
    }

    protected static int mountIndex(Unit unit, WeaponMount mount){
        if(unit == null || unit.mounts == null || mount == null) return 0;
        WeaponMount[] ms = unit.mounts;
        for(int i = 0; i < ms.length; i++) if(ms[i] == mount) return i;
        return 0;
    }

    protected void sweepDeadUnits(){
        cleanCounter += Time.delta;
        if(cleanCounter < 60f) return; // 每秒扫一次
        cleanCounter = 0f;
        if(stateMap.size == 0) return;

        IntSeq ids = new IntSeq(stateMap.size);
        var keys = stateMap.keys();
        while(keys.hasNext) ids.add(keys.next());
        IntSeq toRemove = new IntSeq(Math.min(4, ids.size));
        for(int i = 0; i < ids.size; i++){
            int uid = ids.items[i];
            Unit u = Groups.unit.getByID(uid);
            if(u == null || u.dead || !u.isAdded()) toRemove.add(uid);
        }
        for(int i = 0; i < toRemove.size; i++) stateMap.remove(toRemove.items[i]);
    }

    // ============================================================
    //  对外接口：供外部系统（如列阵召唤的士兵单位）读取长官的当前目标
    // ============================================================
    /** 该单位任一挂点当前锁定的有效目标；没有则返回 null。 */
    public Teamc currentTarget(Unit unit){
        if(unit == null || unit.dead || !unit.isAdded()) return null;
        WState[] arr = stateMap.get(unit.id());
        if(arr == null) return null;
        for(WState st : arr){
            if(st == null) continue;
            if(st.target != null && validTarget(st.target, unit)) return st.target;
        }
        return null;
    }

    /** 该单位是否正在攻击（任一挂点已锁定目标或正在挥击）。 */
    public boolean isAttacking(Unit unit){
        if(unit == null || unit.dead || !unit.isAdded()) return false;
        WState[] arr = stateMap.get(unit.id());
        if(arr == null) return false;
        for(WState st : arr){
            if(st == null) continue;
            if(st.strike != null) return true;
            if(st.target != null && validTarget(st.target, unit)) return true;
        }
        return false;
    }
}
