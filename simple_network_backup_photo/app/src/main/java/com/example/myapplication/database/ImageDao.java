package com.example.myapplication.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface ImageDao {
    // 方法1：查询已存在的路径
    @Query("SELECT path FROM images WHERE path IN (:paths)")
    List<String> getExistingPaths(List<String> paths);

    // 方法2：插入单张图片
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ImageEntity image);

    // 方法3：删除操作
    @Query("DELETE FROM images WHERE md5 = :md5")
    void deleteByMd5(String md5);

    @Query("DELETE FROM images WHERE path = :path")
    void deleteByPath(String path);

    // 方法4：统计总数
    @Query("SELECT COUNT(*) FROM images")
    int count();

    // 批量插入优化
    @Transaction
    default void bulkInsert(List<ImageEntity> images) {
        // 分批处理避免SQLite参数限制
        int chunkSize = 500;
        for (int i = 0; i < images.size(); i += chunkSize) {
            int end = Math.min(images.size(), i + chunkSize);
            insertChunk(images.subList(i, end));
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertChunk(List<ImageEntity> chunk);
}