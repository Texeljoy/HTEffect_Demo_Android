package cn.tillusory.fancy.tools;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import java.io.IOException;
import java.util.List;

/**
 * @ClassName TiCamera
 * @Description 基于CAMERA1简单封装的相机工具库
 * @Author Spica2 7
 * @Date 2021/12/31 14:08
 */
@SuppressWarnings("unused")
public class HtCamera {
  private final String TAG = "HtCamera";

  private Camera camera;

  private Context context;

  private int width = 1280,
      height = 720;

  public HtCamera(Context context) {
    this.context = context;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public Camera.Parameters getParameters() {
    return camera.getParameters();
  }

  public void setParameters(Camera.Parameters parameters) {
    camera.setParameters(parameters);
  }

  public void autoFocus(Camera.Parameters parameters, Camera.AutoFocusCallback callback) {
    camera.setParameters(parameters);
    camera.autoFocus(callback);
  }

  public void openCamera(int cameraId, int width, int height) {

    camera = Camera.open(cameraId);

    Camera.Parameters parameters = camera.getParameters();
    setCameraDisplayOrientation(context, cameraId, camera);
    parameters.setPreviewFormat(ImageFormat.NV21);
    parameters.setPreviewSize(width, height);
    camera.setParameters(parameters);
  }

  public void setPreviewSurface(SurfaceTexture previewSurface) {
    try {
      camera.setPreviewTexture(previewSurface);
    } catch (IOException e) {
      Log.e(TAG, e.getMessage());
      throw new RuntimeException("TiCamera fails to setPreviewSurface");
    }
  }

  public void startPreview() {
    camera.startPreview();
    Log.i(TAG, "TiCamera startPreview");
  }

  public void stopPreview() {
    camera.stopPreview();
    Log.i(TAG, "TiCamera stopPreview");
  }

  public void releaseCamera() {
    if (camera != null) {
      camera.setPreviewCallback(null);
      camera.stopPreview();
      camera.release();
      camera = null;
    }

    Log.i(TAG, "TiCamera releaseCamera");
  }

  public boolean[] getIsSupportedPreviewList() {
    boolean[] list = new boolean[3];
    List<Camera.Size> supportedList = camera.getParameters().getSupportedPreviewSizes();

    for (Camera.Size size : supportedList) {
      if (size.width == 640 && size.height == 480) {
        list[0] = true;
        Log.e(TAG, "480 is support");
      }
      if (size.width == 1280 && size.height == 720) {
        list[1] = true;
        Log.e(TAG, "720 is support");
      }
      if (size.width == 1920 && size.height == 1080) {
        list[2] = true;
        Log.e(TAG, "1080 is support");
      }
    }

    return list;
  }

  private void setCameraDisplayOrientation(Context context, int cameraId, Camera camera) {
    Camera.CameraInfo info = new Camera.CameraInfo();
    Camera.getCameraInfo(cameraId, info);
    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    if (windowManager == null) {
      Log.e(TAG, "WindowManager is null");
      return;
    }
    int rotation = windowManager.getDefaultDisplay().getRotation();
    int degrees = 0;
    switch (rotation) {
      case Surface.ROTATION_0:
        degrees = 0;
        break;
      case Surface.ROTATION_90:
        degrees = 90;
        break;
      case Surface.ROTATION_180:
        degrees = 180;
        break;
      case Surface.ROTATION_270:
        degrees = 270;
        break;
    }

    int result;
    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      result = (info.orientation + degrees) % 360;
      result = (360 - result) % 360;  // compensate the mirror
    } else {  // back-facing
      result = (info.orientation - degrees + 360) % 360;
    }
    camera.setDisplayOrientation(result);
  }

}