package com.spica.camera.manager;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.spica.camera.DefOptions;
import com.spica.camera.FocusRegionHelper;
import com.spica.camera.R;
import com.spica.camera.Size;
import com.spica.camera.VideoRecorder;
import com.spica.camera.annotation.Facing;
import com.spica.camera.annotation.Flash;
import com.spica.camera.listener.OnCameraListener;
import com.spica.camera.widget.AutoFitGlSurfaceView;
import com.texeljoy.hteffect.HTEffect;
import com.texeljoy.hteffect.HTPictureRenderer;
import com.texeljoy.hteffect.HTPreviewRenderer;
import com.texeljoy.hteffect.egl.HTGLUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.spica.camera.DefOptions.FLASH_AUTO;
import static com.spica.camera.DefOptions.FLASH_OFF;
import static com.spica.camera.DefOptions.FLASH_ON;
import static com.spica.camera.DefOptions.FLASH_TORCH;

/**
 * Created by Spica27 on 2021/8/7.
 * <p>
 * 相机管理。
 */
@SuppressWarnings({"deprecation","unused"})
public class CameraManager extends BaseCameraManager {

    /**
     * 相机
     */
    private Camera mCamera;
    /**
     * 相机参数
     */
    private Parameters mCameraParameters;
    /**
     * 相机信息。
     */
    private final Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();


    /**
     * 当前相机 id。
     */
    private int mCameraId = 0;
    /**
     * 预览尺寸。
     */
    private Size mPictureSize;
    /**
     * 视频尺寸。
     */
    private Size mVideoSize;

    private VideoRecorder mVideoRecorder;

    /**
     * 初始化。
     *
     * @param activity Activity。
     * @param autoFitGlSurfaceView 显示预览的 AutoFitTextureView。
     */
    public CameraManager(@NonNull Activity activity,
                         @NonNull AutoFitGlSurfaceView autoFitGlSurfaceView) {
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
    public CameraManager(@NonNull Activity activity, @NonNull AutoFitGlSurfaceView autoFitGlSurfaceView,
                         @NonNull DefOptions options, @Nullable OnCameraListener onCameraListener) {
        super(activity, autoFitGlSurfaceView, options, onCameraListener);

        mAutoFitGlSurfaceView = autoFitGlSurfaceView;



        //创建纹理;
        textureId = HTGLUtils.getExternalOESTextureID();
        texture = new SurfaceTexture(textureId);
        //通知surfaceView更新
        texture.setOnFrameAvailableListener(surfaceTexture -> mAutoFitGlSurfaceView.requestRender());

        if (Camera.getNumberOfCameras() <= 0) {
            if (mOnCameraListener != null) {
                mOnCameraListener.onError(new RuntimeException(mActivity.getString(R.string.sch_no_camera)));
            }
        }
    }

    @Override
    public void openCamera(int viewWidth, int viewHeight) {
        try {

            // 根据相机方向查找对应的相机。
            Camera.CameraInfo info = new Camera.CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == mFacing) {
                    mCameraId = i;
                    break;
                }
            }


            Camera.getCameraInfo(mCameraId, mCameraInfo);


            // 打开相机。
            mCamera = Camera.open(mCameraId);

            mCameraParameters = mCamera.getParameters();

            mCameraParameters.setPreviewFormat(ImageFormat.NV21);
            // 设置对焦模式
            List<String> supportedFocusModes = mCameraParameters.getSupportedFocusModes();
            if (isAutoFocus() && supportedFocusModes != null &&
                supportedFocusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mCameraParameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                setAfAvailable(true);
            } else {
                setAfAvailable(false);
            }
            mCamera.cancelAutoFocus();

            // 设置闪关灯模式
            List<String> supportedFlashModes = mCameraParameters.getSupportedFlashModes();
            if (supportedFlashModes != null && !supportedFlashModes.isEmpty()) {
                if (!(supportedFlashModes.size() == 1 && supportedFlashModes.contains(Parameters.FLASH_MODE_OFF))) {
                    setFlashSupport(true);
                    switchFlash(mFlash);
                } else {
                    setFlashSupport(false);
                }
            } else {
                setFlashSupport(false);
            }

            List<Size> sizeList =
                Size.convert(mCameraParameters.getSupportedPreviewSizes());

            // for (Size item : sizeList) {
            //     Log.e("支持:分辨率", item.getWidth() + "x" + item.getHeight());
            // }

            // 根据传入的长宽从硬件支持的分辨率中选则最接近的

            mPreviewSize = getOptimalSize(
                sizeList,//硬件支持的所有分辨率列表
                viewWidth,//宽
                viewHeight//长
            );

            mCameraParameters.setPreviewSize(mPreviewSize.getWidth(),
                mPreviewSize.getHeight());

            // 设置照片的尺寸
            mPictureSize = getOptimalSize
                (Size.convert(mCameraParameters.getSupportedPictureSizes()),
                    mPreviewSize.getWidth(), mPreviewSize.getHeight());

            mPreviewSize = getOptimalSize
                (Size.convert(mCameraParameters.getSupportedPictureSizes()),
                    mPreviewSize.getWidth(), mPreviewSize.getHeight());

            Log.e("设置采集分辨率：", mPictureSize.getWidth()
                + "X" + mPictureSize.getHeight());
            mCameraParameters.setPictureSize(mPictureSize.getWidth(),
                mPictureSize.getHeight());

            // 视频尺寸。
            List<Camera.Size> videoSizes = mCameraParameters.getSupportedVideoSizes();
            if (videoSizes == null) {
                videoSizes = mCameraParameters.getSupportedPreviewSizes();
            }

            mVideoSize = getOptimalSize(Size.convert(videoSizes),
                mPreviewSize.getWidth(), mPreviewSize.getHeight());

            try {
                if (viewHeight != 0 && viewWidth != 0) {
                    Log.e("设置比例：",
                        Math.min(mPreviewSize.getWidth(), mPreviewSize.getHeight()) + "/" +
                            Math.max(mPreviewSize.getWidth(), mPreviewSize.getHeight()));
                    mAutoFitGlSurfaceView.post(() -> mAutoFitGlSurfaceView.setAspectRatio(
                        Math.min(mPreviewSize.getWidth(), mPreviewSize.getHeight()),
                        Math.max(mPreviewSize.getWidth(), mPreviewSize.getHeight())
                    ));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            mCamera.setParameters(mCameraParameters);

            mCamera.setDisplayOrientation(getOrientation());

            mCamera.setPreviewTexture(texture);

            // 开始预览。

            mCamera.startPreview();
            mAutoFitGlSurfaceView.queueEvent(() -> HTEffect.shareInstance().releaseTextureOESRenderer());

        } catch (Exception e) {
            e.printStackTrace();
            if (mOnCameraListener != null) {
                mOnCameraListener.onError(new RuntimeException(mActivity.getString(R.string.sch_camera_disable)));
            }
        }
    }


    @Override
    public void closeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void switchCamera(@Facing int facing) {
        if (facing != mFacing) {
            mFacing = facing;
            closeCamera();
            mAutoFitGlSurfaceView.queueEvent(() -> {
                showRenderer.destroy();
                showRenderer = new HTPreviewRenderer(mAutoFitGlSurfaceView.getWidth(), mAutoFitGlSurfaceView.getHeight());
                showRenderer.create(!isBackCamera());

                takePictureRenderer.destroy();
                takePictureRenderer = new HTPictureRenderer(mAutoFitGlSurfaceView.getWidth(),mAutoFitGlSurfaceView.getHeight());
                takePictureRenderer.create(!isBackCamera());
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
            switch (mFlash) {
                case FLASH_OFF:
                    mCameraParameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                    break;
                case FLASH_ON:
                    mCameraParameters.setFlashMode(Parameters.FLASH_MODE_ON);
                    break;
                case FLASH_TORCH:
                    mCameraParameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                    break;
                case FLASH_AUTO:
                    mCameraParameters.setFlashMode(Parameters.FLASH_MODE_AUTO);
                    break;
                default:
                    break;
            }
            mCamera.setParameters(mCameraParameters);
        }
    }

    @Override
    public void takePicture() {
        isTakePhoto = true;
        if (isCapturing()) {
            return;
        }
        setCapturing(true);
        if (isAfAvailable() && isAutoFocus()) {
            mCamera.cancelAutoFocus();
            mCamera.autoFocus((success, camera) -> {
                // 自动对焦成功后拍照。
                captureStillPicture();
            });
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
        mCamera.unlock();
        // 开始录像。
        mVideoRecorder = new VideoRecorder(mCamera, getRotation(), mVideoSize, mVideoFile.getPath());
        mVideoRecorder.start();
    }

    @Override
    public void stopVideoRecord() throws Exception {
        if (mVideoRecorder == null) {
            return;
        }
        mCamera.lock();
        // 录像结束。
        mVideoRecorder.stop();
        mOnVideoListener.onVideoRecorded(mVideoFile, thumbForVideo(mVideoFile));
        mVideoRecorder = null;
        setCapturing(false);
    }


    @Override
    public void focusOn(AutoFitGlSurfaceView view, MotionEvent event) {

        mCamera.cancelAutoFocus();

        // 保存当前的焦点模式。
        final String currentFocusMode = mCameraParameters.getFocusMode();

        // 计算聚焦区域。
        Rect rect = FocusRegionHelper.get(event.getX(), event.getY(), view.getWidth(), view.getHeight());
        List<Camera.Area> areaList = Collections.singletonList(new Camera.Area(rect, 800));

        // 设置聚焦区域。
        if (mCameraParameters.getMaxNumFocusAreas() > 0) {
            mCameraParameters.setFocusAreas(areaList);
            mCameraParameters.setFocusMode(Parameters.FOCUS_MODE_MACRO);
        }
        // 设置测光区域。
        if (mCameraParameters.getMaxNumMeteringAreas() > 0) {
            mCameraParameters.setMeteringAreas(areaList);
        }

        mCamera.setParameters(mCameraParameters);

        mCamera.autoFocus((success, camera) -> {
            // 聚焦结束后恢复焦点模式。
            mCameraParameters.setFocusMode(currentFocusMode);
            camera.setParameters(mCameraParameters);
        });
    }

    @Override
    public void zoomIn() {
        if (!mCameraParameters.isZoomSupported()) {
            return;
        }
        // 放大预览内容。
        int zoom = mCameraParameters.getZoom();
        if (zoom < mCameraParameters.getMaxZoom()) {
            mCameraParameters.setZoom(zoom + 1);
        }
        mCamera.setParameters(mCameraParameters);
    }

    @Override
    public void zoomOut() {
        if (!mCameraParameters.isZoomSupported()) {
            return;
        }
        // 缩小预览内容。
        int zoom = mCameraParameters.getZoom();
        if (zoom > 0) {
            mCameraParameters.setZoom(zoom - 1);
        }
        mCamera.setParameters(mCameraParameters);
    }

    /**
     * 拍照。
     */
    private void captureStillPicture() {
        mCameraParameters.setRotation(getRotation());
        mCamera.setParameters(mCameraParameters);
        mCamera.takePicture(null, null, null, (data, camera) -> {
            // 保存照片。
            savePicture(data);
            // 回调。
            mOnPictureListener.getOriginalPicture(mPictureFile, thumbForPicture(mPictureFile));
            camera.cancelAutoFocus();
            // 恢复预览。
            camera.startPreview();
            setCapturing(false);
        });
    }

    /**
     * 获取拍摄方向。
     *
     * @return 方向。
     */
    private int getOrientation() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, cameraInfo);
        if (isBackCamera()) {
            return (cameraInfo.orientation - mSensorDegrees + 360) % 360;
        } else {
            return (cameraInfo.orientation + mSensorDegrees + 180) % 360;
        }
    }

    /**
     * 获取拍摄方向。
     *
     * @return 方向。
     */
    private int getRotation() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, cameraInfo);
        if (isBackCamera()) {
            return (cameraInfo.orientation - mSensorDegrees + 360) % 360;
        } else {
            return (cameraInfo.orientation + mSensorDegrees + 360) % 360;
        }
    }



}
