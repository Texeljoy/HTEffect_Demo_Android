package cn.tillusory.fancy.activity;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import cn.tillusory.fancy.tools.HtCamera;
import cn.tillusory.fancy.view.HtGlSurfaceView;
import com.texeljoy.ht_effect.HTPanelLayout;
import com.texeljoy.ht_effect.utils.HtUICacheUtils;
import com.texeljoy.hteffect.HTEffect;
import com.texeljoy.hteffect.HTPictureRenderer;
import com.texeljoy.hteffect.HTPreviewRenderer;
import com.texeljoy.hteffect.egl.HTGLUtils;
import com.texeljoy.hteffect.model.HTRotationEnum;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SampleCameraActivity extends AppCompatActivity implements GLSurfaceView.Renderer {

  private String TAG = "GLSurfaceViewCameraActivity";

  private HtGlSurfaceView glSurfaceView;
  private HtCamera camera;

  private HTPreviewRenderer showRenderer;
  private HTPictureRenderer takePictureRenderer;

  private SurfaceTexture surfaceTexture;
  private int oesTextureId;

  /**
   * 相机采集的宽高
   */
  private final int imageWidth = 1280;
  private final int imageHeight = 720;

  private final boolean isFrontCamera = true;
  private HTRotationEnum tiRotation;
  private int cameraId;

  private boolean isTakePicture = false;
  private int pictureWidth = 720, pictureHeight = 1280;
  private String picturePath;
  private HandlerThread pictureHandlerThread;
  private Handler pictureHandler;

  protected boolean init = false;

  /**
   * 页面显示的宽高
   */
  private int surfaceWidth, surfaceHeight;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    glSurfaceView = new HtGlSurfaceView(this);
    glSurfaceView.setEGLContextClientVersion(2);
    glSurfaceView.setRenderer(this);
    glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

    setContentView(glSurfaceView);
    HTEffect.shareInstance().releaseTextureOESRenderer();
    addContentView(new HTPanelLayout(this).init(getSupportFragmentManager()),
        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    Button button = new Button(this);
    button.setText("拍照");
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        takePicture();
      }
    });
    addContentView(button,
        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    pictureHandlerThread = new HandlerThread("TakePicture");
    pictureHandlerThread.start();
    pictureHandler = new Handler(pictureHandlerThread.getLooper());
    glSurfaceView.setAspectRatio(pictureWidth, pictureHeight);
    camera = new HtCamera(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    try {
      camera.startPreview();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    try {
      camera.stopPreview();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    init = false;
    HTEffect.shareInstance().releaseTextureOESRenderer();
    pictureHandlerThread.quit();
    camera.releaseCamera();
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    Log.i(TAG, "onSurfaceCreated");
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {

    Log.i(TAG, "onSurfaceChanged width = " + width + ", height = " + height);
    surfaceWidth = width;
    surfaceHeight = height;

    showRenderer = new HTPreviewRenderer(surfaceWidth, surfaceHeight);
    showRenderer.create(isFrontCamera);

    takePictureRenderer = new HTPictureRenderer(pictureWidth, pictureHeight);
    takePictureRenderer.create(isFrontCamera);

    oesTextureId = HTGLUtils.getExternalOESTextureID();
    surfaceTexture = new SurfaceTexture(oesTextureId);
    surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
      @Override
      public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        glSurfaceView.requestRender();
      }
    });

    cameraId = isFrontCamera ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
    tiRotation = isFrontCamera ? HTRotationEnum.HTRotationClockwise270 : HTRotationEnum.HTRotationClockwise90;
    camera.openCamera(cameraId, imageWidth, imageHeight);

    camera.setPreviewSurface(surfaceTexture);
    camera.startPreview();
  }

  @Override
  public void onDrawFrame(GL10 gl) {

    if (!init && showRenderer != null) {
      Log.e("初始化:", "------");
      HTEffect.shareInstance().releaseTextureOESRenderer();
      HtUICacheUtils.initCache(false);
      init = HTEffect.shareInstance().initTextureOESRenderer(imageWidth,imageHeight,tiRotation,isFrontCamera,5);
    }

    int textureId = HTEffect.shareInstance().processTextureOES(oesTextureId);


    // Log.e("渲染:", "------------------------");

    if (showRenderer == null) {
      Log.e("ERROR：", "showRender is null");
      return;
    }

    showRenderer.render(textureId);


    if (isTakePicture) {
      ByteBuffer byteBuffer = ByteBuffer.allocateDirect(pictureWidth * pictureHeight * 4);
      byteBuffer.order(ByteOrder.nativeOrder());
      byteBuffer.position(0);
      takePictureRenderer.takePicture(textureId, byteBuffer);
      saveBitmap(pictureWidth, pictureHeight, byteBuffer);
      isTakePicture = false;
    }
    surfaceTexture.updateTexImage();
  }

  private void takePicture() {
    isTakePicture = true;
  }

  private void saveBitmap(final int width, final int height, final ByteBuffer bf) {

    pictureHandler.post(new Runnable() {
      @Override
      public void run() {

        //根据需要自己调节图片的大小，如果卡顿将质量调低即可
        //                Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.copyPixelsFromBuffer(bf);
        picturePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        new File(picturePath).mkdirs();
        picturePath = picturePath + "/" + System.currentTimeMillis() + ".png";

        boolean isSuccess = saveBitmap(result, new File(picturePath));
        Log.e(TAG, "saveBitmap,path:" + picturePath);

      }
    });
  }

  private boolean saveBitmap(Bitmap bitmap, File file) {
    if (bitmap == null) { return false; }
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
      fos.flush();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return false;
  }

}