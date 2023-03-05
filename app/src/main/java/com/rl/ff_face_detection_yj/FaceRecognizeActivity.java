package com.rl.ff_face_detection_yj;

import android.os.Bundle;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class FaceRecognizeActivity extends AppCompatActivity {

    private static final String TAG = "FaceRecognizeActivity";

    private FaceRecognize faceRecognize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏状态栏和顶部菜单栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_face_recognize);

        TextureView mTextureView = findViewById(R.id.texture_view);
        ImageView mImageView = findViewById(R.id.image_view);
        Button mButtonView = findViewById(R.id.button_capture);
        faceRecognize = new FaceRecognize();
        faceRecognize.onCreate(mTextureView, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        faceRecognize.onResume();
    }

    @Override
    protected void onPause() {
        faceRecognize.onPause();
        super.onPause();
    }
}
