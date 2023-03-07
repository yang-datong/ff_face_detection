package com.rl.ff_face_detection_yj;

import android.app.Application;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FaceRecognize.loadJNIFaceModel(this.getApplicationContext());
    }

    @Override
    public void onTerminate() {
        FaceRecognize.onDestroy();
        super.onTerminate();
    }
}
