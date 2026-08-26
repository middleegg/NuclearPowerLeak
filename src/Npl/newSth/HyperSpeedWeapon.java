package Npl.newSth;

import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

/**
 * HyperSpeedWeapon extends Weapon —— 单位专用的「越打同一目标越快 + 脱战按秒衰减」武器。
 * <p>
 * 直接把 `new Weapon(...)` 换成 `new HyperSpeedWeapon(...)` 即可；
 * 其它字段（x/y/reload/shootCone/bullet 等）的写法和 Weapon 父类完全一致。
 *
 * ============== 叠层规则（与 HyperSpeedTurret 完全一致） ==============
 *   每打同目标一发 → curReloadMul += perHitBonus(0.05)，最高 maxReloadMul(3.0)
 *   无目标经过 1 秒 → curReloadMul -= decayPerSecond(0.15)，直到底 1.0
 *   目标切换        → resetOnTargetSwitch(true) 时直接回 1.0
 *
 *   「v（每秒攻击次数）= 60 / reload × (bullet.reloadMultiplier) × unit.reloadMultiplier × curReloadMul」
 *
 * ============== 推进冷却的核心做法 ==============
 *   这一版不再猜 unit.reloadMultiplier / bullet.reloadMultiplier 的值，而是在 super.update 前后
 *   用 reloadBefore - mount.reload 做差得到"这一帧原版真实推进了多少冷却"，
 *   再额外多减 origInc × (curReloadMul - 1) 。
 *   这样不论 Weapon.update 内部是什么复杂冷却公式，只要原版在推，叠加就 100% 生效。
 *
 * ============== 使用范例（在 UnitType 里） ==============
 * <pre>{@code
 * myUnit = new UnitType("myUnit"){{
 *     weapons.add(new HyperSpeedWeapon("my-weapon"){{
 *         x = -4f; y = 0f;
 *         reload = 20f;                       // 基础：v = 60/20 = 3 发/秒
 *         shootCone = 20f;
 *         range = 180f;
 *         inaccuracy = 1f;
 *         mirror = true;
 *
 *         // —— 叠层参数（下面都是默认值，可以不写）——
 *         perHitBonus     = 0.05f;
 *         maxReloadMul    = 3.0f;
 *         decayPerSecond  = 0.15f;
 *
 *         bullet = new HyperSpeedBulletType(5f, 80f){{
 *             damageScale = 1.5f;   // 默认就是 1.5
 *             speedExp    = 1.1f;   // 默认就是 1.1
 *             hitSize = 8f;
 *             lifetime = 60f;
 *             pierce = true;
 *             pierceCap = 3;
 *             // — PointBulletType 拖尾参数（和 FederalUnitTypes 中其它 Point 弹保持一致）
 *             trailEffect   = NuFx.DespSmokeTail;
 *             trailSpacing  = 3f;
 *             trailInterval = 2f;
 *         }};
 *     }});
 * }};
 * }</pre>
 */
public class HyperSpeedWeapon extends Weapon {

    /* ============== 可配置：叠层规则（和 HyperSpeedTurret 对齐命名） ============== */
    public float perHitBonus = 0.05f;
    public float maxReloadMul = 3.0f;
    public float decayPerSecond = 0.15f;
    public boolean resetOnTargetSwitch = true;

    /* ============== 运行时 per-Unit + per-Mount 状态 ============== */
    public static class WState {
        public float curReloadMul = 1f;
        public Object lastTargetKey = null;
        public float decayAccum = 0f;
        public boolean wasReady = false;
    }

    protected final IntMap<WState[]> stateMap = new IntMap<>();
    protected float cleanCounter = 0f;

    /* ============== 构造 ============== */
    public HyperSpeedWeapon(String name){ super(name); }
    public HyperSpeedWeapon(){ super(); }

    /* ============== 主循环：覆写 Weapon.update(unit, mount) 插入叠层逻辑 ============== */

    @Override
    public void update(Unit unit, WeaponMount mount){
        if (unit == null || unit.dead || mount == null) {
            super.update(unit, mount);
            return;
        }

        WState st = acquireState(unit, mount);
        float reloadBefore = mount.reload;   // ← 原版推进前的冷却（备份）

        // —— 1) 先让原版推冷却、处理 warmup/charging/recoil ——
        super.update(unit, mount);

        // —— 2) 这一帧原版真实推进了多少冷却？用做差得到 origInc（真值，不猜任何系数）——
        float origInc = reloadBefore - mount.reload;
        if (origInc < 0f) origInc = 0f;   // 防止被某些状态"反推冷却"造成负值

        // —— 3) 目标判定 & 衰减（无目标每秒 - decayPerSecond）——
        Object curKey = null;
        boolean hasValid = false;
        if (mount.target != null){
            if (mount.target instanceof Unit u && u.isValid() && !u.dead){
                curKey = new UnitKeyHyperW(u.id());
                hasValid = unit.within(u, range() + 2f);
            } else if (mount.target instanceof Building b && b.isValid() && !b.dead){
                curKey = new BuildKeyHyperW(b.tileX(), b.tileY());
                hasValid = unit.within(b, range() + 2f);
            }
        }
        if (!hasValid){
            Teamc ut = null;
            try {
                UnitController c = unit.controller();
                if (c instanceof AIController ai){
                    java.lang.reflect.Method m = AIController.class.getMethod("target");
                    m.setAccessible(true);
                    ut = (Teamc) m.invoke(ai);
                }
            } catch (Throwable ignored){}
            if (ut instanceof Unit u && u.isValid() && !u.dead){
                curKey = new UnitKeyHyperW(u.id());
                hasValid = unit.within(u, range() + 2f);
            } else if (ut instanceof Building b && b.isValid() && !b.dead){
                curKey = new BuildKeyHyperW(b.tileX(), b.tileY());
                hasValid = unit.within(b, range() + 2f);
            }
        }

        if (!hasValid){
            st.decayAccum += Time.delta;
            while (st.decayAccum >= 1f && st.curReloadMul > 1f){
                st.curReloadMul = Math.max(1f, st.curReloadMul - decayPerSecond);
                st.decayAccum -= 1f;
            }
            if (st.curReloadMul <= 1f){ st.curReloadMul = 1f; st.decayAccum = 0f; }
            st.lastTargetKey = null;
            st.wasReady = (mount.reload <= 0f);
            sweepDeadUnits();
            return;
        }

        // 有目标：换目标就清空
        if (resetOnTargetSwitch && st.lastTargetKey != null && !st.lastTargetKey.equals(curKey)){
            st.curReloadMul = 1.0f;
            st.decayAccum = 0f;
        }
        st.lastTargetKey = curKey;

        // —— 4) 加速冷却：在 origInc（原版已推的量）基础上再额外推 (curReloadMul - 1) 倍
        //    最终：总推进 ≈ origInc × curReloadMul （curReloadMul=1 → 0 额外 → 纯原版行为）
        if (st.curReloadMul > 1f && origInc > 0f && mount.reload > 0f){
            float extra = origInc * (st.curReloadMul - 1f);
            if (extra > 0f){
                mount.reload = Math.max(0f, mount.reload - extra);
                if (reload > 0f){
                    float tgt = mount.reload / reload;
                    mount.smoothReload = tgt; // 直接赋值避免平滑不准
                }
            }
        }

        // —— 5) 触发叠层（每射一发 + perHitBonus）——
        //    非连续：原版推前 reloadBefore>0，推后 mount.reload<=0 且上一帧 wasReady=false → 冷却归零 = 打了一发
        if (!continuous && reloadBefore > 0f && mount.reload <= 0f && !st.wasReady){
            boolean same = (st.lastTargetKey != null) && st.lastTargetKey.equals(curKey);
            if (same){
                st.curReloadMul = Math.min(maxReloadMul, st.curReloadMul + perHitBonus);
            } else if (resetOnTargetSwitch){
                st.curReloadMul = Math.min(maxReloadMul, 1f + perHitBonus);
            }
            st.decayAccum = 0f;
        }
        // 连续武器：每"累计发射 1 发的时间窗口（= reload tick）"叠 +perHitBonus
        if (continuous && mount.bullet != null && mount.bullet.isAdded()){
            // 每 tick 的"等效发射发数" = 1 秒发数 × Time.delta(秒) = (60/reload) × Time.delta
            st.decayAccum += (60f / Math.max(1f, reload)) * Time.delta;
            while (st.decayAccum >= 1f){
                st.curReloadMul = Math.min(maxReloadMul, st.curReloadMul + perHitBonus);
                st.decayAccum -= 1f;
            }
        }

        st.wasReady = (mount.reload <= 0f);
        sweepDeadUnits();
    }

    /* ============== 对外读取当前 v = 每秒攻击次数（给 HyperSpeedBulletType 伤害快照用） ============== */

    public float currentShotsPerSecond(Unit unit, WeaponMount mount){
        if (unit == null || mount == null || reload <= 0f) return 1f;
        float mul = 1f;
        WState[] arr = stateMap.get(unit.id());
        if (arr != null){
            int idx = mountIndex(unit, mount);
            if (idx >= 0 && idx < arr.length && arr[idx] != null){
                mul = arr[idx].curReloadMul;
            }
        }
        float base = 60f / reload;
        float rmult = (bullet != null) ? bullet.reloadMultiplier : 1f;
        float urmult = 1f;
        try { urmult = unit.reloadMultiplier; } catch (Throwable ignore){}
        return base * rmult * urmult * mul;
    }

    /* ============== state 存取 / GC ============== */

    protected WState acquireState(Unit unit, WeaponMount mount){
        int uid = unit.id();
        int idx = mountIndex(unit, mount);
        if (idx < 0) idx = 0;
        WState[] arr = stateMap.get(uid);
        int mountCnt = (unit.mounts != null) ? unit.mounts.length : Math.max(1, idx + 1);
        if (arr == null || arr.length != mountCnt){
            arr = new WState[mountCnt];
            for (int i = 0; i < mountCnt; i++) arr[i] = new WState();
            stateMap.put(uid, arr);
        }
        if (arr[idx] == null) arr[idx] = new WState();
        return arr[idx];
    }

    protected static int mountIndex(Unit unit, WeaponMount mount){
        if (unit == null || unit.mounts == null || mount == null) return 0;
        WeaponMount[] ms = unit.mounts;
        for (int i = 0; i < ms.length; i++) if (ms[i] == mount) return i;
        return 0;
    }

    protected void sweepDeadUnits(){
        cleanCounter += Time.delta;
        if (cleanCounter < 60f) return; // 每秒扫一次
        cleanCounter = 0f;
        if (stateMap.size == 0) return;

        IntSeq ids = new IntSeq(stateMap.size);
        var keys = stateMap.keys();
        while (keys.hasNext) ids.add(keys.next());
        IntSeq toRemove = new IntSeq(Math.min(4, ids.size));
        for (int i = 0; i < ids.size; i++){
            int uid = ids.items[i];
            Unit u = Groups.unit.getByID(uid);
            if (u == null || u.dead || !u.isAdded()) toRemove.add(uid);
        }
        for (int i = 0; i < toRemove.size; i++) stateMap.remove(toRemove.items[i]);
    }

    /* ============== identity keys ============== */
    private record UnitKeyHyperW(int id){
        @Override public boolean equals(Object o){ return o instanceof UnitKeyHyperW u && u.id == id; }
        @Override public int hashCode(){ return id; }
    }
    private record BuildKeyHyperW(int tx, int ty){
        @Override public boolean equals(Object o){ return o instanceof BuildKeyHyperW b && b.tx == tx && b.ty == ty; }
        @Override public int hashCode(){ return tx * 73856093 ^ ty * 19349663; }
    }
}
