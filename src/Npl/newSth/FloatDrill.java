package Npl.newSth;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;

/**
 * FloatDrill extends BurstDrill —— 唯一目的：把 BurstDrill 内部硬编码绘制的 arrowRegion
 * （自动从 {blockName}-arrow.png 加载，绘制坐标不可调）从"原版 draw 里直接画"拆出来，
 * 让我们能用 dx/dy 像素偏移手动修正 floatDrill-arrow.png 贴图像素不在画布正中心的问题。
 *
 * <p>使用方法：把 {@code new BurstDrill("floatDrill") {{ ... }}} 换成
 * {@code new FloatDrill("floatDrill") {{ ... }}} 即可；其它所有字段写法完全一致。
 *
 * <p>偏移量通过 {@link #arrowOffsetX} / {@link #arrowOffsetY} 调整，单位 = 像素（1 tile = 32 像素）。
 */
public class FloatDrill extends BurstDrill {

    /** arrow 贴图的额外 X 像素偏移（正 = 右，负 = 左）。默认 -1 tile */
    public float arrowOffsetX = -32f;
    /** arrow 贴图的额外 Y 像素偏移（正 = 上，负 = 下）。默认 -1 tile */
    public float arrowOffsetY = -32f;

    /**
     * 原版加载到的真实 arrow 贴图（floatDrill-arrow.png），后面由我们自己按偏移画。
     */
    protected TextureRegion realArrow;

    /**
     * 1×1 全透明贴图，用来替换 BurstDrill.arrowRegion / arrowBlurRegion：
     * - BurstDrillBuild.draw() 内部没有 null 检查，直接 Draw.rect(arrowRegion)，
     *   所以不能置 null（会 NPE）；
     * - 换成 1×1 透明 region 后：既不会 NPE，也不会画出任何可见像素。
     */
    protected static TextureRegion blankRegion;

    public FloatDrill(String name){
        super(name);
    }

    /** 懒初始化一次 1×1 透明纹理，所有 FloatDrill 实例共享。 */
    private static TextureRegion blank(){
        if (blankRegion == null){
            // arc Pixmap 构造参数 Pixmap.Format.RGBA8888：初始就是每张像素 0x00000000（全透明），
            // 不需要再调用 setColor / fill（也没有这些方法）。
            Pixmap pix = new Pixmap(1, 1);
            Texture tex = new Texture(pix, false);
            tex.setFilter(Texture.TextureFilter.linear, Texture.TextureFilter.linear);
            pix.dispose();
            blankRegion = new TextureRegion(tex);
        }
        return blankRegion;
    }

    @Override
    public void load(){
        super.load();

        // 1. 把原版自动加载到的 floatDrill-arrow.png 保存一份：后面我们自己按偏移画
        realArrow = arrowRegion;

        // 2. 把 BurstDrill 内部会被硬编码绘制的两个 arrow 相关 region 换成"完全透明但非 null"的 1×1 占位：
        //      arrowRegion      —— 原版冲击钻头静态箭头层
        //      arrowBlurRegion  —— 原版冲击钻头箭头扫动模糊层
        //    这样 BurstDrillBuild.draw() 里的 Draw.rect() 既不会 NPE，也不会画出那两层错位的箭头。
        TextureRegion b = blank();
        arrowRegion = b;
        arrowBlurRegion = b;
    }

    /** FloatDrill 专属 Build：在原版钻机绘制完之后，再按偏移把 realArrow 手动画上去。 */
    public class FloatDrillBuild extends BurstDrillBuild {

        @Override
        public void draw(){
            // 2. 原版全套 BurstDrill 绘制（rotator、glow、rim、mine 动画、liquid 等等全都保留）
            super.draw();

            // 3. 手动画 arrow：和原版一致 → PNG 几何中心对齐钻机中心 + 我们的偏移修正
            if (realArrow == null) return;

            float cx = tile == null ? x : tile.worldx();
            float cy = tile == null ? y : tile.worldy();
            float w = realArrow.width * Draw.scl;
            float h = realArrow.height * Draw.scl;

            // 原版 BurstDrill 的 arrow 是静态叠加层（不随钻机旋转）—— 所以这里直接画不旋转
            Draw.rect(realArrow, cx + arrowOffsetX, cy + arrowOffsetY, w, h);
        }
    }
}
