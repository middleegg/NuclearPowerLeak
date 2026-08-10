
package Npl.content;

import arc.struct.Seq;
import mindustry.type.*;

/**
 * 完全照抄 NewHorizonMod-2.1.2 newhorizon.expand.type.Recipe
 * 本地化改动：
 *   1) 包名改为 Npl.content（原本是 newhorizon.expand.type）
 *   2) 删除 Mindustry159.6 不存在的 PayloadStack / UnlockableContent（没有 getPayloads 体系）
 *   3) 保留原 NH 字段名：inputItem / inputLiquid / outputItem / outputLiquid / craftTime / priority
 *   4) 新增 with(...) 静态辅助（给 NuBlocks 里 `inItem(with(Items.copper,5,Items.lead,3))` 这种写法用）
 *   5) 新增链式 inItem/inLiquid/outItem/outLiquid/addInput/addOutput 重载（兼容我们之前的 NuFactory.sl）
 * 其余字段/构造逻辑一字不动照抄 NH。
 */
public class Recipe {

    public static Recipe empty = new Recipe();

    public Seq<ItemStack>   inputItem    = new Seq<>();
    public Seq<LiquidStack> inputLiquid  = new Seq<>();

    public Seq<ItemStack>   outputItem   = new Seq<>();
    public Seq<LiquidStack> outputLiquid = new Seq<>();

    public float craftTime = 60f;
    public int   priority  = 0;

    // ================================================================
    // ↓↓↓ 完全照抄 NH 原版 Recipe(Object... objects) 可变参数构造 ↓↓↓
    // 唯一改动：删除 UnlockableContent -> PayloadStack 分支
    // ================================================================
    public Recipe(Object... objects) {
        for (int i = 0; i < objects.length / 2; i++) {
            if (objects[i * 2] instanceof Item item && objects[i * 2 + 1] instanceof Integer count) {
                inputItem.add(new ItemStack(item, count));
            } else if (objects[i * 2] instanceof Liquid liquid && objects[i * 2 + 1] instanceof Float count) {
                inputLiquid.add(new LiquidStack(liquid, count));
            } else if (objects[i * 2] instanceof Liquid liquid && objects[i * 2 + 1] instanceof Integer count) {
                // 兼容 int -> float 的液体数量（NH 原版只接受 Float，我们加一个兼容）
                inputLiquid.add(new LiquidStack(liquid, count.floatValue()));
            }
        }
    }

    public Recipe() {}

    // ================================================================
    // ↓↓↓ 本地化新增：静态辅助 with(...) 给 NuBlocks 注册用 ↓↓↓
    // ================================================================
    public static ItemStack[] with(Object... pairs) {
        if (pairs == null || pairs.length == 0) return new ItemStack[0];
        int n = pairs.length / 2;
        ItemStack[] out = new ItemStack[n];
        for (int i = 0; i < n; i++) {
            Object k = pairs[i * 2];
            Object v = pairs[i * 2 + 1];
            if (k instanceof Item it && v instanceof Integer amt) {
                out[i] = new ItemStack(it, amt);
            } else if (k instanceof Item it && v instanceof Number amt) {
                out[i] = new ItemStack(it, amt.intValue());
            } else {
                out[i] = null;
            }
        }
        return out;
    }

    // ================================================================
    // ↓↓↓ 本地化新增：链式 API（兼容 NuFactory.sl 的 addInput/addOutput 写法）
    // 名字完全保留之前的写法：inItem/outItem/inLiquid/outLiquid/addInput/addOutput
    // 支持 4 类入参：(Item,int) / (ItemStack[]) / (ItemStack单个) / (Seq<ItemStack>)
    // ================================================================
    public Recipe time(float craftTime) { this.craftTime = craftTime; return this; }
    public Recipe prio(int p)           { this.priority  = p;        return this; }

    // —— 单参 (Item, int) ——
    public Recipe inItem(Item item, int amount) {
        if (item != null && amount > 0) this.inputItem.add(new ItemStack(item, amount));
        return this;
    }
    public Recipe inLiquid(Liquid liq, float amount) {
        if (liq != null && amount > 0f) this.inputLiquid.add(new LiquidStack(liq, amount));
        return this;
    }
    public Recipe outItem(Item item, int amount) {
        if (item != null && amount > 0) this.outputItem.add(new ItemStack(item, amount));
        return this;
    }
    public Recipe outLiquid(Liquid liq, float amount) {
        if (liq != null && amount > 0f) this.outputLiquid.add(new LiquidStack(liq, amount));
        return this;
    }

    // —— ItemStack[] / LiquidStack[] 数组版（varargs）——
    public Recipe inItem(ItemStack... stacks) {
        if (stacks != null) for (ItemStack s : stacks)
            if (s != null && s.item != null && s.amount > 0) this.inputItem.add(s);
        return this;
    }
    public Recipe outItem(ItemStack... stacks) {
        if (stacks != null) for (ItemStack s : stacks)
            if (s != null && s.item != null && s.amount > 0) this.outputItem.add(s);
        return this;
    }
    public Recipe inLiquid(LiquidStack... stacks) {
        if (stacks != null) for (LiquidStack s : stacks)
            if (s != null && s.liquid != null && s.amount > 0f) this.inputLiquid.add(s);
        return this;
    }
    public Recipe outLiquid(LiquidStack... stacks) {
        if (stacks != null) for (LiquidStack s : stacks)
            if (s != null && s.liquid != null && s.amount > 0f) this.outputLiquid.add(s);
        return this;
    }

    // —— 单个 ItemStack / LiquidStack 版（addInput/addOutput 同义词）——
    public Recipe addInput(ItemStack stack) {
        if (stack != null && stack.item != null && stack.amount > 0) this.inputItem.add(stack);
        return this;
    }
    public Recipe addOutput(ItemStack stack) {
        if (stack != null && stack.item != null && stack.amount > 0) this.outputItem.add(stack);
        return this;
    }
    public Recipe addInput(LiquidStack stack) {
        if (stack != null && stack.liquid != null && stack.amount > 0f) this.inputLiquid.add(stack);
        return this;
    }
    public Recipe addOutput(LiquidStack stack) {
        if (stack != null && stack.liquid != null && stack.amount > 0f) this.outputLiquid.add(stack);
        return this;
    }

    // —— Seq<ItemStack> / Seq<LiquidStack> 版——
    public Recipe inItem(Seq<ItemStack> stacks) {
        if (stacks != null) for (ItemStack s : stacks)
            if (s != null && s.item != null && s.amount > 0) this.inputItem.add(s);
        return this;
    }
    public Recipe outItem(Seq<ItemStack> stacks) {
        if (stacks != null) for (ItemStack s : stacks)
            if (s != null && s.item != null && s.amount > 0) this.outputItem.add(s);
        return this;
    }
    public Recipe inLiquid(Seq<LiquidStack> stacks) {
        if (stacks != null) for (LiquidStack s : stacks)
            if (s != null && s.liquid != null && s.amount > 0f) this.inputLiquid.add(s);
        return this;
    }
    public Recipe outLiquid(Seq<LiquidStack> stacks) {
        if (stacks != null) for (LiquidStack s : stacks)
            if (s != null && s.liquid != null && s.amount > 0f) this.outputLiquid.add(s);
        return this;
    }

    // ================================================================
    // ↓↓↓ 本地化新增：查询 API（null 防护）
    // ================================================================
    public boolean hasAnyOutput() {
        if (outputItem   != null) for (ItemStack s   : outputItem)   if (s != null && s.amount > 0) return true;
        if (outputLiquid != null) for (LiquidStack s : outputLiquid) if (s != null && s.amount > 0.0001f) return true;
        return false;
    }

    public boolean isUnlocked() {
        if (outputItem != null) for (ItemStack s : outputItem) {
            if (s == null || s.item == null) continue;
            if (s.item.isBanned() || !s.item.unlockedNow()) return false;
        }
        if (outputLiquid != null) for (LiquidStack s : outputLiquid) {
            if (s == null || s.liquid == null) continue;
            if (s.liquid.isBanned() || !s.liquid.unlockedNow()) return false;
        }
        return true;
    }
}
