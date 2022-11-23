package com.spica.camera.widget;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.spica.camera.DefOptions;
import com.spica.camera.R;
import com.spica.camera.annotation.Facing;
import com.spica.camera.annotation.Flash;
import com.spica.camera.listener.OnCameraListener;
import com.spica.camera.listener.OnCameraListenerAdapter;
import com.spica.camera.listener.OnPictureListener;
import com.spica.camera.listener.OnVideoListener;
import com.spica.camera.manager.Camera2Manager;
import com.spica.camera.manager.CameraManager;
import com.spica.camera.manager.ICameraManager;
import com.texeljoy.hteffect.HTEffect;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Spica27 on 2021/8/1.
 * <p>
 * 拍照/视频录制的基类。
 */
public abstract class BaseCameraActivity extends AppCompatActivity implements OnPictureListener, OnVideoListener {

  /**
   * Intent 传递配置选项的 key。
   */
  protected static final String INTENT_KEY_OPTIONS = "options";

  protected static Camera.Callback sCallback;

  /**
   * 权限
   */
  private final int permissionRequestCode = 103;
  private final String[] permissionArrayPicture = new String[] {
      Manifest.permission.CAMERA
  };

  /**
   * 视图
   */
  protected AutoFitGlSurfaceView autoFitGlSurfaceView;
  protected FocusView focusView;

  private View ibtnFlash;
  private View ibtnSwitchCamera;


  private final List<View> rotatableViewList = new ArrayList<>();

  /**
   * 配置项
   */
  protected Camera.Options mOptions;

  /**
   * 相机管理器
   */
  protected ICameraManager mCameraManager;

  /**
   * 相机方向
   */
  @Facing
  private int mFacing;

  @Flash
  private int mFlash;


  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {

    // 设置 4.4 及以上版本导航栏透明。
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

    super.onCreate(savedInstanceState);

    HTEffect.shareInstance().releaseTextureOESRenderer();

    initOptions();
    initView();

    if (requestCameraPermission()) {
      initCameraManager();
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  private void initView() {
    setContentView(R.layout.layout_camera);
    ibtnFlash = findViewById(R.id.ibtn_flash);


    ibtnFlash.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        mFlash = mFlash == DefOptions.FLASH_ON ? DefOptions.FLASH_OFF : DefOptions.FLASH_ON;
        mCameraManager.switchFlash(mFlash);
      }
    });

    // 预览
    autoFitGlSurfaceView = findViewById(R.id.aftv_preview);

    autoFitGlSurfaceView.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent event) {
        // 对焦及缩放处理。
        int pointerCount = event.getPointerCount();
        if (event.getAction() == MotionEvent.ACTION_DOWN && pointerCount == 1) {
          focusOn((AutoFitGlSurfaceView) view, event);
        } else if (pointerCount == 2) {
          zoom(event);
        }
        return true;
      }
    });

    // 焦点
    focusView = findViewById(R.id.focus_view);
    focusView.bringToFront();

    // 切换相机
    ibtnSwitchCamera = findViewById(R.id.ibtn_switch_camera);
    ibtnSwitchCamera.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mFacing = mFacing == DefOptions.FACING_BACK ? DefOptions.FACING_FRONT : DefOptions.FACING_BACK;
        mCameraManager.switchCamera(mFacing);
      }
    });

    addRotatableView(ibtnSwitchCamera);

  }

  /**
   * 获取配置项。
   */
  private void initOptions() {
    mOptions = (Camera.Options) getIntent().getSerializableExtra(INTENT_KEY_OPTIONS);
    if (mOptions == null) {
      mOptions = new Camera.Options();
      mOptions.setCameraMode(Camera.Options.CAMERA_MODE_PICTURE);
      mOptions.setMaxProductCount(1);
      mOptions.setOnlyOldApi(false);
    }
    mFacing = mOptions.getFacing();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mCameraManager != null) {
      mCameraManager.onResume();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (mCameraManager != null) {
      mCameraManager.onPause();
    }
    if (isFinishing()) {
      HTEffect.shareInstance().releaseTextureOESRenderer();
    }
  }


  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode != permissionRequestCode) {
      return;
    }
    for (int grantResult : grantResults) {
      if (grantResult == PackageManager.PERMISSION_DENIED) {
        Toast.makeText(this, "请授予必要权限", Toast.LENGTH_SHORT).show();
        finish();
        return;
      }
    }
    initCameraManager();
  }

  /**
   * 申请权限。
   */
  protected boolean requestCameraPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return true;
    }
    String[] permissionArray = permissionArrayPicture;
    for (String permission : permissionArray) {
      if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(permissionArray, permissionRequestCode);
        return false;
      }
    }
    return true;
  }

  /**
   * 初始化相机。
   */
  private void initCameraManager() {
    OnCameraListener onCameraListener = new OnCameraListenerAdapter() {

      @Override
      public void onFlashSupport(boolean isSupport) {
        // 是否支持闪光灯
        ibtnFlash.setVisibility(isSupport ? View.VISIBLE : View.GONE);
      }

      @Override
      public void onSensorChanged(int oldDegrees, int newDegrees) {
        rotatingView(oldDegrees, newDegrees);
      }

      @Override
      public void onError(Exception e) {
        toast(e.getMessage());
        finish();
      }
    };
    if (mOptions.isOnlyOldApi()) {
      mCameraManager = new CameraManager(this, autoFitGlSurfaceView, mOptions, onCameraListener);
      autoFitGlSurfaceView.setEGLContextClientVersion(2);
      autoFitGlSurfaceView.setRenderer((CameraManager) mCameraManager);
      autoFitGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
      Log.e("使用的Camera版本：", ">>> 1 <<<");
      Log.e("原因：", "mOptions.isOnlyOldApi：true");
    } else if (
        Camera2Manager.isSupported(this, mOptions.getFacing())) {
      Log.e("使用的Camera版本：", ">>> 2 <<<");
      mCameraManager = new Camera2Manager(this, autoFitGlSurfaceView, mOptions, onCameraListener);
      autoFitGlSurfaceView.setEGLContextClientVersion(2);
      autoFitGlSurfaceView.setRenderer((Camera2Manager) mCameraManager);
      autoFitGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    } else {
      mCameraManager = new CameraManager(this, autoFitGlSurfaceView, mOptions, onCameraListener);
      autoFitGlSurfaceView.setEGLContextClientVersion(2);
      autoFitGlSurfaceView.setRenderer((CameraManager) mCameraManager);
      autoFitGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
      Log.e("使用的Camera版本：", ">>> 1 <<<");
      Log.e("原因：", "不兼容 Camera2 api");
    }
    mCameraManager.setOnPictureListener(this);
    mCameraManager.setOnVideoListener(this);
  }


  /**
   * 聚焦。
   */
  private void focusOn(AutoFitGlSurfaceView view, MotionEvent event) {
    mCameraManager.focusOn(view, event);
    focusView.focusOn(event.getX(), event.getY());
  }

  private float prevFingerSpacing = -1;

  /**
   * 缩放。
   *
   * @param event 触摸事件。
   */
  private void zoom(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_UP) {
      prevFingerSpacing = -1;
      return;
    }
    float fingerSpacing = getFingerSpacing(event);
    if (prevFingerSpacing < 0) {
      prevFingerSpacing = fingerSpacing;
      return;
    }
    int fingerSpacingValid = 15;
    if (Math.abs(fingerSpacing - prevFingerSpacing) < fingerSpacingValid) {
      return;
    }
    if (fingerSpacing > prevFingerSpacing) {
      mCameraManager.zoomIn();
    } else {
      mCameraManager.zoomOut();
    }
    prevFingerSpacing = fingerSpacing;
  }

  /**
   * 获取手指间的距离。
   *
   * @param event 触摸事件。
   */
  private float getFingerSpacing(MotionEvent event) {
    float x = event.getX(0) - event.getX(1);
    float y = event.getY(0) - event.getY(1);
    return (float) Math.sqrt(x * x + y * y);
  }

  /**
   * 添加根据屏幕方向旋转的视图。
   */
  protected void addRotatableView(View view) {
    rotatableViewList.add(view);
  }

  /**
   * 旋转视图。
   *
   * @param oldDegrees 改变前的角度。
   * @param newDegrees 改变后的角度。
   */
  protected void rotatingView(int oldDegrees, int newDegrees) {

    if (oldDegrees == ICameraManager.SENSOR_UP && newDegrees == ICameraManager.SENSOR_RIGHT) {
      oldDegrees = 360;
    } else if (oldDegrees == ICameraManager.SENSOR_RIGHT && newDegrees == ICameraManager.SENSOR_UP) {
      newDegrees = 360;
    }

    int targetDegrees = newDegrees - oldDegrees;

    for (final View view : rotatableViewList) {
      view.setRotation(oldDegrees);
      view.animate().rotationBy(targetDegrees).setDuration(300L);
    }
  }

  /**
   * Toast。
   */
  protected void toast(final String msg) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(BaseCameraActivity.this, msg, Toast.LENGTH_SHORT).show();
      }
    });
  }


}