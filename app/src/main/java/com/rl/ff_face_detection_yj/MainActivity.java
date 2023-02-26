package com.rl.ff_face_detection_yj;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private JavaCameraView mOpenCvCameraView;

    static {
        System.loadLibrary("opencv_java4");
    }

    private final String TAG = "TAG-->";
    private CascadeClassifier faceDetector;
    private CascadeClassifier eyeDetector;
    private boolean isEye = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraIndex(JavaCamera2View.CAMERA_ID_FRONT);
//        mOpenCvCameraView.setCameraIndex(0); // 后置摄像头
//        mOpenCvCameraView.setDisplayOrientatio
        mOpenCvCameraView.enableView();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        Log.i(TAG, "getCameraViewList");
        List<CameraBridgeViewBase> list = new ArrayList<>();
        list.add(mOpenCvCameraView);
        return list;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            Log.i(TAG, "OpenCV loaded successfully");
        }

//         https://github.com/opencv/opencv/blob/master/data/haarcascades/haarcascade_frontalface_default.xml
        faceDetector = loadCascadeClassifier(R.raw.haarcascade_frontalface_default, "haarcascade_frontalface_default.xml");
//         https://github.com/opencv/opencv/blob/master/data/haarcascades/haarcascade_eye.xml
        eyeDetector = loadCascadeClassifier(R.raw.haarcascade_eye, "haarcascade_eye.xml");
    }


    private CascadeClassifier loadCascadeClassifier(int id, String name) {
        try {
            InputStream is = getResources().openRawResource(id);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, name);
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            is.close();
            os.close();

            return new CascadeClassifier(cascadeFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "loadCascadeClassifier: error -> ", e);
        }
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "onCameraViewStarted: ");
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//        Log.i(TAG, "onCameraFrame: ");
        // 为人脸检测设置参数
        MatOfRect faces = new MatOfRect();
//        MatOfByte mem = new MatOfByte();
        Mat frame = inputFrame.rgba();
        Mat grayFrame = inputFrame.gray();

        // 在灰度图像中检测人脸
        if (faceDetector != null) {
            faceDetector.detectMultiScale(grayFrame, faces);
        }

        // 绘制检测到的人脸矩形
        Rect[] facesArray = faces.toArray();
        for (Rect rect : facesArray) {
            Imgproc.rectangle(frame, rect.tl(), rect.br(), new Scalar(0, 255, 0, 255), 3);
//            eye(frame,rect);
        }


        return frame;
    }

    private void eye(Mat image, Rect face) {
        // 在人脸区域内检测眼睛
        Mat faceROI = image.submat(face);
        MatOfRect eyes = new MatOfRect();
        if (eyeDetector != null)
            eyeDetector.detectMultiScale(faceROI, eyes);

        // 遍历每个眼睛
        for (Rect eye : eyes.toArray()) {
            Log.i(TAG, "eye: " + eye.width);
            // 在图像上绘制眼睛矩形
            Imgproc.rectangle(image, new Point(face.x + eye.x, face.y + eye.y), new Point(face.x + eye.x + eye.width, face.y + eye.y + eye.height), new Scalar(255, 0, 0), 2);

            // 计算眼睛的长宽比
            double aspectRatio = (double) eye.width / (double) eye.height;


            // 如果长宽比小于阈值，则表示眼睛闭合
            if (aspectRatio < 0.2) {
                // 在图像上绘制闭眼标识
                Imgproc.putText(image, "Closed", new Point(face.x + eye.x, face.y + eye.y - 5), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 255), 2);
            }
        }
//        Log.d(TAG, "eye: image -> "+image.toString());

//        HighGui.imshow("Result", image);
//        HighGui.waitKey();
    }
}
