
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

    // ————————— 链式 API（加 null 防护） —————————
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

    // ————————— 查询 API（加 null 防护）—————————
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
