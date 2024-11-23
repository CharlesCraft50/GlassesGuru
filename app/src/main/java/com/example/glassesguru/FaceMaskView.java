package com.example.glassesguru;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class FaceMaskView extends View {
    private List<PointF> facePoints;
    private Rect boundingBox;
    private Paint paint;

    public FaceMaskView(Context context) {
        super(context);
        init();
    }

    public FaceMaskView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FaceMaskView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
    }

    public void setFacePoints(List<PointF> points) {
        facePoints = points;
        invalidate();  // Request to redraw the view
    }

    public void setBoundingBox(Rect bounds) {
        boundingBox = bounds;
        invalidate();  // Request to redraw the view
    }

    public void clearFaceMask() {
        facePoints = null;
        boundingBox = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (facePoints != null) {
            Path path = new Path();
            path.moveTo(facePoints.get(0).x, facePoints.get(0).y);
            for (int i = 1; i < facePoints.size(); i++) {
                path.lineTo(facePoints.get(i).x, facePoints.get(i).y);
            }
            path.close();  // Close the path to form a closed shape
            canvas.drawPath(path, paint);
        }

        if (boundingBox != null) {
            // Draw a rectangle around the detected face
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(boundingBox, paint);
        }
    }
}
