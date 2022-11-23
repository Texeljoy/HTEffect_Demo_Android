package com.spica.camera.manager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.spica.camera.DefOptions;
import com.spica.camera.FocusRegionHelper;
import com.spica.camera.R;
import com.spica.camera.Size;
import com.spica.camera.VideoRecorder;
import com.spica.camera.annotation.Facing;
import com.spica.camera.annotation.Flash;
import com.spica.camera.listener.OnCameraListener;
import com.spica.camera.widget.AutoFitGlSurfaceView;
import com.texeljoy.hteffect.HTPictureRenderer;
import com.texeljoy.hteffect.HTPreviewRenderer;
import com.texeljoy.hteffect.egl.HTGLUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static com.spica.camera.DefOptions.FACING_BACK;
import static com.spica.camera.DefOptions.FACING_FRONT;
import static com.spica.camera.DefOptions.FLASH_AUTO;
import static com.spica.camera.DefOptions.FLASH_OFF;
import static com.spica.camera.DefOptions.FLASH_ON;
import static com.spica.camera.DefOptions.FLASH_TORCH;

/**
 * Created by Spica27 on 2021/8/7.
 * <p>
 * Camera2 管理器。
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@SuppressWarnings({"deprecation","unused"})
public class Camera2Manager extends BaseCameraManager implements ImageReader.OnImageAvailableListener {

  /**
   * 判断是否支持新 API。
   *
   * @param context Context。
   * @param facing 摄像头方向。
   */
  public static boolean isSupported(Context context, @Facing int facing) {
    CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    if (manager == null) {
      Log.e("无法使用Camera：", "获取不到相机服务");
      return false;
    }
    try {
      CameraCharacteristics characteristics;
      for (String id : manager.getCameraIdList()) {
        characteristics = manager.getCameraCharacteristics(id);
        Integer internal = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (internal == null || internal != FACING_MAPPING.get(facing)) {
          continue;
        }
        Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (level == null) {
          Log.e("无法使用Camera：", "leve == null");
        } else if (level != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED) {
          Log.e("Camera2：支持等级", "level:" + level);
        }
        return level != null &&
            (level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED ||
                level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
            );
      }
      Log.e("无法使用Camera：", "没有对应的设备");
      return false;
    } catch (CameraAccessException e) {
      e.printStackTrace();
      Log.e("无法使用Camera：", e.getMessage());
      return false;
    }
  }

  /**
   * 摄像头方向映射。
   */
  private static final SparseIntArray FACING_MAPPING = new SparseIntArray();

  static {
    FACING_MAPPING.put(FACING_BACK, CameraCharacteristics.LENS_FACING_BACK);
    FACING_MAPPING.put(FACING_FRONT, CameraCharacteristics.LENS_FACING_FRONT);
  }

  /**
   * Camera2 保证的最大预览宽度。
   */
  private static final int MAX_PREVIEW_WIDTH = 1920;
  /**
   * Camera2 保证的最大预览高度。
   */
  private static final int MAX_PREVIEW_HEIGHT = 1080;

  /**
   * 相机管理器。
   */
  private final CameraManager mCameraManager;
  /**
   * 当前相机的 id 。
   */
  private String mCameraId;
  /**
   * 所有相机 id。
   */
  private List<String> mCameraIdList;

  /**
   * 相机支持的特性。
   */
  private CameraCharacteristics mCameraCharacteristics;

  /**
   * 相机预览的请求创建器。
   */
  private CaptureRequest.Builder mPreviewBuilder;
  /**
   * 相机预览的会话。
   */
  private CameraCaptureSession mPreviewSession;
  /**
   * 相机预览的请求。
   */
  private CaptureRequest mPreviewRequest;
  /**
   * 已打开的相机。
   */
  private CameraDevice mCameraDevice;

  /**
   * 绘制容器
   */
  private Surface surface;

  /**
   * 缩放等级。
   */
  private int zoomLevel = 0;

  /**
   * 相机模板类型。
   */
  private int templateType = CameraDevice.TEMPLATE_PREVIEW;

  /**
   * 后台任务处理。
   */
  private HandlerThread mBackgroundThread;
  private Handler mBackgroundHandler;

  /**
   * 处理图片捕获。
   */
  private ImageReader mImageReader;

  private AbstractPictureCaptureCallback mPictureCaptureCallback = new AbstractPictureCaptureCallback() {

    @Override
    public void onPrecaptureRequired() {
      mPreviewBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
      setState(STATE_PRECAPTURE);
      try {
        mPreviewSession.capture(mPreviewBuilder.build(), this, null);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onReady() {
      captureStillPicture();
    }

  };

  /**
   * 处理视频录制。
   */
  private VideoRecorder mVideoRecorder;

  /**
   * 初始化。
   *
   * @param activity Activity。
   * @param autoFitGlSurfaceView 显示预览的 AutoFitTextureView。
   */
  public Camera2Manager(@NonNull Activity activity, @NonNull AutoFitGlSurfaceView autoFitGlSurfaceView) {
    this(activity, autoFitGlSurfaceView, new DefOptions(), null);
  }

  /**
   * 初始化。
   *
   * @param activity Activity。
   * @param autoFitGlSurfaceView 显示预览的 AutoFitTextureView。
   * @param options 配置项。
   * @param onCameraListener 相机监听
   */
  public Camera2Manager(@NonNull Activity activity, @NonNull AutoFitGlSurfaceView autoFitGlSurfaceView,
                        @NonNull DefOptions options, @Nullable OnCameraListener onCameraListener) {
    super(activity, autoFitGlSurfaceView, options, onCameraListener);
    mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
    if (mCameraManager == null && mOnCameraListener != null&&autoFitGlSurfaceView!=null) {
      mOnCameraListener.onError(new RuntimeException(mActivity.getString(R.string.sch_camera_disable)));
      return;
    }

    mAutoFitGlSurfaceView = autoFitGlSurfaceView;
    //创建纹理
    Log.e("创建纹理", "创建中...");
    textureId = HTGLUtils.getExternalOESTextureID();
    Log.e("创建纹理", "创建完成");
    texture = new SurfaceTexture(textureId);
    Log.e("SurfaceTexture", "创建完成");
    //通知surfaceView更新
    texture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
      @Override
      public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        try {
          autoFitGlSurfaceView.requestRender();
        }catch (Exception e){
         e.printStackTrace();
        }
      }
    });

    try {
      assert mCameraManager != null;
      mCameraIdList = Arrays.asList(mCameraManager.getCameraIdList());
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
    if (mCameraIdList.isEmpty() && mOnCameraListener != null) {
      mOnCameraListener.onError(new RuntimeException(activity.getString(R.string.sch_no_camera)));
      return;
    }
    mCameraId = mCameraIdList.get(0);
  }

  @Override
  public void onResume() {
    super.onResume();
    // 启动后台线程。
    startBackgroundThread();
  }

  @Override
  public void onPause() {
    super.onPause();
    // 关闭后台线程。
    stopBackgroundThread();
  }

  @SuppressLint("MissingPermission")
  @Override
  public void openCamera(int viewWidth, int viewHeight) {
    try {
      // 根据摄像头方向查找对应的摄像头。
      CameraCharacteristics characteristics;
      for (String id : mCameraIdList) {
        characteristics = mCameraManager.getCameraCharacteristics(id);
        Integer internal = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (internal == null || internal != FACING_MAPPING.get(mFacing)) {
          continue;
        }
        if (id.equals(mCameraId)) {
          break;
        }
        mCameraId = id;
        break;
      }

      if (mPreviewSize == null) {
        mPreviewSize = new Size(0, 0);
      }

      mPreviewSize.setWidth(viewWidth);
      mPreviewSize.setHeight(viewHeight);

      // 更新相机参数。
      setUpCameraOutputs();

      // 打开摄像头。
      mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
          mCameraDevice = cameraDevice;
          // 创建预览会话。
          createPreviewSession(CameraDevice.TEMPLATE_PREVIEW, mImageReader.getSurface());
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
          cameraDevice.close();
          mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
          cameraDevice.close();
          mCameraDevice = null;
          if (mOnCameraListener != null) {
            mOnCameraListener.onError(new RuntimeException(mActivity.getString(R.string.sch_camera_disable)));
          }
        }

      }, mBackgroundHandler);
    } catch (Exception e) {
      e.printStackTrace();
      if (mOnCameraListener != null) {
        mOnCameraListener.onError(new RuntimeException(mActivity.getString(R.string.sch_camera_disable)));
      }
    }
  }

  @Override
  public void closeCamera() {
    if (null != mPreviewSession) {
      mPreviewSession.close();
      mPreviewSession = null;
    }
    if (null != mCameraDevice) {
      mCameraDevice.close();
      mCameraDevice = null;
    }
    if (null != mImageReader) {
      mImageReader.close();
      mImageReader = null;
    }
  }

  @Override
  public void switchCamera(@Facing int facing) {
    if (facing != mFacing) {
      mFacing = facing;
      closeCamera();
      mAutoFitGlSurfaceView.queueEvent(new Runnable() {
        @Override public void run() {
          showRenderer.destroy();
          showRenderer = new HTPreviewRenderer(mAutoFitGlSurfaceView.getWidth(),mAutoFitGlSurfaceView.getHeight());
          showRenderer.create(!isBackCamera());

          takePictureRenderer.destroy();
          takePictureRenderer = new HTPictureRenderer(mAutoFitGlSurfaceView.getWidth(),mAutoFitGlSurfaceView.getHeight());
          takePictureRenderer.create(!isBackCamera());
        }
      });
      // 打开相机。
      if (mPreviewSize!=null){
        openCamera(mPreviewSize.getWidth(),
            mPreviewSize.getHeight());
        return;
      }
      openCamera(mAutoFitGlSurfaceView.getWidth(),
          mAutoFitGlSurfaceView.getHeight());
    }
  }

  @Override
  public void switchFlash(@Flash int flash) {
    if (isFlashSupport()) {
      mFlash = flash;
      updatePreview();
    }
  }

  @Override
  public void takePicture() {
    if (isCapturing()) {
      return;
    }
    setCapturing(true);
    if (isAfAvailable() && isAutoFocus()) {
      // 锁定焦点再拍照。
      lockFocus();
    } else {
      // 直接拍照。
      captureStillPicture();
    }
  }

  @Override
  public void startVideoRecord() throws IOException {
    if (isCapturing()) {
      return;
    }
    setCapturing(true);
    // 初始化视频录制配置。
    mVideoRecorder = new VideoRecorder(getOrientation(), mPreviewSize, mVideoFile.getPath());
    mVideoRecorder.start();
    // 创建录制预览的会话。
    createPreviewSession(CameraDevice.TEMPLATE_RECORD, mVideoRecorder.getSurface());
  }

  @Override
  public void stopVideoRecord() throws Exception {
    if (mVideoRecorder == null) {
      return;
    }
    try {
      // 停止录制。
      mVideoRecorder.stop();
      mOnVideoListener.onVideoRecorded(mVideoFile, thumbForVideo(mVideoFile));
      mVideoRecorder = null;
    } finally {
      // 恢复预览会话。
      createPreviewSession(CameraDevice.TEMPLATE_PREVIEW, mImageReader.getSurface());
      setCapturing(false);
    }
  }

  @Override
  public void focusOn(AutoFitGlSurfaceView view, MotionEvent event) {

    // 保存原本的 af ae 配置。
    final Integer originAfMode = mPreviewBuilder.get(CaptureRequest.CONTROL_AF_MODE);
    final Integer originAeMode = mPreviewBuilder.get(CaptureRequest.CONTROL_AE_MODE);

    // 聚焦及测光区域。
    Rect rect = FocusRegionHelper.get(event.getX(), event.getY(), view.getWidth(), view.getHeight(),
        mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE));
    MeteringRectangle[] rectangles = new MeteringRectangle[] { new MeteringRectangle(rect, 800) };

    // 设置聚焦区域。
    mPreviewBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, rectangles);
    mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
    mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);

    // 设置测光区域。
    mPreviewBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, rectangles);
    mPreviewBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);

    try {
      // 创建请求。
      mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
          super.onCaptureCompleted(session, request, result);
          Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
          if (afState == null) {
            return;
          }
          if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState
              || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
            // 聚焦测光结束，恢复预览。
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, originAfMode);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, originAeMode);
            updatePreview();
          }
        }
      }, mBackgroundHandler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void zoomIn() {
    // 放大预览内容。
    zoom(1);
  }

  @Override
  public void zoomOut() {
    // 缩小预览内容。
    zoom(-1);
  }

  @Override
  public void onImageAvailable(ImageReader reader) {
    // 拍照成长，保存照片。
    Image image = reader.acquireNextImage();
    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    savePicture(bytes);
    image.close();

    mOnPictureListener.getOriginalPicture(mPictureFile, thumbForPicture(mPictureFile));

    if (isAfAvailable() && isAutoFocus()) {
      // 解锁焦点。
      unlockFocus();
    }

    setCapturing(false);
  }

  /**
   * 锁定焦点。
   */
  private void lockFocus() {
    mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
    try {
      mPictureCaptureCallback.setState(AbstractPictureCaptureCallback.STATE_LOCKING);
      mPreviewSession.capture(mPreviewBuilder.build(), mPictureCaptureCallback, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  /**
   * 拍照。
   */
  private void captureStillPicture() {
    try {
      texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
      // 拍照请求创建器。
      CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
      // 设置 Surface。
      captureBuilder.addTarget(mImageReader.getSurface());
      // 设置焦点模式。
      captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, mPreviewBuilder.get(CaptureRequest.CONTROL_AF_MODE));
      // 设置闪光灯。
      setFlashMode(captureBuilder);
      // 设置 Orientation。
      captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation());

      mPreviewSession.capture(captureBuilder.build(), null, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  /**
   * 解锁焦点。
   */
  private void unlockFocus() {
    mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
    try {
      mPreviewSession.capture(mPreviewBuilder.build(), mPictureCaptureCallback, null);
      setFlashMode(mPreviewBuilder);
      // 恢复预览。
      mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
      updatePreview();
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  /**
   * 设置相机相关的成员变量。
   */
  private void setUpCameraOutputs() throws Exception {

    mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);

    StreamConfigurationMap map =
        mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
    if (map == null) {
      if (mOnCameraListener != null) {
        mOnCameraListener.onError(new RuntimeException(mActivity.getString(R.string.sch_camera_disable)));
      }
      return;
    }

    // 获取最佳宽高
    mPreviewSize =
        getOptimalSize(Size.convert(map.getOutputSizes(SurfaceTexture.class)),
            mPreviewSize.getWidth(),
            mPreviewSize.getHeight());

    mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 9);

    mImageReader.setOnImageAvailableListener(this, mBackgroundHandler);

    // 将 GLSurfaceView 的纵横比与我们选择的预览大小相匹配。

    try {
      if (mPreviewSize.getWidth() != 0 && mPreviewSize.getHeight() != 0) {
        Log.e("设置比例：",
            Math.min(mPreviewSize.getWidth(), mPreviewSize.getHeight()) + "/" +
                Math.max(mPreviewSize.getWidth(), mPreviewSize.getHeight()));
        mAutoFitGlSurfaceView.post(new Runnable() {
          @Override public void run() {
            mAutoFitGlSurfaceView.setAspectRatio(
                Math.min(mPreviewSize.getWidth(), mPreviewSize.getHeight()),
                Math.max(mPreviewSize.getWidth(), mPreviewSize.getHeight())
            );
          }
        });
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // 检查是否支持闪光灯。
    Boolean available = mCameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
    setFlashSupport(available == null ? false : available);

    // 检查是否支持调焦。
    int[] afModes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
    setAfAvailable(!(afModes == null || afModes.length == 0 ||
        (afModes.length == 1 && afModes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF)));

  }

  /**
   * 创建相机预览会话。
   *
   * @param templateType 预览类型。
   * @param readerSurface 捕获相机数据（拍照/录像）时使用的 Surface。
   */
  private void createPreviewSession(int templateType, Surface readerSurface) {
    this.templateType = templateType;

    mPictureCaptureCallback.setState(AbstractPictureCaptureCallback.STATE_PREVIEW);

    DisplayMetrics dm = mActivity.getResources().getDisplayMetrics();

    Log.e("采集的分辨率：",
        mPreviewSize.getWidth() + "x" + mPreviewSize.getHeight());

    // 设置默认缓冲区的尺寸为预览的尺寸。
    texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

    // 生成接收预览数据的 Surface 。
    surface = new Surface(texture);

    try {
      // 创建预览的请求创建器。
      mPreviewBuilder = mCameraDevice.createCaptureRequest(templateType);
      mPreviewBuilder.addTarget(surface);
      if (templateType == CameraDevice.TEMPLATE_RECORD) {
        mPreviewBuilder.addTarget(readerSurface);
      }
      if (mPreviewSession != null) {
        mPreviewSession.stopRepeating();
      }
      // 创建预览会话。
      mCameraDevice.createCaptureSession(Arrays.asList(surface, readerSurface),
          new CameraCaptureSession.StateCallback() {

            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
              if (mCameraDevice == null) {
                // 相机已经关闭
                return;
              }
              mPreviewSession = cameraCaptureSession;
              updatePreview();
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
              if (mOnCameraListener != null) {
                mOnCameraListener.onError(new RuntimeException(mActivity.getString(R.string.sch_camera_disable)));
              }
            }
          }, mBackgroundHandler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  /**
   * 更新预览。
   */
  private void updatePreview() {
    try {
      // 设置对焦模式。
      if (isAfAvailable() && isAutoFocus()) {
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, templateType == CameraDevice.TEMPLATE_RECORD ?
                                                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO : CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
      } else {
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
      }
      // 设置闪光灯状态。
      setFlashMode(mPreviewBuilder);
      mPreviewRequest = mPreviewBuilder.build();
      mPreviewSession.setRepeatingRequest(mPreviewRequest,
          templateType == CameraDevice.TEMPLATE_RECORD ? null : mPictureCaptureCallback,
          mBackgroundHandler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  /**
   * 设置闪光灯状态。
   *
   * @param builder 请求创建器。
   */
  private void setFlashMode(CaptureRequest.Builder builder) {
    mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    switch (mFlash) {
      case FLASH_OFF:
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        break;
      case FLASH_ON:
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
        break;
      case FLASH_TORCH:
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
        break;
      case FLASH_AUTO:
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
        break;
      default:
        break;
    }
  }

  /**
   * 缩放
   *
   * @param zoomSpeed 缩放速度 ，负数为缩小，整数为放大。
   */
  private void zoom(int zoomSpeed) {
    Float maxZoom = (mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM));
    if (maxZoom == null) {
      return;
    }
    maxZoom *= 10;

    Rect activeRect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
    if (activeRect == null) {
      return;
    }

    zoomLevel += zoomSpeed;
    if (zoomLevel > maxZoom || zoomLevel < 1) {
      zoomLevel -= zoomSpeed;
      return;
    }

    int difW = (int) (activeRect.width() - activeRect.width() / maxZoom);
    int difH = (int) (activeRect.height() - activeRect.height() / maxZoom);
    int cropW = difW / 100 * zoomLevel;
    int cropH = difH / 100 * zoomLevel;
    cropW -= cropW & 3;
    cropH -= cropH & 3;
    Rect zoom = new Rect(cropW, cropH, activeRect.width() - cropW, activeRect.height() - cropH);
    mPreviewBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
    updatePreview();
  }

  /**
   * 获取拍摄方向。
   *
   * @return 方向。
   */
  private int getOrientation() {
    // noinspection ConstantConditions
    int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
    if (isBackCamera()) {
      return (sensorOrientation - mSensorDegrees + 360) % 360;
    } else {
      return (sensorOrientation + mSensorDegrees + 360) % 360;
    }
  }

  /**
   * 启动后台线程。
   */
  private void startBackgroundThread() {
    mBackgroundThread = new HandlerThread("CameraBackground");
    mBackgroundThread.start();
    mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
  }

  /**
   * 关闭后台线程。
   */
  private void stopBackgroundThread() {
    mBackgroundThread.quitSafely();
    try {
      mBackgroundThread.join();
      mBackgroundThread = null;
      mBackgroundHandler = null;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static abstract class AbstractPictureCaptureCallback extends CameraCaptureSession.CaptureCallback {

    static final int STATE_PREVIEW = 1;
    static final int STATE_LOCKING = 2;
    static final int STATE_LOCKED = 3;
    static final int STATE_PRECAPTURE = 4;
    static final int STATE_WAITING = 5;
    static final int STATE_CAPTURING = 6;

    private int mState;

    void setState(int state) {
      mState = state;
    }

    @Override
    public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
      process(partialResult);
    }

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
      process(result);
    }

    private void process(@NonNull CaptureResult result) {
      switch (mState) {
        case STATE_PREVIEW:
          break;
        case STATE_LOCKING: {
          Integer af = result.get(CaptureResult.CONTROL_AF_STATE);
          if (af == null) {
            break;
          }
          if (af == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
              af == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
            Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
            if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
              setState(STATE_CAPTURING);
              onReady();
            } else {
              setState(STATE_LOCKED);
              onPrecaptureRequired();
            }
          }
          break;
        }
        case STATE_PRECAPTURE: {
          Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
          if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
              ae == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED ||
              ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
            setState(STATE_WAITING);
          }
          break;
        }
        case STATE_WAITING: {
          Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
          if (ae == null || ae != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
            setState(STATE_CAPTURING);
            onReady();
          }
          break;
        }
        default:
          break;
      }
    }

    /**
     * Called when it is ready to take a still picture.
     */
    public abstract void onReady();

    /**
     * Called when it is necessary to run the precapture sequence.
     */
    public abstract void onPrecaptureRequired();

  }

}