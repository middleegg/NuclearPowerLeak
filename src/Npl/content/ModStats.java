package Npl.content;

import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;

public class ModStats {
    // 定义你的自定义 Stat
    public static final Stat Reversible = new Stat("reversible", StatCat.general);
    public static final Stat Magentic = new Stat("magentic", StatCat.general);
    public static final Stat Stability = new Stat("stability", StatCat.general);
    public static final Stat Recipe = new Stat("recipe", StatCat.general);
    public static final Stat modeCount = new Stat("modeCount", StatCat.general);
    // 如果你以后想添加更多自定义属性，在这里继续加：
    // public static final Stat MAGIC_POWER = new Stat("magic-power", StatCat.general);
}