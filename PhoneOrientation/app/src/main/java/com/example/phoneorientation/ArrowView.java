package com.example.phoneorientation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class ArrowView extends View {

    // ── Colors ───────────────────────────────────────────────────────────────
    private static final int COLOR_BG_ACTIVE   = 0xFF0D1B2A;
    private static final int COLOR_BG_INACTIVE = 0xFF1A1A2E;
    private static final int COLOR_RING_ACTIVE = 0xFF4FC3F7;
    private static final int COLOR_RING_IDLE   = 0xFF37474F;
    private static final int COLOR_ARROW       = 0xFF4FC3F7;
    private static final int COLOR_ARROW_IDLE  = 0xFF37474F;
    private static final int COLOR_TAIL        = 0xFF1565C0;
    private static final int COLOR_CENTER      = 0xFFFFFFFF;
    private static final int COLOR_TICK        = 0x554FC3F7;
    private static final int COLOR_LABEL       = 0xFF78909C;

    // ── Paint objects ─────────────────────────────────────────────────────────
    private final Paint bgPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tailPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint= new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── State ─────────────────────────────────────────────────────────────────
    /** Rotation of the arrow in degrees (0 = up, 90 = right, –90 = left). */
    private float arrowAngle = 0f;
    private boolean isActive = false;

    // ── Constructors ──────────────────────────────────────────────────────────

    public ArrowView(Context context) {
        super(context);
        init();
    }

    public ArrowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ArrowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    // ── Initialisation ─────────────────────────────────────────────────────────

    private void init() {
        bgPaint.setStyle(Paint.Style.FILL);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(4f);

        arrowPaint.setStyle(Paint.Style.FILL);

        tailPaint.setStyle(Paint.Style.FILL);

        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(COLOR_CENTER);

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(2f);
        tickPaint.setColor(COLOR_TICK);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(COLOR_LABEL);
        labelPaint.setFakeBoldText(true);
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Sets the arrow rotation angle and redraws.
     *
     * @param degrees 0° = arrow pointing up; positive = clockwise rotation.
     */
    public void setAngle(float degrees) {
        arrowAngle = degrees;
        invalidate();
    }

    /**
     * Controls whether the view renders in its active (lit) or idle (dim) style.
     */
    public void setActive(boolean active) {
        if (this.isActive != active) {
            this.isActive = active;
            invalidate();
        }
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final float w  = getWidth();
        final float h  = getHeight();
        final float cx = w / 2f;
        final float cy = h / 2f;
        final float outerRadius = Math.min(cx, cy) - dpToPx(8);
        final float innerRadius = outerRadius - dpToPx(3);   // ring inner edge

        // ── 1. Background circle ──────────────────────────────────────────────
        bgPaint.setColor(isActive ? COLOR_BG_ACTIVE : COLOR_BG_INACTIVE);
        canvas.drawCircle(cx, cy, outerRadius, bgPaint);

        // ── 2. Outer ring ─────────────────────────────────────────────────────
        ringPaint.setColor(isActive ? COLOR_RING_ACTIVE : COLOR_RING_IDLE);
        canvas.drawCircle(cx, cy, outerRadius, ringPaint);

        // ── 3. Cardinal tick marks ────────────────────────────────────────────
        drawTicks(canvas, cx, cy, innerRadius);

        // ── 4. Cardinal labels (N / S / E / W) ───────────────────────────────
        labelPaint.setTextSize(dpToPx(11));
        float labelR = innerRadius * 0.78f;
        canvas.drawText("N", cx,             cy - labelR + dpToPx(5), labelPaint);
        canvas.drawText("S", cx,             cy + labelR + dpToPx(5), labelPaint);
        canvas.drawText("E", cx + labelR,    cy + dpToPx(5),          labelPaint);
        canvas.drawText("W", cx - labelR,    cy + dpToPx(5),          labelPaint);

        // ── 5. Rotating arrow ─────────────────────────────────────────────────
        canvas.save();
        canvas.rotate(arrowAngle, cx, cy);

        final float shaftLen = innerRadius * 0.58f;
        final float headLen  = innerRadius * 0.28f;
        final float headHalf = innerRadius * 0.16f;
        final float tailLen  = innerRadius * 0.24f;
        final float tailHalf = innerRadius * 0.07f;

        // Arrow head (tip pointing up in local space before rotation)
        arrowPaint.setColor(isActive ? COLOR_ARROW : COLOR_ARROW_IDLE);
        Path head = new Path();
        head.moveTo(cx,                  cy - shaftLen - headLen);  // tip
        head.lineTo(cx - headHalf,       cy - shaftLen);            // left shoulder
        head.lineTo(cx + headHalf,       cy - shaftLen);            // right shoulder
        head.close();
        canvas.drawPath(head, arrowPaint);

        // Arrow shaft
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(dpToPx(4));
        canvas.drawLine(cx, cy - shaftLen, cx, cy + tailLen, arrowPaint);
        arrowPaint.setStyle(Paint.Style.FILL);

        // Tail (small notch indicating back of arrow)
        tailPaint.setColor(isActive ? COLOR_TAIL : COLOR_ARROW_IDLE);
        Path tail = new Path();
        tail.moveTo(cx,            cy + tailLen);
        tail.lineTo(cx - tailHalf, cy + tailLen - dpToPx(4));
        tail.lineTo(cx + tailHalf, cy + tailLen - dpToPx(4));
        tail.close();
        canvas.drawPath(tail, tailPaint);

        canvas.restore();

        // ── 6. Centre pivot dot ───────────────────────────────────────────────
        centerPaint.setColor(isActive ? COLOR_CENTER : COLOR_ARROW_IDLE);
        canvas.drawCircle(cx, cy, dpToPx(5), centerPaint);
    }

    /**
     * Draws short tick marks at the four cardinal points and eight
     * intercardinal positions around the inner edge of the bezel.
     */
    private void drawTicks(Canvas canvas, float cx, float cy, float radius) {
        for (int i = 0; i < 16; i++) {
            double angleRad = Math.toRadians(i * 22.5);
            float tickLen = (i % 4 == 0) ? dpToPx(10) : dpToPx(5);

            float outerX = cx + (float) Math.sin(angleRad) * radius;
            float outerY = cy - (float) Math.cos(angleRad) * radius;
            float innerX = cx + (float) Math.sin(angleRad) * (radius - tickLen);
            float innerY = cy - (float) Math.cos(angleRad) * (radius - tickLen);

            canvas.drawLine(outerX, outerY, innerX, innerY, tickPaint);
        }
    }

    // ── Sizing ─────────────────────────────────────────────────────────────────

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Request a square: prefer the available width, capped at 280 dp
        int size = (int) Math.min(
                MeasureSpec.getSize(widthMeasureSpec),
                dpToPx(280));
        setMeasuredDimension(size, size);
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }
}