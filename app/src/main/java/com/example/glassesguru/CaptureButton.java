package com.example.glassesguru;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class CaptureButton extends View {

    private Paint paint;
    private RectF oval;
    private float startAngle;
    private boolean isLoading;
    private ValueAnimator animator;
    private OnClickListener onClickListener;

    public CaptureButton(Context context) {
        super(context);
        init();
    }

    public CaptureButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CaptureButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFFFFFFFF); // White color
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(12f); // Stroke width

        oval = new RectF();
        startAngle = -90; // Start at the top
        isLoading = false;

        setClickable(true); // Make the view clickable
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float radius = Math.min(width, height) / 2 - paint.getStrokeWidth();

        float centerX = width / 2;
        float centerY = height / 2;

        oval.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        if (isLoading) {
            canvas.drawArc(oval, startAngle, 360 * (animator.getAnimatedFraction()), false, paint);
        } else {
            canvas.drawCircle(centerX, centerY, radius, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isLoading) {
            return false; // Ignore touch events while loading
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Provide visual feedback for button press (e.g., change color or style)
                paint.setColor(0xFFCCCCCC); // Light grey color for feedback
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                // Reset color back to white
                paint.setColor(0xFFFFFFFF);
                invalidate();
                if (onClickListener != null) {
                    onClickListener.onClick(this);
                }
                startLoadingAnimation();
                return true;
            case MotionEvent.ACTION_CANCEL:
                // Reset color back to white if touch is canceled
                paint.setColor(0xFFFFFFFF);
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void startLoadingAnimation() {
        isLoading = true;
        animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(2000); // Duration for loading
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isLoading = false;
                invalidate();
            }
        });
        animator.start();
    }

    public void completeLoading() {
        if (animator != null) {
            animator.end();
        }
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        this.onClickListener = l;
    }
}

