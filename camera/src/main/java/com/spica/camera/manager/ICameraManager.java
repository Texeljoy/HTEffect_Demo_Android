package com.spica.camera.manager;

import android.view.MotionEvent;
import com.spica.camera.annotation.Facing;
import com.spica.camera.annotation.Flash;
import com.spica.camera.listener.OnPictureListener;
import com.spica.camera.listener.OnVideoListener;
import com.spica.camera.widget.AutoFitGlSurfaceView;
import java.io.IOException;

/**
 * Created by Spica27 on 2021/8/7.
 * <p>
 * 相机管理接口。
 */
public interface ICameraManager {

    String PICTURE_TYPE = ".jpg";
    String VIDEO_TYPE = ".mp4";

    int SENSOR_UP = 0;
    int SENSOR_LEFT = 90;
    int SENSOR_DOWN = 180;
    int SENSOR_RIGHT = 270;

    /**
     * 恢复相机状态，在 Activity.onResume() 中调用。
     */
    void onResume();

    /**
     * 暂停相机，在 Activity.onPause() 中调用。
     */
    void onPause();

    /**
     * 打开相机。
     *
     * @param viewWidth  预览视图的宽度。
     * @param viewHeight 预览视图的高度。
     */
    void openCamera(int viewWidth, int viewHeight);

    /**
     * 关闭相机。
     */
    void closeCamera();

    /**
     * 切换相机。
     *
     * @param facing 指定相机的方向。
     */
    void switchCamera(@Facing int facing);

    /**
     * 切换闪光灯状态。
     *
     * @param flash 指定闪光灯状态。
     */
    void switchFlash(@Flash int flash);

    /**
     * 拍照。
     */
    void takePicture();

    /**
     * 开始摄像。
     *
     * @throws IOException 初始化 MediaRecorder 时可能抛出异常。
     */
    void startVideoRecord() throws IOException;

    /**
     * 结束录像。
     *
     * @throws Exception MediaRecorder.stop() 可能抛出异常。
     */
    void stopVideoRecord() throws Exception;

    /**
     * 设置拍照监听器。
     *
     * @param listener 拍照监听器。
     */
    void setOnPictureListener(OnPictureListener listener);

    /**
     * 设置录像监听器。
     *
     * @param listener 录像监听器。
     */
    void setOnVideoListener(OnVideoListener listener);

    /**
     * 设置是否可以自动对焦。
     *
     * @param autoFocus 是否可自动对焦。
     */
    void setAutoFocus(boolean autoFocus);

    /**
     * 是否可以自动对焦。
     *
     * @return 是否可以自动对焦。
     */
    boolean isAutoFocus();

    /**
     * 对焦。
     *
     * @param view  预览视图。
     * @param event 预览视图的动作事件。
     */
    void focusOn(AutoFitGlSurfaceView view, MotionEvent event);

    /**
     * 放大预览。
     */
    void zoomIn();

    /**
     * 缩小预览。
     */
    void zoomOut();

}
