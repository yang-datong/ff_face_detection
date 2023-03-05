# 使用的JavaCameraView版本

## 优点
可以显示人脸识别框

## 缺点
帧率非常慢

```java
package com.rl.ff_face_detection_yj;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity_jni extends CameraActivity {

    static {
        System.loadLibrary("main");
    }

    private CameraBridgeViewBase mOpenCvCameraView;
    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                mOpenCvCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        List<CameraBridgeViewBase> list = new ArrayList<>();
        list.add(mOpenCvCameraView);
        return list;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String workPath = this.getFilesDir().getAbsolutePath();
//        Log.e("TAG", "onCreate: "+workPath );
        if (JNI_Initialization(workPath, "lfw", "haarcascades/haarcascade_frontalface_default.xml",2) == -1) {
            onDestroy();
            System.exit(-1);
        }
        mOpenCvCameraView = findViewById(R.id.java_camera_view);
        mOpenCvCameraView.setCameraIndex(JavaCamera2View.CAMERA_ID_FRONT);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
            }

            @Override
            public void onCameraViewStopped() {
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                Mat gray = inputFrame.gray();
                Mat rgba = inputFrame.rgba();

                // Call JNI methods for face and eye detection
                JNI_FaceDetection(gray.getNativeObjAddr(), rgba.getNativeObjAddr());
                //JNI_EyeDetection(gray.getNativeObjAddr(), rgba.getNativeObjAddr());

                return rgba;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }


    public native void JNI_FaceDetection(long matAddrGray, long matAddrRgba);

    public native void JNI_EyeDetection(long matAddrGray, long matAddrRgba);

    public native int JNI_Initialization(String workPath, String dataDirectory, String cascadeFile,int arithmetic);
}
```

# 使用的Camera2版本

## 优点
帧率正常

## 缺点
不能显示人脸识别框

```java
package com.rl.ff_face_detection_yj;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.util.Collections;

public class MainActivity_jni extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private CameraManager cameraManager;
    private String cameraId;
    private CameraDevice cameraDevice;
    private TextureView textureView;
    private Size previewSize;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private CascadeClassifier cascadeClassifier;

    static {
//        System.loadLibrary("opencv_java4");
        System.loadLibrary("main");
    }

    private Surface previewSurface;

    ImageReader mImageReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        String workPath = this.getFilesDir().getAbsolutePath();
//        Log.e("TAG", "onCreate: "+workPath );
        if (JNI_Initialization(workPath, "lfw", "haarcascades/haarcascade_frontalface_default.xml", 2) == -1) {
            onDestroy();
            System.exit(-1);
        }

        textureView = findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                setupCamera();
//                createCameraPreview();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                // Ignored, Camera does all the work for us
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                // Invoked every time there's a new Camera preview frame
            }
        });

    }

    public void setupCamera() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraId = id;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        // Get the preview size
        try {
            StreamConfigurationMap map = cameraManager.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            previewSize = map.getOutputSizes(SurfaceTexture.class)[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        // Create an ImageReader for capturing still images
        mImageReader = ImageReader.newInstance(previewSize.getWidth(),
                previewSize.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
        mImageReader.setOnImageAvailableListener(imageReader -> {
            Log.i(TAG, "onCreate:");

//            Canvas canvas = previewSurface.lockCanvas(null);
//             绘制矩形框
//            canvas.drawRect(rectF, mRectPaint);
//            previewSurface.unlockCanvasAndPost(canvas);

        }, backgroundHandler);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        try {
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createCameraPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    cameraDevice.close();
                    cameraDevice = null;
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

//        cascadeClassifier = new CascadeClassifier();
//        cascadeClassifier.load(this.getFilesDir() + "/haarcascades/haarcascade_frontalface_default.xml");

    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
//            openCamera();
        } else {
//            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreview() {
        try {

            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            previewSurface = new Surface(surfaceTexture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);
//
            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) {
                        return;
                    }
                    cameraCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, backgroundHandler);


//            Surface imageReaderSurface = mImageReader.getSurface();
//            captureRequestBuilder.addTarget(imageReaderSurface);
//            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReaderSurface), new CameraCaptureSession.StateCallback() {
//                @Override
//                public void onConfigured(CameraCaptureSession session) {
//                    try {
//                        session.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
//                    } catch (CameraAccessException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                @Override
//                public void onConfigureFailed(CameraCaptureSession session) {
//                    // 配置失败的处理逻辑
//                }
//            }, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null) {
            return;
        }
        try {
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

                    Mat mat = textureView.getBitmap().getConfig() == Bitmap.Config.RGB_565 ?
                            new Mat(textureView.getBitmap().getHeight(), textureView.getBitmap().getWidth(), CvType.CV_8UC2) :
                            new Mat(textureView.getBitmap().getHeight(), textureView.getBitmap().getWidth(), CvType.CV_8UC4);
//
                    Utils.bitmapToMat(textureView.getBitmap(), mat);
                    Mat grayMat = new Mat();
                    Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY);


                    // Call JNI methods for face and eye detection
                    JNI_FaceDetection(grayMat.getNativeObjAddr(), mat.getNativeObjAddr());

//
//                    MatOfRect faces = new MatOfRect();
//                    if (cascadeClassifier != null) {
//                        cascadeClassifier.detectMultiScale(grayMat, faces);
//                    }

//                    for (Rect rect : faces.toArray()) {
//                        Imgproc.rectangle(mat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),
//                                new Scalar(255, 0, 0), 2);
//                        Log.e(TAG, "onCaptureCompleted: ");
//                    }

                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }


    public native void JNI_FaceDetection(long matAddrGray, long matAddrRgba);

    public native void JNI_EyeDetection(long matAddrGray, long matAddrRgba);

    public native int JNI_Initialization(String workPath, String dataDirectory, String cascadeFile, int arithmetic);
}
```

# 使用的Camera2版本，没有运行成功，代码不错，还没添加Opencv代码

```java
package com.rl.ff_face_detection_yj;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.icu.text.SimpleDateFormat;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.rl.ff_face_detection_yj.MainActivity;
import com.rl.ff_face_detection_yj.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity_jni2 extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private TextureView mTextureView;
    private ImageView mImageView;

    private CameraManager mCameraManager;
    private String mCameraId;
    private Size mPreviewSize;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mCaptureSession;
    private ImageReader mImageReader;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private Mat mRgba;
    private Mat mGray;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded successfully");
                    // Load native library after OpenCV initialization
                    System.loadLibrary("opencv_java4");
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        mTextureView = findViewById(R.id.texture_view);
        mImageView = findViewById(R.id.image_view);

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }

        Button bt = findViewById(R.id.button_capture);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Initialize OpenCV loader
//        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);

        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            openCamera();
        } else {
            mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                        createCameraPreviewSession();

                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

                }
            });
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void openCamera() {
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // Find the rear-facing camera
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics =
                        mCameraManager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING)
                        == CameraCharacteristics.LENS_FACING_FRONT) {   //正面镜头
//                        == CameraCharacteristics.LENS_FACING_BACK) {  //反面镜头
                    mCameraId = cameraId;
                    break;
                }
            }

            // Get the preview size
            StreamConfigurationMap map =
                    mCameraManager.getCameraCharacteristics(mCameraId)
                            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//            mPreviewSize = map.getOutputSizes(TextureView.class)[0];
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            // Create an ImageReader for capturing still images
            mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(),
                    mPreviewSize.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
            mImageReader.setOnImageAvailableListener(imageReader -> {

            }, mBackgroundHandler);

            // Open the camera
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice cameraDevice) {
                        mCameraDevice = cameraDevice;
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                        mCameraDevice.close();
                        mCameraDevice = null;
                    }

                    @Override
                    public void onError(@NonNull CameraDevice cameraDevice, int i) {
                        mCameraDevice.close();
                        mCameraDevice = null;
                    }
                }, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(texture);

            // Create a capture request builder
            CaptureRequest.Builder mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Create a capture session for the preview
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (mCameraDevice == null) {
                                return;
                            }

                            // When the session is ready, start displaying the preview
                            mCaptureSession = session;
                            try {
                                // Auto focus should be continuous for camera preview
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                // Flash is automatically enabled when necessary
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, start displaying the preview
                                CaptureRequest mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(MainActivity_jni2.this, "Configuration failed", Toast.LENGTH_SHORT).show();
                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        // Check if flash is supported
        Boolean isFlashSupported =
                null;
        try {
            isFlashSupported = mCameraManager.getCameraCharacteristics(mCameraId)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        if (isFlashSupported == null) {
            return;
        }

        // Turn on flash if needed
        if (isFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }


    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private void takePicture() {
        try {
            if (mCameraDevice == null) {
                return;
            }
            // Create a capture request builder for taking a picture
            CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Set auto focus and auto flash
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Set orientation based on device orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            // Capture the image
            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), mCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                                             long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    Log.d(TAG, "Capture started");
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Log.d(TAG, "Capture completed");
//                    unlockFocus();
                }

                @Override
                public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request,
                                            CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    Log.e(TAG, "Capture failed");
//                    unlockFocus();
                }
            };


    private void saveImageToGallery(Image image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions");
            return;
        }

        try {
            // Save the image to file
            FileOutputStream fos = new FileOutputStream(pictureFile);
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            fos.write(bytes);

            fos.close();

            // Add the image to the gallery
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(pictureFile);
            mediaScanIntent.setData(contentUri);
            sendBroadcast(mediaScanIntent);

            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    private File getOutputMediaFile() {
        // Check if external storage is mounted
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return null;
        }

        // Create a unique file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "IMG_" + timeStamp + ".jpg";

        // Create the directory
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        // Create the file
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + fileName);
        return mediaFile;
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(this)
                    .setMessage("Camera permission is required for this app")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity_jni2.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission has been granted, start the camera preview
                openCamera();
            } else {
                // Camera permission has been denied, show an error message
                Toast.makeText(this, "Camera permission was not granted", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showToast(final String text) {
        final Activity activity = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
```