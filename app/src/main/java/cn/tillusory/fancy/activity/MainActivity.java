package cn.tillusory.fancy.activity;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import cn.tillusory.fancy.R;
import cn.tillusory.fancy.adapter.HomePageAdapter;
import cn.tillusory.fancy.tools.ToastUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * MainActivity
 * Created by Anko on 2018/1/10.
 */
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

  // 是否拥有相机权限
  public static boolean canUseCamera = false;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    initPermission(); // 初始化权限
    initView();
  }

  // 初始化view
  private void initView() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      //适配高刷新率
      Display.Mode[] modes =
          getWindow().getWindowManager().getDefaultDisplay().getSupportedModes();

      List<Display.Mode> modeList = Arrays.asList(modes);

      Collections.sort(modeList, new Comparator<Display.Mode>() {
        @Override public int compare(Display.Mode mode, Display.Mode t1) {
          return (int) (t1.getRefreshRate() - mode.getRefreshRate());
        }
      });

      Log.e("设置刷新率", modeList.get(0).getRefreshRate() + "");
      WindowManager.LayoutParams lp = getWindow().getAttributes();
      lp.preferredDisplayModeId = modeList.get(0).getModeId();
      getWindow().setAttributes(lp);

    }
    RecyclerView rvList = findViewById(R.id.rv_list);
    rvList.setAdapter(new HomePageAdapter(this));
  }

  private void initPermission() {
    if (checkPermission(this)) {
      canUseCamera = true;// 已经拥有权限
    } else {
      requestPermission(this, "请授予必要权限", 0);
    }
  }

  public static boolean checkPermission(Activity context) {
    return EasyPermissions.hasPermissions(context, Manifest.permission.CAMERA);
  }

  public static void requestPermission(Activity context, String tip,
                                       int requestCode) {
    EasyPermissions.requestPermissions(context, tip, requestCode,Manifest.permission.CAMERA);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String[] permissions, @NonNull int[] grantResults) {

    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
  }

  @Override
  public void onPermissionsGranted(int requestCode,
                                   @NonNull List<String> perms) {
    ToastUtils.getInstance().toast("用户授权成功");
    canUseCamera = true;
  }

  @Override
  public void onPermissionsDenied(int requestCode,
                                  @NonNull List<String> perms) {
    ToastUtils.getInstance().toast("用户授权失败");
    if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
      new AppSettingsDialog.Builder(this).build().show();
    }
  }
}
