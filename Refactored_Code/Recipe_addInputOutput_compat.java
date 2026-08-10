/*
 * Recipe.java —— 与 NuFactory.sl:80 的真实调用方式兼容版
 *
 * ⚠️ 关键发现：用户 src/Npl/content/NuFactory.sl 第 80、81 行真实的 Recipe 调用是：
 *     recipe1.addInput(new ItemStack(copper, 10));
 *     recipe1.addOutput(new ItemStack(lead, 5));
 *   而我们之前写的链式 API 叫 .inItem()/.outItem()，**名字对不上，编译报错**！
 *
 * ⚠️ 所以这个版本在保持链式 API 不变的同时，加上 NH 风格的 4 个方法：
 *     addInput(ItemStack)   /  addInput(LiquidStack)
 *     addOutput(ItemStack)  /  addOutput(LiquidStack)
 *   保证 NuFactory.sl 那两行 100% 能编译。
 *
 * ⚠️ 其他所有链式/构造/查询 API 100% 保留之前空安全的写法不变。
 */
package Npl.content;

import arc.struct.Seq;
import mindustry.type.*;

public class Recipe {

    public static final Recipe empty = new Recipe();

    public Seq<ItemStack>   inputItem    = new Seq<>();
    public Seq<LiquidStack> inputLiquid  = new Seq<>();

    public Seq<ItemStack>   outputItem   = new Seq<>();
    public Seq<LiquidStack> outputLiquid = new Seq<>();

    public float craftTime = 60f;
    public int   priority  = 0;

    // —— 空构造 ——
    public Recipe() {}

    // —— 显式构造 ——
    public Recipe(float craftTime,
                  Seq<ItemStack> inputItem,    Seq<LiquidStack> inputLiquid,
                  Seq<ItemStack> outputItem,   Seq<LiquidStack> outputLiquid) {
        this.craftTime    = craftTime;
        if (inputItem    != null) this.inputItem    .addAll(inputItem);
        if (inputLiquid  != null) this.inputLiquid  .addAll(inputLiquid);
        if (outputItem   != null) this.outputItem   .addAll(outputItem);
        if (outputLiquid != null) this.outputLiquid .addAll(outputLiquid);
    }

    // —— 仿 NH 的可变参数构造 ——
    public Recipe(Object... pairs) {
        if (pairs == null) return;
        for (int i = 0; i < pairs.length / 2; i++) {
            Object key   = pairs[i * 2];
            Object value = pairs[i * 2 + 1];
            if (key instanceof Item it && value instanceof Integer cnt) {
                inputItem.add(new ItemStack(it, cnt));
            } else if (key instanceof Liquid liq && value instanceof Float cnt) {
                inputLiquid.add(new LiquidStack(liq, cnt));
            } else if (key instanceof Liquid liq && value instanceof Integer cnt) {
                inputLiquid.add(new LiquidStack(liq, cnt.floatValue()));
            }
        }
    }

    // ================================================================
    //  ✅ NewHorizon 风格 4 个 addInput / addOutput
    //     （对应 NuFactory.sl:80 recipe1.addInput(new ItemStack(...)) 的真实调用）
    // ================================================================
    public Recipe addInput(ItemStack stack) {
        if (stack != null && stack.item != null && stack.amount > 0) this.inputItem.add(stack);
        return this;
    }
    public Recipe addInput(LiquidStack stack) {
        if (stack != null && stack.liquid != null && stack.amount > 0f) this.inputLiquid.add(stack);
        return this;
    }
    public Recipe addOutput(ItemStack stack) {
        if (stack != null && stack.item != null && stack.amount > 0) this.outputItem.add(stack);
        return this;
    }
    public Recipe addOutput(LiquidStack stack) {
        if (stack != null && stack.liquid != null && stack.amount > 0f) this.outputLiquid.add(stack);
        return this;
    }

    // ================================================================
    //  链式 API（保留用户习惯的 inItem/outItem）
    // ================================================================
    public Recipe time(float craftTime) { this.craftTime = craftTime; return this; }
    public Recipe prio(int p)           { this.priority  = p;        return this; }

    public Recipe inItem(Item item, int amount) {
        if (item == null || amount <= 0) return this;
        this.inputItem.add(new ItemStack(item, amount));
        return this;
    }
    public Recipe inLiquid(Liquid liq, float amount) {
        if (liq == null || amount <= 0f) return this;
        this.inputLiquid.add(new LiquidStack(liq, amount));
        return this;
    }
    public Recipe outItem(Item item, int amount) {
        if (item == null || amount <= 0) return this;
        this.outputItem.add(new ItemStack(item, amount));
        return this;
    }
    public Recipe outLiquid(Liquid liq, float amount) {
        if (liq == null || amount <= 0f) return this;
        this.outputLiquid.add(new LiquidStack(liq, amount));
        return this;
    }

    // ================================================================
    //  查询 API（空安全）
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
