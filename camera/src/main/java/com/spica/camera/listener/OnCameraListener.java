package com.spica.camera.listener;

import com.spica.camera.annotation.SensorDegrees;

/**
 * Created by Spica27 on 2021/8/7.
 * <p>
 * 相机监听。
 */
public interface OnCameraListener {

    /**
     * 是否支持闪光灯的回调。
     *
     * @param isSupport 是否支持闪光灯。
     */
    void onFlashSupport(boolean isSupport);

    /**
     * 传感器角度改变。
     *
     * @param oldDegrees 改变前的角度。
     * @param newDegrees 改变后的角度。
     */
    void onSensorChanged(@SensorDegrees int oldDegrees, @SensorDegrees int newDegrees);

    /**
     * 相机出错。
     *
     * @param e Exception, e.getMessage() 可获得错误提示。
     */
    void onError(Exception e);

}
