package com.spica.camera.manager;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ThumbnailUtils;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.provider.MediaStore;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.spica.camera.DefOptions;
import com.spica.camera.Size;
import com.spica.camera.annotation.Facing;
import com.spica.camera.annotation.Flash;
import com.spica.camera.annotation.SensorDegrees;
import com.spica.camera.listener.OnCameraListener;
import com.spica.camera.listener.OnPictureListener;
import com.spica.camera.listener.OnVideoListener;
import com.spica.camera.widget.AutoFitGlSurfaceView;
import com.texeljoy.ht_effect.utils.HtUICacheUtils;
import com.texeljoy.hteffect.HTEffect;
import com.texeljoy.hteffect.HTPictureRenderer;
import com.texeljoy.hteffect.HTPreviewRenderer;
import com.texeljoy.hteffect.model.HTRotationEnum;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Spica27 on 2021/8/8.
 * <p>
 * 相机管理基类。
 */
abstract class BaseCameraManager implements ICameraManager, SensorEventListener, GLSurfaceView.Renderer {

    Activity mActivity;

    AutoFitGlSurfaceView mAutoFitGlSurfaceView;

    SurfaceTexture texture;

    int textureId = -1;

    protected boolean init = false;

    /**
     * 当前相机是否支持调焦。
     */
    private boolean isAfAvailable = false;
    /**
     * 当前相机是否支持闪光灯。
     */
    private boolean isFlashSupport = false;
    /**
     * 是否支持自动对焦。
     */
    private boolean isAutoFocus = true;

    /**
     * 是否正在拍摄。
     */
    private boolean isCapturing = false;

    /**
     * 相机预览尺寸
     */
    Size mPreviewSize;

    HTPreviewRenderer showRenderer;

    HTPictureRenderer takePictureRenderer;

    /**
     * 相机方向。
     */
    @Facing
    int mFacing;
    /**
     * 摄像机模式。
     */
    @Flash
    int mFlash;

    /**
     * 拍摄图片原图的保存路径。
     * getExternalFilesDir(null)/camera_picture.mp4
     * /storage/emulated/0/Android/data/[packageName]/files/camera_picture.jpg
     */
    File mPictureFile;

    /**
     * 拍摄图片的处理后的图片的保存路径
     */
    File mProgressFile;

    /**
     * 拍摄视频的保存路径。
     * getExternalFilesDir(null)/camera_video.mp4
     * /storage/emulated/0/Android/data/[packageName]/files/camera_video.mp4
     */
    File mVideoFile;

    /**
     * 相机监听器。
     */
    @Nullable
    OnCameraListener mOnCameraListener;
    /**
     * 拍照监听器。
     */
    OnPictureListener mOnPictureListener;
    /**
     * 拍视频的监听器。
     */
    OnVideoListener mOnVideoListener;

    /**
     * 传感器管理器。
     */
    private SensorManager sensorManager;
    /**
     * 传感器角度。
     */
    @SensorDegrees
    int mSensorDegrees = SENSOR_UP;

    /**
     * 标记来采集美颜后的画面
     */
    boolean isTakePhoto;

    /**
     * 初始化。
     *
     * @param activity Activity。
     * @param autoFitGlSurfaceView 显示预览的 AutoFitTextureView。
     */
    BaseCameraManager(@NonNull Activity activity, @NonNull AutoFitGlSurfaceView autoFitGlSurfaceView) {
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
    BaseCameraManager(@NonNull Activity activity, @NonNull AutoFitGlSurfaceView autoFitGlSurfaceView,
                      @NonNull DefOptions options, @Nullable OnCameraListener onCameraListener) {
        mActivity = activity;
        mAutoFitGlSurfaceView = autoFitGlSurfaceView;
        isAutoFocus = options.isAutoFocus();
        mFacing = options.getFacing();
        mFlash = options.getFlash();
        this.mOnCameraListener = onCameraListener;

        mVideoFile = new File(activity.getExternalFilesDir(null), String.format("camera_video%s", VIDEO_TYPE));
        mPictureFile = new File(activity.getExternalFilesDir(null), String.format("camera_picture%s", PICTURE_TYPE));
        mProgressFile = new File(activity.getExternalFilesDir(null), String.format("progress_picture%s", PICTURE_TYPE));
        sensorManager = (SensorManager) activity.getSystemService(Activity.SENSOR_SERVICE);
    }

    @Override
    public void onResume() {
        // 注册传感器监听器。
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL);

        mAutoFitGlSurfaceView.post(new Runnable() {
            @Override public void run() {
                // 打开相机。
                if (mPreviewSize != null) {
                    openCamera(mPreviewSize.getWidth(),
                        mPreviewSize.getHeight());
                    return;
                }
                openCamera(mAutoFitGlSurfaceView.getWidth(),
                    mAutoFitGlSurfaceView.getHeight());
            }
        });

    }

    @Override
    public void onPause() {
        // 关闭相机。
        closeCamera();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void setOnPictureListener(OnPictureListener listener) {
        this.mOnPictureListener = listener;
    }

    @Override
    public void setOnVideoListener(OnVideoListener listener) {
        this.mOnVideoListener = listener;
    }

    @Override
    public void setAutoFocus(boolean autoFocus) {
        this.isAutoFocus = autoFocus;
    }

    @Override
    public boolean isAutoFocus() {
        return isAutoFocus;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // 传感器方向发生改变。
        synchronized (this) {
            if (sensorEvent.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
                return;
            }
            final int oldDegrees = mSensorDegrees;
            float minXY = -1.5F, maxXY = 1.5F;
            float x = sensorEvent.values[0], y = sensorEvent.values[1];
            if (x < maxXY && x > minXY) {
                if (y > maxXY) {
                    mSensorDegrees = SENSOR_UP;
                } else if (y < minXY) {
                    mSensorDegrees = SENSOR_DOWN;
                }
            } else if (y < maxXY && y > minXY) {
                if (x > maxXY) {
                    mSensorDegrees = SENSOR_LEFT;
                } else if (x < minXY) {
                    mSensorDegrees = SENSOR_RIGHT;
                }
            }
            if (mSensorDegrees != oldDegrees && mOnCameraListener != null) {
                mOnCameraListener.onSensorChanged(oldDegrees, mSensorDegrees);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {

    }

    @Override public void onSurfaceChanged(GL10 gl10, int width, int height) {
        showRenderer = new HTPreviewRenderer(width, height);
        showRenderer.create(!isBackCamera());
        takePictureRenderer = new HTPictureRenderer(width, height);
        takePictureRenderer.create(!isBackCamera());
        if (mAutoFitGlSurfaceView == null || mPreviewSize == null) {
            return;
        }
    }

    @Override public void onDrawFrame(GL10 gl10) {
        texture.updateTexImage();

        if (mPreviewSize == null) {
            return;
        }

        if (!init && showRenderer != null) {
            Log.e("初始化:", "------");
            HTEffect.shareInstance().releaseTextureOESRenderer();
            HtUICacheUtils.initCache(false);

            Log.e("width:", mPreviewSize.getWidth() + "px");
            Log.e("height:", mPreviewSize.getHeight() + "px");

            init = HTEffect.shareInstance().initTextureOESRenderer(
                mPreviewSize.getWidth(),
                mPreviewSize.getHeight(),
                !isBackCamera() ?
                HTRotationEnum.HTRotationClockwise270 : HTRotationEnum.HTRotationClockwise90,
                !isBackCamera(),
                5
            );

        }
        textureId = HTEffect.shareInstance().processTextureOES(textureId);

        if (showRenderer == null) {
            Log.e("ERROR：", "showRender is null");
            return;
        }

        showRenderer.render(textureId);

        if (isTakePhoto) {
            ByteBuffer buf = ByteBuffer.allocateDirect(mAutoFitGlSurfaceView.getWidth() * mAutoFitGlSurfaceView.getHeight() * 4);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.position(0);
            GLES20.glReadPixels(0, 0,
                mAutoFitGlSurfaceView.getWidth(),
                mAutoFitGlSurfaceView.getHeight(),
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                buf);

            buf.rewind();
            Bitmap bmp = Bitmap.createBitmap(mAutoFitGlSurfaceView.getWidth(), mAutoFitGlSurfaceView.getHeight(), Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(buf);

            Matrix matrix = new Matrix();
            matrix.preScale(1f, -1f);
            Bitmap bmp2 = Bitmap.createBitmap(bmp, 0, 0,
                bmp.getWidth(), bmp.getHeight(), matrix, false);

            mOnPictureListener.getProcessedPicture(saveBitmap(mProgressFile.getAbsolutePath()
                .replace(PICTURE_TYPE, String.format("_thumb%s", PICTURE_TYPE)), bmp2));

            bmp2.recycle();

            isTakePhoto = false;
        }

    }

    /**
     * 当前是否正在使用后置摄像头。
     */
    boolean isBackCamera() {
        return mFacing == DefOptions.FACING_BACK;
    }

    /**
     * 是否支持调焦。
     */
    boolean isAfAvailable() {
        return isAfAvailable;
    }

    /**
     * 设置是否支持调焦。
     *
     * @param afAvailable 是否支持调焦。
     */
    void setAfAvailable(boolean afAvailable) {
        isAfAvailable = afAvailable;
    }

    /**
     * 是否支持闪光灯。
     */
    boolean isFlashSupport() {
        return isFlashSupport;
    }

    /**
     * 设置是否支持闪光灯。
     *
     * @param flashSupport 是否支持闪光灯。
     */
    void setFlashSupport(boolean flashSupport) {
        isFlashSupport = flashSupport;
        if (mOnCameraListener != null) {
            mOnCameraListener.onFlashSupport(isFlashSupport);
        }
    }

    /**
     * 是否正在拍摄。
     */
    boolean isCapturing() {
        return isCapturing;
    }

    /**
     * 设置是否正在拍摄。
     *
     * @param capturing 是否正在拍摄。
     */
    void setCapturing(boolean capturing) {
        isCapturing = capturing;
    }

    /**
     * 获取合适的预览尺寸。
     *
     * @param sizeList 支持的尺寸列表。
     * @param viewWidth view的宽度。
     * @param viewHeight view的高度。
     */
    Size getOptimalSize(List<Size> sizeList, int viewWidth, int viewHeight) {

        if (sizeList == null) {
            Log.e("参数列表为空", "返回原宽高");
            return new Size(viewWidth, viewHeight);
        }

        final double aspectTolerance = 0.1;
        double targetRatio = (double) viewHeight / viewWidth;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Size size : sizeList) {
            Log.e("支持的宽高", size.getWidth() + "x" + size.getHeight());
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > aspectTolerance) {
                continue;
            }
            if (Math.abs(size.getHeight() - viewHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - viewHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizeList) {
                if (Math.abs(size.getHeight() - viewHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - viewHeight);
                }
            }
        }
        Log.e("返回宽高", optimalSize.getWidth() + "x" + optimalSize.getHeight());
        return optimalSize;
    }

    /**
     * 保存图片。
     *
     * @param imageBytes 图片字节数组。
     */
    void savePicture(byte[] imageBytes) {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(mPictureFile);
            if (isBackCamera()) {
                output.write(imageBytes);
            } else {
                // 水平翻转前置摄像头的图片。
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                Matrix matrix = new Matrix();
                matrix.setScale(-1, 1);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 为图片保存缩略图。
     *
     * @param file 原文件。
     * @return 缩略图文件。
     */
    File thumbForPicture(File file) {
        // 生成缩略图。
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        Bitmap thumb = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        return saveBitmap(file.getAbsolutePath().replace(PICTURE_TYPE, String.format("_thumb%s", PICTURE_TYPE)), thumb);
    }

    /**
     * 为视频保存缩略图。
     *
     * @param file 原文件。
     * @return 缩略图文件。
     */
    File thumbForVideo(File file) {
        // 生成缩略图。
        Bitmap thumb = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);
        return saveBitmap(file.getAbsolutePath().replace(VIDEO_TYPE, String.format("_thumb%s", PICTURE_TYPE)), thumb);
    }

    /**
     * 保存图片。
     */
    private File saveBitmap(String targetPath, Bitmap bitmap) {
        File file = new File(targetPath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

}
