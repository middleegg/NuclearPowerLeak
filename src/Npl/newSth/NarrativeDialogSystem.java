package Npl.newSth;

import arc.*;
import arc.Events;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.utils.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

/**
 * 剧情对话系统：屏幕内嵌对话框（不覆盖全屏）。
 *  - 打字机效果（逐字弹出），可点击跳过
 *  - 动态数量选择按钮
 *  - 可折叠（收起为标题条，点击展开）
 *  - 双定位：屏幕固定位置 / 跟随世界实体
 *  - 双触发：代码直接调用 / 靠近区域自动触发
 *  - 科幻辐射风格（深绿底 + 绿边 + 角标 + 顶侧光带）
 *
 * 用法：
 *   NarrativeDialogSystem.init();
 *   NarrativeDialogSystem.show(new DialogEntry{{ text = "..."; choices = new DialogChoice[]{...}; }});
 */
public class NarrativeDialogSystem {

    // ==================== 数据 ====================

    /** 一个对话选项。 */
    public static class DialogChoice{
        public String text;
        public Runnable callback;

        public DialogChoice(String text, Runnable callback){
            this.text = text;
            this.callback = callback;
        }
    }

    /** 一段对话。 */
    public static class DialogEntry{
        /** 发言者名称（null 则不显示名称栏）。 */
        public String speaker = null;
        /** 正文。 */
        public String text = "";
        /** 选项（为空数组 = 纯叙述，点击面板关闭；非空 = 必须选择）。 */
        public DialogChoice[] choices = {};

        // ---- 定位 ----
        /** true = 跟随实体；false = 屏幕固定。 */
        public boolean followEntity = false;
        /** 跟随目标（followEntity=true 时）。 */
        @Nullable
        public Unit targetEntity;
        /** 实体跟随时的屏幕偏移（相对于实体投影点，向上为 +）。 */
        public float followOffsetY = 30f;
        /** 屏幕模式下的位置（对话框左下角屏幕坐标）。 */
        public float screenX = 200f;
        public float screenY = 160f;

        // ---- 速度 ----
        /** 打字机速度（字符/秒）。 */
        public float charsPerSecond = 28f;

        /** 关闭后的回调（选项 callback 之后触发）。 */
        public Runnable onClose = null;
    }

    /** 靠近触发记录。 */
    public static class ProximityTrigger{
        public float worldX, worldY, radius;
        public DialogEntry entry;
        public boolean consumed = false;

        public ProximityTrigger(float worldX, float worldY, float radius, DialogEntry entry){
            this.worldX = worldX;
            this.worldY = worldY;
            this.radius = radius;
            this.entry = entry;
        }
    }

    // ==================== 状态 ====================

    private static DialogElement element;
    private static final Queue<DialogEntry> queue = new Queue<>();
    private static DialogEntry current = null;

    private static final Seq<ProximityTrigger> triggers = new Seq<>();

    private static boolean visible = false;
    private static boolean collapsed = false;

    // 打字机
    private static float typeTimer = 0f;
    private static int visibleChars = 0;
    private static boolean typing = false;

    // 按钮
    private static final Seq<ButtonRect> buttonRects = new Seq<>();
    private static int hoverIndex = -1;

    // 动画
    private static float openTimer = 0f;
    private static final float OPEN_TIME = 12f;

    private static class ButtonRect{
        float x, y, w, h;
        DialogChoice choice;
        ButtonRect(float x, float y, float w, float h, DialogChoice c){ this.x=x; this.y=y; this.w=w; this.h=h; this.choice=c; }
    }

    // ==================== 外观常量 ====================

    private static final float PANEL_W = 380f;
    private static final float PAD = 14f;
    private static final float HEADER_H = 30f;
    private static final float TITLE_H = 26f;
    private static final float BODY_LINE_H = 20f;
    private static final float BUTTON_H = 30f;
    private static final float BUTTON_GAP = 6f;
    private static final float CORNER = 8f;
    private static final float BORDER = 2f;
    private static final int MAX_BODY_LINES = 6;

    private static final Color BG        = Color.valueOf("0a140a");
    private static final Color BORDER_HI = Color.valueOf("3dff6e");
    private static final Color BORDER_LO = Color.valueOf("166030");
    private static final Color SPEAKER   = Color.valueOf("7CFF6E");
    private static final Color BODY      = Color.valueOf("e6ffe6");
    private static final Color BTN_BG    = Color.valueOf("0c200c");
    private static final Color BTN_HOVER = Color.valueOf("1a5020");
    private static final Color BTN_LINE  = Color.valueOf("3dff6e");
    private static final Color GLOW      = Color.valueOf("aeffd0");

    private static final Font FONT = Fonts.outline;
    private static final GlyphLayout layout = new GlyphLayout();

    // ==================== 生命周期 ====================

    public static void init(){
        element = new DialogElement();
        element.setZIndex(9999);
        Core.scene.add(element);
        installListener();

        Events.run(Trigger.update, NarrativeDialogSystem::update);
    }

    private static void update(){
        if(!visible || current == null){
            updateProximity();
            return;
        }

        // 打字机推进
        if(typing){
            typeTimer += Time.delta;
            int target = current.text.length();
            visibleChars = Mathf.clamp((int)(typeTimer * current.charsPerSecond), 0, target);
            if(visibleChars >= target){
                visibleChars = target;
                typing = false;
            }
        }

        // 展开动画
        if(openTimer < OPEN_TIME) openTimer = Mathf.clamp(openTimer + Time.delta);

        // 实体跟随定位
        if(current.followEntity && current.targetEntity != null && !current.targetEntity.dead){
            Vec2 sp = Core.camera.project(Tmp.v1.set(current.targetEntity.x, current.targetEntity.y));
            float px = sp.x;
            float py = sp.y + current.followOffsetY;
            element.setPosition(px - PANEL_W / 2f, py);
        }

        // 按钮悬停检测
        hoverIndex = -1;
        if(!typing && !collapsed && current.choices.length > 0){
            float mx = Core.input.mouseX();
            float my = Core.input.mouseY();
            float ex = element.x, ey = element.y;
            for(int i = 0; i < buttonRects.size; i++){
                ButtonRect b = buttonRects.get(i);
                if(mx >= ex + b.x && mx <= ex + b.x + b.w && my >= ey + b.y && my <= ey + b.y + b.h){
                    hoverIndex = i;
                    break;
                }
            }
        }
    }

    private static void updateProximity(){
        Unit u = mindustry.Vars.player.unit();
        if(u == null || u.dead) return;
        for(ProximityTrigger t : triggers){
            if(t.consumed) continue;
            float d = Mathf.dst(u.x, u.y, t.worldX, t.worldY);
            if(d <= t.radius){
                t.consumed = true;
                show(t.entry);
                return;
            }
        }
    }

    // ==================== 公开 API ====================

    /** 立即显示一段对话（打断当前）。 */
    public static void show(DialogEntry entry){
        current = entry;
        queue.clear();
        beginEntry();
    }

    /** 加入队列（在当前对话结束后依次播放）。 */
    public static void enqueue(DialogEntry entry){
        queue.addLast(entry);
    }

    /** 关闭当前对话，如有队列则播放下一段。 */
    public static void close(){
        if(current != null && current.onClose != null){
            current.onClose.run();
        }
        advanceQueue();
    }

    /** 添加靠近触发器（世界坐标 + 半径，玩家进入即触发一次）。 */
    public static void addProximityTrigger(float worldX, float worldY, float radius, DialogEntry entry){
        triggers.add(new ProximityTrigger(worldX, worldY, radius, entry));
    }

    /** 重置靠近触发器（使其可再次触发）。 */
    public static void resetTriggers(){
        for(ProximityTrigger t : triggers) t.consumed = false;
    }

    public static boolean isVisible(){
        return visible;
    }

    public static DialogEntry current(){
        return current;
    }

    // ==================== 内部流程 ====================

    private static void advanceQueue(){
        if(queue.size > 0){
            current = queue.removeFirst();
            beginEntry();
        } else {
            current = null;
            visible = false;
            element.toBack();
        }
    }

    private static void beginEntry(){
        visible = true;
        collapsed = false;
        element.touchable = Touchable.enabled;
        element.toFront();

        typeTimer = 0f;
        visibleChars = 0;
        typing = current.text != null && !current.text.isEmpty();
        openTimer = 0f;

        // 屏幕模式默认位置
        if(!current.followEntity){
            element.setPosition(current.screenX, current.screenY);
        }

        buildLayout();
    }

    /** 计算文本换行与按钮位置。 */
    private static void buildLayout(){
        buttonRects.clear();
        if(current == null) return;

        float contentX = PAD + BORDER;
        float panelContentW = PANEL_W - 2f * (PAD + BORDER);
        float bodyTop = current.speaker != null ? (TITLE_H + 1f) : 0f;

        // 仅在有选项时计算按钮位置（渲染时确定，这里预存 y 基准）
        // 按钮 y 在 draw 中根据实际文本行数计算，此处仅记录选项数
    }

    /** 文本换行。 */
    private static String[] wrap(String text, float maxWidth){
        if(text == null || text.isEmpty()) return new String[]{""};
        Seq<String> out = new Seq<>();
        for(String para : text.split("\n", -1)){
            StringBuilder line = new StringBuilder();
            for(int i = 0; i < para.length(); i++){
                char c = para.charAt(i);
                String test = line.toString() + c;
                if(textWidth(test) > maxWidth && line.length() > 0){
                    out.add(line.toString());
                    line.setLength(0);
                }
                line.append(c);
            }
            out.add(line.toString());
        }
        return out.toArray(String.class);
    }

    /** 计算文本宽度（Font 无 getTextWidth，改用 GlyphLayout）。 */
    private static float textWidth(String s){
        layout.setText(FONT, s);
        return layout.width;
    }

    // ==================== 渲染 ====================

    private static float easeOpen(){
        float p = Mathf.clamp(openTimer / OPEN_TIME);
        return Interp.smooth.apply(p);
    }

    private static class DialogElement extends Element{
        @Override
        public void draw(){
            if(!visible || current == null) return;

            float open = easeOpen();
            float w = PANEL_W;
            float fullH = calcFullHeight();
            float animH = collapsed ? TITLE_H : (TITLE_H + (fullH - TITLE_H) * open);

            float rx = this.x, ry = this.y;

            Draw.z(Layer.flyingUnit + 5f);

            // 背景
            Draw.color(BG, 0.92f);
            Fill.rect(rx + w / 2f, ry + animH / 2f, w, animH);

            // 边框
            Draw.color(BORDER_HI, 1f);
            Lines.stroke(BORDER);
            Lines.rect(rx + 1f, ry + 1f, w - 2f, animH - 2f);

            // 内侧暗边
            Draw.color(BORDER_LO, 0.7f);
            Lines.stroke(1f);
            Lines.rect(rx + 4f, ry + 4f, w - 8f, animH - 8f);

            // 四角标
            drawCorners(rx, ry, animH);

            // 收起态：只画标题
            if(collapsed){
                drawTitle(rx, ry + animH - PAD - BORDER - TITLE_H, w, true);
                Draw.reset();
                return;
            }

            float contentX = rx + PAD + BORDER;
            float contentW = w - 2f * (PAD + BORDER);
            float topY = ry + animH - PAD - BORDER;

            // 顶部光带
            Draw.color(GLOW, 0.10f);
            Fill.rect(rx + w / 2f, topY - 2f, w - 4f, 4f);

            // 名称栏
            if(current.speaker != null){
                drawTitle(rx, ry + animH - PAD - BORDER - TITLE_H, w, false);
                topY -= TITLE_H + 2f;
            }

            // 正文（打字机）
            String[] lines = wrap(current.text, contentW);
            float bodyY = topY;
            drawTypewriterText(lines, contentX, bodyY, contentW);

            float textLineCount = Math.min(lines.length, MAX_BODY_LINES);
            float buttonsTop = bodyY - textLineCount * BODY_LINE_H - 10f;

            // 按钮
            if(!typing && current.choices.length > 0){
                layoutAndDrawButtons(rx + PAD + BORDER, buttonsTop, contentW);
            }

            // 完成指示（无选项时）
            if(!typing && current.choices.length == 0){
                float blink = Mathf.absin(Time.time, 3f);
                Color c = SPEAKER.cpy().a(0.5f + 0.5f * blink);
                FONT.setColor(c);
                FONT.getData().setScale(0.9f);
                FONT.draw("▼ 点击继续", rx + w / 2f, ry + PAD + 6f, Align.center);
                FONT.setColor(Color.white);
                FONT.getData().setScale(1f);
            }

            Draw.reset();
        }

        private void drawCorners(float x, float y, float h){
            Draw.color(BORDER_HI, 0.9f);
            Lines.stroke(2f);
            float cx = x + CORNER, cy = y + CORNER;
            float c2 = CORNER;
            // 左下
            Lines.line(x + 1f, cy, x + 1f + c2, cy);
            Lines.line(cx, y + 1f, cx, y + 1f + c2);
            // 左上
            Lines.line(x + 1f, y + h - cy, x + 1f + c2, y + h - cy);
            Lines.line(cx, y + h - 1f - c2, cx, y + h - 1f);
            // 右下
            Lines.line(x + PANEL_W - 1f - c2, cy, x + PANEL_W - 1f, cy);
            Lines.line(x + PANEL_W - cx, y + 1f, x + PANEL_W - cx, y + 1f + c2);
            // 右上
            Lines.line(x + PANEL_W - 1f - c2, y + h - cy, x + PANEL_W - 1f, y + h - cy);
            Lines.line(x + PANEL_W - cx, y + h - 1f - c2, x + PANEL_W - cx, y + h - 1f);
        }

        private void drawTitle(float x, float titleY, float w, boolean collapsed){
            float ty = titleY + (TITLE_H) / 2f + 4f;
            if(collapsed){
                // 收起态：显示名称 + 展开提示
                if(current.speaker != null){
                    FONT.setColor(SPEAKER);
                    FONT.getData().setScale(1f);
                    FONT.draw(current.speaker, x + PAD + BORDER + 4f, ty, Align.left);
                }
                FONT.setColor(BORDER_HI);
                FONT.getData().setScale(0.9f);
                FONT.draw("▼ 展开", x + w - PAD - BORDER - 4f, ty, Align.right);
                FONT.setColor(Color.white);
                FONT.getData().setScale(1f);
                return;
            }
            // 展开态名称栏底色
            Draw.color(0f, 0f, 0f, 0.35f);
            Fill.rect(x + w / 2f, titleY + TITLE_H / 2f, w, TITLE_H);
            Draw.color(BORDER_HI, 0.5f);
            Lines.stroke(1f);
            Lines.line(x + PAD, titleY, x + w - PAD, titleY);
            FONT.setColor(SPEAKER);
            FONT.getData().setScale(1f);
            FONT.draw(current.speaker, x + PAD + BORDER + 4f, ty, Align.left);
            FONT.setColor(Color.white);
            FONT.getData().setScale(1f);
        }

        /** 打字机渲染：按 visibleChars 逐行绘制。 */
        private void drawTypewriterText(String[] lines, float x, float baseY, float maxW){
            FONT.setColor(BODY);
            FONT.getData().setScale(1f);
            int charsLeft = visibleChars;
            int maxLines = Math.min(lines.length, MAX_BODY_LINES);
            for(int i = 0; i < maxLines && charsLeft > 0; i++){
                String full = lines[i];
                String seg = full.substring(0, Math.min(full.length(), charsLeft));
                FONT.draw(seg, x, baseY - i * BODY_LINE_H, 0f, Align.left, false);
                charsLeft -= full.length();
            }
        }

        private void layoutAndDrawButtons(float x, float topY, float contentW){
            float btnW = contentW;
            buttonRects.clear();
            int n = current.choices.length;
            for(int i = 0; i < n; i++){
                float by = topY - (i + 1f) * BUTTON_H - i * BUTTON_GAP;
                ButtonRect br = new ButtonRect(x, by, btnW, BUTTON_H, current.choices[i]);
                buttonRects.add(br);

                boolean hover = (hoverIndex == i);
                // 底
                Draw.color(hover ? BTN_HOVER : BTN_BG, 0.95f);
                Fill.rect(x + btnW / 2f, by + BUTTON_H / 2f, btnW, BUTTON_H);
                // 边
                Draw.color(BTN_LINE, hover ? 1f : 0.7f);
                Lines.stroke(hover ? 2f : 1f);
                Lines.rect(x + 0.5f, by + 0.5f, btnW - 1f, BUTTON_H - 1f);
                // 高亮条
                if(hover){
                    Draw.color(GLOW, 0.5f);
                    Fill.rect(x + btnW / 2f, by + BUTTON_H - 2f, btnW, 3f);
                }
                // 文字
                FONT.setColor(hover ? Color.white : BODY);
                FONT.getData().setScale(1f);
                FONT.draw(current.choices[i].text, x + btnW / 2f, by + BUTTON_H / 2f + 5f, Align.center);
            }
            FONT.setColor(Color.white);
            FONT.getData().setScale(1f);
        }

        private float calcFullHeight(){
            if(current == null) return TITLE_H;
            String[] lines = wrap(current.text, PANEL_W - 2f * (PAD + BORDER));
            int lineCount = Math.min(lines.length, MAX_BODY_LINES);
            float bodyH = lineCount * BODY_LINE_H;
            float titleH = current.speaker != null ? (TITLE_H + 2f) : 0f;
            int n = current.choices.length;
            float btnH = n > 0 ? (n * BUTTON_H + (n - 1) * BUTTON_GAP) : 0f;
            float bottomPad = (current.choices.length == 0) ? (PAD + 18f) : PAD;
            return PAD + BORDER + titleH + bodyH + btnH + bottomPad + 10f;
        }

        @Override
        public float getWidth(){ return PANEL_W; }

        @Override
        public float getHeight(){ return calcFullHeight(); }

        @Override
        @Nullable
        public Element hit(float x, float y, boolean touchable){
            if(!visible || current == null) return null;
            float fullH = collapsed ? TITLE_H : calcFullHeight();
            if(x < 0 || y < 0 || x > PANEL_W || y > fullH) return null;
            return this;
        }
    }

    // ==================== 输入 ====================

    /** 安装点击监听。 */
    static void installListener(){
        element.addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(!visible || current == null) return false;

                if(typing){
                    // 跳过打字机
                    visibleChars = current.text.length();
                    typing = false;
                    return true;
                }

                if(collapsed){
                    // 展开
                    collapsed = false;
                    openTimer = 0f;
                    return true;
                }

                // 检查按钮
                for(ButtonRect b : buttonRects){
                    if(x >= b.x && x <= b.x + b.w && y >= b.y && y <= b.y + b.h){
                        if(b.choice.callback != null) b.choice.callback.run();
                        close();
                        return true;
                    }
                }

                // 无选项时点击面板关闭
                if(current.choices.length == 0){
                    close();
                    return true;
                }
                return true;
            }
        });
    }
}
