package com.rl.ff_face_detection_yj;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.icu.text.SimpleDateFormat;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;

public class FaceRecognize {

    private static final String TAG = "FaceRecognize";

    private TextureView mTextureView;

    private CameraManager mCameraManager;
    private String mCameraId;
    private Size mPreviewSize;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mCaptureSession;
    private ImageReader mImageReader;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private static int jni_initialization_status = -1; //用子线程加载人脸识别模型

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private Activity activity;

    private ImageView showImageView = null;

    static {
        System.loadLibrary("faceRecognize");
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Dialog progressDialog = null;
    private boolean inUploadingFaceImage = false;


    public static void loadJNIFaceModel(Context application) {
        new Thread(() -> {
            Log.e(TAG, "run: jni initialization");
            String workPath = application.getFilesDir().getAbsolutePath();
//            String workPath = "/sdcard";
            jni_initialization_status = JNI_Initialization(workPath, "lfw", "haarcascades/haarcascade_frontalface_default.xml", 2);
            Log.e(TAG, "initialization finish -> jni_initialization_status:" + jni_initialization_status);
        }).start();
    }

    public static void onDestroy() {
        JNI_Close();
    }

    public void onCreate(TextureView textureView, Activity activity) {
        if (textureView == null || activity == null) return;
        this.activity = activity;
        mTextureView = textureView;
        if (jni_initialization_status != 0) {
            showToast("等待模型加载中，如果长时间未加载成功则模型错误");
            return;
        }
        mTextureView.setSurfaceTextureListener(surfaceTextureListener);
    }

    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            Log.e(TAG, "onSurfaceTextureAvailable: ");
            createCameraPreviewSession();

        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            Log.e(TAG, "onSurfaceTextureSizeChanged: ");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            Log.e(TAG, "onSurfaceTextureDestroyed: ");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
//            Log.e(TAG, "onSurfaceTextureUpdated: " );
        }
    };

    public void onResume() {
        startBackgroundThread();
        if (!mTextureView.isAvailable()) {
            openCamera();
        }
    }

    public void onPause() {
        closeCamera();
        stopBackgroundThread();
    }

    private void openCamera() {
        mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
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
                // 图像可用时保存图像
                Image image = imageReader.acquireLatestImage();
//                saveImageToGallery(image);


                if (image == null) {
                    if (progressDialog != null && progressDialog.isShowing())
                        activity.runOnUiThread(() -> progressDialog.dismiss());
                    inUploadingFaceImage = false;
//                    Log.e(TAG, "setOnImageAvailableListener image == null ");
                    return;
                }
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                saveImage(data);
                image.close();
            }, mBackgroundHandler);

            // Open the camera
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
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
                        Log.e(TAG, "onDisconnected: ");
                    }

                    @Override
                    public void onError(@NonNull CameraDevice cameraDevice, int i) {
                        mCameraDevice.close();
                        mCameraDevice = null;
                        Log.e(TAG, "onError: ");
                    }
                }, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (mCaptureSession != null) {
            Log.e(TAG, "mCaptureSession.close(): ");
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            Log.e(TAG, "mCameraDevice.close(): ");
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            Log.e(TAG, "mImageReader.close(): ");
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
                            Toast.makeText(activity, "Configuration failed", Toast.LENGTH_SHORT).show();
//                            Log.e(TAG, "Configuration failed " );
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

    public void uploadFaceImage(ImageView showImageView) {
        this.showImageView = showImageView;
    }

    public void takePicture() {
        if (jni_initialization_status != 0) {
            Log.e(TAG, "waiting jni initialization finnish ...");
            return;
        }
        try {
//            if (mCameraDevice == null) {
//                return;
//            }
            // Create a capture request builder for taking a picture
            // 初始化ProgressDialog对象
            if (progressDialog == null) {
                progressDialog = new Dialog(activity, R.style.DialogStyle);
                progressDialog.setCancelable(false);
                progressDialog.setContentView(R.layout.dialog_loading);

                // 设置ProgressDialog的位置和大小
                WindowManager.LayoutParams layoutParams = progressDialog.getWindow().getAttributes();
                layoutParams.gravity = Gravity.CENTER;
                layoutParams.width = 500;
                layoutParams.height = 500;
                layoutParams.alpha = 0.7f;
                progressDialog.getWindow().setAttributes(layoutParams);
            }

            // 显示ProgressDialog
            if (!progressDialog.isShowing())
                progressDialog.show();

            if (inUploadingFaceImage) {
                Log.e(TAG, "uploading face image ...");
                return;
            }

            inUploadingFaceImage = true;
            CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Set auto focus and auto flash
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Set orientation based on device orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            // Capture the image
//            mCaptureSession.stopRepeating();
//            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), mCaptureCallback, null);

            // 恢复相机预览
//            mCaptureSession.setRepeatingRequest(captureBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            inUploadingFaceImage = false;
            e.printStackTrace();
        }
    }

    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                                             long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
//                    Log.e(TAG, "Capture started");

                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
//                    Log.e(TAG, "Capture completed");
//                    unlockFocus();

                    if (mCaptureSession == null || mCameraDevice == null || mImageReader == null)
                        return;
                    if (showImageView != null) return;

                    //进行人脸识别会导致图片截取很缓慢
                    Mat mat = mTextureView.getBitmap().getConfig() == Bitmap.Config.RGB_565 ?
                            new Mat(mTextureView.getBitmap().getHeight(), mTextureView.getBitmap().getWidth(), CvType.CV_8UC2) :
                            new Mat(mTextureView.getBitmap().getHeight(), mTextureView.getBitmap().getWidth(), CvType.CV_8UC4);

                    Utils.bitmapToMat(mTextureView.getBitmap(), mat);
                    Mat grayMat = new Mat();
                    Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY);

                    if (jni_initialization_status != 0) {
                        Log.e(TAG, "waiting jni initialization finnish ...");
                        return;
                    }

//                    Log.e(TAG, "onCaptureCompleted: ");
                    JNI_FaceDetection(grayMat.getNativeObjAddr(), mat.getNativeObjAddr());
                }

                @Override
                public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request,
                                            CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    Log.e(TAG, "Capture failed error ->" + request.describeContents());
//                    unlockFocus();
                }
            };

    private int getImageRotation() {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int imageRotation;
        CameraCharacteristics mCameraCharacteristics = null;
        try {
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
            imageRotation = (sensorOrientation + degrees) % 360;
            imageRotation = (360 - imageRotation) % 360;
        } else {
            imageRotation = (sensorOrientation - degrees + 360) % 360;
        }
        return imageRotation;
    }

    //保存到本地文件
    private void saveImage(byte[] data) {
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "IMG_" + timeStamp + ".jpg");
//        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "IMG.jpg");
        File file = new File(activity.getFilesDir().getAbsolutePath(), "/IMG.jpg");
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
//            matrix.postRotate(getImageRotation());
            matrix.postRotate(-90);
            matrix.postScale(-1, 1);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.write(data);
            outputStream.close();
            Log.e(TAG, "Image saved to " + file.getAbsolutePath());

            inUploadingFaceImage = false;
            if (progressDialog != null && progressDialog.isShowing())
                activity.runOnUiThread(() -> progressDialog.dismiss());

            activity.runOnUiThread(() -> {
                showImageView.setImageBitmap(rotatedBitmap);
                showImageView.setVisibility(View.VISIBLE);
            });


            if (JNI_JustSaveFaceImage(file.getAbsolutePath()) == -1)
                showToast("未识别到人脸，上传人脸失败!!!");
            else
                showToast("上传人脸成功");

            activity.runOnUiThread(() ->
                    showImageView.setVisibility(View.GONE)
            );

            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            if (!rotatedBitmap.isRecycled()) {
                rotatedBitmap.recycle();
            }

        } catch (IOException e) {
            Log.e(TAG, "saveImage: e->" + e.getMessage());
            e.printStackTrace();
        }
    }

    //保存到相机图库
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
            activity.sendBroadcast(mediaScanIntent);

            Toast.makeText(activity, "Image saved to gallery", Toast.LENGTH_SHORT).show();
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

    private void showToast(final String text) {
//        final Activity activity = activity;
        if (activity != null && !activity.isDestroyed() && !activity.isFinishing())
            activity.runOnUiThread(() -> Toast.makeText(activity, text, Toast.LENGTH_SHORT).show());
    }

    public static native int JNI_Initialization(String workPath, String dataDirectory, String cascadeFile, int arithmetic);

    public static native void JNI_Close();

    public native void JNI_FaceDetection(long matAddressGray, long matAddressRgba);

    public native void JNI_EyeDetection(long matAddressGray, long matAddressRgba);

    public native int JNI_JustSaveFaceImage(String oldFaceImagePath);
}
