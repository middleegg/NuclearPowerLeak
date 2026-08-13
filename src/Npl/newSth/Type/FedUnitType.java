package  Npl.newSth.Type;

import arc.graphics.g2d.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;

/**
 * UnityUnitType（PU132 {@code unity.type.UnityUnitType} 的简化移植版）。
 * <p>
 * <h3>这是什么？为什么需要它？</h3>
 * <p>原版 {@link UnitType} 的武器渲染顺序是「按 weapons 列表顺序 + mirror 镜像」绘制的，
 * 但分段（虫子）单位有两类特殊需求，原版 UnitType 不直接支持：
 * <ol>
 *   <li><b>段身武器需要按 segmentIndex 分组镜像</b> —— 头部武器在 init 时统一镜像，
 *       段身武器要在头部加载完后再排序，否则镜像 weapon.otherSide 会乱。</li>
 *   <li><b>底层武器</b>（bottomWeapons）—— 某些武器要在单位贴图下方绘制（如阴影/底盘炮），
 *       需要在 {@link #drawWeapons(Unit)} 里临时降低 z 值。</li>
 * </ol>
 * <p>
 * 本精简版只保留这两类核心能力：
 * <ul>
 *   <li>{@link #segWeapSeq} —— 段身武器序列，{@link #sortSegWeapons(Seq)} 负责镜像+排序</li>
 *   <li>{@link #bottomWeapons} —— 底层武器列表，{@link #drawWeapons(Unit)} 按 z 区分绘制</li>
 *   <li>{@link #weaponXs} —— 武器初始 x 坐标，供 ShootArmorAbility 等做镜像偏移</li>
 * </ul>
 * <p>
 * <h3>已移除的高级功能</h3>
 * <p>原 PU132 UnityUnitType 还包含 Worm/Copter/Tentacle/Decoration/CLeg/Monolith 等大量分支，
 * 依赖链很深。本精简版把它们都删掉，只保留多节单位真正需要的字段，
 * 让代码更易读、依赖更少。需要的 Worm 逻辑由 {@code SegmentWormEntity} 直接实现。
 * <p>
 * <h3>constructor 默认值</h3>
 * <p>注意：本类默认 {@code constructor = UnitEntity::create}。
 * 真正的虫子头部/段身 UnitType 在 {@code Z_Units.load()} 里会覆盖为
 * {@code SegmentWormEntity::create} / {@code SegmentUnitEntity::create}。
 */
@SuppressWarnings("unchecked")
public class FedUnitType extends UnitType{
    /** 段身武器序列：在 {@link #init()} 中通过 {@link #sortSegWeapons(Seq)} 镜像+排序后填充 */
    public final Seq<Weapon> segWeapSeq = new Seq<>();
    /** 底层武器列表：这些武器会在单位贴图下方绘制（z 值更低） */
    public Seq<Weapon> bottomWeapons = new Seq<>();
    /** 武器初始 x 坐标列表，供 ShootArmorAbility 等能力做镜像偏移用 */
    public FloatSeq weaponXs = new FloatSeq();

    /**
     * @param name 单位内部名（不带 mod 前缀，atlas 会自动加 modname-）
     */
    public FedUnitType(String name){
        super(name);
        // 关闭轮廓描边：分段单位的段身贴图已自带轮廓，重复描边会出现锯齿
        outlines = false;
        // v155.4 适配：PU132 通过注解处理器自动设置 constructor，简化版需手动指定默认构造器
        constructor = mindustry.gen.UnitEntity::create;
    }

    /**
     * 初始化：在所有 weapons 配置完成后调用。
     *
     * <p>顺序很重要：
     * <ol>
     *   <li>先调用 {@code super.init()} 让原版 UnitType 处理武器镜像</li>
     *   <li>记录每个 weapon 的初始 x 坐标（镜像前）</li>
     *   <li>对段身武器单独镜像排序</li>
     *   <li>补充 bottomWeapons 中漏掉的「镜像对」</li>
     * </ol>
     */
    @Override
    public void init(){
        super.init();

        // 记录每个 weapon 的初始 x 坐标，ShootArmorAbility 镜像武器时需要知道原 x
        weapons.each(w -> weaponXs.add(w.x));

        // 段身武器镜像+排序（保留用于段身单位）
        sortSegWeapons(segWeapSeq);

        // bottomWeapons 中如果有 mirror=true 的武器，它的对侧武器也要加入 bottomWeapons，
        // 否则镜像那一半会画在普通层，看起来「一半在底一半在上」
        Seq<Weapon> addBottoms = new Seq<>();
        for(Weapon w : weapons){
            if(bottomWeapons.contains(w) && w.otherSide != -1){
                addBottoms.add(weapons.get(w.otherSide));
            }
        }

        bottomWeapons.addAll(addBottoms.distinct());
    }

    /**
     * 段身武器镜像 + 排序。
     *
     * <p>对每个 mirror=true 的武器，复制一份并 x 取反、flipSprite 翻转，
     * 设置 otherSide 互相指向。同时把 reload 和 recoilTime 翻倍
     * （因为现在两个武器轮流开火，单边频率减半才能保持总输出一致）。
     *
     * @param weaponSeq 待镜像排序的武器序列（会被原地替换为镜像后的完整列表）
     */
    public void sortSegWeapons(Seq<Weapon> weaponSeq){
        Seq<Weapon> mapped = new Seq<>();
        for(int i = 0, len = weaponSeq.size; i < len; i++){
            Weapon w = weaponSeq.get(i);
            // 兜底：reload<0 时 recoilTime 也会被原版逻辑当 -1 处理，这里同步修正
            if(w.recoilTime < 0f){
                w.recoilTime = w.reload;
            }
            mapped.add(w);

            if(w.mirror){
                Weapon copy = w.copy();
                copy.x *= -1;
                copy.shootX *= -1;
                copy.flipSprite = !copy.flipSprite;
                mapped.add(copy);

                // 镜像后两边轮流开火，单边 reload 翻倍保持总输出不变
                w.reload *= 2;
                copy.reload *= 2;
                w.recoilTime *= 2;
                copy.recoilTime *= 2;
                // otherSide 互相指向，方便后续按对查找
                w.otherSide = mapped.size - 1;
                copy.otherSide = mapped.size - 2;
            }
        }

        weaponSeq.set(mapped);
    }

    /**
     * 绘制所有武器，对 bottomWeapons 中的武器临时降低 z 值，让它们画在单位贴图下方。
     *
     * @param unit 要绘制的单位
     */
    @Override
    public void drawWeapons(Unit unit){
        float z = Draw.z();

        applyColor(unit);
        //for(WeaponMount mount : unit.mounts){
        //    Weapon weapon = mount.weapon;
            // 底层武器：z 略低，画在单位贴图下方（0.0001 的差值足以区分层级）
        //    if(bottomWeapons.contains(weapon)) Draw.z(z - 0.0001f);

        //    weapon.draw(unit, mount);
        //    Draw.z(z);
        //}

        Draw.reset();
    }
}
