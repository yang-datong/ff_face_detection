package com.rl.ff_face_detection_yj;

import android.app.Application;
import android.util.Log;

import org.opencv.android.OpenCVLoader;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化 OpenCV
        if (!OpenCVLoader.initDebug()) {
            // OpenCV 初始化失败
            Log.e("OpenCV", "Failed to initialize OpenCV.");
        }
    }
}
