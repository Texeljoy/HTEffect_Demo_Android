package com.spica.camera.listener;

import java.io.File;

/**
 * Created by Spica27 on 2021/8/7.
 * <p>
 * 视频录制监听。
 */
public interface OnVideoListener {

    /**
     * 视频录制成功。
     *
     * @param file      视频文件。
     * @param thumbFile 缩略图文件。
     */
    void onVideoRecorded(File file, File thumbFile);

}
