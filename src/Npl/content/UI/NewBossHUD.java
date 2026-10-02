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
 * Boss离玩家越远整条越淡（最近 1.0 → 超过 1600 距离降到 0.35），减少遮挡视野
 */
public class NewBossHUD extends Table {
    /** 血条宽度 / 高度（缩小后不再横向铺满、遮挡视野） */
    private static final float barWidth = 620f;
    private static final float barHeight = 22f;
    /** 玩家距离小于 closeRange 时完全不透明，超过 fadeRange 时降到 minAlpha */
    public static final float closeRange = 700f;
    public static final float fadeRange = 1600f;
    private static final float minAlpha = 0.35f;

    private Unit target;
    private final Bar bar;
    private final Bar shieldBar;
    private final Label nameLabel;
    private final Label hpLabel;

    private static final float defeatedDuration = 180f;
    private float defeatedTimer = 0f;
    private float maxShieldSeen = 0f;
    /** 本帧的远程淡化系数（0 = 玩家附近，1 = 超过 fadeRange），由 BossBarManager 每帧设置 */
    private float distanceFade = 0f;

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
        nameLabel.setFontScale(1.05f);

        // 血量数字 — 叠加在血条内部
        hpLabel = new Label("");
        hpLabel.setAlignment(Align.center);
        hpLabel.setColor(Color.white);
        hpLabel.setFontScale(0.85f);

        // Stack 叠加：血条(底) → 护盾条(中) → 血量数字(顶)
        Stack barStack = new Stack();
        barStack.add(bar);
        barStack.add(shieldBar);
        barStack.add(hpLabel);

        // 布局
        add(nameLabel).padBottom(2f).row();
        add(barStack).width(barWidth).height(barHeight);

        update(() -> {
            // ========== 击败倒计时显示（递减由管理器 tickDefeat 驱动） ==========
            if (defeatedTimer > 0f) {
                visible = true;
                color.a = 1f; // 击败提示始终清晰
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
                color.a = 1f;
                nameLabel.setText("");
                hpLabel.setText("");
                shieldBar.visible = false;
                bar.setColor(Color.valueOf("222222"));
                return;
            }

            visible = true;
            // 远处淡出：贴脸 1.0，超过 fadeRange 平滑降到 minAlpha
            color.a = 1f - distanceFade * (1f - minAlpha);

            // 名称
            nameLabel.setText(target.type.localizedName);
            nameLabel.setColor(Color.valueOf("ffd244"));

            // 血量数字
            int cur = (int)target.health;
            int max = (int)target.maxHealth;
            hpLabel.setText(cur + " / " + max);
            hpLabel.setColor(Color.white);

            // ========== 护盾条 ==========
            // 以"当前护盾"为基准，而不是历史峰值：否则首次挂载时基准为 0，护盾条永远不显示
            if (target.shield > maxShieldSeen) {
                maxShieldSeen = target.shield;
            }
            if (target.shield > 0f && maxShieldSeen > 0f) {
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

    /**
     * 分配/切换目标。只有目标真的变化时才重置状态，
     * 并且不触碰 defeatedTimer —— 否则击败提示会被后续活Boss的分配抹掉。
     */
    public void assignTarget(Unit unit) {
        if (this.target == unit) return;
        this.target = unit;
        this.defeatedTimer = 0f;
        // 以新目标的当前护盾为基准，护盾条才能立即显示
        this.maxShieldSeen = (unit != null) ? Math.max(0f, unit.shield) : 0f;
        this.distanceFade = 0f;
        if (unit != null) {
            visible = true;
        }
    }

    /** 兼容旧调用（等价于 assignTarget） */
    public void setTarget(Unit unit) {
        assignTarget(unit);
    }

    /**
     * 由 BossBarManager 每帧传入与玩家的「距离淡化系数」（0 = 贴脸，1 = 足够远），
     * 避免在全屏范围硬切可见性。
     */
    public void setDistanceFade(float t) {
        this.distanceFade = (t < 0f) ? 0f : (t > 1f) ? 1f : t;
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
