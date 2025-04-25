package com.example.myapplication.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapplication.MyApplication;
import com.example.myapplication.database.AppDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageLoadWorker extends Worker {

    public ImageLoadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            List<String> imageUris = loadImagePaths();
            if(imageUris.isEmpty()){
                // 新方法不行,只能用暴力方法.
                imageUris = loadImagePathsLegacy();
            }

            AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
            ImageProcessor processor = new ImageProcessor(getApplicationContext());
            List<String> filteredUris = processor.filterExistingUris(imageUris, database.imageDao());

            MyApplication app = MyApplication.getInstance();
            app.setImageUris(filteredUris);
            Data outputData = new Data.Builder()
                    .putInt("image_count", filteredUris.size())
                    .build();
            return Result.success(outputData);
        } catch (Exception e) {
            Log.e("ImageLoadWorker", "Error in doWork", e);
            return Result.failure();
        }
    }

    private List<String> loadImagePaths() {
        List<String> paths = new ArrayList<>();

        // 构建需要扫描的目标目录列表
        File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        String[] targetDirs = {
                new File(dcimDir, "Camera").getAbsolutePath(),
                new File(dcimDir.getParent(), "DCIM/Screenshots").getAbsolutePath()
        };

        // 构建查询条件
        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new ArrayList<>();
        for (String dir : targetDirs) {
            if (!selection.toString().isEmpty()) {
                selection.append(" OR ");
            }
            selection.append(MediaStore.Images.Media.DATA).append(" LIKE ?");
            selectionArgs.add(dir + "/%");
        }

        // 定义需要查询的列
        String[] projection = {MediaStore.Images.Media.DATA};

        // 按添加时间倒序排序
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection.toString(),
                selectionArgs.toArray(new String[0]),
                sortOrder)) {

            if (cursor != null) {
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                // 用于快速判断父目录的集合
                Set<String> targetDirSet = new HashSet<>(Arrays.asList(targetDirs));

                while (cursor.moveToNext()) {
                    String path = cursor.getString(dataColumn);
                    File file = new File(path);

                    // 检查文件父目录是否在目标目录中
                    if (targetDirSet.contains(file.getParent())) {
                        paths.add(path);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "MediaStore查询失败", e);
        }

        return paths;
    }

    private List<String> loadImagePathsLegacy() {
        List<String> paths = new ArrayList<>();

        List<File> scanDirs = new ArrayList<>();
        File dcimDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        if (dcimDir.exists()) {
            scanDirs.add(dcimDir);
        }

        File[] screenshotDirs = new File[] {
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_SCREENSHOTS).getPath()),
                new File(dcimDir.getParent(), "Screenshots"),
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots")
        };
        for (File dir : screenshotDirs) {
            if (dir.exists()) {
                scanDirs.add(dir);
            }
        }

        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Log.d("MainActivity",uri.getPath());

        for (File dir : scanDirs) {
            File[] files = dir.listFiles((file) -> {
                String name = file.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
            });
            Log.d("TAG",String.valueOf(files.length));
            if (files != null) {
                for (File file : files) {
                    paths.add(file.getAbsolutePath());
                }
            }
        }
        return paths;
    }
}