package com.spica.camera.listener;

import java.io.File;

/**
 * Created by Spica27 on 2021/8/7.
 * <p>
 * 拍照监听。
 */
public interface OnPictureListener {

    /**
     * 拍照成功。
     * 返回的原始图片
     *
     * @param file 照片文件。
     * @param thumbFile 缩略图文件。
     */
    void getOriginalPicture(File file, File thumbFile);


    /**
     * 拍照成功。
     * 返回的处理后的图片
     *
     * @param file 照片文件
     */
    void getProcessedPicture(File file);

}
