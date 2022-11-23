
package com.spica.camera.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import com.spica.camera.R;

/**
 * Created by Spica 27 on 2021/8/10.
 * <p>
 * 圆角显示的 ImageView。
 */
public class RoundImageView extends AppCompatImageView {

    /**
     * 按宽度自适应高度。
     */
    private final int WRAPPER_WIDTH = 1;
    /**
     * 按高度自适应宽度。
     */
    private final int WRAPPER_HEIGHT = 2;

    private Path mPath = new Path();
    private RectF mRectF;

    /**
     * 圆角半径
     */
    private float radio;
    /**
     * 自适应方式。
     */
    private int wrapper;

    public RoundImageView(Context context) {
        this(context, null);
    }

    public RoundImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RoundImageView);
        radio = array.getFloat(R.styleable.RoundImageView_radio, 0);
        wrapper = array.getInt(R.styleable.RoundImageView_wrapper, WRAPPER_WIDTH);
        array.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size;
        if (wrapper == WRAPPER_HEIGHT) {
            size = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        } else {
            size = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        }
        setMeasuredDimension(size, size);
        if (radio < 0) {
            radio = size;
        }
        if (mRectF == null) {
            mRectF = new RectF();
        }
        mRectF.set(0, 0, size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPath.reset();
        mPath.addRoundRect(mRectF, radio, radio, Path.Direction.CCW);
        canvas.clipPath(mPath);
        super.onDraw(canvas);
    }

}