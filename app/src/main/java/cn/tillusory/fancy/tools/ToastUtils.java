package cn.tillusory.fancy.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;

public class ToastUtils {

  private final Context context;

  public ToastUtils(Context context) {
    this.context = context;
  }

  @SuppressLint("StaticFieldLeak")
  private static ToastUtils toastUtils;

  public static void init(Context context) {
    toastUtils = new ToastUtils(context);
  }

  public static ToastUtils getInstance() {
    return toastUtils;
  }

  public void toast(String message) {
    Toast.makeText(context, message,
        Toast.LENGTH_SHORT).show();
  }

}
