package com.spica.camera;

import com.spica.camera.annotation.Facing;
import com.spica.camera.annotation.Flash;
import java.io.Serializable;

/**
 * Created by Spica27 on 2021/8/7.
 * <p>
 * 基础配置。
 */
public class DefOptions implements Serializable {

    /**
     * 摄像头方向，后置。
     */
    public static final int FACING_BACK = 0;
    /**
     * 摄像头方向，前置。
     */
    public static final int FACING_FRONT = 1;

    /**
     * 闪光灯状态，关闭。
     */
    public static final int FLASH_OFF = 0;
    /**
     * 闪光灯状态，打开。
     */
    public static final int FLASH_ON = 1;
    /**
     * 闪光灯状态，自动。
     */
    public static final int FLASH_AUTO = 2;
    /**
     * 闪光灯状态，常量。
     */
    public static final int FLASH_TORCH = 3;

    private boolean isAutoFocus = true;
    @Facing
    private int facing = FACING_FRONT;
    @Flash
    private int flash = FLASH_OFF;

    /**
     * 是否支持自动对焦。
     */
    public boolean isAutoFocus() {
        return isAutoFocus;
    }

    /**
     * 设置是否自动对焦, 默认 true。
     *
     * @param autoFocus 是否自动对焦。
     */
    public void setAutoFocus(boolean autoFocus) {
        isAutoFocus = autoFocus;
    }

    /**
     * 获取相机方向。
     */
    @Facing
    public int getFacing() {
        return facing;
    }

    /**
     * 设置相机方向, 默认 FACING_BACK。
     *
     * @param facing 相机方向。
     */
    public void setFacing(@Facing int facing) {
        this.facing = facing;
    }

    /**
     * 获取闪光灯状态。
     */
    @Flash
    public int getFlash() {
        return flash;
    }

    /**
     * 设置闪光灯状态，FLASH_ON。
     *
     * @param flash 闪光灯状态。
     */
    public void setFlash(@Flash int flash) {
        this.flash = flash;
    }

}
