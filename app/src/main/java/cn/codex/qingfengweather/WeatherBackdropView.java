package cn.codex.qingfengweather;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import cn.codex.qingfengweather.data.WeatherEffect;

import java.util.Random;

/** A subtle, native-canvas weather backdrop with no image or animation dependencies. */
public final class WeatherBackdropView extends View {
    private static final long FRAME_DELAY_MILLIS = 42L;
    private static final int PARTICLE_COUNT = 42;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Particle[] particles = new Particle[PARTICLE_COUNT];
    private final Runnable frame = new Runnable() {
        @Override public void run() {
            if (shouldAnimate()) {
                long now = SystemClock.uptimeMillis();
                if (lastFrameMillis != 0L) phase += Math.min(0.08f, (now - lastFrameMillis) / 1000f);
                lastFrameMillis = now;
                invalidate();
                postDelayed(this, FRAME_DELAY_MILLIS);
            }
        }
    };

    private WeatherEffect effect = WeatherEffect.UNKNOWN;
    private boolean dynamicEnabled = true;
    private boolean foregroundActive;
    private float phase;
    private long lastFrameMillis;
    private Shader backgroundShader;
    private Shader glowShader;
    private int shaderWidth = -1;
    private int shaderHeight = -1;

    public WeatherBackdropView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        Random random = new Random(80421L);
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle(random.nextFloat(), random.nextFloat(),
                    0.045f + random.nextFloat() * 0.11f,
                    0.65f + random.nextFloat() * 1.8f,
                    random.nextFloat() * (float) (Math.PI * 2));
        }
    }

    public void setWeatherEffect(WeatherEffect value) {
        WeatherEffect next = value == null ? WeatherEffect.UNKNOWN : value;
        if (effect == next) return;
        effect = next;
        phase = 0f;
        lastFrameMillis = 0L;
        invalidateShaders();
        invalidate();
        updateAnimation();
    }

    public void setDynamicEnabled(boolean enabled) {
        if (dynamicEnabled == enabled) return;
        dynamicEnabled = enabled;
        invalidateShaders();
        invalidate();
        updateAnimation();
    }

    public void setForegroundActive(boolean active) {
        if (foregroundActive == active) return;
        foregroundActive = active;
        updateAnimation();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAnimation();
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(frame);
        lastFrameMillis = 0L;
        super.onDetachedFromWindow();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidateShaders();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;
        ensureShaders(width, height);
        paint.setShader(backgroundShader);
        paint.setAlpha(255);
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);

        if (!dynamicEnabled) return;
        switch (effect) {
            case SUNNY:
                drawSunGlow(canvas, width, height);
                break;
            case PARTLY_CLOUDY:
                drawSunGlow(canvas, width, height);
                drawClouds(canvas, width, height, 12);
                break;
            case CLOUDY:
                drawClouds(canvas, width, height, 15);
                break;
            case FOG:
                drawFog(canvas, width, height);
                break;
            case RAIN:
                drawRain(canvas, width, height, 30);
                break;
            case THUNDERSTORM:
                drawClouds(canvas, width, height, 20);
                drawRain(canvas, width, height, 37);
                break;
            case SNOW:
                drawSnow(canvas, width, height);
                break;
            case UNKNOWN:
            default:
                break;
        }
    }

    private void drawSunGlow(Canvas canvas, int width, int height) {
        if (glowShader == null) return;
        paint.setShader(glowShader);
        paint.setAlpha(effect == WeatherEffect.SUNNY ? 55 : 38);
        canvas.drawCircle(width * 0.82f, height * 0.13f, Math.max(width, height) * 0.45f, paint);
        paint.setShader(null);
    }

    private void drawClouds(Canvas canvas, int width, int height, int alpha) {
        float drift = (float) Math.sin(phase * 0.18f) * width * 0.025f;
        for (int i = 0; i < 3; i++) {
            float centerX = width * (0.17f + i * 0.36f) + drift * (i % 2 == 0 ? 1 : -1);
            float centerY = height * (0.15f + (i % 2) * 0.11f);
            float cloudWidth = width * (0.38f + (i % 2) * 0.07f);
            float cloudHeight = cloudWidth * 0.24f;
            paint.setColor(i == 1 ? Color.WHITE : Color.rgb(187, 203, 220));
            paint.setAlpha(alpha);
            oval.set(centerX - cloudWidth * 0.50f, centerY - cloudHeight * 0.36f,
                    centerX + cloudWidth * 0.50f, centerY + cloudHeight * 0.48f);
            canvas.drawOval(oval, paint);
            oval.set(centerX - cloudWidth * 0.30f, centerY - cloudHeight * 0.80f,
                    centerX + cloudWidth * 0.03f, centerY + cloudHeight * 0.30f);
            canvas.drawOval(oval, paint);
            oval.set(centerX - cloudWidth * 0.06f, centerY - cloudHeight * 1.0f,
                    centerX + cloudWidth * 0.32f, centerY + cloudHeight * 0.32f);
            canvas.drawOval(oval, paint);
        }
        paint.setAlpha(255);
    }

    private void drawFog(Canvas canvas, int width, int height) {
        for (int i = 0; i < 5; i++) {
            float wave = (float) Math.sin(phase * 0.10f + i * 1.2f) * height * 0.012f;
            float y = height * (0.16f + i * 0.105f) + wave;
            paint.setColor(Color.WHITE);
            paint.setAlpha(27 - i % 2 * 5);
            oval.set(-width * 0.1f, y, width * 1.1f, y + height * 0.035f);
            canvas.drawRoundRect(oval, height * 0.02f, height * 0.02f, paint);
        }
        paint.setAlpha(255);
    }

    private void drawRain(Canvas canvas, int width, int height, int alpha) {
        paint.setColor(Color.rgb(87, 142, 196));
        paint.setStrokeWidth(Math.max(1f, getResources().getDisplayMetrics().density));
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Particle p = particles[i];
            float x = p.x * width + (float) Math.sin(phase * 0.28f + p.seed) * width * 0.008f;
            float y = ((p.y * height + phase * height * p.speed) % (height + height * 0.08f)) - height * 0.04f;
            paint.setAlpha(Math.round(alpha * (0.55f + (p.seed % 0.45f))));
            float length = 9f + p.size * 5f;
            canvas.drawLine(x, y, x + length * 0.30f, y + length, paint);
        }
        paint.setAlpha(255);
    }

    private void drawSnow(Canvas canvas, int width, int height) {
        paint.setColor(Color.WHITE);
        for (Particle p : particles) {
            float x = p.x * width + (float) Math.sin(phase * 0.5f + p.seed) * width * 0.012f;
            float y = ((p.y * height + phase * height * p.speed * 0.30f) % (height + 12f)) - 6f;
            paint.setAlpha(75 + Math.round((p.seed % 1f) * 70));
            canvas.drawCircle(x, y, p.size * 1.5f, paint);
        }
        paint.setAlpha(255);
    }

    private boolean shouldAnimate() {
        return dynamicEnabled && foregroundActive && effect != WeatherEffect.UNKNOWN
                && getVisibility() == VISIBLE && isAttachedToWindow();
    }

    private void updateAnimation() {
        removeCallbacks(frame);
        lastFrameMillis = 0L;
        if (shouldAnimate()) postDelayed(frame, FRAME_DELAY_MILLIS);
    }

    private void ensureShaders(int width, int height) {
        if (shaderWidth == width && shaderHeight == height && backgroundShader != null) return;
        shaderWidth = width;
        shaderHeight = height;
        WeatherEffect backgroundEffect = dynamicEnabled ? effect : WeatherEffect.UNKNOWN;
        int top;
        int bottom;
        switch (backgroundEffect) {
            case SUNNY:
                top = Color.rgb(235, 243, 253); bottom = Color.rgb(250, 247, 239); break;
            case PARTLY_CLOUDY:
                top = Color.rgb(235, 243, 252); bottom = Color.rgb(244, 247, 250); break;
            case CLOUDY:
                top = Color.rgb(232, 239, 247); bottom = Color.rgb(245, 247, 250); break;
            case FOG:
                top = Color.rgb(235, 240, 243); bottom = Color.rgb(247, 248, 249); break;
            case RAIN:
                top = Color.rgb(231, 240, 249); bottom = Color.rgb(240, 246, 251); break;
            case THUNDERSTORM:
                top = Color.rgb(226, 234, 245); bottom = Color.rgb(239, 243, 249); break;
            case SNOW:
                top = Color.rgb(235, 244, 253); bottom = Color.rgb(249, 251, 254); break;
            case UNKNOWN:
            default:
                top = Color.rgb(242, 246, 252); bottom = Color.rgb(242, 246, 252); break;
        }
        backgroundShader = new LinearGradient(0, 0, width, height, top, bottom, Shader.TileMode.CLAMP);
        int glow = Color.rgb(255, 226, 162);
        glowShader = new RadialGradient(width * 0.82f, height * 0.13f,
                Math.max(width, height) * 0.45f, new int[]{glow, Color.TRANSPARENT},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP);
    }

    private void invalidateShaders() {
        backgroundShader = null;
        glowShader = null;
        shaderWidth = -1;
        shaderHeight = -1;
    }

    private static final class Particle {
        final float x;
        final float y;
        final float speed;
        final float size;
        final float seed;

        Particle(float x, float y, float speed, float size, float seed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.size = size;
            this.seed = seed;
        }
    }
}
