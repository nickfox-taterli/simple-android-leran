package com.example.myapplication;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {
    private static MyApplication instance;
    private List<String> imageUris = new ArrayList<>();
    private String imageUrisToUpload;

    // 获取单例实例
    public static MyApplication getInstance() {
        if (instance == null) {
            synchronized (MyApplication.class) {
                if (instance == null) {
                    instance = new MyApplication();
                }
            }
        }
        return instance;
    }

    // 用于 Worker 保存数据
    public synchronized void setImageUris(List<String> uris) {
        imageUris = uris;
    }

    // 用于其他地方获取数据
    public synchronized List<String> getImageUris() {
        return imageUris;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化单例
        instance = this;
    }


    public String getImageUrisToUpload() {
        return imageUrisToUpload;
    }

    public void setImageUrisToUpload(String imageUrisToUpload) {
        this.imageUrisToUpload = imageUrisToUpload;
    }
}

