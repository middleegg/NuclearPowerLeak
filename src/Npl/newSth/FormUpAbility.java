package Npl.newSth;

import arc.graphics.Color;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.entities.units.UnitController;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import Npl.newSth.AI.LingZhenGuardAI;

/**
 * 列阵 Ability。
 * <p>
 * 母体（长官）一旦开始攻击，立刻以自身为中心铺开一个 n×n 的方阵（n = {@link #SquareSize}，
 * 若为偶数则向上取最近的奇数，以保证存在正中央格）：正中央是母体本人，
 * 其余 n²-1 格全部由陆地抵御单位（士兵）占位，同时喊一句「列阵！！」。
 * <p>
 * 阵位按「离中心的远近」排序（先内圈后外圈），因此：
 * <ul>
 *   <li>士兵按阵位落位，队形始终保持方阵；</li>
 *   <li>当有士兵阵亡时，其余士兵整体朝主单位方向靠拢补位（外圈的顶上内圈的空缺）。</li>
 * </ul>
 * <p>
 * 一次召唤内不再补充成员（死一个就少一个）；本批士兵全部阵亡后进入 {@link #window} 空窗期，
 * 空窗期结束且长官仍在攻击时才会重新结阵。
 * <p>
 * 士兵通过 {@link LingZhenGuardAI} 跟随阵位、贴地行走、遇墙助推，并统一瞄准长官正在攻击的目标。
 * 若出生点被墙体/建筑占据，则该名士兵直接以助推状态出生，翻越障碍后再落地。
 */
public class FormUpAbility extends Ability{
    /** 抵御单位类型（未配置时回退到 FederalUnitTypes.lingZhenGuard）。 */
    public UnitType guardType;
    /** 方阵边长（阵型参数 n）。方阵为 n×n，正中央是母体本人，士兵数量 = n²-1。
     *  若 n 为偶数则向上取最近的奇数：偶数没有真正的正中央格，无法构成对称方阵。 */
    public int SquareSize = 5;
    /** 相邻阵位的间距（局内单位）。 */
    public float spacing = 20f;

    /** 本批士兵全灭后、下一次结阵的空窗期（帧）。 */
    public float window = 60f * 6f;
    /** 抵御单位的存活时间（帧），到点自行死亡。 */
    public float unitLifetime = 60f * 30f;

    /** 长官停火判定为「脱离控制」所需的时长（帧），注入给士兵。 */
    public float loseDelay = 60f * 3f;
    /** 脱离控制后的缓冲期（帧），缓冲期内不掉血。 */
    public float bufferTime = 60f * 5f;
    /** 缓冲期结束后每秒扣除的最大生命值比例（如 0.25 = 25%/秒），注入给士兵。 */
    public float decayPerSecond = 0.25f;

    protected final Seq<Unit> guards = new Seq<>();
    /** 是否处于「一批召唤」中：期间不补充成员。 */
    protected boolean batchActive = false;
    protected float timer = 0f;

    /** 阵位缓存（按离中心远近排序的偏移）。 */
    protected Seq<int[]> slotCache;

    /** 母体用于判断「是否在攻击」的龙爪武器（按单位类型缓存）。 */
    protected DragonClawWeapon claw;
    protected UnitType clawType;

    /** 「列阵！！」喊话特效（静态复用，与 NuFx.textSkillGold 同一套机制）。 */
    protected static final Effect shout = new TextPopupEffect(90f, 160f, e -> {}){{
        text          = "列阵！！";
        textColor     = new Color(0xEDF8FEFF);
        textSize      = 2.0f;
        outlineColor  = new Color(0x3E2723cc);
        outlineOffset = 2.6f;
        riseDistance  = 60f;
        popupEnd      = 0.3f;
        popupFrom     = 0f;
        popupPeak     = 1.55f;
        popupTo       = 1.05f;
        useShake      = true;
        shakeAmplitude = 1.4f;
        useShadow     = true;
        fadeStart     = 0.55f;
    }};

    public FormUpAbility(){
    }

    @Override
    public void update(Unit unit){
        super.update(unit);
        if(unit == null || unit.dead || !unit.isAdded()) return;

        if(timer > 0f) timer -= Time.delta;

        int before = guards.size;
        guards.removeAll(g -> g == null || g.dead || !g.isAdded());

        // 有士兵阵亡 → 其余士兵朝主单位方向靠拢补位
        if(guards.size != before) repack();

        // 本批士兵全灭 → 召结束，进入空窗期
        if(batchActive && guards.size == 0){
            batchActive = false;
            timer = window;
        }

        // 本次召唤内不补充成员
        if(batchActive) return;
        // 空窗期未过
        if(timer > 0f) return;
        if(resolveGuard() == null) return;

        // 只要主单位「在开火」就结阵：玩家手控点射 / AI 射击 / 龙爪正在挥击都算
        if(!leaderAttacking(unit)) return;

        formUp(unit);
    }

    /**
     * 主单位是否正在攻击。
     * <p>
     * 统一交给 {@link LingZhenGuardAI#inCombat(Unit)} 判定：
     * <ul>
     *   <li>玩家控制：按住攻击键未松开即算（对着空地/墙体开火也算）；</li>
     *   <li>AI 控制：攻击范围内存在可攻击目标即算。</li>
     * </ul>
     * 另外，龙爪自身有进行中的挥击时也算攻击（保持结阵节奏）。
     */
    protected boolean leaderAttacking(Unit unit){
        if(LingZhenGuardAI.inCombat(unit)) return true;
        DragonClawWeapon w = resolveClaw(unit);
        return w != null && w.isAttacking(unit);
    }

    protected UnitType resolveGuard(){
        if(guardType == null){
            guardType = Npl.content.FederalUnitTypes.lingZhenGuard;
        }
        return guardType;
    }

    /** 取出母体身上的龙爪武器（用于判断是否开火 + 读取共享目标）。 */
    protected DragonClawWeapon resolveClaw(Unit unit){
        if(unit.type == null || unit.type.weapons == null) return null;
        if(claw != null && clawType == unit.type) return claw;

        claw = null;
        clawType = null;
        for(Weapon weapon : unit.type.weapons){
            if(weapon instanceof DragonClawWeapon dc){
                claw = dc;
                clawType = unit.type;
                break;
            }
        }
        return claw;
    }

    /** 出生点是否被墙体/建筑占据。 */
    protected boolean spawnBlocked(float x, float y){
        if(Vars.world == null) return false;
        var tile = Vars.world.tileWorld(x, y);
        return tile == null || tile.solid();
    }

    /** 实际阵型边长：偶数向上取最近的奇数，保证存在真正的正中央格（母体所在格）。 */
    protected int gridSize(){
        return (SquareSize % 2 == 0) ? SquareSize + 1 : SquareSize;
    }

    /** 阵位列表：gridSize×gridSize 去掉正中央（母体所在格），按「离中心的远近」由内向外排序，共 gridSize²-1 个。 */
    protected Seq<int[]> slots(){
        if(slotCache == null){
            slotCache = new Seq<>();
            int side = gridSize();
            int cr = side / 2, cc = side / 2;
            int maxRing = Math.max(cr, cc);
            for(int ring = 1; ring <= maxRing; ring++){
                for(int r = 0; r < side; r++){
                    for(int c = 0; c < side; c++){
                        int dr = Math.abs(r - cr), dc = Math.abs(c - cc);
                        if(Math.max(dr, dc) != ring) continue;
                        slotCache.add(new int[]{c - cc, r - cr});
                    }
                }
            }
        }
        return slotCache;
    }

    /** 把士兵「朝主单位方向靠拢」：存活士兵按原阵位顺序整体挤进最靠内的若干阵位。 */
    protected void repack(){
        guards.sort((a, b) -> Integer.compare(slotOf(a), slotOf(b)));

        Seq<int[]> slots = slots();
        for(int i = 0; i < guards.size; i++){
            assignSlot(aiOf(guards.get(i)), Math.min(i, slots.size - 1));
        }
    }

    protected int slotOf(Unit u){
        UnitController c = u == null ? null : u.controller();
        return c instanceof LingZhenGuardAI ai ? ai.slot : Integer.MAX_VALUE;
    }

    protected LingZhenGuardAI aiOf(Unit u){
        UnitController c = u == null ? null : u.controller();
        return c instanceof LingZhenGuardAI ai ? ai : null;
    }

    /** 把某个士兵分配到指定阵位（更新其偏移与跟随目标点）。 */
    protected void assignSlot(LingZhenGuardAI ai, int slot){
        if(ai == null) return;
        int[] s = slots().get(slot);
        ai.slot = slot;
        ai.offX = s[0] * spacing;
        ai.offY = s[1] * spacing;
    }

    /** 以母体为中心铺开 SquareSize×SquareSize 方阵（士兵数量 = SquareSize²-1），并喊话。 */
    protected void formUp(Unit unit){
        UnitType type = resolveGuard();
        if(type == null) return;

        Seq<int[]> slots = slots();
        int n = slots.size;
        guards.clear();

        for(int i = 0; i < n; i++){
            int[] s = slots.get(i);
            float ox = s[0] * spacing;
            float oy = s[1] * spacing;
            float gx = unit.x + ox;
            float gy = unit.y + oy;

            Unit g = type.create(unit.team);
            g.set(gx, gy);
            g.rotation = unit.rotation;

            // 出生点不适合 → 直接以助推状态出生，翻越障碍后再落地
            if(type.canBoost && spawnBlocked(gx, gy)){
                g.elevation = 1f;
            }

            LingZhenGuardAI ai = new LingZhenGuardAI();
            ai.leader = unit;
            ai.slot = i;
            ai.offX = ox;
            ai.offY = oy;
            ai.life = unitLifetime;
            ai.loseDelay = loseDelay;
            ai.bufferTime = bufferTime;
            ai.decayPerSecond = decayPerSecond;
            g.controller(ai);

            g.add();
            Fx.spawn.at(gx, gy);
            guards.add(g);
        }

        shout.at(unit.x, unit.y);
        batchActive = true;
        timer = 0f;
    }
}
