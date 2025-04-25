package com.example.myapplication.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapplication.MyApplication;
import com.example.myapplication.database.AppDatabase;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.List;

import okhttp3.*;


public class ImageUploadWorker extends Worker {
    private static final String TAG = "ImageUploadWorker";

    public ImageUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String imageUri = MyApplication.getInstance().getImageUrisToUpload();

        if (imageUri == null ) {
            return Result.failure();
        }

        boolean success = uploadImage(imageUri);

        if (success) {
            MyApplication.getInstance().setImageUrisToUpload(imageUri);
            return Result.success();
        } else {
            return Result.failure();
        }
    }

    private boolean uploadImage(String imageUri, String serverFileName) {
        OkHttpClient client = new OkHttpClient();
        File file = new File(imageUri);
        Gson gson = new Gson();

        if (!file.exists()) {
            return false;
        }

        // 如果未指定服务器文件名，则使用原文件名
        String uploadFileName = (serverFileName == null || serverFileName.isEmpty())
                ? file.getName() : serverFileName;

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("filename",uploadFileName)
                .addFormDataPart("file", uploadFileName,
                        RequestBody.create(MediaType.parse("image/*"), file))
                .build();

        Request request = new Request.Builder()
                .url("https://note.242345.xyz/DCIM/index.php") // 替换为你的上传URL
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return false;
            }

            // 解析服务器返回的JSON
            String responseBody = response.body().string();
            ServerResponse serverResponse = gson.fromJson(responseBody, ServerResponse.class);

            // 根据服务器返回的success字段判断是否成功
            if (serverResponse != null && serverResponse.success){
                AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
                ImageProcessor processor = new ImageProcessor(getApplicationContext());
                processor.processImage(imageUri, database.imageDao());
                return true;
            }else{
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, "上传图片失败", e);
            return false;
        }
    }

    private boolean uploadImage(String imageUri) {
        return uploadImage(imageUri,null);
    }

    // 定义用于解析服务器响应的内部类
    private static class ServerResponse {
        boolean success;
        String message;
        String filePath;

        // GSON需要无参构造函数
        public ServerResponse() {}
    }
}