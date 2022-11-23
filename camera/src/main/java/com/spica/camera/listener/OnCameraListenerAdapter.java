package com.spica.camera.listener;

/**
 * Created by Spica27 on 2021/8/7.
 * <p>
 * {@link OnCameraListener} 的空实现。
 */
public class OnCameraListenerAdapter implements OnCameraListener {

    @Override
    public void onFlashSupport(boolean isSupport) {
    }

    @Override
    public void onSensorChanged(int oldDegrees, int newDegrees) {
    }

    @Override
    public void onError(Exception e) {
    }

}
