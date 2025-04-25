    package com.example.myapplication;

    import android.annotation.SuppressLint;
    import android.content.Context;
    import android.content.Intent;
    import android.os.Bundle;
    import android.os.Environment;
    import android.provider.Settings;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.BaseAdapter;
    import android.widget.GridView;
    import android.widget.ImageView;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.activity.EdgeToEdge;
    import androidx.annotation.NonNull;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.graphics.Insets;
    import androidx.core.view.ViewCompat;
    import androidx.core.view.WindowInsetsCompat;
    import androidx.lifecycle.Observer;
    import androidx.work.Data;
    import androidx.work.OneTimeWorkRequest;
    import androidx.work.WorkInfo;
    import androidx.work.WorkManager;
    import androidx.work.WorkRequest;
    import com.bumptech.glide.Glide;
    import com.example.myapplication.util.ImageLoadWorker;
    import com.example.myapplication.util.ImageUploadWorker;
    import com.example.myapplication.util.PermissionHelper;

    import java.nio.file.Paths;
    import java.util.Collections;
    import java.util.List;

    @SuppressLint("DefaultLocale")
    public class MainActivity extends AppCompatActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_main);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            // 超敏感权限,不能用普通方法申请.
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }

            // 申请有限的图片读取权限
            if (PermissionHelper.checkAndRequestPermissions(this)) {
                // 所有权限都已授予
                startImageLoadingWork();
            } else {
                Toast.makeText(this,"权限申请不正确,应用不会继续执行.",Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (PermissionHelper.handlePermissionResult(requestCode, permissions, grantResults)) {
                // 所有权限都已授予
                startImageLoadingWork();
            } else {
                // 有权限被拒绝
                String description = PermissionHelper.getDeniedPermissionDescription(this);
                Toast.makeText(this,description,Toast.LENGTH_LONG).show();
            }
        }

        private void startImageLoadingWork() {
            WorkRequest workRequest = new OneTimeWorkRequest.Builder(ImageLoadWorker.class).build();
            WorkManager.getInstance(this).enqueue(workRequest);

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(workRequest.getId())
                    .observe(this, new Observer<WorkInfo>() {
                        @Override
                        public void onChanged(WorkInfo workInfo) {
                            if (workInfo != null && workInfo.getState().isFinished()) {
                                if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                    // 读取返回的 image count
                                    Data outputData = workInfo.getOutputData();
                                    int imageCount = outputData.getInt("image_count", 0);

                                    if (imageCount > 0) {
                                        // 从单例 Application 获取真实的 URI 列表
                                        List<String> imageUris = MyApplication.getInstance().getImageUris();

                                        if (imageUris != null && !imageUris.isEmpty()) {
                                            setupGridView(imageUris);
                                            startImageUploadWork(imageUris.get(0));
                                        } else {
                                            Toast.makeText(MainActivity.this,
                                                    "获取到的列表是空的.", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(MainActivity.this,
                                                "手机里没有照片?", Toast.LENGTH_SHORT).show();
                                    }

                                } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                                    Toast.makeText(MainActivity.this,
                                            "图片不能加载?", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                    });
        }

        private void startImageUploadWork(String imageUri) {
            // 将URI列表保存到Application中，以便Worker可以访问
            MyApplication.getInstance().setImageUrisToUpload(imageUri);

            // 创建上传工作请求
            OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(ImageUploadWorker.class)
                    .build();

            WorkManager.getInstance(this).enqueue(uploadWorkRequest);

            // 观察上传任务状态
            WorkManager.getInstance(this).getWorkInfoByIdLiveData(uploadWorkRequest.getId())
                    .observe(this, new Observer<WorkInfo>() {
                        @Override
                        public void onChanged(WorkInfo workInfo) {
                            if (workInfo != null && workInfo.getState().isFinished()) {
                                if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                    // 上传完成后重新加载图片
                                    Toast.makeText(MainActivity.this, String.format("上传 %s 成功", Paths.get(imageUri).getFileName().toString()), Toast.LENGTH_SHORT).show();startImageLoadingWork();
                                    startImageLoadingWork();
                                } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                                    // 上传失败处理
                                    Toast.makeText(MainActivity.this, "图片上传失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
        }

        private void setupGridView(List<String> imageUris) {
            GridView gridView = findViewById(R.id.grid_view);
            TextView tvLoading = findViewById(R.id.tv_loading);
            tvLoading.setVisibility(View.GONE);
            gridView.setVisibility(View.VISIBLE);
            gridView.setAdapter(new ImageAdapter(this, imageUris));
        }

        public static class ImageAdapter extends BaseAdapter {
            private final Context context;
            private final List<String> imageUris;

            public ImageAdapter(Context context, List<String> imageUris) {
                this.context = context;
                this.imageUris = imageUris != null ? imageUris : Collections.emptyList();
            }

            @Override
            public int getCount() {
                return imageUris.size();
            }

            @Override
            public Object getItem(int position) {
                return imageUris.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ImageView imageView;
                if (convertView == null) {
                    imageView = new ImageView(context);
                    imageView.setLayoutParams(new GridView.LayoutParams(500, 500));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                } else {
                    imageView = (ImageView) convertView;
                }

                Glide.with(context)
                        .load("file://" + imageUris.get(position))
                        .thumbnail(0.1f)
                        .into(imageView);

                return imageView;
            }
        }

    }