
package com.spica.camera.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.spica.camera.R;

/**
 * Created by Spica 27 on 2021/8/13.
 * <p>
 * 显示对焦动画的视图。
 */
public class FocusView extends View {

    private Handler mHandler = new Handler();

    private boolean isFocusing = false;
    private RectF mFocRectF;
    private int lineSize;
    private Bitmap zoneBitmap;
    private ValueAnimator scaleAnimator;
    private Paint focusPaint;

    public FocusView(Context context) {
        this(context, null);
    }

    public FocusView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FocusView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mFocRectF = new RectF();

        lineSize = dp2px(5);
        focusPaint = new Paint();
        focusPaint.setAntiAlias(true);
        zoneBitmap = ((BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.img)).getBitmap();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isFocusing) {
            drawFocusZone(canvas);
        }
    }

    private void drawFocusZone(Canvas canvas) {
        if (mFocRectF != null) {
            canvas.drawBitmap(zoneBitmap, null, mFocRectF, focusPaint);
        }
    }

    /**
     * 聚焦。
     *
     * @param x 中心点 x 坐标。
     * @param y 中心点 y 坐标。
     */
    public void focusOn(final float x, final float y) {
        isFocusing = true;
        if (scaleAnimator != null && scaleAnimator.isRunning()) {
            scaleAnimator.cancel();
        }
        // 开始聚焦动画。
        scaleAnimator = ValueAnimator.ofInt(dp2px(45), dp2px(28));
        scaleAnimator.setDuration(200);
        scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int offset = (int) valueAnimator.getAnimatedValue();
                mFocRectF.set(x - offset, y - offset, x + offset, y + offset);
                postInvalidate();
            }
        });
        scaleAnimator.start();
        // 定时停止聚焦动画。
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isFocusing = false;
                postInvalidate();
            }
        }, 666L);
    }

    private int dp2px(int dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density + 0.5f);
    }

}