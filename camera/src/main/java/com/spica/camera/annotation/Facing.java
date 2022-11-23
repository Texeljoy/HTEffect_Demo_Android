package com.spica.camera.annotation;

import androidx.annotation.IntDef;
import com.spica.camera.DefOptions;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Spica27 on 2021/8/7.
 * <p>
 * 摄像头方向。
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@IntDef({ DefOptions.FACING_BACK, DefOptions.FACING_FRONT})
public @interface Facing {
}
