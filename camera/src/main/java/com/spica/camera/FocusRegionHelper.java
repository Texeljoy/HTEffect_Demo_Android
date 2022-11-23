package com.spica.camera;

import android.graphics.Rect;
import android.os.Build;
import androidx.annotation.RequiresApi;
/**
 * Created by Spica27 on 2021/8/7.
 * <p>
 * 焦点范围处理类。
 */
@SuppressWarnings("SuspiciousNameCombination")
public class FocusRegionHelper {

    /**
     * 指定成像区域选择的半径。
     */
    private static final int HALF_AREA_SIZE = 150;

    /**
     * 获取 View 上指定位置对应的相机成像区域。
     *
     * @param x          View 上的 x 坐标。
     * @param y          View 上的 y 坐标。
     * @param viewWidth  View 的宽度。
     * @param viewHeight View 的高度。
     * @return 成像区域。
     */
    public static Rect get(float x, float y, int viewWidth, int viewHeight) {
        if (viewWidth < viewHeight) {
            int tmp = (int) x;
            x = y;
            y = viewWidth - tmp;
            tmp = viewWidth;
            viewWidth = viewHeight;
            viewHeight = tmp;
        }
        int centerX = (int) (x / viewWidth * 2000 - 1000);
        int centerY = (int) (y / viewHeight * 2000 - 1000);

        return new Rect(clamp(centerX - HALF_AREA_SIZE, -1000, 1000),
                clamp(centerY - HALF_AREA_SIZE, -1000, 1000),
                clamp(centerX + HALF_AREA_SIZE, -1000, 1000),
                clamp(centerY + HALF_AREA_SIZE, -1000, 1000));
    }

    /**
     * 获取 View 上指定位置对应的相机成像区域。
     *
     * @param x          View 上的 x 坐标。
     * @param y          View 上的 y 坐标。
     * @param viewWidth  View 的宽度。
     * @param viewHeight View 的高度。
     * @param region     CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
     * @return 成像区域。
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static Rect get(float x, float y, int viewWidth, int viewHeight, Rect region) {
        if (viewWidth < viewHeight) {
            int tmp = (int) x;
            x = y;
            y = viewWidth - tmp;
            tmp = viewWidth;
            viewWidth = viewHeight;
            viewHeight = tmp;
        }
        int centerX = (int) (x / viewWidth * region.width());
        int centerY = (int) (y / viewHeight * region.height());
        return new Rect(clamp(centerX - HALF_AREA_SIZE, 0, region.width()),
                clamp(centerY - HALF_AREA_SIZE, 0, region.height()),
                clamp(centerX + HALF_AREA_SIZE, 0, region.width()),
                clamp(centerY + HALF_AREA_SIZE, 0, region.height()));
    }

    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

}
