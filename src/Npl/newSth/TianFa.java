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
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Strings;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Sounds;
import mindustry.gen.Posc;
import mindustry.gen.Tex;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import Npl.content.ModStats;
import Npl.content.NuItems;

/**a
 * TianFa（天罚）· 3×3 重型天降打击炮台
 *
 * 核心机制 —— 弹药存放制：
 *   每 stockInterval 帧消耗 1 枚弹药物品，存放 1 发弹药，上限 maxStock（默认 12）。
 *   射程内出现敌人时立即触发齐射：现存多少发就发射多少枚导弹（连续波次，每 waveDelay 帧一枚），
 *   发射后立即重新开始积累。
 *
 * 打击流程（每枚导弹独立状态机，由炮台建筑自身管理，不走子弹系统）：
 *   阶段 0 rise  ：导弹从炮台垂直升空（发射台动画：发射特效 + 震屏 + 尾焰）
 *   阶段 1 warn  ：锁定落点，金色圆环从 0 扩到 strikeRadius，后半段红色脉冲填充，
 *                  中心显示百分比倒计时（同 RingAuraAbility 风格）
 *   阶段 2 fall  ：填充完毕，边缘红光闪烁 + 落点阴影逐渐放大，Lotus 贴图从 dropHeight 高空坠下
 *   阶段 3 after ：落地结算 —— 平台式衰减范围伤害（内 falloffInner 半径满伤，外缘线性衰减到 0），
 *                  Lotus 落地残留淡出 + 泛红闪光；可选开启残留伤害区
 *
 * 可选附效（全部默认关闭，实例化时置 true 开启）：
 *   applyBurn      ：重燃 —— 范围内敌人点燃 25伤/秒 × 1.5秒（burnTime 可调）
 *   applyKnockback ：击退 —— 冲击波将范围内敌人推离落点
 *   applySlow      ：减速 —— 范围内敌人短暂减速
 *   leaveLinger    ：残留区 —— 落点留下 lingerTime 秒伤害区，对单位+建筑持续生效
 *
 * 多目标自动分配：齐射时收集射程内所有敌人（单位+建筑），导弹轮流分配；
 * 敌人全灭后剩余导弹落在其最后已知位置附近。
 */
public class TianFa extends ItemTurret{

    // ============= 弹药存放制 =============
    /** 弹药存放上限 */
    public int maxStock = 12;
    /** 每存放一发弹药所需的帧数 */
    public float stockInterval = 60f;
    /** 齐射时相邻两枚导弹的发射间隔（帧） */
    public float waveDelay = 6f;

    // ============= 打击参数 =============
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
    /** 平台式衰减内圈比例：内 60% 半径满伤，外 40% 线性衰减 */
    public float falloffInner = 0.6f;
    /** Lotus 贴图大小 */
    public float lotusSize = 30f;

    // ============= 预警视觉 =============
    public Color warnColor = Color.valueOf("ffd27a");
    public Color warnFill = Color.valueOf("ff3b2f");

    // ============= 可选附效（默认全部关闭） =============
    /** 重燃：点燃敌人 25伤/秒 × burnTime 秒 */
    public boolean applyBurn = false;
    public float burnTime = 1.5f;
    /** 击退：冲击波推离 */
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

    public TianFa(String name){
        super(name);
        // 占位弹药映射：ItemTurret 需要至少一条 ammoTypes 才能接受物品输入；
        // 原版射击流程仍被 hasAmmo()=false 禁用，实际消耗由弹药存放制接管
        ammoTypes.put(NuItems.sulFurFrag, new BulletType(0f, 0f){{ lifetime = 1f; }});
        targetAir = true;
        targetGround = true;
        shootSound = Sounds.none;
    }

    @Override
    public void setBars(){
        super.setBars();
        // 原版"ammo"条基于已被禁用的原版弹药系统，恒为0；替换为自定义存量条
        addBar("ammo", (TianFaBuild build) -> new Bar(
            () -> "弹药 " + build.stock + "/" + maxStock,
            () -> build.stock >= maxStock ? Pal.accent : build.stock > 0 ? Pal.ammo : Pal.gray,
            () -> (float)build.stock / maxStock
        ));
    }

    @Override
    public void load(){
        super.load();
        lotusRegion = Core.atlas.find("nu-Lotus");
    }

    @Override
    public void setStats(){
        super.setStats();
        // 详情面板的具体构建逻辑统一放在 ModStats 中
        ModStats.buildTianFaStats(this, stats);
    }

    /** 单枚导弹的完整状态机 */
    public static class Strike{
        /** 发射原点（炮台位置） */
        final float sx, sy;
        /** 锁定的目标（升空期间持续跟踪，升空结束时锁定其位置） */
        Posc target;
        /** 目标最后已知位置 */
        float tx, ty;
        /** 0=升空 1=预警 2=坠落 3=落地余波（淡出+闪光+可选残留区） */
        int phase = 0;
        float t = 0f;
        float rot = 0f;
        /** 残留区已结算的计时 */
        float lingerAcc = 0f;
        /** 本发导弹落地后留下的火焰残留区时长（秒），0=无（来自 TianFaAmmo.fireZone） */
        float zoneTime = 0f;
        /** 本发导弹火焰残留区每秒伤害（来自 TianFaAmmo.fireZoneDps） */
        float zoneDps = 0f;
        /** 本发导弹所用的弹道类型（来自消耗掉的弹药物品），决定伤害/半径/状态/特效 */
        BulletType bullet;

        Strike(float sx, float sy, Posc target, BulletType bullet){
            this.sx = sx;
            this.sy = sy;
            this.target = target;
            this.tx = target != null ? target.getX() : sx;
            this.ty = target != null ? target.getY() : sy;
            this.bullet = bullet;
        }
    }

    /**
     * 天罚专属弹道类型：在原版 BulletType 之上，补充"落地专属效果"字段。
     * 天罚走的是弹药存放制，落地结算完全由 landStrike() 读取本类字段完成，
     * 所以这些字段只对天罚有意义，不会影响原版子弹发射流程。
     */
    public static class TianFaAmmo extends BulletType{
        /** 落地是否产生火焰：点燃打击半径内的敌方单位 */
        public boolean landFire = false;
        /** 落地火焰的燃烧时长（秒）；&lt;=0 时使用炮台默认 burnTime */
        public float fireDuration = 0f;
        /** 落地是否留下火焰残留区（持续灼烧单位与建筑） */
        public boolean fireZone = false;
        /** 火焰残留区时长（秒） */
        public float fireZoneTime = 6f;
        /** 火焰残留区每秒伤害（对单位与建筑） */
        public float fireZoneDps = 22f;
        /** 落地冲击：额外击退力度（0=不额外击退） */
        public float landKnockback = 0f;

        public TianFaAmmo(float damage, float splashRadius){
            super(0f, 0f);
            this.lifetime = 1f;
            this.damage = damage;
            this.splashDamage = damage;
            this.splashDamageRadius = splashRadius;
        }
    }

    /** 生成某弹道"落地专属效果"的文字描述（详情面板与信息面板共用） */
    public static String ammoLandFx(BulletType bt){
        if(!(bt instanceof TianFaAmmo ta)) return "";
        StringBuilder sb = new StringBuilder();
        if(ta.landFire) sb.append("火焰");
        if(ta.fireZone){
            if(sb.length() > 0) sb.append(" + ");
            sb.append("火焰残留区(").append((int)ta.fireZoneTime).append("s, ")
              .append((int)ta.fireZoneDps).append("/s)");
        }
        if(ta.landKnockback > 0f){
            if(sb.length() > 0) sb.append(" + ");
            sb.append("冲击击退");
        }
        return sb.toString();
    }

    public class TianFaBuild extends ItemTurretBuild{
        /** 当前弹药存量 */
        int stock = 0;
        /** 与 stock 一一对应的弹道类型队列（先进先出），决定每发导弹的实际效果；不参与存档 */
        transient Seq<BulletType> stockAmmo = new Seq<>();
        /** 存放计时 */
        float stockTimer = 0f;
        /** 齐射中尚未发射的导弹数 */
        int pending = 0;
        /** 齐射波次计时 */
        float waveTimer = 0f;
        /** 齐射目标轮询索引 */
        int assignIndex = 0;
        /** 进行中的打击（不参与存档序列化） */
        transient Seq<Strike> strikes = new Seq<>();
        /** 齐射可用目标池 */
        transient Seq<Posc> volleyTargets = new Seq<>();

        @Override
        public boolean hasAmmo(){
            // 禁用 ItemTurret 原版射击流程，全部由自定义逻辑接管
            return false;
        }

        @Override
        public void findTarget(){
            // 原版 findTarget() 内部会调用 findEnemy()，而 findEnemy() 必须读取 peekAmmo() 返回的弹道参数；
            // 天罚已禁用原版弹药系统（hasAmmo()=false → peekAmmo() 恒为 null），直接调用原版会抛：
            // NullPointerException: Cannot read field "unitSort" because "ammo" is null
            // 因此这里改用不依赖弹药的自主索敌：纯防空走 bestEnemy，其余走 bestTarget（地面单位 + 敌方建筑）。
            float r = range();
            if(targetAir && !targetGround){
                target = Units.bestEnemy(team, x, y, r,
                    e -> !e.dead() && !e.isGrounded(),
                    unitSort);
            }else{
                target = Units.bestTarget(team, x, y, r,
                    e -> !e.dead() && (e.isGrounded() || targetAir) && (!e.isGrounded() || targetGround),
                    b -> targetGround && targetBlocks,
                    unitSort);
            }
        }

        @Override
        public boolean shouldConsume(){
            // 原版依赖 hasAmmo()，已被我们禁用；改为"有料可存或未存满就耗电"
            return items.any() || stock < maxStock;
        }

        /** 供电判定：直接看电网真实供电率，绕开 efficiency 的历史谜团 */
        boolean hasPowerSupply(){
            return cheating() || power != null && power.status > 0.001f;
        }

        @Override
        public mindustry.world.meta.BlockStatus status(){
            if(!enabled) return mindustry.world.meta.BlockStatus.noInput;
            if(hasPowerSupply()) return mindustry.world.meta.BlockStatus.active;
            return mindustry.world.meta.BlockStatus.noInput;
        }

        // ========= 物品输入：绕开原版"物品→弹药"转换，直接进物品栏 =========
        @Override
        public boolean acceptItem(mindustry.gen.Building source, mindustry.type.Item item){
            return items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public void handleItem(mindustry.gen.Building source, mindustry.type.Item item){
            items.add(item, 1);
        }

        // ========= 信息面板 =========
        @Override
        public void display(Table table){
            table.table(Tex.buttonTrans, t -> {
                t.left().defaults().left().growX();

                t.add("[accent]天罚 · 天降打击系统[]").row();
                t.image().height(3f).color(Pal.accent).growX().row();

                // 存量
                t.add("[lightgray]弹药存量:[] " +
                    (stock >= maxStock ? "[accent]" : stock > 0 ? "[orange]" : "[gray]") +
                    stock + " / " + maxStock + "[]").row();

                // 状态
                String status;
                String statusColor;
                if(!enabled){
                    status = "已被禁用";
                    statusColor = "[red]";
                }else if(items == null || items.total() <= 0){
                    status = "缺少弹药物品（需输入弹药）";
                    statusColor = "[red]";
                }else if(!hasPowerSupply()){
                    status = "缺电 · 停止运作";
                    statusColor = "[red]";
                }else if(pending > 0){
                    status = "齐射中 · 剩余 " + pending + " 枚";
                    statusColor = "[accent]";
                }else if(strikes.size > 0){
                    status = "打击进行中 · " + strikes.size + " 枚";
                    statusColor = "[accent]";
                }else if(stock >= maxStock){
                    status = "弹药已满 · 等待敌人";
                    statusColor = "[accent]";
                }else{
                    status = "存放中 · 下一发 " + (int)Mathf.ceil((stockInterval - stockTimer) / 60f) + "s";
                    statusColor = "[orange]";
                }
                t.add("[lightgray]状态:[] " + statusColor + status + "[]").row();

                // 供电率（power.status 为电网真实供电率；沙盒无限资源下效率恒为满）
                float ps = power != null ? power.status : 0f;
                String psColor = ps >= 0.999f ? "[accent]" : ps > 0f ? "[orange]" : "[red]";
                t.add("[lightgray]供电率:[] " + psColor + (int)(ps * 100f) + "%[]").row();

                // 物品存量
                t.add("[lightgray]弹药物品:[] " + (items == null || items.total() <= 0
                    ? "[red]无[]" : "[orange]" + items.first().localizedName + " ×" + items.get(items.first()) + "[]")).row();

                // 各弹药物品的打击参数（按弹道类型差异化）
                for(var e : ammoTypes){
                    BulletType bt = e.value;
                    if(bt == null) continue;
                    float d = bt.damage > 0f ? bt.damage : bt.splashDamage;
                    if(d <= 0f) d = strikeDamage;
                    float rr = bt.splashDamageRadius > 0f ? bt.splashDamageRadius : strikeRadius;
                    t.add("[lightgray]· " + e.key.localizedName + ":[] " + (int)d +
                        " [gray]/ " + (int)rr + " " + StatUnit.blocks.localized() + "[]").row();
                }

                // 已开启的附效
                StringBuilder fx = new StringBuilder();
                if(applyBurn) fx.append("点燃 ");
                if(applyKnockback) fx.append("击退 ");
                if(applySlow) fx.append("减速 ");
                if(leaveLinger) fx.append("残留区(" + (int)lingerTime + "s)");
                t.add("[lightgray]附效:[] " + (fx.length() == 0 ? "[gray]无[]" : "[orange]" + fx + "[]")).row();
            }).growX().pad(6f).row();
            super.display(table);
        }

        @Override
        public void updateTile(){
            super.updateTile();

            // ========= 打击状态机推进 =========
            // 刻意放在断电判定之前：已经发射出去的导弹不受断电影响，照常走完
            // 升空 → 预警 → 坠落 → 落地结算，动画不会凝固在半空中。
            for(int i = strikes.size - 1; i >= 0; i--){
                Strike s = strikes.get(i);
                updateStrike(s);
                if(s.phase > 3){
                    strikes.remove(i);
                }
            }

            if(!hasPowerSupply()) return;

            if(target == null || !validateTarget()){
                findTarget();
            }

            // ========= 炮管朝向 =========
            // 原版 TurretBuild 的转向逻辑整体被包在 if(hasAmmo()) 内，天罚 hasAmmo() 恒为 false，
            // 导致炮管永不旋转；这里照抄原版规则自行处理：刷新目标点 → 修正 NaN → 缓慢转向目标。
            if(target != null && validateTarget()){
                targetPosition(target);
                if(Float.isNaN(rotation)) rotation = 0f;
                if(shouldTurn()) turnToTarget(Angles.angle(x, y, targetPos.x, targetPos.y));
            }

            // ========= 弹药存放 =========
            stockTimer += Time.delta;
            if(stockTimer >= stockInterval){
                stockTimer = 0f;
                if(stock < maxStock && items != null && items.total() > 0 && items.first() != null){
                    mindustry.type.Item it = items.first();
                    // 记录这发弹药对应的弹道类型，供发射时结算差异化效果
                    stockAmmo.add(ammoTypes.get(it));
                    items.remove(it, 1);
                    stock++;
                }
            }

            // ========= 触发齐射 =========
            if(pending <= 0 && stock > 0 && target != null && validateTarget()){
                pending = stock;
                stock = 0;
                stockTimer = 0f;
                assignIndex = 0;
                collectTargets();
            }

            // ========= 连续波次发射 =========
            if(pending > 0){
                waveTimer += Time.delta;
                if(waveTimer >= waveDelay){
                    waveTimer = 0f;
                    launchStrike();
                    pending--;
                }
            }

        }

        /** 收集射程内所有敌方目标（单位优先，其次建筑），供多目标轮询分配 */
        private void collectTargets(){
            volleyTargets.clear();
            Units.nearbyEnemies(team, x, y, range(), u -> {
                if(!u.dead() && u.isAdded()) volleyTargets.add(u);
            });
            if(volleyTargets.isEmpty()){
                // 没有敌方单位时找敌方建筑
                try{
                    Groups.build.intersect(x - range(), y - range(), range() * 2f, range() * 2f, b -> {
                        if(b != null && b.team != team && b.team != mindustry.game.Team.derelict && !b.dead){
                            volleyTargets.add(b);
                        }
                    });
                }catch(NullPointerException ignored){}
            }
        }

        /** 发射一枚导弹：轮流分配目标 */
        private void launchStrike(){
            Posc tgt = null;
            if(volleyTargets.size > 0){
                // 轮询前先剔除已死亡目标
                while(!volleyTargets.isEmpty()){
                    Posc cand = volleyTargets.get(assignIndex % volleyTargets.size);
                    boolean valid = (cand instanceof Unit u) ? !u.dead() : (cand instanceof Building b && !b.dead);
                    if(valid){ tgt = cand; break; }
                    volleyTargets.remove(assignIndex % volleyTargets.size);
                }
                assignIndex++;
            }
            if(tgt == null && target != null && validateTarget()){
                tgt = target;
            }
            Strike s = new Strike(x, y, tgt, popAmmo());
            strikes.add(s);
            // 发射台动画：发射特效 + 震屏
            Fx.launch.at(x, y);
            Fx.shockwave.at(x, y);
            Effect.shake(2f, 4f, this);
        }

        /** 取出一发已存弹药的弹道类型（先进先出）；队列为空时退回当前物品或默认弹道 */
        private BulletType popAmmo(){
            if(stockAmmo.size > 0) return stockAmmo.remove(0);
            if(items != null && items.total() > 0 && items.first() != null) return ammoTypes.get(items.first());
            return ammoTypes.get(NuItems.sulFurFrag);
        }

        // ========= 阶段推进 =========
        private void updateStrike(Strike s){
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
                    float after = 20f; // 淡出+闪光时长
                    float totalLinger = Math.max(leaveLinger ? lingerTime : 0f, s.zoneTime);
                    if(totalLinger > 0f){
                        s.lingerAcc += Time.delta;
                        if(s.lingerAcc >= lingerTick){
                            s.lingerAcc -= lingerTick;
                            lingerDamage(s);
                        }
                        if(s.t >= totalLinger * 60f){
                            s.phase = 4;
                        }
                    }else if(s.t >= after){
                        s.phase = 4;
                    }
                    break;
                }
            }
        }

        // ========= 落地结算：按弹道类型差异化 + 平台式衰减 + 可选附效 =========
        /** 本发导弹的有效伤害半径：优先取弹道的 splashDamageRadius，否则退回炮台默认 strikeRadius */
        private float strikeRadiusOf(Strike s){
            if(s.bullet != null && s.bullet.splashDamageRadius > 0f) return s.bullet.splashDamageRadius;
            return strikeRadius;
        }

        /** 本发导弹的有效中心伤害：优先取弹道的 damage（其次 splashDamage），否则退回炮台默认 strikeDamage */
        private float strikeDamageOf(Strike s){
            if(s.bullet != null){
                float d = s.bullet.damage > 0f ? s.bullet.damage : s.bullet.splashDamage;
                if(d > 0f) return d;
            }
            return strikeDamage;
        }

        /** 弹道自带的附效：状态 + 击退（与炮台全局附效叠加） */
        private void applyBulletEffects(Strike s, Unit u, float f){
            if(s.bullet == null) return;
            if(s.bullet.status != null && s.bullet.statusDuration > 0f){
                u.apply(s.bullet.status, s.bullet.statusDuration);
            }
            if(s.bullet.knockback > 0f && f > 0f){
                float ang = Angles.angle(s.tx, s.ty, u.x, u.y);
                u.vel.add(Mathf.cos(ang) * s.bullet.knockback * f * 10f, Mathf.sin(ang) * s.bullet.knockback * f * 10f);
            }
        }

        private void landStrike(Strike s){
            float r = strikeRadiusOf(s);
            float dmg = strikeDamageOf(s);
            float inner = r * falloffInner;

            // 对敌方单位
            Units.nearbyEnemies(team, s.tx, s.ty, r, u -> {
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
                    if(b == null || b.dead || b.team == team || b.team == mindustry.game.Team.derelict) return;
                    float d = Mathf.dst(b.x, b.y, s.tx, s.ty);
                    if(d > r) return;
                    float f = d <= inner ? 1f : Mathf.clamp(1f - (d - inner) / (r - inner));
                    b.damage(dmg * f);
                });
            }catch(NullPointerException ignored){}

            // 天罚专属落地效果（由 TianFaAmmo 字段驱动）
            if(s.bullet instanceof TianFaAmmo ta){
                // 落地火焰：点燃半径内敌人
                if(ta.landFire){
                    float dur = ta.fireDuration > 0f ? ta.fireDuration : burnTime;
                    Units.nearbyEnemies(team, s.tx, s.ty, r, u -> {
                        if(u.dead() || !u.isAdded()) return;
                        if(Mathf.dst(u.x, u.y, s.tx, s.ty) <= r) u.apply(StatusEffects.burning, dur * 60f);
                    });
                }
                // 冲击击退：额外把半径内敌人推离落点
                if(ta.landKnockback > 0f){
                    Units.nearbyEnemies(team, s.tx, s.ty, r, u -> {
                        if(u.dead() || !u.isAdded()) return;
                        if(Mathf.dst(u.x, u.y, s.tx, s.ty) > r) return;
                        float ang = Angles.angle(s.tx, s.ty, u.x, u.y);
                        u.vel.add(Mathf.cos(ang) * ta.landKnockback * 10f, Mathf.sin(ang) * ta.landKnockback * 10f);
                    });
                }
                // 火焰残留区：交给 phase 3 持续结算
                if(ta.fireZone){
                    s.zoneTime = Math.max(s.zoneTime, ta.fireZoneTime);
                    s.zoneDps = Math.max(s.zoneDps, ta.fireZoneDps);
                }
            }

            // 弹道自带的落地特效
            if(s.bullet != null && s.bullet.hitEffect != null){
                s.bullet.hitEffect.at(s.tx, s.ty);
            }
            Fx.explosion.at(s.tx, s.ty);
            Fx.shockwave.at(s.tx, s.ty);
            Effect.shake(4f, 8f, new Vec2(s.tx, s.ty));
        }

        /** 残留区伤害：对单位+建筑 */
        private void lingerDamage(Strike s){
            float r = strikeRadiusOf(s);
            float dps = s.zoneDps > 0f ? s.zoneDps : lingerDps;
            float amount = dps * lingerTick / 60f;
            Units.nearbyEnemies(team, s.tx, s.ty, r, u -> {
                if(!u.dead()) u.damage(amount);
            });
            try{
                Groups.build.intersect(s.tx - r, s.ty - r, r * 2f, r * 2f, b -> {
                    if(b != null && !b.dead && b.team != team && b.team != mindustry.game.Team.derelict){
                        if(Mathf.dst(b.x, b.y, s.tx, s.ty) <= r) b.damage(amount);
                    }
                });
            }catch(NullPointerException ignored){}
        }

        // ========= 绘制 =========
        @Override
        public void draw(){
            super.draw();
            drawStrikes();
            drawStockPips();
        }

        private void drawStrikes(){
            for(Strike s : strikes){
                switch(s.phase){
                    case 0: drawRise(s); break;
                    case 1: drawWarn(s); break;
                    case 2: drawFall(s); break;
                    case 3: drawAfter(s); break;
                }
            }
            Draw.reset();
        }

        /** 阶段 0：导弹垂直升空 + 尾焰 */
        private void drawRise(Strike s){
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
            if(lotusRegion != null && lotusRegion.found()){
                Draw.color(Color.white, 0.95f);
                Draw.rect(lotusRegion, s.sx, my, lotusSize * 0.5f, lotusSize * 0.5f, s.rot);
            }else{
                Draw.color(warnColor, 0.95f);
                Fill.circle(s.sx, my, 4f);
            }
            Draw.reset();
        }

        /** 阶段 1：预警 —— 圆环从 0 扩到全径，后半段红色填充，中心百分比倒计时 */
        private void drawWarn(Strike s){
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
            Draw.reset();
        }

        /** 阶段 2：填充完毕 —— 边缘红光闪烁 + 落点阴影放大 + Lotus 坠落 */
        private void drawFall(Strike s){
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
            if(lotusRegion != null && lotusRegion.found()){
                Draw.color(Color.white, 1f);
                Draw.rect(lotusRegion, s.tx, y, lotusSize * scale, lotusSize * scale, s.rot);
            }
            Draw.reset();
        }

        /** 阶段 3：落地余波 —— Lotus 残留淡出 + 泛红闪光 + 可选残留区 */
        private void drawAfter(Strike s){
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
            if(lotusRegion != null && lotusRegion.found() && fade > 0f){
                Draw.z(Layer.effect + 1f);
                Draw.color(Color.white, fade);
                Draw.rect(lotusRegion, s.tx, s.ty, lotusSize, lotusSize, s.rot);
            }
            // 残留伤害区：脉冲半透明圈
            float lingerTot = Math.max(leaveLinger ? lingerTime : 0f, s.zoneTime);
            if(lingerTot > 0f){
                float remain = Mathf.clamp(1f - s.t / (lingerTot * 60f));
                float pulse = 0.5f + 0.5f * Mathf.sin(s.t * 0.3f);
                Draw.z(Layer.effect);
                Draw.color(warnFill, 0.10f * remain);
                Fill.circle(s.tx, s.ty, rad);
                Draw.color(warnFill, (0.25f + 0.2f * pulse) * remain);
                Lines.stroke(1.5f);
                Lines.circle(s.tx, s.ty, rad * (0.9f + 0.1f * pulse));
            }
            Draw.reset();
        }

        /** 炮台上方绘制弹药存量指示条：满格亮黄、空格暗灰，缺电时整体变灰 */
        private void drawStockPips(){
            Draw.z(Layer.block + 1f);
            float spacing = 4.5f;
            float px = x - (maxStock - 1) * spacing / 2f;
            float py = y + size * 4f + 5f;
            float pulse = stock >= maxStock ? 0.75f + 0.25f * Mathf.absin(Time.time, 6f, 1f) : 1f;
            for(int i = 0; i < maxStock; i++){
                if(i < stock){
                    Draw.color(hasPowerSupply() ? warnColor : Color.gray, 0.9f * pulse);
                    Fill.circle(px + i * spacing, py, 2.2f);
                }else{
                    Draw.color(Color.gray, 0.35f);
                    Fill.circle(px + i * spacing, py, 1.4f);
                }
            }
            Draw.reset();
        }
    }
}
