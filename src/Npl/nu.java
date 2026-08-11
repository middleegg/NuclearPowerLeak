package Npl;

import arc.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import mindustry.ui.dialogs.*;
import Npl.content.*;
import Npl.content.envBlocks;
import Npl.newSth.NewItemsType;
import Npl.newSth.Type.*;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.ui.*;
import mindustry.type.Item;

public class nu extends Mod {

    public nu(){
        Events.on(ClientLoadEvent.class, e -> {

            /* 启动 10 秒后弹出「青蛙主弹窗」 */
            Time.runTask(10f, () -> {
                // ★ Bundle 用法 1：标题 → 用 Core.bundle.get("key") 取 bundle 里写的值
                //   不用写死 "frog"，语言切英文自动取 bundle.properties 的值，切中文取 bundle_zh_CN.properties
                BaseDialog dialog = new BaseDialog(Core.bundle.get("ui.nu.frog.dialog.title"));

                // ┌───────── 第 0 行：左边青蛙图 + 右边文字并排 ─────────┐
                /* 左边（同一行第 0 列）：青蛙图片的子 Table */
                dialog.cont.table(left -> {
                    left.image(Core.atlas.find("nu-frog")).size(200).pad(20);
                }).left();

                /* 同一行第 1 列：右边文字 + 两个按钮的子 Table */
                dialog.cont.table(right -> {
                    // ★ Bundle 用法 2：普通 add("文字") → add(Core.bundle.get("key"))
                    right.add(Core.bundle.get("ui.nu.frog.behold")).padBottom(20).row();
                    right.add(Core.bundle.get("ui.nu.frog.desc")).color(Color.lightGray).padBottom(40).row();
                    // ★ Bundle 用法 3：按钮上的文字 → button(Core.bundle.get("key"), onClick)
                    right.button(Core.bundle.get("ui.nu.frog.btn.cute"), dialog::hide).size(240, 60).padBottom(10).row();
                    // ★ Bundle 用法 4："介绍" 按钮也走 bundle
                    right.button(Core.bundle.get("ui.nu.frog.btn.intro"), () -> {
                        dialog.hide();
                        showFrogProfile(dialog);
                    }).size(180, 60);
                }).left().padLeft(30).row();

                // ┌───────── 第 1 行：下面一条灰色面板区 ─────────┐
                dialog.cont.table(Styles.grayPanel, t -> {
                    // ★ Bundle 用法 5：欢迎语
                    t.add(Core.bundle.get("ui.nu.frog.welcome")).pad(20).row();
                    t.image(Core.atlas.find("nu-frog")).size(120).pad(10).row();
                    // ★ Bundle 用法 6："关掉" 按钮
                    t.button(Core.bundle.get("ui.nu.frog.btn.close"), dialog::hide).size(120, 40);
                }).growX().pad(20).row();

                // ┌───────── 第 2 行：底部标准 OK 按钮 ─────────┐
                dialog.cont.button(Core.bundle.get("ui.nu.frog.btn.ok"), dialog::hide).size(100, 50);

                dialog.show();
            });

        });
    }

    @Override
    public void loadContent() {
        NuItems.load();
        NuLiquid.load();
        Azer.load();
        NuBlocks.load();
        NuStatus.load();
        FederalUnitType.load();
        envBlocks.load();
    }

    /* ──────────────────────────────────────────────────────
       跳转到的「青蛙档案」界面
       参数 backTo = 点"返回"按钮要回到哪一个弹窗
       ────────────────────────────────────────────────────── */
    private void showFrogProfile(BaseDialog backTo) {
        // ★ Bundle 用法 7：档案弹窗的标题
        BaseDialog profile = new BaseDialog(Core.bundle.get("ui.nu.frog.profile.title"));

        // 档案内容（灰色面板包一层）
        profile.cont.table(Styles.grayPanel, t -> {
            // ★ Bundle 用法 8：标签 + 值 分两段各查一次 bundle
            t.row();
            t.add(Core.bundle.get("ui.nu.frog.profile.value.name")).color(Color.cyan).padLeft(30f).row();

            t.add(Core.bundle.get("ui.nu.frog.profile.label.description")).left();
            t.row();
            t.add(Core.bundle.get("ui.nu.frog.profile.value.weight")).color(Color.yellow).padLeft(30f).row();

            t.add(Core.bundle.get("ui.nu.frog.profile.label.content")).color(Color.scarlet).left();
        }).growX().pad(20f).row();
        // 底部两个按钮：返回 / 关闭
        profile.buttons.defaults().size(200f, 54f).pad(8f);
        profile.buttons.button(Core.bundle.get("ui.nu.frog.profile.btn.back"), () -> {
            profile.hide();
            backTo.show();
        });
        profile.buttons.button(Core.bundle.get("ui.nu.frog.profile.btn.close"), profile::hide);

        profile.show();
    }

}
