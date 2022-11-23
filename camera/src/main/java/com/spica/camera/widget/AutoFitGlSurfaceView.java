package com.spica.camera.widget;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * A {@link GLSurfaceView} that can be adjusted to a specified aspect ratio.
 */
public class AutoFitGlSurfaceView extends GLSurfaceView {

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    public AutoFitGlSurfaceView(Context context) {
        this(context, null);
    }

    public AutoFitGlSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                int calcHeight = width * mRatioHeight / mRatioWidth;
                if (calcHeight >= height) {
                    setMeasuredDimension(width, calcHeight);
                } else {
                    setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
                }
            } else {
                int calcWidth = height * mRatioWidth / mRatioHeight;
                if (calcWidth >= width) {
                    setMeasuredDimension(calcWidth, height);
                } else {
                    setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
                }
            }

        }

    }

}
