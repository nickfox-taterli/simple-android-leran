package com.example.myapplication.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionHelper {
    // 定义所有需要的权限
    public static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
    };

    // 权限对应的说明（用于申请时展示）
    private static final Map<String, String> PERMISSION_DESCRIPTIONS = new HashMap<String, String>() {{
        put(Manifest.permission.READ_EXTERNAL_STORAGE, "需要读取存储权限以访问您的文件");
        put(Manifest.permission.WRITE_EXTERNAL_STORAGE, "需要写入存储权限以保存文件");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            put(Manifest.permission.READ_MEDIA_IMAGES, "需要访问您的图片以便选择或编辑");
            put(Manifest.permission.READ_MEDIA_VIDEO, "需要访问您的视频以便选择或编辑");
            put(Manifest.permission.READ_MEDIA_AUDIO, "需要访问您的音频文件以便选择或播放");
        }
    }};
    // 请求码
    private static final int PERMISSION_REQUEST_CODE = 1001;

    // 检查并申请权限
    public static boolean checkAndRequestPermissions(Activity activity) {

        List<String> missingPermissions = new ArrayList<>();

        // 检查所有需要的权限
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (missingPermissions.isEmpty()) {
            return true; // 所有权限都已授予
        }

        // 申请缺失的权限
        requestPermissions(activity, missingPermissions.toArray(new String[0]));
        return false;
    }

    // 处理权限申请结果
    public static boolean handlePermissionResult(int requestCode, String[] permissions,
                                                 int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false; // 有权限被拒绝
                }
            }
            return true; // 所有权限都被授予
        }
        return false;
    }

    // 检查是否所有权限都已授予
    public static boolean hasAllPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // 获取被拒绝的权限说明
    public static String getDeniedPermissionDescription(Context context) {
        StringBuilder description = new StringBuilder();
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                if (PERMISSION_DESCRIPTIONS.containsKey(permission)) {
                    if (description.length() > 0) {
                        description.append("\n");
                    }
                    description.append(PERMISSION_DESCRIPTIONS.get(permission));
                }
            }
        }
        return description.toString();
    }

    // 私有方法：实际发起权限申请
    private static void requestPermissions(Activity activity, String[] permissions) {
        // 如果需要解释，可以在这里显示一个对话框
        // 这里简单起见直接申请权限
        ActivityCompat.requestPermissions(activity, permissions, PERMISSION_REQUEST_CODE);
    }
}