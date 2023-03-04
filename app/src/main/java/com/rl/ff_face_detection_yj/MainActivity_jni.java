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
