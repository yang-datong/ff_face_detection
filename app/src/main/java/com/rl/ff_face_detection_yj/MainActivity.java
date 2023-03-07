package com.rl.ff_face_detection_yj;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 0x111;
    private boolean permissionsPass = true;

    private static String[] PERMISSIONS_STORAGE = {
//            Manifest.permission.READ_EXTERNAL_STORAGE,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button bDetection = findViewById(R.id.detection);
        Button bUpload = findViewById(R.id.upload);


        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, PERMISSIONS_STORAGE[0]) != PackageManager.PERMISSION_GRANTED) {
//                || ContextCompat.checkSelfPermission(this, PERMISSIONS_STORAGE[1]) != PackageManager.PERMISSION_GRANTED
//                || ContextCompat.checkSelfPermission(this, PERMISSIONS_STORAGE[2]) != PackageManager.PERMISSION_GRANTED) {
            permissionsPass = false;
            ActivityCompat.requestPermissions(this,
                    PERMISSIONS_STORAGE, REQUEST_CAMERA_PERMISSION);
        }
        bDetection.setOnClickListener(v -> {
            if (permissionsPass)
                startActivity(new Intent(this, FaceRecognizeActivity.class));
        });
        bUpload.setOnClickListener(v -> {
            if (permissionsPass)
                startActivity(new Intent(this, UploadFaceActivity.class));
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
//            if (grantResults.length == 3
//                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
//                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
//                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionsPass = true;
                // Camera permission has been granted, start the camera preview
            } else {
                // Camera permission has been denied, show an error message
                Toast.makeText(this, "Camera permission was not granted", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}