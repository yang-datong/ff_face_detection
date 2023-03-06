package com.rl.ff_face_detection_yj;

import android.os.Bundle;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class UploadFaceActivity extends AppCompatActivity {

    private static final String TAG = "UploadFaceActivity";

    private FaceRecognize faceRecognize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏状态栏和顶部菜单栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_upload_face);

        TextureView mTextureView = findViewById(R.id.texture_view);
        ImageView mImageView = findViewById(R.id.image_view);
        Button mButtonView = findViewById(R.id.button_capture);
        faceRecognize = new FaceRecognize();
        faceRecognize.onCreate(mTextureView, this);
        faceRecognize.uploadFaceImage(mImageView);
        mButtonView.setOnClickListener((v) -> faceRecognize.takePicture());

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

    @Override
    protected void onDestroy() {
        faceRecognize.onDestroy();
        super.onDestroy();
    }
}
