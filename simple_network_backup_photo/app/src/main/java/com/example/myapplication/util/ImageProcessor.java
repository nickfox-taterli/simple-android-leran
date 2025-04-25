package com.example.myapplication.util;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.example.myapplication.database.ImageDao;
import com.example.myapplication.database.ImageEntity;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessor {
    private final Context context;

    public ImageProcessor(Context context) {
        this.context = context;
    }

    // 方法1实现：过滤已存在的图片
    public List<String> filterExistingUris(List<String> uris, ImageDao dao) {
        List<String> result = new ArrayList<>();

        // 分批查询优化
        int chunkSize = 100;
        for (int i = 0; i < uris.size(); i += chunkSize) {
            int end = Math.min(uris.size(), i + chunkSize);
            List<String> chunk = uris.subList(i, end);

            List<String> existing = dao.getExistingPaths(chunk);
            for (String uri : chunk) {
                if (!existing.contains(uri)) {
                    result.add(uri);
                }
            }
        }

        return result;
    }

    // 方法2实现：处理并插入单张图片
    public void processImage(String path, ImageDao dao) {
        File file = new File(path);
        if (!file.exists()) return;

        String md5 = calculateMD5(file);
        long size = file.length();
        int[] dimensions = getImageDimensions(file);

        ImageEntity entity = new ImageEntity(
                path, md5, size, dimensions[0], dimensions[1]);

        dao.insert(entity);
    }

    private String calculateMD5(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            fis.close();

            byte[] md5Bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : md5Bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private int[] getImageDimensions(File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        return new int[]{options.outWidth, options.outHeight};
    }
}