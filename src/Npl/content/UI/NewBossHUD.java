package Npl.content.UI;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Tmp;
import Npl.content.*;
import mindustry.gen.Unit;
import mindustry.ui.Bar;

/**
 * Boss血条
 * 名称在上方，血量数字显示在血条内部（叠加在Bar上）
 * 如果Boss有护盾，额外叠加显示一条护盾条（用 NuColor.CoreColor）
 */
public class NewBossHUD extends Table {
    private Unit target;
    private final Bar bar;
    private final Bar shieldBar;
    private final Label nameLabel;
    private final Label hpLabel;

    private static final float defeatedDuration = 180f;
    private float defeatedTimer = 0f;
    private float maxShieldSeen = 0f;

    public NewBossHUD() {
        // 血条
        bar = new Bar(
                () -> "",
                () -> {
                    if (target == null || target.maxHealth <= 0) return Color.valueOf("5a2d3c");
                    float r = Mathf.clamp(target.health / target.maxHealth);
                    if (r < 0.5f) {
                        return Tmp.c1.set(Color.valueOf("cc2233")).lerp(Color.valueOf("e88811"), r * 2f);
                    } else {
                        return Tmp.c1.set(Color.valueOf("e88811")).lerp(Color.valueOf("ffd244"), (r - 0.5f) * 2f);
                    }
                },
                () -> {
                    if (target == null || target.maxHealth <= 0) return 0f;
                    return Mathf.clamp(target.health / target.maxHealth);
                }
        );
        bar.setColor(Color.valueOf("222222"));
        bar.outline(NuColor.BloodColor.cpy().lerp(Color.white, 0.3f), 3f);
        bar.blink(Color.valueOf("ffd244").cpy().a(0.5f));

        // 护盾条 — 叠加在血条上方，只有有护盾时才显示
        shieldBar = new Bar(
                () -> "",
                () -> NuColor.CoreColor.cpy().lerp(Color.white, 0.3f),
                () -> {
                    if (target == null || maxShieldSeen <= 0f) return 0f;
                    return Mathf.clamp(target.shield / maxShieldSeen);
                }
        );
        shieldBar.setColor(NuColor.CoreColor.cpy().a(0.6f));
        shieldBar.outline(NuColor.CoreColor, 2f);
        shieldBar.visible = false;

        // 名称
        nameLabel = new Label("");
        nameLabel.setAlignment(Align.center);
        nameLabel.setColor(Color.valueOf("ffffff"));
        nameLabel.setFontScale(1.1f);

        // 血量数字 — 叠加在血条内部
        hpLabel = new Label("");
        hpLabel.setAlignment(Align.center);
        hpLabel.setColor(Color.white);
        hpLabel.setFontScale(0.9f);

        // Stack 叠加：血条(底) → 护盾条(中) → 血量数字(顶)
        Stack barStack = new Stack();
        barStack.add(bar);
        barStack.add(shieldBar);
        barStack.add(hpLabel);

        // 布局
        add(nameLabel).padBottom(2f).row();
        add(barStack).width(750f).height(28f);

        update(() -> {
            // ========== 击败倒计时显示（递减由管理器 tickDefeat 驱动） ==========
            if (defeatedTimer > 0f) {
                visible = true;
                String name = (target != null && target.type != null) ? target.type.localizedName : "Boss";
                nameLabel.setText("[gray]已击败: " + name + "[]");
                hpLabel.setText("");
                shieldBar.visible = false;
                bar.setColor(Color.gray);
                return;
            }

            // ========== Boss 存活状态 ==========
            if (target == null || target.type == null || target.dead || !target.isAdded()) {
                visible = false;
                nameLabel.setText("");
                hpLabel.setText("");
                shieldBar.visible = false;
                bar.setColor(Color.valueOf("222222"));
                return;
            }

            visible = true;

            // 名称
            nameLabel.setText(target.type.localizedName);
            nameLabel.setColor(Color.valueOf("ffd244"));

            // 血量数字
            int cur = (int)target.health;
            int max = (int)target.maxHealth;
            hpLabel.setText(cur + " / " + max);
            hpLabel.setColor(Color.white);

            // ========== 护盾条 ==========
            if (target.shield > 0f) {
                if (target.shield > maxShieldSeen) {
                    maxShieldSeen = target.shield;
                }
                shieldBar.visible = true;
            } else {
                shieldBar.visible = false;
                maxShieldSeen = 0f;
            }

            // 满血时名称发光
            if (Mathf.equal(target.health, target.maxHealth, 0.01f)) {
                nameLabel.setColor(NuColor.PaleColor.cpy().lerp(Color.valueOf("ffd244"), 0.5f));
            }
        });

        setTransform(true);
    }

    public void setTarget(Unit unit) {
        this.target = unit;
        this.defeatedTimer = 0f;
        this.maxShieldSeen = 0f;
        if (unit != null) {
            visible = true;
        }
    }

    public Unit getTarget() {
        return target;
    }

    public void markDefeated() {
        if (defeatedTimer <= 0f) {
            defeatedTimer = defeatedDuration;
        }
    }

    public boolean isDefeated() {
        return defeatedTimer > 0f;
    }

    /** 由管理器每帧驱动击败倒计时（不依赖场景树，切存档不会冻结） */
    public void tickDefeat(float delta) {
        if (defeatedTimer > 0f) {
            defeatedTimer -= delta;
            if (defeatedTimer <= 0f) {
                defeatedTimer = 0f;
                visible = false;
                nameLabel.setText("");
                hpLabel.setText("");
                shieldBar.visible = false;
                target = null;
            }
        }
    }

    /** 完全重置（切存档时调用，状态不跨存档保留） */
    public void reset() {
        defeatedTimer = 0f;
        maxShieldSeen = 0f;
        target = null;
        visible = false;
        nameLabel.setText("");
        hpLabel.setText("");
        shieldBar.visible = false;
    }

    public boolean isFree() {
        return (target == null || target.dead) && defeatedTimer <= 0f;
    }
}
