package Npl.newSth.assembler;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static arc.math.Mathf.*;
import static mindustry.Vars.*;

/**
 * 【点击切换方案版】装配机
 * =============================================================
 * 原版 UnitAssembler：靠周围放 UnitAssemblerModule（不同 tier 模块建筑）
 * 决定 currentTier，currentTier 决定生产 plans[i] 的单位。
 *
 * 这个子类：
 *   ① 不再需要任何 UnitAssemblerModule（moduleFits 永远 false）
 *   ② 用 player 可控的 selectedPlan（plans 下标）替代原版 currentTier 机制
 *   ③ configurable=true，点建筑弹出配置面板，面板里显示每个方案对应的单位图标
 *   ④ 切换走原版 config(Integer.class) 网络同步（不需要 @Remote 注解）
 *   ⑤ 生产/消耗/进度/渲染/存档 全部复用原版 UnitAssemblerBuild 逻辑
 *
 * 用法（NuBlocks 里）：
 * <pre>
 *   myAssembler = new ClickSwitchAssembler("myAssembler"){{
 *       size = 3;
 *       health = 300;
 *       areaSize = 9;                 // 装配区（格）
 *       droneType = UnitTypes.assemblyDrone;
 *       dronesCreated = 4;
 *       plans.add(new AssemblerUnitPlan(UnitTypes.dagger,
 *           120f, PayloadStack.list(Blocks.copperWallLarge, 4, Blocks.siliconSmelter, 1)));
 *       plans.add(new AssemblerUnitPlan(UnitTypes.crawler,
 *           240f, PayloadStack.list(Blocks.copperWallLarge, 8, Blocks.siliconSmelter, 2)));
 *       consumePower(5f);
 *       requirements(Category.units, with(Items.copper, 400, Items.silicon, 200));
 *   }};
 * </pre>
 */
public class ClickSwitchAssembler extends UnitAssembler {

    /** 配置面板里单位图标网格的列数（参考 UnitFactory.selectionColumns，默认 5） */
    public int selectionColumns = 5;

    public ClickSwitchAssembler(String name){
        super(name);

        // 点建筑能弹出配置面板
        configurable = true;

        // 注册整数配置：点击图标 → configure(planIndex) → 走原版 Call.tileConfig 网络同步
        // 服务端/客户端都会执行这个 lambda（Mindustry 自动处理同步）
        config(Integer.class, (ClickSwitchAssemblerBuild build, Integer i) -> {
            int n = plans.size;
            if(n == 0) return;
            int next = (i == null || i < 0 || i >= n) ? 0 : i;
            if(build.selectedPlan == next) return;
            build.selectedPlan = next;
            build.progress = 0f;       // 换方案：清零当前进度
            build.lastTier = -1;       // 触发父类"tier变动重置"保护分支
        });

        // 清空配置（右键/ESC 取消选中）→ 回到第 0 个方案
        configClear((ClickSwitchAssemblerBuild build) -> {
            build.selectedPlan = 0;
            build.progress = 0f;
            build.lastTier = -1;
        });
    }

    /* ================================================================
     *  Building：重写 plan() + 关模块 + 配置面板
     * ================================================================ */
    public class ClickSwitchAssemblerBuild extends UnitAssemblerBuild {

        /** 当前选中的方案下标（默认 0）。替代原版 currentTier 机制 */
        public int selectedPlan = 0;

        /* ————————————————————————————————————————
         *  彻底关闭"模块建筑决定 tier"机制
         * ———————————————————————————————————————— */
        @Override
        public boolean moduleFits(Block other, float ox, float oy, int rotation){
            return false;          // 永远不接受模块
        }

        public void updateModules(mindustry.world.blocks.units.UnitAssemblerModule.UnitAssemblerModuleBuild build){}
        public void removeModule(mindustry.world.blocks.units.UnitAssemblerModule.UnitAssemblerModuleBuild build){}
        @Override public void checkTier(){}

        /* ————————————————————————————————————————
         *  用 selectedPlan 驱动 production
         * ———————————————————————————————————————— */
        @Override
        public AssemblerUnitPlan plan(){
            int n = plans.size;
            return plans.get(n == 0 ? 0 : clamp(selectedPlan, 0, n - 1));
        }

        /** 对外暴露当前配置值（给配置面板/存档网络同步用）*/
        @Override
        public Object config(){
            return selectedPlan;
        }

        @Override
        public void drawSelect(){
            // 去掉原版高亮 modules 的代码（没有模块），只画装配区外框
            Drawf.dashRect(Pal.accent, getRect(Tmp.r1, x, y, rotation));
        }

        /* =====================================================
         *  配置面板：点建筑 → 弹出单位图标网格
         *  每个 plan 显示对应 unit.uiIcon，点击即切换到该方案
         * ===================================================== */
        @Override
        public void buildConfiguration(Table table){
            if(plans.isEmpty()){
                table.table(Styles.black3, t -> t.add("@none").color(Color.lightGray));
                return;
            }

            int idx = 0;
            for(int i = 0; i < plans.size; i++){
                AssemblerUnitPlan p = plans.get(i);
                if(p == null || p.unit == null) continue;
                // 未解锁的单位不显示在面板上（参考 UnitFactory 的 retainAll(unlockedNow)）
                if(!p.unit.unlockedNow() || p.unit.isBanned()) continue;

                if(idx % selectionColumns == 0) table.row();

                final int pi = i;
                ImageButton b = table.button(Tex.whiteui, Styles.squareTogglei, () -> configure(pi))
                    .size(50f).get();
                b.clearChildren();
                b.image(p.unit.uiIcon).scaling(Scaling.fit).size(34);
                // 选中态：当前 selectedPlan == i 时高亮
                b.update(() -> b.setChecked(selectedPlan == pi));

                table.add().pad(2);
                idx++;
            }
        }

        /* =====================================================
         *  存档读写：额外写 selectedPlan
         * ===================================================== */
        @Override
        public byte version(){
            return 2;   // 1 = 父类；2 = 新增 selectedPlan
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(selectedPlan);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 2){
                int n = plans.size;
                selectedPlan = clamp(read.i(), 0, Math.max(0, n - 1));
                lastTier = -1;      // 防止进度错乱
            }
        }
    }
}
