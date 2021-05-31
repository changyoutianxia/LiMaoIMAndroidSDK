package com.xinbida.limaoim.utils;

import android.text.TextUtils;

import com.xinbida.limaoim.LiMaoIMApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * 2020-04-01 21:51
 * 文件操作
 */
public class LiMFileUtils {
    private LiMFileUtils() {

    }

    private static class LiMFileUtilsBinder {
        static final LiMFileUtils limFileUtils = new LiMFileUtils();
    }

    public static LiMFileUtils getInstance() {
        return LiMFileUtilsBinder.limFileUtils;
    }

    public boolean fileCopy(String oldFilePath, String newFilePath) {
        //如果原文件不存在
        if (!fileExists(oldFilePath)) {
            return false;
        }
        //获得原文件流
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(new File(oldFilePath));

            byte[] data = new byte[1024];
            //输出流
            FileOutputStream outputStream = new FileOutputStream(new File(newFilePath));
            //开始处理流
            while (inputStream.read(data) != -1) {
                outputStream.write(data);
            }
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean fileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    private String getSDPath() {
        return Objects.requireNonNull(LiMaoIMApplication.getInstance().getContext().getExternalFilesDir(null)).getAbsolutePath();
//        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    private void createFileDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                //按照指定的路径创建文件夹
                file.mkdirs();
            } catch (Exception ignored) {
            }
        }
    }

    private void createFile(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            try {
                //在指定的文件夹中创建文件
                dir.createNewFile();
            } catch (Exception ignored) {
            }
        }

    }

    public String saveFile(String oldPath, String channelId, byte channelType, String fileName) {
        if (TextUtils.isEmpty(channelId) || TextUtils.isEmpty(oldPath)) return "";
        File f = new File(oldPath);
        String tempFileName = f.getName();
        String prefix = tempFileName.substring(tempFileName.lastIndexOf(".") + 1);

        String filePath = String.format("%s/%s/%s/%s", getSDPath(), LiMaoIMApplication.getInstance().getFileCacheDir(), channelType, channelId);
        createFileDir(filePath);//创建文件夹
        String newFilePath = String.format("%s/%s.%s", filePath, fileName, prefix);
        createFile(newFilePath);//创建文件
        fileCopy(oldPath, newFilePath);//复制文件
        return newFilePath;
    }
}
