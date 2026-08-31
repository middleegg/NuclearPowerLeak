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
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.units.WeaponMount;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Posc;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.Weapon;
import mindustry.ui.Fonts;

/**
 * TianFaWeapon extends Weapon —— 单位专用的「天罚」武器，功能与 TianFa 炮台基本一致。
 *
 * ============== 与 TianFa 炮台的差异 ==============
 *   1) 炮台靠"输入弹药物品"攒存量；单位没有物品栏，因此这里改为"时间攒存量"：
 *      每 stockInterval 帧自动 +1 发，上限 maxStock。想保留物品消耗可自行在外部扣资源。
 *   2) 炮台坐标是世界坐标；武器坐标以"载具单位"为原点，发射台位置由武器挂点算出，
 *      打击落点仍是世界坐标（因此单位移动不影响已发射导弹的落点）。
 *   3) 完全接管 Weapon.update，不调用 super.update —— 避免父类用占位弹丸真的开火；
 *      同时自行维护 mount.rotation / mount.warmup，保证挂点朝向与预热动画正常。
 *
 * ============== 使用范例（在 UnitType 里） ==============
 * <pre>{@code
 * myUnit = new UnitType("myUnit"){{
 *     weapons.add(new TianFaWeapon("my-weapon"){{
 *         x = 0f; y = -6f;
 *         mirror = false;
 *         reload = 480f;          // 只影响父类的冷却条，本武器不使用
 *
 *         // —— 弹药存放制 ——
 *         maxStock      = 6;
 *         stockInterval = 60f;
 *         waveDelay     = 6f;
 *
 *         // —— 打击参数 ——
 *         strikeRange  = 320f;
 *         riseTime     = 0.8f;
 *         warnTime     = 1.2f;
 *         fallTime     = 0.55f;
 *         dropHeight   = 340f;
 *         riseHeight   = 260f;
 *         strikeRadius = 60f;
 *         strikeDamage = 340f;
 *         falloffInner = 0.6f;
 *         lotusSize    = 30f;
 *
 *         // —— 可选附效（默认全关）——
 *         applyBurn = true;
 *         leaveLinger = true;
 *     }});
 * }};
 * }</pre>
 */
public class TianFaWeapon extends Weapon{

    // ============= 弹药存放制（单位版：时间攒弹） =============
    /** 弹药存放上限 */
    public int maxStock = 6;
    /** 每存放一发弹药所需的帧数 */
    public float stockInterval = 60f;
    /** 齐射时相邻两枚导弹的发射间隔（帧） */
    public float waveDelay = 6f;

    // ============= 打击参数 =============
    /** 索敌 / 打击范围（同时作为 range() 返回值，决定载具 AI 的作战距离） */
    public float strikeRange = 320f;
    /** 能否打空中 */
    public boolean targetAir = true;
    /** 能否打地面 */
    public boolean targetGround = true;
    /** 能否打敌方建筑 */
    public boolean targetBlocks = true;
    /** 导弹升空时长（秒） */
    public float riseTime = 0.8f;
    /** 预警时长（秒）：圆环扩充 + 填充 */
    public float warnTime = 1.2f;
    /** Lotus 坠落时长（秒） */
    public float fallTime = 0.55f;
    /** Lotus 起始下落高度（像素） */
    public float dropHeight = 340f;
    /** 导弹升空高度（像素） */
    public float riseHeight = 260f;
    /** 落地伤害半径 */
    public float strikeRadius = 60f;
    /** 落地中心伤害（外缘按平台式衰减递减到 0） */
    public float strikeDamage = 340f;
    /** 平台式衰减内圈比例：内 falloffInner 半径满伤，外圈线性衰减 */
    public float falloffInner = 0.6f;
    /** Lotus 贴图大小 */
    public float lotusSize = 30f;
    /** 挂点转向速度（度/帧） */
    public float turnSpeed = 8f;

    // ============= 预警视觉 =============
    public Color warnColor = Color.valueOf("ffd27a");
    public Color warnFill = Color.valueOf("ff3b2f");

    // ============= 可选附效（默认全部关闭） =============
    /** 重燃：点燃敌人 burnTime 秒 */
    public boolean applyBurn = false;
    public float burnTime = 1.5f;
    /** 击退：冲击波推离落点 */
    public boolean applyKnockback = false;
    public float knockbackForce = 2.4f;
    /** 减速：短暂减速 */
    public boolean applySlow = false;
    public float slowTime = 2f;
    /** 残留区：落点留下持续伤害区（对单位+建筑） */
    public boolean leaveLinger = false;
    public float lingerTime = 10f;
    /** 残留区每秒伤害 */
    public float lingerDps = 10f;
    /** 残留区伤害结算间隔（帧） */
    public float lingerTick = 10f;

    private static TextureRegion lotusRegion;

    public TianFaWeapon(String name){
        super(name);
        // 父类构造已把 bullet 设为 Bullets.placeholder（非 null），保证 range() 等父类方法不会 NPE
        shootSound = mindustry.gen.Sounds.none;
    }

    public TianFaWeapon(){
        this("");
    }

    @Override
    public void load(){
        super.load();
        lotusRegion = Core.atlas.find("nu-Lotus");
    }

    /** 覆写射程：本武器不发射弹丸，直接用配置的打击范围（决定载具 AI 交战距离） */
    @Override
    public float range(){
        return strikeRange;
    }

    // ============================================================
    //  per-unit + per-mount 运行时状态
    // ============================================================
    /** 单个武器挂点的完整运行时状态 */
    public static class WState{
        /** 当前弹药存量 */
        public int stock = 0;
        /** 存放计时 */
        public float stockTimer = 0f;
        /** 齐射中尚未发射的导弹数 */
        public int pending = 0;
        /** 齐射波次计时 */
        public float waveTimer = 0f;
        /** 齐射目标轮询索引 */
        public int assignIndex = 0;
        /** 当前锁定的目标 */
        public Teamc target = null;
        /** 进行中的打击 */
        public final Seq<Strike> strikes = new Seq<>();
        /** 齐射可用目标池 */
        public final Seq<Posc> volleyTargets = new Seq<>();
    }

    /** 单枚导弹的完整状态机 */
    public static class Strike{
        /** 归属阵营（决定伤害判定） */
        final Team team;
        /** 发射原点（武器挂点世界坐标） */
        final float sx, sy;
        /** 锁定的目标（升空期间持续跟踪，升空结束时锁定其位置） */
        Posc target;
        /** 目标最后已知位置 */
        float tx, ty;
        /** 0=升空 1=预警 2=坠落 3=落地余波 */
        int phase = 0;
        float t = 0f;
        float rot = 0f;
        /** 残留区已结算的计时 */
        float lingerAcc = 0f;
        /** 本发导弹所用的弹道类型（取自武器自身的 bullet），决定伤害/半径/状态/特效 */
        BulletType bullet;

        Strike(Team team, float sx, float sy, Posc target, BulletType bullet){
            this.team = team;
            this.sx = sx;
            this.sy = sy;
            this.target = target;
            this.tx = target != null ? target.getX() : sx;
            this.ty = target != null ? target.getY() : sy;
            this.bullet = bullet;
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

        // ---------- 挂点朝向 / 预热（可视化瞄准） ----------
        if(st.target != null && validTarget(st.target, unit)){
            float ang = Angles.angle(mountWorldX(unit), mountWorldY(unit), st.target.getX(), st.target.getY());
            mount.rotation = Angles.moveToward(mount.rotation, ang, turnSpeed * Time.delta);
            mount.warmup = Mathf.lerpDelta(mount.warmup, 1f, shootWarmupSpeed);
        }else{
            mount.warmup = Mathf.lerpDelta(mount.warmup, 0f, shootWarmupSpeed);
        }

        // ---------- 弹药存放 ----------
        st.stockTimer += Time.delta;
        if(st.stockTimer >= stockInterval){
            st.stockTimer = 0f;
            if(st.stock < maxStock) st.stock++;
        }

        // ---------- 触发齐射 ----------
        if(st.pending <= 0 && st.stock > 0 && st.target != null && validTarget(st.target, unit)){
            st.pending = st.stock;
            st.stock = 0;
            st.stockTimer = 0f;
            st.assignIndex = 0;
            collectTargets(unit, st);
        }

        // ---------- 连续波次发射 ----------
        if(st.pending > 0){
            st.waveTimer += Time.delta;
            if(st.waveTimer >= waveDelay){
                st.waveTimer = 0f;
                launchStrike(unit, st);
                st.pending--;
            }
        }

        // ---------- 打击状态机推进 ----------
        for(int i = st.strikes.size - 1; i >= 0; i--){
            Strike s = st.strikes.get(i);
            updateStrike(s);
            if(s.phase > 3) st.strikes.remove(i);
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

    /** 就近索敌：优先敌方单位，其次敌方建筑 */
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
        if(best[0] != null) return best[0];

        if(targetGround && targetBlocks){
            try{
                Groups.build.intersect(unit.x - range(), unit.y - range(), range() * 2f, range() * 2f, b -> {
                    if(b == null || b.dead || b.team == unit.team || b.team == Team.derelict) return;
                    float d = Mathf.dst2(unit.x, unit.y, b.x, b.y);
                    if(d < bestD[0]){
                        bestD[0] = d;
                        best[0] = b;
                    }
                });
            }catch(NullPointerException ignored){}
        }
        return best[0];
    }

    /** 收集射程内所有敌方目标（单位优先，其次建筑），供多目标轮询分配 */
    protected void collectTargets(Unit unit, WState st){
        st.volleyTargets.clear();
        Units.nearbyEnemies(unit.team, unit.x, unit.y, range(), u -> {
            if(!u.dead() && u.isAdded()) st.volleyTargets.add(u);
        });
        if(st.volleyTargets.isEmpty()){
            try{
                Groups.build.intersect(unit.x - range(), unit.y - range(), range() * 2f, range() * 2f, b -> {
                    if(b != null && !b.dead && b.team != unit.team && b.team != Team.derelict){
                        st.volleyTargets.add(b);
                    }
                });
            }catch(NullPointerException ignored){}
        }
    }

    // ============================================================
    //  发射
    // ============================================================
    /** 发射一枚导弹：轮流分配目标 */
    protected void launchStrike(Unit unit, WState st){
        Posc tgt = null;
        if(st.volleyTargets.size > 0){
            while(!st.volleyTargets.isEmpty()){
                Posc cand = st.volleyTargets.get(st.assignIndex % st.volleyTargets.size);
                boolean valid = (cand instanceof Unit u) ? !u.dead() : (cand instanceof Building b && !b.dead);
                if(valid){
                    tgt = cand;
                    break;
                }
                st.volleyTargets.remove(st.assignIndex % st.volleyTargets.size);
            }
            st.assignIndex++;
        }
        if(tgt == null && st.target != null && validTarget(st.target, unit)){
            tgt = st.target;
        }

        float sx = mountWorldX(unit);
        float sy = mountWorldY(unit);
        st.strikes.add(new Strike(unit.team, sx, sy, tgt, bullet));

        Fx.launch.at(sx, sy);
        Fx.shockwave.at(sx, sy);
        Effect.shake(2f, 4f, unit);
    }

    // ============================================================
    //  阶段推进
    // ============================================================
    protected void updateStrike(Strike s){
        s.t += Time.delta;
        s.rot += 90f * Time.delta;

        switch(s.phase){
            case 0: { // 升空
                if(s.target != null){
                    boolean valid = (s.target instanceof Unit u) ? !u.dead() : (s.target instanceof Building b && !b.dead);
                    if(valid){
                        s.tx = s.target.getX();
                        s.ty = s.target.getY();
                    }
                }
                if(s.t >= riseTime * 60f){
                    s.phase = 1;
                    s.t = 0f;
                }
                break;
            }
            case 1: { // 预警
                if(s.t >= warnTime * 60f){
                    s.phase = 2;
                    s.t = 0f;
                }
                break;
            }
            case 2: { // 坠落
                if(s.t >= fallTime * 60f){
                    s.phase = 3;
                    s.t = 0f;
                    landStrike(s);
                }
                break;
            }
            case 3: { // 落地余波：淡出 + 闪光 + 可选残留区
                float after = 20f;
                if(leaveLinger){
                    s.lingerAcc += Time.delta;
                    if(s.lingerAcc >= lingerTick){
                        s.lingerAcc -= lingerTick;
                        lingerDamage(s);
                    }
                    if(s.t >= lingerTime * 60f){
                        s.phase = 4;
                    }
                }else if(s.t >= after){
                    s.phase = 4;
                }
                break;
            }
        }
    }

    // ============================================================
    //  落地结算：按弹道类型差异化 + 平台式衰减 + 可选附效
    // ============================================================
    /** 本发导弹的有效伤害半径：优先取弹道的 splashDamageRadius，否则退回武器默认 strikeRadius */
    protected float strikeRadiusOf(Strike s){
        if(s.bullet != null && s.bullet.splashDamageRadius > 0f) return s.bullet.splashDamageRadius;
        return strikeRadius;
    }

    /** 本发导弹的有效中心伤害：优先取弹道的 damage（其次 splashDamage），否则退回武器默认 strikeDamage */
    protected float strikeDamageOf(Strike s){
        if(s.bullet != null){
            float d = s.bullet.damage > 0f ? s.bullet.damage : s.bullet.splashDamage;
            if(d > 0f) return d;
        }
        return strikeDamage;
    }

    /** 弹道自带的附效：状态 + 击退（与武器全局附效叠加） */
    protected void applyBulletEffects(Strike s, Unit u, float f){
        if(s.bullet == null) return;
        if(s.bullet.status != null && s.bullet.statusDuration > 0f){
            u.apply(s.bullet.status, s.bullet.statusDuration);
        }
        if(s.bullet.knockback > 0f && f > 0f){
            float ang = Angles.angle(s.tx, s.ty, u.x, u.y);
            u.vel.add(Mathf.cos(ang) * s.bullet.knockback * f * 10f, Mathf.sin(ang) * s.bullet.knockback * f * 10f);
        }
    }

    protected void landStrike(Strike s){
        float r = strikeRadiusOf(s);
        float dmg = strikeDamageOf(s);
        float inner = r * falloffInner;

        // 对敌方单位
        Units.nearbyEnemies(s.team, s.tx, s.ty, r, u -> {
            if(u.dead() || !u.isAdded()) return;
            float d = Mathf.dst(u.x, u.y, s.tx, s.ty);
            if(d > r) return;
            float f = d <= inner ? 1f : Mathf.clamp(1f - (d - inner) / (r - inner));
            u.damage(dmg * f);
            applyBulletEffects(s, u, f);
            if(applyBurn) u.apply(StatusEffects.burning, burnTime * 60f);
            if(applySlow) u.apply(StatusEffects.slow, slowTime * 60f);
            if(applyKnockback && f > 0f){
                float ang = Angles.angle(s.tx, s.ty, u.x, u.y);
                u.vel.add(Mathf.cos(ang) * knockbackForce * f * 10f, Mathf.sin(ang) * knockbackForce * f * 10f);
            }
        });

        // 对敌方建筑
        try{
            Groups.build.intersect(s.tx - r, s.ty - r, r * 2f, r * 2f, b -> {
                if(b == null || b.dead || b.team == s.team || b.team == Team.derelict) return;
                float d = Mathf.dst(b.x, b.y, s.tx, s.ty);
                if(d > r) return;
                float f = d <= inner ? 1f : Mathf.clamp(1f - (d - inner) / (r - inner));
                b.damage(dmg * f);
            });
        }catch(NullPointerException ignored){}

        // 弹道自带的落地特效
        if(s.bullet != null && s.bullet.hitEffect != null){
            s.bullet.hitEffect.at(s.tx, s.ty);
        }
        Fx.explosion.at(s.tx, s.ty);
        Fx.shockwave.at(s.tx, s.ty);
        Effect.shake(4f, 8f, new Vec2(s.tx, s.ty));
    }

    /** 残留区伤害：对单位+建筑 */
    protected void lingerDamage(Strike s){
        float r = strikeRadiusOf(s);
        float amount = lingerDps * lingerTick / 60f;
        Units.nearbyEnemies(s.team, s.tx, s.ty, r, u -> {
            if(!u.dead()) u.damage(amount);
        });
        try{
            Groups.build.intersect(s.tx - r, s.ty - r, r * 2f, r * 2f, b -> {
                if(b != null && !b.dead && b.team != s.team && b.team != Team.derelict){
                    if(Mathf.dst(b.x, b.y, s.tx, s.ty) <= r) b.damage(amount);
                }
            });
        }catch(NullPointerException ignored){}
    }

    // ============================================================
    //  绘制：先画父类挂点贴图，再叠加本挂点的所有打击
    // ============================================================
    @Override
    public void draw(Unit unit, WeaponMount mount){
        super.draw(unit, mount);
        if(unit == null || unit.dead || mount == null) return;

        WState[] arr = stateMap.get(unit.id());
        if(arr == null) return;
        int idx = mountIndex(unit, mount);
        if(idx < 0 || idx >= arr.length || arr[idx] == null) return;
        WState st = arr[idx];
        if(st.strikes.size == 0) return;

        float oldZ = Draw.z();
        for(Strike s : st.strikes){
            switch(s.phase){
                case 0: drawRise(s); break;
                case 1: drawWarn(s); break;
                case 2: drawFall(s); break;
                case 3: drawAfter(s); break;
            }
        }
        // 复位：颜色/描边 + 还原图层，避免污染同一单位的后续挂点绘制
        Draw.reset();
        Draw.z(oldZ);
    }

    /** 阶段 0：导弹垂直升空 + 尾焰 */
    protected void drawRise(Strike s){
        float p = Mathf.clamp(s.t / (riseTime * 60f));
        float ease = p * p; // 加速上升
        float my = s.sy + ease * riseHeight;
        Draw.z(Layer.effect);
        // 尾焰：一串渐隐圆点
        for(int i = 1; i <= 4; i++){
            float ty = my - i * 10f * (0.5f + ease);
            Draw.color(Color.valueOf("ffae57"), 0.35f - i * 0.07f);
            Fill.circle(s.sx, ty, 5f - i * 0.8f);
        }
        // 弹体：小号 Lotus 旋转上升
        TextureRegion lr = lotus();
        if(lr != null && lr.found()){
            Draw.color(Color.white, 0.95f);
            Draw.rect(lr, s.sx, my, lotusSize * 0.5f, lotusSize * 0.5f, s.rot);
        }else{
            Draw.color(warnColor, 0.95f);
            Fill.circle(s.sx, my, 4f);
        }
        Draw.color();
    }

    /** 阶段 1：预警 —— 圆环从 0 扩到全径，后半段红色填充，中心百分比倒计时 */
    protected void drawWarn(Strike s){
        float p = Mathf.clamp(s.t / (warnTime * 60f));
        float rad = strikeRadiusOf(s);
        float ringR = rad * p;
        float fillP = Mathf.clamp((p - 0.55f) / 0.45f, 0f, 1f);
        float pulse = 1f + 0.08f * Mathf.sin(s.t * 0.5f);

        Draw.z(Layer.effect);
        // 红色脉冲填充
        Draw.color(warnFill, (0.12f + 0.35f * p) * pulse);
        Fill.circle(s.tx, s.ty, rad * fillP * pulse);
        // 金色预警环
        Draw.color(warnColor, 0.95f);
        Lines.stroke(2.5f);
        Lines.circle(s.tx, s.ty, ringR);
        // 环头亮点
        Draw.color(Color.white, 0.9f);
        Fill.circle(s.tx + ringR, s.ty, 3f);
        // 中心百分比倒计时（与填充同步）
        Font font = Fonts.outline != null ? Fonts.outline : Fonts.def;
        if(font != null){
            Color c = warnColor.cpy().lerp(warnFill, fillP);
            font.setColor(c.r, c.g, c.b, 0.95f);
            font.draw((int)(fillP * 100f) + "%", s.tx, s.ty + font.getLineHeight() / 2f, Align.center);
        }
        Draw.color();
    }

    /** 阶段 2：填充完毕 —— 边缘红光闪烁 + 落点阴影放大 + Lotus 坠落 */
    protected void drawFall(Strike s){
        float p = Mathf.clamp(s.t / (fallTime * 60f));
        float rad = strikeRadiusOf(s);
        Draw.z(Layer.effect);
        // 边缘红光闪烁（填充完成后提示即将落地）
        float blink = 0.5f + 0.5f * Mathf.sin(s.t * 1.2f);
        Draw.color(warnFill, 0.35f + 0.45f * blink);
        Lines.stroke(3f + 2f * blink);
        Lines.circle(s.tx, s.ty, rad);
        // 残留的填充底色
        Draw.color(warnFill, 0.25f);
        Fill.circle(s.tx, s.ty, rad);
        // 落点阴影：逐渐放大的暗圈
        Draw.color(Color.black, 0.35f * p);
        Fill.circle(s.tx, s.ty, rad * 0.35f * p);
        // Lotus 从天而降
        float y = s.ty + (1f - p) * dropHeight;
        float scale = 0.6f + 0.4f * p;
        Draw.z(Layer.effect + 1f);
        TextureRegion lr = lotus();
        if(lr != null && lr.found()){
            Draw.color(Color.white, 1f);
            Draw.rect(lr, s.tx, y, lotusSize * scale, lotusSize * scale, s.rot);
        }
        Draw.color();
    }

    /** 阶段 3：落地余波 —— Lotus 残留淡出 + 泛红闪光 + 可选残留区 */
    protected void drawAfter(Strike s){
        float after = 20f;
        float rad = strikeRadiusOf(s);
        float fade = Mathf.clamp(1f - s.t / after);
        Draw.z(Layer.effect);
        // 泛红闪光：快速扩散的红色圆环 + 填充
        if(fade > 0f){
            float fp = 1f - fade;
            Draw.color(warnFill, 0.5f * fade);
            Fill.circle(s.tx, s.ty, rad * (0.4f + 0.6f * fp));
            Draw.color(Color.white, 0.7f * fade);
            Lines.stroke(4f * fade);
            Lines.circle(s.tx, s.ty, rad * (0.5f + 0.8f * fp));
        }
        // Lotus 落地残留淡出
        TextureRegion lr = lotus();
        if(lr != null && lr.found() && fade > 0f){
            Draw.z(Layer.effect + 1f);
            Draw.color(Color.white, fade);
            Draw.rect(lr, s.tx, s.ty, lotusSize, lotusSize, s.rot);
        }
        // 残留伤害区：脉冲半透明圈
        if(leaveLinger){
            float remain = Mathf.clamp(1f - s.t / (lingerTime * 60f));
            float pulse = 0.5f + 0.5f * Mathf.sin(s.t * 0.3f);
            Draw.z(Layer.effect);
            Draw.color(warnFill, 0.10f * remain);
            Fill.circle(s.tx, s.ty, rad);
            Draw.color(warnFill, (0.25f + 0.2f * pulse) * remain);
            Lines.stroke(1.5f);
            Lines.circle(s.tx, s.ty, rad * (0.9f + 0.1f * pulse));
        }
        Draw.color();
    }

    /** 懒加载 Lotus 贴图（与 TianFa 共用同一图集资源名） */
    protected static TextureRegion lotus(){
        if(lotusRegion == null || !lotusRegion.found()){
            lotusRegion = Core.atlas.find("nu-Lotus");
        }
        return lotusRegion;
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
}
