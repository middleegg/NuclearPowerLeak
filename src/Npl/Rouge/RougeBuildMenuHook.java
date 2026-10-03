package Npl.Rouge;

import arc.*;
import arc.graphics.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType;
import mindustry.type.Category;
import mindustry.ui.*;

import java.lang.reflect.*;

import static mindustry.Vars.*;

/**
 * RougeBuildMenuHook —— 在建造面板左侧新增一个标签，点击打开 Rouge 外场演绎界面
 * ==============================================================
 * 仅在 Rouge 模式下显示该标签
 */
public class RougeBuildMenuHook {

    public static Category ROUGE_CATEGORY;

    private static boolean hooked = false;
    private static Table categoryTable = null;

    public static void init(){
        try {
            addRougeCategory();
        } catch (Exception e) {
            Log.err("[RougeBuildMenuHook] Failed to add category", e);
        }
    }

    private static void addRougeCategory() throws Exception {
        if (ROUGE_CATEGORY != null) return;

        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);

        ROUGE_CATEGORY = (Category) unsafe.allocateInstance(Category.class);

        Field nameField = Enum.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(ROUGE_CATEGORY, "rouge");

        Field ordinalField = Enum.class.getDeclaredField("ordinal");
        ordinalField.setAccessible(true);
        ordinalField.setInt(ROUGE_CATEGORY, Category.all.length);

        Field allField = Category.class.getDeclaredField("all");
        allField.setAccessible(true);
        Category[] oldAll = (Category[]) allField.get(null);
        Category[] newAll = new Category[oldAll.length + 1];
        System.arraycopy(oldAll, 0, newAll, 0, oldAll.length);
        newAll[oldAll.length] = ROUGE_CATEGORY;
        allField.set(null, newAll);

        Log.info("[RougeBuildMenuHook] Category 'rouge' registered, ordinal=" + ROUGE_CATEGORY.ordinal());
    }

    public static void hookPlacementFragment(){
        if (hooked) return;
        hooked = true;

        Events.on(EventType.WorldLoadEvent.class, event -> {
            Time.run(30f, () -> updateCategoryTab());
        });

        Time.run(60f, RougeBuildMenuHook::updateCategoryTab);
    }

    private static void updateCategoryTab(){
        try {
            if (!RougeSave.isInRougeMode) {
                removeRougeButton();
                return;
            }

            Group hudGroup = ui.hudGroup;
            if (hudGroup == null) return;

            findAndAddCategoryButton(hudGroup);
        } catch (Exception e) {
            Log.err("[RougeBuildMenuHook] Failed to update category tab", e);
        }
    }

    private static void removeRougeButton(){
        if (categoryTable != null) {
            for (Element child : categoryTable.getChildren()) {
                if ("rouge-category-tab".equals(child.name)) {
                    child.remove();
                    break;
                }
            }
        }
    }

    private static void findAndAddCategoryButton(Group group){
        for (Element elem : group.getChildren()) {
            if (elem instanceof Table) {
                Table table = (Table) elem;
                if ("blockCatTable".equals(table.name)) {
                    categoryTable = table;
                    addRougeButtonToCategoryTable(table);
                    return;
                }
            }
            if (elem instanceof Group) {
                findAndAddCategoryButton((Group) elem);
            }
        }
    }

    private static void addRougeButtonToCategoryTable(Table catTable){
        for (Element child : catTable.getChildren()) {
            if ("rouge-category-tab".equals(child.name)) return;
        }

        catTable.row();
        if (RougeNet.isClient()) {
            catTable.button("[gray]ROUGE[]", Styles.defaultt, () -> {
                RougeTechTree.showReadOnly();
            }).size(80, 40).pad(2).name("rouge-category-tab").color(Color.gray);
        } else {
            catTable.button("[#FF4444]ROUGE[]", Styles.defaultt, () -> {
                RougeTechTree.show();
            }).size(80, 40).pad(2).name("rouge-category-tab");
        }

        Log.info("[RougeBuildMenuHook] ROUGE category tab added (host=" + RougeNet.isHost() + ", client=" + RougeNet.isClient() + ")");
    }
}
