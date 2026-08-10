/*
 * 属于你自己的 Recipe 配方类（放在 Npl.newSth 包下，完全不依赖 NewHorizon）
 *
 * ⚠️ 字段命名和 NewHorizon Recipe.java 100% 对齐：
 *     inputItem    / inputLiquid    （输入物品 / 液体）
 *     outputItem   / outputLiquid   （输出物品 / 液体）
 *     craftTime    （单配方耗时，单位 tick，60 tick = 1 秒）
 *     priority     （配方优先级，保留字段，手动切配方模式下一般不用）
 *
 * ⚠️ 为什么不用 NewHorizon 的 Object... 可变参数构造函数？
 *     因为在 Java 里 "Item + Integer" 配对很容易因类型擦除写错运行时崩；
 *     这里改成「显式链式 add」更安全更清晰，也提供 Object... 构造函数仿 NH 风格。
 *
 * 以后想扩展 Payload（单位/方块载荷），只要把 NewHorizon 那 6 个 PayloadStack 字段加进来就行。
 */
package Npl.newSth;

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

    // ========================= 构造函数（3 种） =========================

    /** 空构造（仅给 empty 常量用） */
    public Recipe() {}

    /** 显式参数构造（最安全推荐） */
    public Recipe(float craftTime,
                  Seq<ItemStack> inputItem,    Seq<LiquidStack> inputLiquid,
                  Seq<ItemStack> outputItem,   Seq<LiquidStack> outputLiquid) {
        this.craftTime    = craftTime;
        if (inputItem    != null) this.inputItem    .addAll(inputItem);
        if (inputLiquid  != null) this.inputLiquid  .addAll(inputLiquid);
        if (outputItem   != null) this.outputItem   .addAll(outputItem);
        if (outputLiquid != null) this.outputLiquid .addAll(outputLiquid);
    }

    /**
     * 仿 NewHorizon 风格的可变参数构造：
     *   - 偶数位: Item / Liquid
     *   - 奇数位: Integer(Item 数量) / Float(Liquid 数量)
     *   - 默认 60 tick 完成，要改 craftTime 请在后面 .time(xx)
     *
     * 例子：
     *   new Recipe(Items.copper, 5, Items.lead, 2)              → 5 铜 + 2 铅 输入
     *   new Recipe(Liquids.water, 3f, Items.sand, 1).time(120) → 3 水 + 1 沙 输入，120 tick
     * ⚠️ 注意：这种构造只加 "inputs"，outputs 请用链式 outItem() / outLiquid() 追加。
     */
    public Recipe(Object... pairs) {
        for (int i = 0; i < pairs.length / 2; i++) {
            Object key   = pairs[i * 2];
            Object value = pairs[i * 2 + 1];
            if (key instanceof Item it && value instanceof Integer cnt) {
                inputItem.add(new ItemStack(it, cnt));
            } else if (key instanceof Liquid liq && value instanceof Float cnt) {
                inputLiquid.add(new LiquidStack(liq, cnt));
            }
            // 如果 (Liquid, Integer) 传了 (Liquids.water, 3)，自动转 float，避免用户写 3f 麻烦
            else if (key instanceof Liquid liq && value instanceof Integer cnt) {
                inputLiquid.add(new LiquidStack(liq, cnt.floatValue()));
            }
        }
    }

    // ========================= 链式 API（推荐写法） =========================

    public Recipe time(float craftTime) { this.craftTime = craftTime; return this; }
    public Recipe prio(int p)           { this.priority  = p;        return this; }

    public Recipe inItem(Item item, int amount) {
        this.inputItem.add(new ItemStack(item, amount)); return this;
    }
    public Recipe inLiquid(Liquid liq, float amount) {
        this.inputLiquid.add(new LiquidStack(liq, amount)); return this;
    }

    public Recipe outItem(Item item, int amount) {
        this.outputItem.add(new ItemStack(item, amount)); return this;
    }
    public Recipe outLiquid(Liquid liq, float amount) {
        this.outputLiquid.add(new LiquidStack(liq, amount)); return this;
    }

    // ========================= 便捷查询（RecipeCrafter 内部用） =========================

    /** 配方是否「至少有一个输出」？空输出的配方不该生产。 */
    public boolean hasAnyOutput() {
        return (outputItem   != null && outputItem.any())
            || (outputLiquid != null && outputLiquid.any());
    }

    /** 配方是否「有解锁 + 没禁用」？用在 created() 里找默认配方。 */
    public boolean isUnlocked() {
        if (outputItem != null) for (ItemStack s : outputItem) {
            if (s.item == null || s.item.isBanned() || !s.item.unlockedNow()) return false;
        }
        return true;
    }
}
