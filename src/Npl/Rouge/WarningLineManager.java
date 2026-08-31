package Npl.Rouge;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.graphics.*;
import mindustry.ui.*;

public class WarningLineManager {

    private static final float FADE_IN_TIME = 15f;
    private static final float STAY_TIME = 90f;
    private static final float FLASH_TIME = 30f;
    private static final float FADE_OUT_TIME = 20f;
    private static final float TOTAL_TIME = FADE_IN_TIME + STAY_TIME + FLASH_TIME + FADE_OUT_TIME;

    private static final float LINE_HEIGHT = 40f;
    private static final float LINE_WIDTH = 500f;
    private static final float BORDER_THICKNESS = 3f;

    private static final Queue<String> messageQueue = new Queue<>();
    private static String currentMessage = null;
    private static float timer = 0f;
    private static boolean active = false;

    private static WarningLineElement drawElement = null;

    public static void init(){
        drawElement = new WarningLineElement();
        Core.scene.add(drawElement);
    }

    public static void show(String message){
        messageQueue.addLast(message);
    }

    public static void update(){
        if(!active && messageQueue.size > 0){
            currentMessage = messageQueue.removeFirst();
            timer = 0f;
            active = true;
        }

        if(active){
            timer += Time.delta;
            if(timer >= TOTAL_TIME){
                active = false;
                currentMessage = null;
                timer = 0f;
            }
        }
    }

    private static float calculateAlpha(){
        if(timer < FADE_IN_TIME){
            return Interp.smooth.apply(timer / FADE_IN_TIME);
        }else if(timer < FADE_IN_TIME + STAY_TIME){
            return 1f;
        }else if(timer < FADE_IN_TIME + STAY_TIME + FLASH_TIME){
            return 1f;
        }else{
            float fadeProgress = (timer - FADE_IN_TIME - STAY_TIME - FLASH_TIME) / FADE_OUT_TIME;
            return 1f - Interp.smooth.apply(fadeProgress);
        }
    }

    private static float calculateFlashAlpha(){
        if(timer >= FADE_IN_TIME + STAY_TIME && timer < FADE_IN_TIME + STAY_TIME + FLASH_TIME){
            float flashProgress = (timer - FADE_IN_TIME - STAY_TIME) / FLASH_TIME;
            return Mathf.absin(flashProgress * Mathf.PI2 * 4f, 1f);
        }
        return 0f;
    }

    private static float calculateSlideOffset(){
        if(timer < FADE_IN_TIME){
            float progress = timer / FADE_IN_TIME;
            float ease = Interp.smooth.apply(progress);
            return (1f - ease) * -100f;
        }
        return 0f;
    }

    public static boolean isActive(){
        return active;
    }

    public static void clear(){
        active = false;
        currentMessage = null;
        timer = 0f;
        messageQueue.clear();
    }

    private static class WarningLineElement extends Element {
        @Override
        public void draw(){
            if(!active || currentMessage == null) return;

            float screenW = Core.graphics.getWidth();
            float screenH = Core.graphics.getHeight();
            float centerX = screenW / 2f;
            float centerY = screenH / 2f;

            float alpha = calculateAlpha();
            if(alpha <= 0f) return;

            float flashAlpha = calculateFlashAlpha();
            float offsetX = calculateSlideOffset();

            float drawX = centerX + offsetX;
            float drawY = centerY;

            Draw.z(66f);

            Draw.color(Color.red, alpha * 0.7f);
            Fill.rect(drawX, drawY, LINE_WIDTH, LINE_HEIGHT);

            float borderAlpha = alpha * (0.8f + flashAlpha * 0.2f);
            Draw.color(Color.yellow, borderAlpha);
            Lines.stroke(BORDER_THICKNESS);
            Lines.rect(drawX - LINE_WIDTH / 2f, drawY - LINE_HEIGHT / 2f, LINE_WIDTH, LINE_HEIGHT);

            Lines.stroke(1f);
            Draw.color(Color.yellow, borderAlpha * 0.6f);
            Lines.rect(drawX - LINE_WIDTH / 2f + 4f, drawY - LINE_HEIGHT / 2f + 4f, LINE_WIDTH - 8f, LINE_HEIGHT - 8f);

            if(flashAlpha > 0f){
                Draw.color(Color.white, alpha * flashAlpha * 0.3f);
                Fill.rect(drawX, drawY, LINE_WIDTH - 8f, LINE_HEIGHT - 8f);
            }

            Draw.z(Layer.playerName);
            Font font = Fonts.outline;
            font.getData().setScale(1.1f);
            Color fontColor = new Color(Color.white);
            fontColor.a = alpha;
            font.setColor(fontColor);
            font.draw(currentMessage, drawX, drawY + 5f, Align.center);
            font.getData().setScale(1f);
            font.setColor(Color.white);

            Draw.reset();
        }
    }
}
