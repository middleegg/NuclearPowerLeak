//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。
package Npl.content;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.Items.*;
import mindustry.type.Item;
import Npl.newSth.NewItemsType;
import Npl.newSth.Type.coins;

public class NuItems {
    // 1. 声明你的自定义物品（类型使用 NewItemsType）
    //生铁，T型煤，硫芯，单晶体，污凝体，磁铁，脆纶，气浮石，原胶，棱能晶，胶种，碱银，橡胶，铀晶，铊，圣铁，原铀，塑源，铊化物，铀，瓶装磁暴
    public static NewItemsType
    bigIron,Tcoal,sulFurFrag,monoSiliCrystal,dirtyCoagulum,magent,frailPolyester,pumice,oriRubber,prismCrystal,rubberFrag,
    alkSliver,rubber,uranCrystal,thallium,sacredIron,oriUranium,remakeSource,thallide,uranium,bottledMagenticStorm;
    public static Item
    graphite,sand,pyratite;
    public static coins coinsItem;
    public static void load() {
        bigIron= new NewItemsType("bigIron",Color.valueOf("7e7e7e")){{
            hardness=1;
            cost=1.3f;
            alwaysUnlocked = true;
            healthScaling=0.01f;
            reversible = 0.6f;
        }};
        Tcoal = new NewItemsType("Tcoal", Color.valueOf("9c9480")){{
            hardness = 2;
            alwaysUnlocked = false;
            flammability = 1.7f;
            explosiveness = 0.2f;
        }};
        sulFurFrag = new NewItemsType("sulFurFrag", Color.valueOf("b44632")) {{
            hardness = 2;
            alwaysUnlocked = false;
            cost = 0.3f;
            radioactivity = 0.6f;
            flammability = 1.0f;
            explosiveness = 1.5f;// 设置自定义字段
        }};
        monoSiliCrystal = new NewItemsType("monoSiliCrystal", Color.valueOf("616161")) {{
            alwaysUnlocked = false;
            cost = 0.7f;
        }};
        dirtyCoagulum = new NewItemsType("dirtyCoagulum",Color.valueOf("B3B0FF")){{
            alwaysUnlocked = false;
            flammability = 2.5f;
            explosiveness = 0.6f;
        }};
        magent = new NewItemsType("magent",Color.valueOf("b42828")){{
            alwaysUnlocked = false;
            cost = 0.6f;
            reversible = 0.9f;
            magentic = 0.6f;
            charge = 0.7f;
        }};
        frailPolyester = new NewItemsType("frailPolyester",Color.valueOf("00b1ff")){{
            alwaysUnlocked = false;
            cost = 0.6f;
            hardness = 1;
            flammability = 0.4f;
            charge = 0.3f;
            reversible = 1.4f;
        }};
        pumice = new NewItemsType("pumice",Color.valueOf("c8c8c8")){{
            alwaysUnlocked = false;
            cost = 1.3f;
            hardness = 3;
            charge = 0.6f;
            reversible = 1.8f;
        }};
        oriRubber = new NewItemsType("oriRubber",Color.valueOf("956f4e")){{
            alwaysUnlocked = false;
            cost = 1f;
            hardness = 3;
            flammability = 2.35f;
        }};
        prismCrystal = new NewItemsType("prismCrysstal",Color.valueOf("E1FFFB")){{
            alwaysUnlocked = false;
            cost = 1f;
            hardness = 4;
            reversible = 1.5f;
            charge = 1.3f;
        }};
        rubberFrag = new NewItemsType("rubberFrag",Color.valueOf("956f4e")){{
            alwaysUnlocked = false;
            cost = 1f;
            flammability = 1.45f;
        }};
        alkSliver = new NewItemsType("alkSliver",Color.valueOf("e6e6e6")){{
            alwaysUnlocked = false;
            cost = 0.8f;
            stability = 1f;
        }};
        rubber = new NewItemsType("rubber",Color.valueOf("432f1f")){{
            alwaysUnlocked = false;
            cost = 0.9f;
            flammability = 2.0f;
        }};
        uranCrystal = new NewItemsType("uranCrystal",Color.valueOf("9adba1")){{
            alwaysUnlocked = false;
            cost = 0.8f;
            radioactivity = 0.9f;
            explosiveness = 0.4f;
        }};
        thallium = new NewItemsType("thallium",Color.valueOf("C98FFF")){{
            alwaysUnlocked = false;
            hardness = 4;
            cost = 0.8f;
            radioactivity = 0.9f;
            stability = 0.6f;
        }};
        sacredIron = new NewItemsType("sacredIron",Color.valueOf("DEFFFB")){{
            cost = 0.8f;
            charge = 1.4f;
        }};
        oriUranium = new NewItemsType("oriUranium",Color.valueOf("50826e")){{
            alwaysUnlocked = false;
            cost = 0.5f;
            radioactivity= 1.2f;
            explosiveness=0.6f;
        }};
        remakeSource = new NewItemsType("remakeSource",Color.valueOf("FFEA8F")){{
            reversible = 4f;
            stability = 1f;
            radioactivity=2.4f;
            explosiveness=0.1f;
            charge = 1.4f;
        }};
        thallide = new NewItemsType("thallide",Color.valueOf("9a7da1")){{
            alwaysUnlocked = false;
            reversible = 2f;
            stability = 0.2f;
            radioactivity=0.8f;
            explosiveness=0.45f;
        }};
        uranium = new NewItemsType("uranium",Color.valueOf("3D6E70")){{
            alwaysUnlocked = false;
            cost = 0.5f;
            radioactivity= 1.7f;
            explosiveness=1f;
        }};
        bottledMagenticStorm = new NewItemsType("bottledMagenticStorm",Color.valueOf("c0ecff")){{
            alwaysUnlocked = false;
            reversible = 2f;
            stability = -0.2f;
            radioactivity=1.6f;
            charge=3f;
            explosiveness=5f;
            magentic=3f;
        }};
        sand = mindustry.content.Items.sand;
        graphite = mindustry.content.Items.graphite;
        pyratite = mindustry.content.Items.pyratite;

        // 自定义货币 coins
        coinsItem = new coins("coins", Color.valueOf("FFD700")){{
            frames = 8;
        }};

        // 如果你以后需要像 NH 那样添加物品到特定分类，参考：
        // mindustry.content.Items.serpuloItems.addAll(/* your items */);
    }
}