package cn.tillusory.fancy.activity;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;

import com.spica.camera.widget.BaseCameraActivity;
import com.texeljoy.ht_effect.HTPanelLayout;
import java.io.File;

/**
 * 优先Camera2 Api 如果硬件等级低于level3则使用 Camera 1 Api
 */
public class Camera2Activity extends BaseCameraActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addContentView(new HTPanelLayout(this).init(getSupportFragmentManager()),
        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
  }

  @Override
  public void getOriginalPicture(File file, File thumbFile) {
    //

  }

  @Override
  public void getProcessedPicture(File file) {

  }

  @Override
  public void onVideoRecorded(File file, File thumbFile) {

  }

}