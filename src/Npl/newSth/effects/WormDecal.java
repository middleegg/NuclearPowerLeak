package  Npl.newSth.effects;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import mindustry.gen.Unit;

/**
 * 虫子（多节段）单位的「液压装饰」渲染器（移植自 PU132 的 WormDecal）。
 * <p>
 * 在多节段单位系统中的角色：
 * <ul>
 *   <li>多节段单位由一串独立的段身单位串联而成，相邻两段（当前段 base 与父段 other/头部）
 *       之间会有一段肉眼可见的「连接结构」，本类负责把它画出来。</li>
 *   <li>视觉上由三部分组成：一条基线（连接两端的线段）、端点贴图（base 端 / end 端）、
 *       以及若干中间段贴图，模拟机械液压管的层次感。</li>
 *   <li>两侧对称绘制（用 {@link Mathf#signs} 遍历 -1/+1），让装饰看起来像左右两根液压管，
 *       而不是单根孤零零的线。</li>
 * </ul>
 * <p>
 * 参考：PU132 main/src/unity/type/WormDecal.java
 */
public class WormDecal {
    /** 复用的临时向量，避免每帧 new 出大量 Vec2 造成 GC 抖动。 */
    private static final Vec2 v1 = new Vec2();

    /** base 端（当前段身）连接点在段身局部坐标下的偏移（X 为侧向，受 sign 翻转；Y 为前后）。 */
    public float baseX, baseY, endX, endY;
    /** 贴图相对连接点的内缩偏移，避免贴图和线段端点重叠。 */
    public float baseOffset;
    /** 中间段贴图数量，段数越多液压管看起来越细密。 */
    public int segments = 1;
    /** 基线颜色与粗细。 */
    public Color lineColor = Color.white;
    public float lineWidth = 2f;
    /** 装饰名称（用于拼贴图路径，如 "oppression-hydraulics"）。 */
    String name;
    /** base 端、end 端贴图；中间段贴图数组。 */
    public TextureRegion baseRegion, endRegion;
    public TextureRegion[] segmentRegions;
    /**
     * 贴图是否已加载。采用「延迟加载」：第一次 draw 时才调用 {@link #load()}，
     * 这样即便某些单位没配置贴图也不会在构造阶段就报错。
     */
    private boolean loaded = false;

    public WormDecal(String name) {
        this.name = name;
    }

    /**
     * 加载本装饰需要的所有贴图。
     *
     * <p>★ mod 贴图在 atlas 中会自动带上 modname- 前缀（例如 "create-oppression-hydraulics-base"），
     * 但有时也会用不带前缀的命名。因此 {@link #findRegion} 会优先尝试带前缀的，找不到再退回不带前缀的，
     * 兼容两种命名习惯。
     */
    public void load() {
        String modName = "create-" + name;
        baseRegion = findRegion(modName + "-base", name + "-base");
        endRegion = findRegion(modName + "-end", name + "-end");
        segmentRegions = new TextureRegion[segments];
        for (int i = 0; i < segmentRegions.length; i++) {
            segmentRegions[i] = findRegion(modName + "-" + i, name + "-" + i);
        }
        loaded = true;
    }

    /** 先试带 mod 前缀的路径，找不到再试不带前缀的，保证两种命名都能命中。 */
    private static TextureRegion findRegion(String prefixed, String unprefixed) {
        TextureRegion r = Core.atlas.find(prefixed);
        if (r.found()) return r;
        return Core.atlas.find(unprefixed);
    }

    /**
     * 在 base（当前段身）与 other（父段/头部）之间绘制液压装饰。
     *
     * <p>调用方式：{@code wormDecal.draw(unit, unit.parent())}，即 base=段身，other=头部。
     *
     * <p>本方法把所有元素（线 + 端点圆 + 中间段贴图 + 端点贴图）画在「当前渲染层级」上，
     * 适合不需要和单位本体做分层遮挡的简单场景。
     *
     * <p>★ 手机端兼容：绘制贴图前对每个 TextureRegion 单独做 found() 检查，
     * 并用 try-catch 包裹，万一贴图加载失败也不会让整个游戏闪退，只是这一帧装饰缺失。
     */
    public void draw(Unit base, Unit other) {
        if (other == null) return;
        if (!loaded) load();

        // Mathf.signs = {-1, 1}，遍历一次画出左右两侧对称的液压管。
        for (int s : Mathf.signs) {
            // ★ 每侧绘制前都重置一次颜色，防止上一侧 applyColor 残留的 mixcol 影响本侧。
            Draw.mixcol();
            Draw.color(lineColor);
            Lines.stroke(lineWidth);

            // 根据 base 段身的朝向，把局部偏移 (baseX*s, baseY) 旋转到世界坐标，得到 base 端连接点。
            v1.trns(base.rotation - 90f, baseX * s, baseY).add(base);
            float bx = v1.x, by = v1.y;
            // 同理算出 other（父段）端的连接点。
            v1.trns(other.rotation - 90f, endX * s, endY).add(other);
            float ex = v1.x, ey = v1.y;
            // 两端连线方向，后续贴图都要沿这个角度摆放。
            float angle = Angles.angle(bx, by, ex, ey);

            // 两端各画一个实心小圆，让线段端点看起来有「关节」感而不是生硬的线头。
            Fill.circle(bx, by, lineWidth / 2f);
            Fill.circle(ex, ey, lineWidth / 2f);
            Lines.line(bx, by, ex, ey, false);

            // —— 贴图绘制：每个贴图独立检查 found()，有就画，没有就跳过 ——
            try {
                base.type.applyColor(base);

                // 把端点向连线内侧内缩 baseOffset 像素，腾出贴图摆放空间。
                float endW = endRegion.found() ? (endRegion.width * Draw.scl * 0.5f) - baseOffset : 0f;
                float baseW = baseRegion.found() ? (baseRegion.width * Draw.scl * 0.5f) - baseOffset : 0f;

                v1.trns(angle + 180f, endW).add(ex, ey);
                ex = v1.x;
                ey = v1.y;
                v1.trns(angle, baseW).add(bx, by);
                bx = v1.x;
                by = v1.y;

                // 中间段贴图：从 end 端往 base 端倒序画，按等分比例 p 在连线上插值定位。
                if (segmentRegions != null) {
                    for (int i = segmentRegions.length - 1; i >= 0; i--) {
                        TextureRegion r = segmentRegions[i];
                        if (r.found()) {
                            float p = (i + 1f) / (segments + 1f);
                            v1.set(bx, by).lerp(ex, ey, p);
                            Draw.rect(r, v1.x, v1.y, angle);
                        }
                    }
                }

                // 末端贴图（父段方向）。
                if (endRegion.found()) {
                    Draw.rect(endRegion, ex, ey, angle + 180f);
                }

                // 基端贴图（当前段身方向）。
                if (baseRegion.found()) {
                    Draw.rect(baseRegion, bx, by, angle);
                }
            } catch (Throwable t) {
                // 手机端防御：贴图绘制失败时静默继续，保证游戏不崩。
            }
        }
        Draw.reset();
    }

    /**
     * 分层绘制液压装饰，是 {@link #draw} 的「升级版」，用于需要和单位本体做正确遮挡的场景。
     *
     * <p>★ draw 与 drawBelow 的核心区别（渲染层级）：
     * <ul>
     *   <li>{@link #draw}：所有元素都画在调用者当前所在的 Draw.z() 层级上，
     *       不会和单位本体做分层，可能出现装饰整体压在段身贴图之上的「贴纸感」。</li>
     *   <li>{@link #drawBelow}：把装饰按连接位置拆成三层渲染层级——
     *       <ul>
     *         <li>base 端（当前段身侧）画在 {@code oldZ - 0.00005}，即<b>段身贴图下方</b>，
     *             这样段身会盖住 base 端贴图的根部，看起来像液压管「从段身里伸出来」；</li>
     *         <li>中间段按距离在 oldZ 上下做线性插值，形成由低到高的自然过渡；</li>
     *         <li>end 端（父段侧）画在 {@code oldZ + 0.00005}，即<b>段身贴图上方</b>，
     *             让父段方向的贴图压在后面段身之上，符合「前段挡后段」的视觉直觉。</li>
     *       </ul>
     *       这样装饰和单位本体的遮挡关系才正确，不会出现穿模或贴纸感。</li>
     * </ul>
     *
     * <p>0.00005 这个微小偏移量是有意为之：Mindustry 的 Draw.z 是浮点排序，
     * 用极小的差值既能拉开层级顺序，又不会把装饰推到完全不同的渲染批次里，
     * 避免批渲染被打断带来的性能损失。
     */
    public void drawBelow(Unit base, Unit other) {
        if (other == null) return;
        if (!loaded) load();

        // 先判断是否有任何贴图可用，没有任何贴图时跳过贴图分支，只画线段。
        boolean hasTextures = baseRegion.found() && endRegion.found();
        if (!hasTextures && segmentRegions != null) {
            for (TextureRegion r : segmentRegions) {
                if (r.found()) { hasTextures = true; break; }
            }
        }

        // 记住进入时的渲染层级，画完之后要还原，避免污染调用者的渲染状态。
        float oldZ = Draw.z();

        for (int s : Mathf.signs) {
            v1.trns(base.rotation - 90f, baseX * s, baseY).add(base);
            float bx = v1.x, by = v1.y;
            v1.trns(other.rotation - 90f, endX * s, endY).add(other);
            float ex = v1.x, ey = v1.y;
            float angle = Angles.angle(bx, by, ex, ey);

            // 线段部分层级不变，和 draw 一致。
            Draw.mixcol();
            Draw.color(lineColor);
            Fill.circle(bx, by, lineWidth / 2f);
            Fill.circle(ex, ey, lineWidth / 2f);
            Lines.stroke(lineWidth);
            Lines.line(bx, by, ex, ey, false);

            if (hasTextures) {
                try {
                    base.type.applyColor(base);

                    float endW = endRegion.found() ? (endRegion.width * Draw.scl * 0.5f) - baseOffset : 0f;
                    float baseW = baseRegion.found() ? (baseRegion.width * Draw.scl * 0.5f) - baseOffset : 0f;

                    v1.trns(angle + 180f, endW).add(ex, ey);
                    ex = v1.x;
                    ey = v1.y;
                    v1.trns(angle, baseW).add(bx, by);
                    bx = v1.x;
                    by = v1.y;

                    // ★ 中间段：按距离 p 在 oldZ 上下做线性插值，
                    //   base 端(p=0) → oldZ-0.00005（贴图下方），end 端(p=1) → oldZ+0.00005（贴图上方），
                    //   形成由低到高的层级过渡，避免中间段整体压在某一侧之上。
                    if (segmentRegions != null) {
                        for (int i = segmentRegions.length - 1; i >= 0; i--) {
                            TextureRegion r = segmentRegions[i];
                            if (r.found()) {
                                float p = (i + 1f) / (segments + 1f);
                                v1.set(bx, by).lerp(ex, ey, p);
                                float zInterp = Mathf.lerp(oldZ - 0.00005f, oldZ + 0.00005f, p);
                                Draw.z(zInterp);
                                Draw.rect(r, v1.x, v1.y, angle);
                            }
                        }
                    }

                    // ★ end 端（父段方向）：画在段身贴图上方，让父段方向的液压管压在后面段身之上。
                    Draw.z(oldZ + 0.00005f);
                    if (endRegion.found()) {
                        Draw.rect(endRegion, ex, ey, angle + 180f);
                    }

                    // ★ base 端（当前段身方向）：画在段身贴图下方，让段身盖住根部，模拟「从段身伸出」。
                    Draw.z(oldZ - 0.00005f);
                    if (baseRegion.found()) {
                        Draw.rect(baseRegion, bx, by, angle);
                    }
                } catch (Throwable t) {}
            }
        }

        // 还原渲染层级与颜色状态。
        Draw.z(oldZ);
        Draw.reset();
    }
}
