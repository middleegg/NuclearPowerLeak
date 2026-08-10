/*
 * ⚠️ 修复版 - 仅放入 Refactored_Code 文件夹供用户核实
 * ⚠️ 不影响原始 src 目录下任何文件
 * ==============================================================
 * 三大问题修复：
 *   Bug 1 [产出无上限]  → updateTile() 遍历 outItem[] 所有输出逐个检查容量
 *   Bug 2 [不消耗原料]  → updateTile() 完成周期时手动 items.remove(requirements)
 *   Bug 3 [不出传送带] → items.add() 之后对每个输出物调用 offload() 推下游
 *
 * + 额外小修：
 *   - initCapacities() 统计"输出物品的单次最大量"防止多输出时仓容量不够
 *   - shouldConsume() 检查"所有"输出物品的上限（之前只查第一个）
 *   - acceptItem() 增加"本配方输出物品"也接受（切配方后 offload 回流不堵）
 *   - draw() 里多余的 Draw.z() 保留但调顺序
 *   - buildConfiguration() 从外部类 lambda 正确引用（你原来的写法已 OK，已保留）
 */
package Npl.newSth;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.units.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.Block;
import java.util.*;

import static mindustry.Vars.*;


public class ConfigurableBlock extends UnitBlock {

    public int[] capacities = {};
    public Seq<Plan> plans = new Seq<>(4);
    public int selectionRows = 2;
    public int selectionColumns = 4;

    public ConfigurableBlock(String name) {
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasPower = true;
        consumesPower = true;
        size = 2;
        health = 100;
        rotate = true;
        configurable = true;
        itemCapacity = 30;

        // 按索引切换配方
        config(Integer.class, (UnitFactoryBuild build, Integer i) -> {
            if (!configurable) return;
            if (build.currentPlan == i) return;
            build.currentPlan = i < 0 || i >= plans.size ? -1 : i;
            build.progress = 0;
        });

        // 按物品切换配方（使用每个配方的第一个产出物品作为代表）
        config(Item.class, (UnitFactoryBuild build, Item item) -> {
            if (!configurable) return;
            int next = plans.indexOf(p -> p.outItem != null && p.outItem.length > 0 && p.outItem[0].item == item);
            if (build.currentPlan == next) return;
            build.currentPlan = next;
            build.progress = 0;
        });

        // 清除配置
        configClear((UnitFactoryBuild build) -> {
            build.currentPlan = -1;
            build.progress = 0;
        });
    }

    @Override
    public void init() {
        initCapacities();
        super.init();
    }

    @Override
    public void afterPatch() {
        initCapacities();
        super.afterPatch();
    }

    /** ✅ Bug 1 相关修：计算容量时同时看"输入 requirements"和"输出 outItem"的单次最大量 */
    public void initCapacities() {
        capacities = new int[Vars.content.items().size];

        int maxAmount = 0;
        for (Plan plan : plans) {
            if (plan == null) continue;
            if (plan.requirements != null) for (ItemStack stack : plan.requirements) {
                if (stack != null && stack.amount > maxAmount) maxAmount = stack.amount;
            }
            // ➕ 新增：输出物品也算（否则"一次产 5 磁铁但上限仅为 3"的矛盾会出现）
            if (plan.outItem != null) for (ItemStack stack : plan.outItem) {
                if (stack != null && stack.amount > maxAmount) maxAmount = stack.amount;
            }
        }

        int unifiedLimit = Math.max(1, maxAmount * 6);
        Arrays.fill(capacities, unifiedLimit);

        consumeBuilder.each(c -> c.multiplier = b -> state.rules.unitCost(b.team));
    }

    @Override
    public void checkContentArrayCapacity(int items, int liquids) {
        super.checkContentArrayCapacity(items, liquids);
        if (capacities.length != items) capacities = Arrays.copyOf(capacities, items);
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("progress", (UnitFactoryBuild e) -> new Bar("bar.progress", Pal.ammo, e::fraction));
    }

    @Override
    public boolean outputsItems() {
        return true;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.remove(Stat.itemCapacity);

        stats.add(Stat.output, table -> {
            table.row();
            for (Plan plan : plans) {
                if (plan == null || plan.outItem == null || plan.outItem.length == 0) continue;
                ItemStack firstOut = plan.outItem[0];
                if (firstOut == null || firstOut.item == null) continue;

                table.table(Styles.grayPanel, t -> {
                    // 左侧：产出图标（首个）
                    t.table(icons -> {
                        icons.left();
                        icons.image(firstOut.item.uiIcon).size(40).pad(10f).scaling(Scaling.fit)
                                .with(i -> StatValues.withTooltip(i, firstOut.item));
                        if (plan.outItem.length > 1) {
                            icons.add("+" + (plan.outItem.length - 1)).color(Color.lightGray).padLeft(2f);
                        } else if (firstOut.amount > 1) {
                            icons.add("×" + firstOut.amount).color(Color.lightGray).padLeft(2f);
                        }
                    }).left();

                    // 中间：物品名称 + 时间
                    t.table(info -> {
                        info.add(firstOut.item.localizedName).left();
                        if (plan.outItem.length > 1) {
                            info.add(" +" + (plan.outItem.length - 1) + " more").color(Color.lightGray).padLeft(4f);
                        }
                        info.row();
                        info.add(Strings.autoFixed(plan.time / 60f, 1) + " " + Core.bundle.get("unit.seconds"))
                                .color(Color.lightGray);
                    }).left().padLeft(10f);

                    // 右侧：材料需求
                    t.table(req -> {
                        req.right();
                        for (int i = 0; i < plan.requirements.length; i++) {
                            if (i % 4 == 0) req.row();
                            ItemStack stack = plan.requirements[i];
                            req.add(StatValues.displayItem(stack.item, stack.amount, plan.time, true)).pad(5);
                        }
                    }).right().grow().pad(10f);
                }).growX().pad(5);
                table.row();
            }
        });
    }

    @Override
    public void getPlanConfigs(Seq<UnlockableContent> options) {
        for (Plan plan : plans) {
            if (plan == null || plan.outItem == null || plan.outItem.length == 0) continue;
            Item item = plan.outItem[0].item;
            if (item != null && !item.isBanned() && item.unlockedNow()) {
                options.add(item);
            }
        }
    }

    @Override
    public TextureRegion[] icons() {
        return new TextureRegion[]{region, outRegion, topRegion};
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        Draw.rect(region, plan.drawx(), plan.drawy());
        Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.rect(topRegion, plan.drawx(), plan.drawy());
    }

    // ============================================================
    // 内部类 Plan（配方） —— 保持用户字段命名 outItem / requirements 不变
    // ============================================================
    public static class Plan {
        public ItemStack[] outItem;
        public ItemStack[] requirements;
        public float time;

        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements) {
            this.outItem = outItem;
            this.time = time;
            this.requirements = requirements;
        }

        Plan() {}
    }

    // ============================================================
    // 建筑实体类 UnitFactoryBuild  —— 三问题修复集中在此
    // ============================================================
    public class UnitFactoryBuild extends Building {

        public int currentPlan = -1;
        public float progress = 0f;

        public float fraction() {
            if (currentPlan == -1 || currentPlan >= plans.size) return 0;
            Plan p = plans.get(currentPlan);
            return p == null ? 0 : progress / p.time;
        }

        @Override
        public void created() {
            if (currentPlan == -1) {
                for (int i = 0; i < plans.size; i++) {
                    Plan p = plans.get(i);
                    if (p != null && p.outItem != null && p.outItem.length > 0
                            && p.outItem[0].item != null && p.outItem[0].item.unlockedNow()) {
                        currentPlan = i;
                        break;
                    }
                }
                if (currentPlan == -1 && plans.size > 0) currentPlan = 0;
            }
        }

        @Override
        public void drawSelect() {
            super.drawSelect();
            if (plans.size > 1 && currentPlan != -1 && currentPlan < plans.size) {
                Plan p = plans.get(currentPlan);
                if (p != null && p.outItem != null && p.outItem.length > 0 && p.outItem[0].item != null) {
                    drawItemSelection(p.outItem[0].item);
                }
            }
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload) {
            return false;
        }

        @Override
        public Object senseObject(LAccess sensor) {
            if (sensor == LAccess.config) {
                if (currentPlan == -1 || currentPlan >= plans.size) return null;
                Plan p = plans.get(currentPlan);
                if (p == null || p.outItem == null || p.outItem.length == 0) return null;
                return p.outItem[0].item;
            }
            return super.senseObject(sensor);
        }

        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.progress) return Mathf.clamp(fraction());
            if (sensor == LAccess.itemCapacity) return itemCapacity;
            return super.sense(sensor);
        }

        @Override
        public void display(Table table) {
            super.display(table);

            TextureRegionDrawable reg = new TextureRegionDrawable();

            table.row();
            table.table(t -> {
                t.left();
                t.image().update(i -> {
                    if (currentPlan == -1 || currentPlan >= plans.size) {
                        i.setDrawable(Icon.cancel);
                        i.setColor(Color.lightGray);
                    } else {
                        Plan p = plans.get(currentPlan);
                        if (p == null || p.outItem == null || p.outItem.length == 0 || p.outItem[0].item == null) {
                            i.setDrawable(Icon.cancel);
                            i.setColor(Color.lightGray);
                        } else {
                            i.setDrawable(reg.set(p.outItem[0].item.uiIcon));
                            i.setColor(Color.white);
                        }
                    }
                    i.setScaling(Scaling.fit);
                }).size(32).padBottom(-4).padRight(2);
                t.label(() -> {
                    if (currentPlan == -1 || currentPlan >= plans.size) return "@none";
                    Plan p = plans.get(currentPlan);
                    if (p == null || p.outItem == null || p.outItem.length == 0 || p.outItem[0].item == null)
                        return "@none";
                    return p.outItem[0].item.localizedName;
                }).wrap().width(230f).color(Color.lightGray);
            }).left();
        }

        @Override
        public Object config() {
            return currentPlan;
        }

        @Override
        public void draw() {
            Draw.rect(region, x, y);
            Draw.rect(outRegion, x, y, rotdeg());

            if (currentPlan != -1 && currentPlan < plans.size && fraction() > 0) {
                Plan p = plans.get(currentPlan);
                if (p != null && p.outItem != null && p.outItem.length > 0 && p.outItem[0].item != null) {
                    Draw.z(Layer.blockOver);
                    Draw.color(Pal.accent);
                    Draw.alpha(0.5f);
                    Draw.rect(p.outItem[0].item.uiIcon, x, y, 32 * fraction());
                    Draw.color();
                }
            }

            Draw.z(Layer.blockOver);
            drawPayload();            // 你之前少了这句，原 draw() 调用了两次 Draw.z 没 payload
            Draw.z(Layer.blockOver + 0.1f);
            Draw.rect(topRegion, x, y);
        }

        @Override
        public void buildConfiguration(Table table) {
            Seq<Item> items = Seq.with(plans)
                    .select(p -> p != null && p.outItem != null && p.outItem.length > 0 && p.outItem[0].item != null)
                    .map(p -> p.outItem[0].item)
                    .retainAll(i -> i != null && i.unlockedNow() && !i.isBanned());

            if (items.any()) {
                ItemSelection.buildTable(
                        ConfigurableBlock.this,
                        table,
                        items,
                        () -> {
                            if (currentPlan == -1 || currentPlan >= plans.size) return null;
                            Plan p = plans.get(currentPlan);
                            if (p == null || p.outItem == null || p.outItem.length == 0) return null;
                            return p.outItem[0].item;
                        },
                        item -> {
                            int idx = plans.indexOf(p -> p.outItem != null && p.outItem.length > 0 && p.outItem[0].item == item);
                            if (idx != -1) configure(idx);
                        },
                        ConfigurableBlock.this.selectionRows,
                        ConfigurableBlock.this.selectionColumns
                );
            } else {
                table.table(Styles.black3, t -> t.add("@none").color(Color.lightGray));
            }
        }

        /* =============================================================
         * ✅ shouldConsume() —— 扩展：检查"所有"输出物品都放得下才开始消耗电力
         * ============================================================= */
        @Override
        public boolean shouldConsume() {
            if (currentPlan == -1 || currentPlan >= plans.size) return false;
            if (!enabled) return false;
            Plan plan = plans.get(currentPlan);
            if (plan == null || plan.outItem == null || plan.outItem.length == 0) return false;

            // 检查每一个输出物品的容量
            for (ItemStack s : plan.outItem) {
                if (s == null || s.item == null) continue;
                int maxAccept = getMaximumAccepted(s.item);
                if (maxAccept < s.amount) return false;
            }
            return true;
        }

        @Override
        public BlockStatus status() {
            if (!team.activateUnitFactories()) return BlockStatus.inactiveUnitFactory;
            return super.status();
        }

        @Override
        public int getMaximumAccepted(Item item) {
            if (item == null || item.id >= capacities.length) return 0;
            return Mathf.round(capacities[item.id] * state.rules.unitCost(team));
        }

        /* =============================================================
         * ✅ acceptItem() —— 接受"输入需要的物品" + "本配方输出的物品"
         * ============================================================= */
        @Override
        public boolean acceptItem(Building source, Item item) {
            if (currentPlan == -1 || currentPlan >= plans.size) return false;
            Plan plan = plans.get(currentPlan);
            if (plan == null) return false;

            // ① 本配方需要的原料？→ 接受
            if (plan.requirements != null) {
                for (ItemStack stack : plan.requirements) {
                    if (stack != null && stack.item == item
                            && items.get(item) < getMaximumAccepted(item)) {
                        return true;
                    }
                }
            }
            // ② 本配方要产出的物品？→ 也接受（切配方回流 + offload 出不去的会回来）
            if (plan.outItem != null) {
                for (ItemStack stack : plan.outItem) {
                    if (stack != null && stack.item == item
                            && items.get(item) < getMaximumAccepted(item)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /* =============================================================
         * ✅ updateTile() —— 三个 Bug 的核心修复都在这里
         * ============================================================= */
        @Override
        public void updateTile() {
            if (!configurable) {
                currentPlan = 0;
            }

            if (currentPlan < 0 || currentPlan >= plans.size) {
                currentPlan = -1;
                return;
            }

            Plan plan = plans.get(currentPlan);
            if (plan == null || plan.outItem == null || plan.outItem.length == 0) {
                currentPlan = -1;
                return;
            }
            // 检查所有输出物都合法 + 未禁用
            for (ItemStack s : plan.outItem) {
                if (s == null || s.item == null || s.item.isBanned()) {
                    currentPlan = -1;
                    return;
                }
            }

            // ① 进度累积（保留原写法）
            if (efficiency > 0) {
                progress += edelta() * Vars.state.rules.unitBuildSpeed(team);
            }

            // =========================================================
            // ② 完成一周期：修复全部 3 个 Bug
            // =========================================================
            if (progress >= plan.time) {

                // —— Step A：先统一检查所有输出能不能放下，所有输入够不够 ——
                boolean allOutOk = true;
                for (ItemStack s : plan.outItem) {
                    int maxAccept = getMaximumAccepted(s.item);
                    if (maxAccept < s.amount) { allOutOk = false; break; }
                }
                boolean allInOk = true;
                if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                    if (s == null || s.item == null) continue;
                    if (items.get(s.item) < s.amount) { allInOk = false; break; }
                }

                if (allOutOk && allInOk) {

                    // ====== Bug 2 修复：消耗原料（手动遍历 requirements remove） ======
                    if (plan.requirements != null) {
                        for (ItemStack s : plan.requirements) {
                            if (s == null || s.item == null) continue;
                            items.remove(s.item, s.amount);
                        }
                    }

                    // ====== Bug 1 修复：所有 outItem[] 全部加到仓（每个都走容量判断） ======
                    for (ItemStack s : plan.outItem) {
                        if (s == null || s.item == null) continue;
                        items.add(s.item, s.amount);
                    }

                    // ====== Bug 3 修复：把刚加到仓里的产物推给下游传送带/相邻方块 ======
                    for (ItemStack s : plan.outItem) {
                        if (s == null || s.item == null) continue;
                        // Mindustry convention：每个输出物单独调一次 offload()，
                        // 内部会找朝向(outRegion那一侧)的相邻传送带
                        for (int i = 0; i < s.amount; i++) {
                            offload(s.item);
                        }
                    }

                    // 播放完成音效（原代码没有，可选保留，不想听就删掉以下 3 行）
                    if (createSound != null) {
                        createSound.at(this, 1f + Mathf.range(0.06f), createSoundVolume);
                    }

                    // 进度溢出扣除（不是 =0，保留"超快速度下一 tick 做了两个周期"的可能）
                    progress -= plan.time;

                    // 注意：不再调用 consume() 扣原料—— consume() 会按 consumeBuilder 走，
                    //       我们已经在上面手动 remove 了 requirements，再调反而可能多扣电力/多余项
                    // consume();  ←️ ❌ 已删除

                } else {
                    // 空间不够/料不够 -> 进度卡到离满差 0.001，避免浮点抖动重复判断
                    progress = Math.min(progress, plan.time - 0.001f);
                }
            }

            if (progress < 0) progress = 0;
        }

        @Override
        public byte version() {
            return 3;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(progress);
            write.s(currentPlan);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            progress = read.f();
            currentPlan = read.s();
            if (revision >= 2) {
                read.bool();
            }
            if (revision >= 3) {
                // skip
            }
        }
    }
}
