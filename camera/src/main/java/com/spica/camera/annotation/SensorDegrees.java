
package com.spica.camera.annotation;

import androidx.annotation.IntDef;
import com.spica.camera.manager.ICameraManager;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Spica27 on 2021/8/7.
 * <p>
 * 传感器角度。
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@IntDef({ ICameraManager.SENSOR_UP, ICameraManager.SENSOR_LEFT, ICameraManager.SENSOR_DOWN, ICameraManager.SENSOR_RIGHT})
public @interface SensorDegrees {
}