
package com.spica.camera.widget;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import com.spica.camera.DefOptions;
import com.spica.camera.manager.ICameraManager;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Created by Spica 27 on 2021/8/13.
 * <p>
 * 相机。
 */
@SuppressWarnings("unused")
public class Camera {



  /**
   * 回调
   */
  public interface Callback {

    /**
     * 拍摄结束并已选择文件时的回调函数。
     *
     * @param fileList 已选择的文件列表。
     * 图片文件后缀：{@link ICameraManager#PICTURE_TYPE}，
     * 视频文件后缀：{@link ICameraManager#VIDEO_TYPE}。
     */
    void callback(@NonNull List<String> fileList);

  }

  /**
   * Created by Spica27 on 2021/8/9.
   * <p>
   * 相机配置。
   */
  public static class Options extends DefOptions {

    /**
     * 相机模式，既能拍摄图片，也能拍摄视频。
     */
    public static final int CAMERA_MODE_BOTH = 0;
    /**
     * 相机模式，只能拍摄图片。
     */
    public static final int CAMERA_MODE_PICTURE = 1;
    /**
     * 相机模式，只能拍摄指定时间长度的视频。
     */
    public static final int CAMERA_MODE_VIDEO = 2;
    /**
     * 相机模式，只能拍摄视频，但不限制拍摄的时间长度。
     */
    public static final int CAMERA_MODE_VIDEO_INFINITE = 3;

    /**
     * 是否仅使用旧版 API 。
     */
    private boolean isOnlyOldApi = false;
    /**
     * 最大的视频录制时长。
     */
    private long maxVideoRecordTime = 10 * 1000L;
    /**
     * 最大的拍摄照片/视频数量。
     */
    private int maxProductCount = 1;
    /**
     * 相机模式。
     */
    @CameraMode
    private int cameraMode = CAMERA_MODE_PICTURE;

    /**
     * 是否仅使用旧版 API。
     */
    public boolean isOnlyOldApi() {
      return isOnlyOldApi;
    }

    /**
     * 设置是否仅使用旧版 API。
     *
     * @param onlyOldApi 是否仅使用旧版 API。
     */
    public void setOnlyOldApi(boolean onlyOldApi) {
      isOnlyOldApi = onlyOldApi;
    }

    /**
     * 获取最大的视频录制时长。
     */
    public long getMaxVideoRecordTime() {
      return maxVideoRecordTime;
    }

    /**
     * 设置最大的视频录制时长。
     *
     * @param maxVideoRecordTime 最大的视频录制时长，单位 ms。
     */
    public void setMaxVideoRecordTime(@IntRange(from = 0) long maxVideoRecordTime) {
      this.maxVideoRecordTime = maxVideoRecordTime;
    }

    /**
     * 获取最大的拍摄照片/视频数量。
     */
    public int getMaxProductCount() {
      return maxProductCount;
    }

    /**
     * 设置最大的拍摄照片/视频数量。
     *
     * @param maxProductCount 最大的拍摄照片/视频数量。
     */
    public void setMaxProductCount(@IntRange(from = 1) int maxProductCount) {
      this.maxProductCount = maxProductCount;
    }

    /**
     * 获取相机模式。
     */
    @CameraMode
    public int getCameraMode() {
      return cameraMode;
    }

    /**
     * 设置相机模式。
     *
     * @param cameraMode 相机模式。
     */
    public void setCameraMode(@CameraMode int cameraMode) {
      this.cameraMode = cameraMode;
    }

    @Retention(RetentionPolicy.SOURCE)
    @Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
    @IntDef({ CAMERA_MODE_BOTH, CAMERA_MODE_PICTURE, CAMERA_MODE_VIDEO, CAMERA_MODE_VIDEO_INFINITE }) @interface CameraMode {
    }

  }

}