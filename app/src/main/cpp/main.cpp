#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#include <android/log.h>

#define TAG "TAG---->"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__ )
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,  TAG, __VA_ARGS__ )

using namespace cv;

JNIEXPORT void JNICALL faceDetection(JNIEnv *env, jobject thiz, jlong matAddrGray, jlong matAddrRgba) {
    Mat &mGr = *(Mat *) matAddrGray;
    Mat &mRgb = *(Mat *) matAddrRgba;

    // Load the cascade classifier
    CascadeClassifier faceCascade;
    faceCascade.load("/data/local/tmp/haarcascade_frontalface_default.xml");

    // Detect faces
    std::vector<Rect> faces;
    faceCascade.detectMultiScale(mGr, faces, 1.1, 2, 0 | CASCADE_SCALE_IMAGE, Size(30, 30));

    // Draw rectangles around detected faces
    for (size_t i = 0; i < faces.size(); i++) {
        rectangle(mRgb, faces[i], Scalar(255, 0, 0), 2, LINE_AA);
    }
}

JNIEXPORT void JNICALL eyeDetection(JNIEnv *env, jobject thiz, jlong matAddrGray, jlong matAddrRgba) {
    Mat &mGr = *(Mat *) matAddrGray;
    Mat &mRgb = *(Mat *) matAddrRgba;
    // Load the cascade classifier
    //CascadeClassifier eyeCascade;
    //eyeCascade.load("/data/local/tmp/haarcascade_eye.xml");

    //// Detect eyes
    //std::vector<Rect> eyes;
    //eyeCascade.detectMultiScale(mGr, eyes, 1.1, 2, 0 | CASCADE_SCALE_IMAGE, Size(30, 30));

    //// Draw rectangles around detected eyes
    //for (size_t i = 0; i < eyes.size(); i++) {
    //    rectangle(mRgb, eyes[i], Scalar(0, 255, 0), 2, LINE_AA);
    //}
}


static JNINativeMethod nativeMethods[] = {
    {"faceDetection", "(JJ)V", (void*)faceDetection},
    {"eyeDetection", "(JJ)V", (void*)eyeDetection}
};


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    jclass clazz = env->FindClass("com/rl/ff_face_detection_yj/MainActivity_jni");
    if (clazz == NULL) {
        return -1;
    }

    if (env->RegisterNatives(clazz, nativeMethods, sizeof(nativeMethods) / sizeof(nativeMethods[0])) != JNI_OK) {
        return -1;
    }

    result = JNI_VERSION_1_6;

    return result;
}
