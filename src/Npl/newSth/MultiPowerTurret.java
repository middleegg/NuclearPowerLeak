package Npl.newSth;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.entities.bullet.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.defense.turrets.PowerTurret;

/**
 * 多模式电力炮塔 · 继承 PowerTurret
 * <p>
 * 像 {@link HunfuBlock} 一样，点击炮塔时弹出"攻击模式"选择面板（图标按钮网格），
 * 切换后炮塔使用对应模式的子弹类型开火。每种模式由 {@link Mode} 描述。
 * <p>
 * 切换通过 config(Integer / Item) 触发：UI 按钮点选 → config(Item) → 匹配 mode.icon；
 * 逻辑处理器 / 存档 → config(Integer) → 直接按下标切换。
 *
 * <h3>使用范例（在 NuBlocks.load() 里）：</h3>
 * <pre>{@code
 * multiTurret = new MultiPowerTurret("multiTurret"){{
 *     requirements(Category.turret, with(
 *         NuItems.bigIron, 120,
 *         NuItems.monoSiliCrystal, 80,
 *         Items.graphite, 60
 *     ));
 *     size = 2;
 *     health = 2000;
 *     range = 160f;
 *     reload = 60f;
 *     consumePower(6f);
 *     shootSound = Sounds.shootBig;
 *
 *     // —— 注册多种攻击模式：
 *     //    图标优先级：iconName(自定义贴图名) > icon(物品/液体) > 数字占位(1~5)
 *     modes = Seq.with(
 *         // ① 自定义贴图：把 multiTurret-laser.png 放到 assets/sprites/ 下，名字即 atlas 名
 *         new Mode("laser", new LaserBulletType(){{
 *             damage = 200f; length = 200f; lifetime = 24f;
 *         }}, "multiTurret-laser"),
 *         // ② 用物品贴图（UnlocakbleContent）
 *         new Mode("standard", new BasicBulletType(3f, 80f){{
 *             lifetime = 60f; width = 8f; height = 16f;
 *         }}, NuItems.bigIron),
 *         // ③ 什么图都不提供 → 自动用数字占位(第3个模式=character-overlay28=数字"3")
 *         new Mode("heal", new SweepBulletType(0f){{
 *             heal = 2f; scanRadius = 120f; fieldAngle = 90f;
 *         }})
 *     );
 * }};
 * }</pre>
 */
public class MultiPowerTurret extends PowerTurret{

    /** 所有攻击模式。必须在 init 前通过 {{...}} 赋值。 */
    public Seq<Mode> modes = new Seq<>(4);
    /** UI 选择面板每行按钮数 */
    public int selectionColumns = 4;

    public MultiPowerTurret(String name){
        super(name);
        configurable = true;
        saveConfig = true;

        // ① 整数索引：逻辑处理器 / 存档恢复 切模式
        config(Integer.class, (MultiPowerTurretBuild build, Integer i) -> {
            if(i == null) return;
            if(i < 0 || i >= modes.size){
                build.currentMode = 0;
                return;
            }
            if(build.currentMode == i) return;
            build.currentMode = i;
            build.target = null;        // 切换后重新选目标
            build.reloadCounter = 0f;   // 重置装填进度
        });

        // ② 物品对象：UI 按钮点选（按 mode.icon 匹配）
        config(Item.class, (MultiPowerTurretBuild build, Item item) -> {
            if(item == null) return;
            int idx = modes.indexOf(m -> m != null && m.icon == item);
            if(idx >= 0) build.configure(idx);
        });

        // ③ 清空：退回第 0 个模式（炮塔至少要有一个可用模式）
        configClear((MultiPowerTurretBuild build) -> {
            build.currentMode = 0;
        });
    }

    @Override
    public void init(){
        // 没配置模式 → 用 shootType 兜底造一个默认模式，避免 NPE
        if(modes.isEmpty() && shootType != null){
            modes.add(new Mode("default", shootType));
        }
        // 让 shootType 同步到第 0 个模式（保证 setStats / limitRange 等用 shootType 的逻辑正确）
        if(!modes.isEmpty() && modes.get(0).bullet != null){
            shootType = modes.get(0).bullet;
        }
        super.init();
    }

    /* ============================================================
     *                      Mode 内部类
     * ============================================================ */
    public static class Mode{
        public String name;
        public BulletType bullet;
        /** UI 按钮图标（物品或液体）。null 时继续看 iconName，再没有就回退到数字图标。 */
        public @Nullable UnlockableContent icon;
        /** 自定义 atlas 贴图名（优先级最高），如 "multiTurret-mode-laser" / "nu-xxx"。
         *  贴图放在 assets/sprites/ 下任意子目录，mod 加载时会被 Core.atlas 扫描到。 */
        public @Nullable String iconName;
        /** 模式说明文字（显示在信息面板名称旁），可为 null */
        public @Nullable String description;

        public Mode(String name, BulletType bullet, UnlockableContent icon){
            this.name = name;
            this.bullet = bullet;
            this.icon = icon;
        }
        public Mode(String name, BulletType bullet, String iconName){
            this.name = name;
            this.bullet = bullet;
            this.iconName = iconName;
        }
        public Mode(String name, BulletType bullet){
            this(name, bullet, (UnlockableContent)null);
        }
    }

    /**
     * 取模式对应 UI 贴图。
     * 优先级：
     *   ① mode.iconName（自定义 atlas 名，找到即用）
     *   ② mode.icon（UnlockableContent 自带 uiIcon）
     *   ③ 数字占位：第 0~4 个模式对应 character-overlay26~30（1 2 3 4 5），
     *      超过 5 个时循环复用（每 10 个一组继续走字符表）。
     *  所有兜底都找不到 → 返回 error 贴图代替。
     */
    public TextureRegion modeIcon(int modeIdx, Mode m){
        if(m != null){
            if(m.iconName != null && !m.iconName.isEmpty()){
                TextureRegion r = Core.atlas.find(m.iconName);
                if(r.found()) return r;
            }
            if(m.icon != null && m.icon.uiIcon != null && m.icon.uiIcon.found()){
                return m.icon.uiIcon;
            }
        }
        // character-overlay 编号：26='1', 27='2', 28='3', 29='4', 30='5', 31='6', 32='7', 33='8', 34='9', 35='0'
        int ord = Math.max(0, modeIdx);
        int overlayId = 26 + (ord % 10);
        TextureRegion r = Core.atlas.find("character-overlay" + overlayId);
        if(r.found()) return r;
        return Core.atlas.find("error");
    }

    public class MultiPowerTurretBuild extends PowerTurretBuild{

        /** 当前选中的模式索引，默认 0 */
        public int currentMode = 0;

        /** 当前模式对象，null 表示无模式 */
        public Mode currentModeObj(){
            if(currentMode < 0 || currentMode >= modes.size) return null;
            return modes.get(currentMode);
        }

        /* —— 关键：让射击流程使用当前模式的子弹 —— */
        @Override
        public BulletType peekAmmo(){
            Mode m = currentModeObj();
            return (m != null && m.bullet != null) ? m.bullet : shootType;
        }

        @Override
        public BulletType useAmmo(){
            return peekAmmo();
        }

        @Override
        public Object config(){
            return currentMode;
        }

        /* —— UI 选择面板：图标按钮网格 —— */
        @Override
        public void buildConfiguration(Table table){
            if(modes.isEmpty()){
                table.table(Styles.black3, t -> t.add("@none").color(Color.lightGray));
                return;
            }
            int idx = 0;
            TextureRegionDrawable draw = new TextureRegionDrawable();
            for(int i = 0; i < modes.size; i++){
                Mode m = modes.get(i);
                if(m == null) continue;
                if(idx % selectionColumns == 0) table.row();
                final int modeIdx = i;
                ImageButton b = table.button(Tex.whiteui, Styles.squareTogglei, () -> configure(modeIdx)).size(50f).get();
                b.clearChildren();
                b.image().update(img -> {
                    TextureRegion r = modeIcon(modeIdx, m);
                    img.setDrawable(draw.set(r));
                    img.setScaling(Scaling.fit);
                }).size(34);
                b.update(() -> b.setChecked(currentMode == modeIdx));
                table.add().pad(2);
                idx++;
            }
        }

        /* —— 信息面板：显示当前模式名 + 图标 —— */
        @Override
        public void display(Table table){
            super.display(table);
            TextureRegionDrawable reg = new TextureRegionDrawable();
            table.row();
            table.table(t -> {
                t.left();
                t.image().update(i -> {
                    Mode m = currentModeObj();
                    TextureRegion r = modeIcon(currentMode, m);
                    i.setDrawable(reg.set(r));
                    i.setColor(Color.white);
                    i.setScaling(Scaling.fit);
                }).size(32).padBottom(-4).padRight(2);
                t.label(() -> {
                    Mode m = currentModeObj();
                    return m == null ? "@none" : m.name;
                }).wrap().width(230f).color(Color.lightGray);
            }).left();
        }

        /* —— 选中时画射程圈（不同模式子弹的 rangeChange 可能不同）—— */
        @Override
        public void drawSelect(){
            super.drawSelect();
            Drawf.dashCircle(x, y, range(), team.color);
        }

        /* —— 存档 —— */
        @Override
        public byte version(){ return 1; }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(currentMode);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                currentMode = read.s();
                if(currentMode < 0 || currentMode >= modes.size) currentMode = 0;
            } else {
                currentMode = 0;
            }
        }
    }
}
