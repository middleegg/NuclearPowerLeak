package Npl.events;

import arc.*;
import arc.util.*;
import Npl.*;
import arc.scene.Element;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import mindustry.ui.dialogs.*;
import mindustry.content.*;
import Npl.content.*;
import Npl.content.envBlocks;
import Npl.newSth.NewItemsType;
import Npl.newSth.Type.*;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.gen.Unit;
import mindustry.ui.*;
import mindustry.type.Item;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.core.GameState;
import static mindustry.Vars.*;

/**
 * groundZero 战役地图 · 敌方单位死亡一次性触发事件。
 *
 * <p>会话级一次性：每次进入 groundZero 只会触发一次；退出地图后 flag 重置，
 * 下次再进该地图仍可再触发一次。
 */
public class OneEvent {

    // ====================== 静态字段区 ======================

    /** 一次性开关：进入 groundZero 后触发过一次就置 true，后续全短路。 */
    private static volatile boolean gzDeathFired = false;

    /** 目标预设地图名（groundZero = 原版「零点」战役地图）。 */
    private static final String TARGET_PRESET = "frozenForest";

    /** 防止 OneEvent.load() 被多次调用时重复注册 Events。 */
    private static boolean loaded = false;

    // ====================== 对外入口 ======================

    /**
     * nu.loadContent() 统一调用：注册 Events 监听。
     * 带幂等保护，重复调用无害。
     */
    public static void load() {
        if (loaded) return;       // 已加载过 → 直接返回，防止重复挂监听器
        loaded = true;            // 标记：已经加载过
        new OneEvent();           // 构造器里做真正的 Events.on 注册
    }

    // ====================== 构造器（事件注册）======================

    /** 构造器里挂 Events 监听；配合 loaded flag 保证只挂一次。 */
    public OneEvent() {

        // ------------------------------------------------------------------
        // 监听 1：单位受伤事件 → 用来捕获「敌方单位在 groundZero 死亡」的那一帧
        // 说明：Mindustry v159 没有 UnitDestroyEvent，只能靠 UnitDamageEvent
        //       里判断 unit.dead 来捕获死亡（死亡当帧 damaged + dead=true）。
        // ------------------------------------------------------------------
        Events.on(UnitDamageEvent.class, e -> {

            // ---- 归属层判断（越便宜的判断放越前面，减少浪费）----

            //if (gzDeathFired) return;
            // ↑ ① 会话级一次性自封：触发过一次后，以后所有事件都在这一行直接短路，几乎零开销。
            if (e == null || e.unit == null) return;
            // ↑ ② 空值防御（极少数极端 tick 回调里 e 或 e.unit 可能为 null）。
            if (state.rules.sector == null) return;
            // ↑ ③ 必须是战役 sector 地图（沙盒/编辑器/服务器自定义地图不会有 sector）。
            if (state.rules.sector.preset == null) return;
            // ↑ ④ 必须有关联的预设（groundZero 作为原版战役预设一定有）。
            if (!TARGET_PRESET.equals(state.rules.sector.preset.name)) return;
            // ↑ ⑤ 精确匹配预设名 = "groundZero"，只在零点地图生效。
            if (e.unit.team != state.rules.waveTeam) return;
            // ↑ ⑥ 必须是敌方单位：team 等于波次规则里配置的敌人 team。
            if (!e.unit.dead) return;
            // ↑ ⑦ 死亡当帧捕获：damage 事件 99% 时单位还活着，dead=false，直接跳过。
            // ---- 以上 7 层全部通过 → 第一次命中目标事件 ----
            //gzDeathFired = true;
            // ↑ 先锁死 flag，避免 onGroundZeroEnemyDeath() 内部逻辑耗时期间又被重复触发。
            onGroundZeroEnemyDeath(e.unit);
            // ↑ 回调具体业务逻辑（参数 deadEnemy 是刚死掉的敌方单位对象）。
        });
        // ------------------------------------------------------------------
        // 监听 2：游戏状态切换 → 重置一次性 flag（保证会话级语义）
        // 当玩家从「正在玩地图（playing）」切换到任何非 playing 状态
        // （主菜单 / 加载 / 失败画面 / 胜利画面），就把 fired 还原为 false，
        // 下次再进入 groundZero 时仍能再触发一次。
        // ------------------------------------------------------------------
        Events.on(StateChangeEvent.class, e -> {
            if (e.from == mindustry.core.GameState.State.playing
                    && e.to   != mindustry.core.GameState.State.playing) {
                gzDeathFired = false;
            }
        });
    }
    // ====================== 回调：具体业务逻辑 ======================

    /**
     * groundZero 里有敌方单位阵亡时被调用（会话内一次）。
     *
     * @param deadEnemy 刚死掉的敌方单位；可拿 .type / .killer / .x / .y 等上下文
     */
    private static void onGroundZeroEnemyDeath(Unit deadEnemy) {

        // 示例：死掉的单位是 dagger（匕首）时，弹出视觉小说对话框
        if (deadEnemy.type == UnitTypes.mace) {
            // ★ 暂停游戏：记录当前是否已暂停，关闭对话框时据此恢复
            dialogPausedGame = state.isPaused();
            state.set(GameState.State.paused);

            // 命名内部类：构造器内已经处理 show() + setSize/Position + 显示 page 0
            //   → 直接 new 就会显示，不用再 scene.add()
            new DaggerDialog();
        }
        // TODO 在这里写你想做的事，例如：
        //   - 扣资源/加资源：Items.remove(Items.copper, 100);
        //   - 发公告：Call.sendMessage(...) 或 Vars.ui.hudfrag.showToast(...)
        //   - 生成特效：Fx.nuclearcloud.at(deadEnemy.x, deadEnemy.y)
        //   - 胜利/失败：调用 Vars.state.rules.win() / lose()
        //
        // 可拿的上下文：
        //   deadEnemy.type       → 死的是什么单位
        //   deadEnemy.killer     → 是谁杀的（Unitc，可能是玩家单位/建筑/turret，null 表示非战斗死亡）
        //   deadEnemy.x, .y      → 死亡坐标
    }

    // ====================== 命名内部类：dagger 死亡 → 多页视觉小说对话框 ======================

    /** 对话框打开前游戏是否已暂停（关闭时据此决定是否恢复）。 */
    private static boolean dialogPausedGame = false;

    /**
     * 视觉小说风格对话框（支持多页/分支切换）。
     *
     * <p>使用：
     * <pre>
     *   new DaggerDialog();   // 直接 new 即显示（构造器里调了 show()）
     * </pre>
     *
     * <p>页面结构：
     * <ul>
     *   <li>Page 0 — 初始对话（角色名+文本+3 选项）
     *   <li>Page 1 — 选了「帝国已经忘记了…」后的分支
     *   <li>Page 2 — 选了「我只是想找个…」后的分支
     *   <li>Page 3 — 选了「私人原因…」后的分支
     * </ul>
     * 每个分支点完「继续」后自动关对话框并恢复游戏。
     * 想扩展更多页/更复杂的对话树，在 showPage() 里加 case 即可。
     *
     * <p>想改大小/位置：直接改构造器里 POPUP_W / POPUP_H / setPosition(...)
     * <p>想改角色图：改 buildCharImg() 里的 Icon.box → 你自己的 atlas sprite
     */
    public static class DaggerDialog extends Dialog {

        // ===== 可自定义尺寸（写在类里，showPage 里多处都要引用）=====
        /** 对话框宽度（像素） */
        private static final float POPUP_W = 700f;
        /** 对话框高度（像素） */
        private static final float POPUP_H = 520f;
        /** 左侧角色图片边长（像素） */
        private static final float CHAR_SIZE = 180f;

        // ===== 🎨 背景图接口：你画完图之后只改下面这 1-3 行即可 =====
        //
        // 用法（三步走）：
        //   1) 把 PNG 放到：D:\NuclearPowerLeak\assets\sprites\对话框背景.png
        //   2) BG_SPRITE 改成图片文件名（不带 .png 后缀），例如 "对话框背景"
        //   3) 如果是九宫格边框图（四角不变形），就把 BG_9PATCH 改成 true，并填 4 个边距
        //      如果是整张平铺/拉伸的普通背景图，BG_9PATCH 保持 false
        //
        // 例（普通背景，整张拉伸）：
        //   BG_SPRITE  = "my_dialog_bg";
        //   BG_9PATCH  = false;
        //
        // 例（九宫格边框，四角保护 12px）：
        //   BG_SPRITE        = "my_dialog_frame";
        //   BG_9PATCH        = true;
        //   BG_9PATCH_PAD    = new int[]{12, 12, 12, 12}; // {左, 右, 上, 下} 像素

        /** 背景图在 atlas 中的名字（= sprites 下的文件名，不含 .png）。
         *  null 或 "" 表示不换背景，继续使用 Mindustry 原版 Dialog 的灰色窗体。 */
        private static final String BG_SPRITE = null;

        /** 是否使用九宫格拉伸（推荐用于带装饰性边框的背景图）。 */
        private static final boolean BG_9PATCH = false;

        /** 九宫格边距 {左, 右, 上, 下}，单位像素。仅 BG_9PATCH=true 时生效。 */
        private static final int[] BG_9PATCH_PAD = {12, 12, 12, 12};

        // ========================================================================
        // 构造器：创建 → show → 锁定尺寸/位置 → 翻到 page 0
        // ========================================================================
        public DaggerDialog() {
            // 标题给空串（我们自己在页面里写角色名，不需要 Dialog 默认的标题栏）
            super("");

            // ===== 🎨 应用自定义背景图（接口：BG_SPRITE / BG_9PATCH / BG_9PATCH_PAD）=====
            //   BG_SPRITE 为 null 或找不到 → 不改动，保持原 Dialog 的灰色窗体
            //   BG_SPRITE 配置且能找到 → 替换成你自己的背景
            applyCustomBackground();

            // ★ 禁用 fillParent：防止 Dialog 自动 resize 成屏幕大小
            this.setFillParent(false);

            // ★ 必须先 show()：Mindustry Dialog 只有 show() 后才会正确注册到输入系统
            //   （否则按钮点击不响应）。show() 内部会走 Dialog 的完整初始化流程。
            this.show();

            // ★ show() 会把 size 改成全屏，所以 show 之后必须立刻覆盖 setSize/setPosition
            this.setSize(POPUP_W, POPUP_H);

            // 屏幕正中央
            float cx = Core.graphics.getWidth()  / 2f - POPUP_W / 2f;
            float cy = Core.graphics.getHeight() / 2f - POPUP_H / 2f;
            this.setPosition(cx, cy);

            // 行为设置
            this.setModal(true);       // 全屏半透灰遮罩 + 鼠标锁定（游戏暂停靠 setPaused，遮罩是双保险）
            this.setMovable(false);    // 锁定位置
            this.setResizable(false);  // 锁定大小

            // 初始页：page 0
            this.showPage(0);
        }

        // ========================================================================
        // 翻页：清空 cont/buttons → 按页码重建
        // ========================================================================
        /**
         * 切换到第 pageIdx 页。
         *
         * <p>做法：先 clearChildren 清空当前内容，再根据 pageIdx switch 重建。
         * 想扩展更多页，直接在 switch 里加 case。
         *
         * @param pageIdx 目标页码（0 是初始页）
         */
        public void showPage(int pageIdx) {
            // 清掉上一页残留的子节点（cont/buttons 两个容器本身保留引用，不清容器）
            this.cont.clearChildren();
            this.buttons.clearChildren();

            switch (pageIdx) {
                case 0: buildPage0(); break;
                case 1: buildPage1(); break;
                case 2: buildPage2(); break;
                case 3: buildPage3(); break;
                default: close();   // 越界页 → 直接关
            }
        }

        // ========================================================================
        // 工具：统一的「角色名 + 对话文本 + 分隔线」上半段布局
        // 复用率最高的一块，单独抽出来。
        // ========================================================================
        private void buildHeader(String speakerName, String dialogText) {
            // 角色名（accent 色 + 左对齐）
            this.cont.add("[accent]" + speakerName + ":[]")
                     .left()
                     .padLeft(20f).padTop(15f).row();

            // 对话文本（自动换行，宽度 = 对话框 - 左右各 20px）
            this.cont.labelWrap(dialogText)
                     .width(POPUP_W - 40f)
                     .padLeft(20f).padTop(5f).padRight(20f).row();

            // 分隔线（accent 色）
            this.cont.image()
                     .color(Pal.accent)
                     .fillX()
                     .height(2f)
                     .pad(10f, 15f, 10f, 15f).row();
        }

        // ========================================================================
        // 工具：返回下半部分的「角色图 + 选项按钮」底层一行 Table
        //   返回值 = 右侧 options Table，调用处直接往里面 .button(...) 加选项
        // ========================================================================
        private Table buildBottomWithChoices() {
            Table bottomRow = this.cont.table().growX().get();

            // 左：角色图片（Icon.box 占位，换 sprite 改这里）
            bottomRow.add(buildCharImg())
                     .size(CHAR_SIZE)
                     .pad(10f).get();

            // 右：选项按钮容器
            Table choices = bottomRow.table().grow().pad(10f).get();
            return choices;
        }

        // ========================================================================
        // 工具：单个角色图片（集中管理，以后换 sprite 只改这里）
        // ========================================================================
        private Element buildCharImg() {
            // TODO：换成你的角色 sprite。示例：
            //   return new Image(new TextureRegion(Core.atlas.find("你的角色名")));
            return new Image(Icon.box);
        }
        // ========================================================================
        // 工具：加一个「继续」按钮到 buttons 区 → 点击后执行 runnable → 关对话框
        // ========================================================================
        private void addContinueButton(Runnable onContinue) {
            this.buttons.add().growX();   // 左弹簧 → 按钮右对齐
            this.buttons.button(
                "继续",
                Icon.right,
                Styles.flatt,
                () -> {
                    if (onContinue != null) onContinue.run();
                    this.close();          // 关对话框 → 自动恢复游戏
                }
            ).size(160f, 50f).pad(10f);
        }

        // ========================================================================
        // 🎨 背景图应用逻辑（接口实现）
        //   - BG_SPRITE = null/"" → 跳过，保持原版灰色窗体
        //   - atlas.find() 返回错误纹理（白色方块）→ 也跳过，避免显示空白占位图
        //   - 正常找到 → 按 BG_9PATCH 决定是整张拉伸还是九宫格拉伸
        // ========================================================================
        private void applyCustomBackground() {
            // 1) 空配置 → 直接跳过
            if (BG_SPRITE == null || BG_SPRITE.isEmpty()) return;

            // 2) 从 atlas 找图
            arc.graphics.g2d.TextureRegion region = Core.atlas.find(BG_SPRITE);
            if (region == null) return;

            // 3) Mindustry 找不到 sprite 会返回一个 1x1 白图，用它的"错误标记"来判断是否真的存在
            //    atlas.isFound(region) 或 region.texture != null 都可以；这里再保险加一层
            if (!Core.atlas.has(BG_SPRITE)) return;

            // 4) 选背景类型：九宫格 / 整张拉伸
            if (BG_9PATCH && BG_9PATCH_PAD != null && BG_9PATCH_PAD.length == 4) {
                // ---- 九宫格拉伸（推荐用于带边框的背景图）----
                int left = BG_9PATCH_PAD[0];
                int right = BG_9PATCH_PAD[1];
                int top = BG_9PATCH_PAD[2];
                int bottom = BG_9PATCH_PAD[3];
                this.setBackground(new arc.scene.style.NinePatchDrawable(
                    new arc.graphics.g2d.NinePatch(region, left, right, top, bottom)
                ));
            } else {
                // ---- 整张平铺/拉伸（普通背景图）----
                this.setBackground(new arc.scene.style.TextureRegionDrawable(region));
            }
        }

        // ========================================================================
        // 工具：关闭对话框（统一入口，供任何按钮/分支调用）
        // ========================================================================
        public void close() {
            this.remove();
        }

        // ========================================================================
        // 覆盖 Element.remove()：对话框关闭时恢复游戏
        // ========================================================================
        @Override public boolean remove() {
            boolean ret = super.remove();
            // 如果打开前游戏没暂停，关闭时恢复
            if (!dialogPausedGame) {
                state.set(GameState.State.playing);
            }
            dialogPausedGame = false;
            return ret;
        }

        // ========================================================================
        // Page 0 — 初始对话（你现在的那页：问"是什么让你..." + 3 个选项）
        // ========================================================================
        private void buildPage0() {
            buildHeader(
                "[white]月华",
                "你叫什么名字？"
            );

            Table choices = buildBottomWithChoices();

            choices.button(
                "...",
                Styles.flatt,
                () -> showPage(1)                 // ← 跳分支 1，不关
            ).width(280f).padBottom(8f).row();

            choices.button(
                "(摇头)不知道",
                Styles.flatt,
                () -> showPage(2)                 // ← 跳分支 2，不关
            ).width(280f).padBottom(8f).row();

            choices.button(
                "我忘了",
                Styles.flatt,
                () -> showPage(3)                 // ← 跳分支 3，不关
            ).width(280f).row();
        }

        // ========================================================================
        // Page 1 — 分支：选了「帝国已经忘记了我们最初来到这里的目的」
        // ========================================================================
        private void buildPage1() {
            buildHeader(
                "[white]月华",
                "诶？怎么是个哑巴？"
            );

            Table choices = buildBottomWithChoices();
            choices.add().grow();   // 右边选项区空出来 → 继续按钮放在 buttons 里

            // 底部加「继续」按钮 → 点了执行分支 1 业务，再关对话框
            addContinueButton(() -> {
                // TODO：分支 1 的实际业务逻辑
                //   例：扣铜 100 → Items.remove(Items.copper, 100);
                //   例：发资源 → Items.add(NewItemsType.xxx, 10);
                //   例：弹窗提示 → ui.hudfrag.showToast(...);
            });
        }

        // ========================================================================
        // Page 2 — 分支：选了「我只是想找个能真正对抗污染的地方」
        // ========================================================================
        private void buildPage2() {
            buildHeader(
                "[white]月华",
                "你，失忆了吗？"
            );

            Table choices = buildBottomWithChoices();
            choices.add().grow();

            addContinueButton(() -> {
                // TODO：分支 2 的实际业务逻辑
            });
        }

        // ========================================================================
        // Page 3 — 分支：选了「私人原因，这很重要吗？」
        // ========================================================================
        private void buildPage3() {
            buildHeader(
                "[white]月华",
                "……失忆了吗？"
            );

            Table choices = buildBottomWithChoices();
            choices.add().grow();

            addContinueButton(() -> {
                // TODO：分支 3 的实际业务逻辑
            });
        }
    }
}
