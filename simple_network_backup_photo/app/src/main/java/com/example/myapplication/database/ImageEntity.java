package com.example.myapplication.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "images",
        indices = {
                @Index(value = {"path"}, unique = true),
                @Index(value = {"md5"}, unique = true)
        })
public class ImageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id = 0;

    public final String path;
    public final String md5;
    public final long size;
    public final int width;
    public final int height;
    public final long lastModified;

    public ImageEntity(String path, String md5, long size, int width, int height) {
        this.path = path;
        this.md5 = md5;
        this.size = size;
        this.width = width;
        this.height = height;
        this.lastModified = System.currentTimeMillis();
    }
}