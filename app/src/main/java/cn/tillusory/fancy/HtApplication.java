package cn.tillusory.fancy;

import android.app.Application;
import cn.tillusory.fancy.tools.ToastUtils;
import com.texeljoy.hteffect.HTEffect;
import com.texeljoy.hteffect.HTEffect.InitCallback;

/**
 * TiApplication
 * Created by Anko on 2018/1/9.
 */
public class HtApplication extends Application {

    // 鉴权是否完成
    public static boolean hasInit = false;

    @Override
    public void onCreate() {
        super.onCreate();
        HTEffect.shareInstance().initHTEffect( this, "YOUR_APP_ID", new InitCallback() {
            @Override public void onInitSuccess() {
                hasInit = true;

            }

            @Override public void onInitFailure() {
                hasInit = false;

            }
        });



        ToastUtils.init(this);
    }





}
