package com.rl.ff_face_detection_yj;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

public class OpencvCameraActivity extends CameraActivity {

    private static final String TAG = "OpencvCam";

    private JavaCameraView javaCameraView;
    private Button switchCameraBtn;
    private int cameraId = JavaCamera2View.CAMERA_ID_ANY;

    private CameraBridgeViewBase.CvCameraViewListener2 cvCameraViewListener2 = new CameraBridgeViewBase.CvCameraViewListener2() {
        @Override
        public void onCameraViewStarted(int width, int height) {
            Log.i(TAG, "onCameraViewStarted width=" + width + ", height=" + height);
        }

        @Override
        public void onCameraViewStopped() {
            Log.i(TAG, "onCameraViewStopped");
        }

        @Override
        public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
            return inputFrame.rgba();
        }
    };

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            Log.i(TAG, "onManagerConnected status=" + status + ", javaCameraView=" + javaCameraView);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    if (javaCameraView != null) {
                        javaCameraView.setCvCameraViewListener(cvCameraViewListener2);
                        // 禁用帧率显示
                        javaCameraView.disableFpsMeter();
                        javaCameraView.enableView();
                    }
                }
                break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    //复写父类的 getCameraViewList 方法，把 javaCameraView 送到父 Activity，一旦权限被授予之后，javaCameraView 的 setCameraPermissionGranted 就会自动被调用。
    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        Log.i(TAG, "getCameraViewList");
        List<CameraBridgeViewBase> list = new ArrayList<>();
        list.add(javaCameraView);
        return list;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findView();
        setListener();
    }

    private void findView() {
        javaCameraView = (JavaCameraView)findViewById(R.id.java_camera_view);
    }

    private void setListener() {
//        switchCameraBtn.setOnClickListener(view -> {
//            switch (cameraId) {
//                case JavaCamera2View.CAMERA_ID_ANY:
//                case JavaCamera2View.CAMERA_ID_BACK:
//                    cameraId = JavaCamera2View.CAMERA_ID_FRONT;
//                    break;
//                case JavaCamera2View.CAMERA_ID_FRONT:
//                    cameraId = JavaCamera2View.CAMERA_ID_BACK;
//                    break;
//            }
//            Log.i(TAG, "cameraId : " + cameraId);
//            切换前后摄像头，要先禁用，设置完再启用才会生效
//            javaCameraView.disableView();
//            javaCameraView.setCameraIndex(cameraId);
//            javaCameraView.enableView();
//        });
                            cameraId = JavaCamera2View.CAMERA_ID_FRONT;

            Log.i(TAG, "cameraId : " + cameraId);
    //切换前后摄像头，要先禁用，设置完再启用才会生效
            javaCameraView.disableView();
            javaCameraView.setCameraIndex(cameraId);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
            javaCameraView.enableView();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "initDebug true");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.i(TAG, "initDebug false");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
    }
}

