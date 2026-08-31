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
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.world.meta.Stat;
import Npl.content.ModStats;
import Npl.content.NuFx;

/**
 * 多模式电力炮塔 · 继承 PowerTurret
 * <p>
 * 像 {@link HunfuBlock} 一样，点击炮塔时弹出"攻击模式"选择面板（图标按钮网格），
 * 可多选攻击方式，炮塔会同时使用所有选中的模式开火。
 * 每种模式由 {@link Mode} 描述，包含独立的电力消耗、射程、装填时间，多选时电力消耗叠加。
 * <p>
 * 切换通过 config(Integer / Item) 触发：UI 按钮点选 → config(Item) → 匹配 mode.icon；
 * 逻辑处理器 / 存档 → config(Integer) → 直接按下标切换。
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

        // 动态电力消耗：有目标（瞄准）时就消耗，射击时也消耗
        consumePowerDynamic(building -> {
            if(!(building instanceof MultiPowerTurretBuild)) return 0f;
            MultiPowerTurretBuild b = (MultiPowerTurretBuild)building;
            if(b.target == null && !b.isShooting) return 0f;
            return b.getTotalPowerUse();
        });

        // ① 整数索引：逻辑处理器 / 存档恢复 切模式（切换 = 多选）
        config(Integer.class, (MultiPowerTurretBuild build, Integer i) -> {
            if(i == null) return;
            if(i < 0 || i >= modes.size) return;
            build.toggleMode(i);
        });

        // ② 物品对象：UI 按钮点选（按 mode.icon 匹配）
        config(Item.class, (MultiPowerTurretBuild build, Item item) -> {
            if(item == null) return;
            int idx = modes.indexOf(m -> m != null && m.icon == item);
            if(idx >= 0) build.toggleMode(idx);
        });

        // ③ 清空：退回只选中第 0 个模式
        configClear((MultiPowerTurretBuild build) -> {
            build.selectedModes = 1;
            build.target = null;
            build.resetAllReload();
        });
    }

    @Override
    public void init(){
        // 没配置模式 → 用 shootType 兜底造一个默认模式，避免 NPE
        if(modes.isEmpty() && shootType != null){
            modes.add(new Mode("default", shootType, 0f, 0f, 0f));
        }
        // 让 shootType 同步到第 0 个模式（保证 setStats / limitRange 等用 shootType 的逻辑正确）
        if(!modes.isEmpty() && modes.get(0).bullet != null){
            shootType = modes.get(0).bullet;
        }
        super.init();
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.damage);
        stats.remove(Stat.reload);
        stats.remove(Stat.range);
        stats.remove(Stat.ammo);

        if(modes.isEmpty()) return;
        for(int i = 0; i < modes.size; i++){
            Mode m = modes.get(i);
            if(m == null) continue;
            final int modeIdx = i;
            stats.add(ModStats.modeInfo, t -> {
                t.background(Styles.black6);
                t.margin(6f);
                t.add("[accent]" + m.name).left().row();
                t.add("[lightgray]耗电: " + Strings.autoFixed(m.powerUse * 60f, 2) + " ⚡/s").left().row();
                t.add("[lightgray]射程: " + Strings.autoFixed(m.range > 0f ? m.range : range, 0)
                    + "  装填: " + Strings.autoFixed((m.reload > 0f ? m.reload : reload) / 60f, 2) + "s").left().row();
                if(m.description != null) t.add(m.description).left();
            });
        }
    }

    /* ============================================================
     *                      Mode 内部类
     * ============================================================ */
    public static class Mode{
        public String name;
        public BulletType bullet;
        /** 该模式的电力消耗，多选时叠加 */
        public float powerUse;
        /** 该模式的射程，0 = 使用炮塔默认 range */
        public float range;
        /** 该模式的装填时间，0 = 使用炮塔默认 reload */
        public float reload;
        /** UI 按钮图标（物品或液体）。null 时继续看 iconName，再没有就回退到数字图标。 */
        public @Nullable UnlockableContent icon;
        /** 自定义 atlas 贴图名（优先级最高），如 "multiTurret-mode-laser" / "nu-xxx"。 */
        public @Nullable String iconName;
        /** 模式说明文字（显示在信息面板名称旁），可为 null */
        public @Nullable String description;

        public Mode(String name, BulletType bullet, float powerUse, float range, float reload){
            this.name = name;
            this.bullet = bullet;
            this.powerUse = powerUse;
            this.range = range;
            this.reload = reload;
        }
        public Mode(String name, BulletType bullet, UnlockableContent icon, float powerUse, float range, float reload){
            this.name = name;
            this.bullet = bullet;
            this.icon = icon;
            this.powerUse = powerUse;
            this.range = range;
            this.reload = reload;
        }
        public Mode(String name, BulletType bullet, String iconName, float powerUse, float range, float reload){
            this.name = name;
            this.bullet = bullet;
            this.iconName = iconName;
            this.powerUse = powerUse;
            this.range = range;
            this.reload = reload;
        }
        // 向后兼容构造函数（range/reload/powerUse 默认 0）
        public Mode(String name, BulletType bullet){
            this(name, bullet, 0f, 0f, 0f);
        }
        public Mode(String name, BulletType bullet, float powerUse){
            this(name, bullet, powerUse, 0f, 0f);
        }
        public Mode(String name, BulletType bullet, UnlockableContent icon){
            this(name, bullet, icon, 0f, 0f, 0f);
        }
        public Mode(String name, BulletType bullet, UnlockableContent icon, float powerUse){
            this(name, bullet, icon, powerUse, 0f, 0f);
        }
        public Mode(String name, BulletType bullet, String iconName){
            this(name, bullet, iconName, 0f, 0f, 0f);
        }
        public Mode(String name, BulletType bullet, String iconName, float powerUse){
            this(name, bullet, iconName, powerUse, 0f, 0f);
        }
    }

    /** 未选中模式的图标色调：保持原色、仅压低不透明度，避免乘深灰后变成黑块。 */
    private static final Color modeOffColor = new Color(1f, 1f, 1f, 0.5f);
    /** 配置弹窗模式按钮：选中/未选中边框色 + 内部底色（自绘，保证三个按钮尺寸位置一致）。 */
    private static final Color modeBorderOn = Color.valueOf("FFD75A");
    private static final Color modeBorderOff = Color.valueOf("6B6B6B");
    private static final Color modeInnerColor = Color.valueOf("1E1E1E");

    /**
     * 取模式对应 UI 贴图：只用 nu-AttackMode-{n}（n 从 1 开始，对应模式下标 +1）。
     * 贴图缺失时 Core.atlas.find 会自动回退到 error 贴图。
     */
    public TextureRegion modeIcon(int modeIdx, Mode m){
        return Core.atlas.find("nu-AttackMode-" + (Math.max(0, modeIdx) + 1));
    }

    /**
     * 按模式序号决定开火口的炮口特效：
     * 0 = 常规火花 / 1 = 激光充能 / 2 = 治疗光环，其余回退到常规火花。
     * 与 {@link #modeIcon} 使用同一套下标，保证图标与特效一一对应。
     */
    public Effect modeMuzzleEffect(int modeIdx){
        switch(modeIdx){
            case 1: return NuFx.LaserChargeSmallWhite;
            case 2: return NuFx.healMuzzle;
            default: return Fx.shootBig;
        }
    }

    /** 模式切换反馈特效：治疗模式用绿色脉冲波纹 + 十字，其它模式用能量环。 */
    public Effect modeSwitchEffect(int modeIdx){
        return modeIdx == 2 ? NuFx.healSwitch : NuFx.chargeRing;
    }

    public class MultiPowerTurretBuild extends PowerTurretBuild{

        /** 选中的模式位掩码，bit i = 1 表示选中第 i 个模式，默认只选第 0 个 */
        public int selectedModes = 1;

        /** 每个模式的装填进度（与 modes 下标对应） */
        private float[] modeReload;

        /** 本帧是否实际射击了（用于动态电力消耗） */
        public boolean isShooting = false;

        public boolean isModeSelected(int idx){
            return (selectedModes & (1 << idx)) != 0;
        }

        public void toggleMode(int idx){
            if(idx < 0 || idx >= modes.size) return;
            selectedModes ^= (1 << idx);
            if(selectedModes == 0) selectedModes = 1;
            target = null;
            resetAllReload();

            // 切换反馈：在炮塔处播放一次性特效（治疗=绿爆发，其余=能量环）
            Effect fx = modeSwitchEffect(idx);
            if(fx != null) fx.at(x, y, rotation);
        }

        /** 当前选中的有效模式数量（供 UI 摘要使用，无分配）。 */
        public int getSelectedCount(){
            int count = 0;
            for(int i = 0; i < modes.size; i++){
                if(isModeSelected(i) && modes.get(i) != null) count++;
            }
            return count;
        }

        public void resetAllReload(){
            if(modeReload != null){
                for(int i = 0; i < modeReload.length; i++){
                    modeReload[i] = 0f;
                }
            }
        }

        public Seq<Mode> getSelectedModes(){
            Seq<Mode> result = new Seq<>();
            for(int i = 0; i < modes.size; i++){
                if(isModeSelected(i) && modes.get(i) != null){
                    result.add(modes.get(i));
                }
            }
            return result;
        }

        /**
         * 计算当前选中模式的总电力消耗
         */
        public float getTotalPowerUse(){
            float total = 0f;
            for(int i = 0; i < modes.size; i++){
                if(isModeSelected(i) && modes.get(i) != null){
                    total += modes.get(i).powerUse;
                }
            }
            return total;
        }

        /**
         * 获取模式的实际射程（mode.range > 0 时用模式自己的，否则用炮塔默认）
         */
        public float getModeRange(Mode m){
            return m.range > 0f ? m.range : range;
        }

        /**
         * 获取模式的实际装填时间（mode.reload > 0 时用模式自己的，否则用炮塔默认）
         */
        public float getModeReload(Mode m){
            return m.reload > 0f ? m.reload : reload;
        }

        /**
         * 返回选中模式中的最大射程（用于 range() 和 UI 显示）
         */
        public float getSelectedMaxRange(){
            float max = 0f;
            for(int i = 0; i < modes.size; i++){
                if(isModeSelected(i) && modes.get(i) != null){
                    float r = getModeRange(modes.get(i));
                    if(r > max) max = r;
                }
            }
            return max > 0f ? max : range;
        }

        @Override
        public float range(){
            return getSelectedMaxRange();
        }

        /* —— 让射击流程使用当前模式的子弹 —— */
        @Override
        public BulletType peekAmmo(){
            Seq<Mode> selected = getSelectedModes();
            if(selected.isEmpty()) return shootType;
            Mode m = selected.first();
            return (m != null && m.bullet != null) ? m.bullet : shootType;
        }

        @Override
        public BulletType useAmmo(){
            return peekAmmo();
        }

        /* —— 多选模式同时开火，每个模式独立装填 —— */
        @Override
        public void updateShooting(){
            if(modeReload == null || modeReload.length != modes.size){
                modeReload = new float[modes.size];
            }
            Seq<Mode> selected = getSelectedModes();
            if(selected.isEmpty()){ isShooting = false; return; }
            isShooting = false;
            for(Mode m : selected){
                if(m.bullet == null) continue;
                int idx = modes.indexOf(m);
                float reloadTime = getModeReload(m);

                if(modeReload[idx] >= reloadTime){
                    shoot(m.bullet);
                    isShooting = true;
                    modeReload[idx] = 0f;

                    // 按模式发射不同的炮口特效（跟随炮管位置与角度）
                    Effect muzzle = modeMuzzleEffect(idx);
                    if(muzzle != null){
                        float wx = x + Angles.trnsx(rotation - 90f, shootX, shootY);
                        float wy = y + Angles.trnsy(rotation - 90f, shootX, shootY);
                        muzzle.at(wx, wy, rotation);
                    }
                } else {
                    modeReload[idx] += delta() * efficiency;
                }
            }
        }

        @Override
        public Object config(){
            return selectedModes;
        }

        /* —— UI 选择面板：图标按钮网格（多选切换）—— */
        @Override
        public void buildConfiguration(Table table){
            if(modes.isEmpty()){
                table.table(Styles.black3, t -> t.add("@none").color(Color.lightGray));
                return;
            }
            // 标题
            table.add("[accent]攻击模式").left().padBottom(4f).row();

            // 图标按钮网格
            Table grid = new Table();
            int idx = 0;
            for(int i = 0; i < modes.size; i++){
                Mode m = modes.get(i);
                if(m == null) continue;
                if(idx % selectionColumns == 0 && idx != 0) grid.row();
                final int modeIdx = i;
                // 【关键】每个模式各自一个 drawable，否则所有按钮会共用同一个实例、全部显示最后设置的贴图
                final TextureRegionDrawable d = new TextureRegionDrawable(modeIcon(modeIdx, m));
                // 自绘无底色按钮：边框/底色完全由下方三层图像控制，三个按钮尺寸、位置严格一致
                ImageButton.ImageButtonStyle st = new ImageButton.ImageButtonStyle();
                st.up = null; st.over = null; st.down = null; st.checked = null;

                ImageButton b = new ImageButton(st);
                b.clicked(() -> configure(modeIdx));
                b.clearChildren();

                // 三层：外边框 → 内部底色 → 图标（用 Wrapper 控制各自尺寸，实现统一的描边效果）
                Image border = new Image(Tex.whiteui);
                Image inner = new Image(Tex.whiteui);
                Image icon = new Image();
                Table borderWrap = new Table();
                borderWrap.add(border).size(56f);
                Table innerWrap = new Table();
                innerWrap.add(inner).size(50f);
                Table iconWrap = new Table();
                iconWrap.add(icon).size(44f);
                Stack stack = new Stack();
                stack.add(borderWrap);
                stack.add(innerWrap);
                stack.add(iconWrap);
                b.add(stack).grow();

                b.update(() -> {
                    boolean on = isModeSelected(modeIdx);
                    b.setChecked(on);
                    border.setColor(on ? modeBorderOn : modeBorderOff);
                    inner.setColor(modeInnerColor);
                    icon.setDrawable(d);
                    icon.setScaling(Scaling.fit);
                    // 选中亮起、未选中仅压低透明度（保持原色，避免彩色方块被染黑）
                    icon.setColor(on ? Color.white : modeOffColor);
                });
                b.addListener(new Tooltip(t -> {
                    t.background(Styles.black6);
                    t.margin(4f);
                    t.add(m.name).color(Color.white).left().row();
                    t.add("[accent]耗电: " + Strings.autoFixed(m.powerUse * 60f, 2) + " ⚡/s").left().row();
                    t.add("[lightgray]射程: " + Strings.autoFixed(getModeRange(m), 0) + " [accent]装填: " + Strings.autoFixed(getModeReload(m) / 60f, 2) + "s").left().row();
                    if(m.description != null){
                        t.add("[lightgray]" + m.description).left().wrap().width(200f);
                    }
                }));
                grid.add(b).size(60f).pad(3f);
                idx++;
            }
            table.add(grid).row();

            // 底部摘要：已选数量 + 实时总耗电
            table.label(() -> "[lightgray]已选 [accent]" + getSelectedCount() + "[lightgray]/" + modes.size
                + "   [accent]总耗电: " + Strings.autoFixed(getTotalPowerUse() * 60f, 2) + " [lightgray]⚡/s")
                .left().padTop(4f).fontScale(0.85f);
        }

        /* —— 信息面板：显示各模式详情 + 总电力消耗 —— */
        @Override
        public void display(Table table){
            super.display(table);

            // 分区标题
            table.row();
            table.add("[accent]模式详情").left().padTop(6f).padBottom(2f);

            // 显示每个模式的详细信息
            for(int i = 0; i < modes.size; i++){
                Mode m = modes.get(i);
                if(m == null) continue;
                final int modeIdx = i;
                // 每个模式各自一个 drawable，避免全部显示成最后一张贴图
                final TextureRegionDrawable d = new TextureRegionDrawable(modeIcon(modeIdx, m));
                table.row();
                table.table(t -> {
                    t.left();
                    // 模式图标
                    t.image().update(img -> {
                        img.setDrawable(d);
                        img.setColor(isModeSelected(modeIdx) ? Color.white : modeOffColor);
                        img.setScaling(Scaling.fit);
                    }).size(40).padBottom(-2).padRight(6);
                    // 模式名称 + 状态
                    t.table(info -> {
                        info.left();
                        info.add(m.name).color(isModeSelected(modeIdx) ? Color.white : Color.gray).left().row();
                        info.add("[lightgray]耗电: " + Strings.autoFixed(m.powerUse * 60f, 2) + " ⚡/s").left().row();
                        info.add("[lightgray]射程: " + Strings.autoFixed(getModeRange(m), 0) + " 装填: " + Strings.autoFixed(getModeReload(m) / 60f, 2) + "s").left();
                    }).left();
                    // 选中状态标记（随切换实时刷新）
                    t.label(() -> isModeSelected(modeIdx) ? "[accent]● 启用" : "[darkgray]○ 关闭").left().padLeft(4f);
                }).left();
            }

            // 显示总电力消耗
            table.row();
            table.table(t -> {
                t.left();
                t.image(Core.atlas.find("power")).size(20).padBottom(-2).padRight(2);
                t.label(() -> {
                    float total = getTotalPowerUse();
                    return "[accent]总耗电: " + Strings.autoFixed(total * 60f, 2) + " [lightgray]⚡/s";
                }).wrap().width(230f).color(Color.lightGray);
            }).left();

            // 显示总射程
            table.row();
            table.table(t -> {
                t.left();
                t.label(() -> {
                    float maxRange = getSelectedMaxRange();
                    return "[lightgray]射程: " + Strings.autoFixed(maxRange, 0);
                }).wrap().width(230f).color(Color.lightGray);
            }).left();
        }

        /* —— 选中时画射程圈 —— */
        @Override
        public void drawSelect(){
            super.drawSelect();
            Drawf.dashCircle(x, y, getSelectedMaxRange(), team.color);
        }

        /* —— 存档 —— */
        @Override
        public byte version(){ return 2; }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(selectedModes);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 2){
                selectedModes = read.i();
                if(selectedModes == 0) selectedModes = 1;
            } else {
                int oldMode = read.s();
                selectedModes = 1 << oldMode;
            }
        }
    }
}
