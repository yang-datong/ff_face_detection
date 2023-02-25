#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_rl_ff_1face_1detection_1yj_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

//
//#include <jni.h>
//#include <string>
//#include <opencv2/opencv.hpp>
//
//using namespace cv;
//
//extern "C" {
//JNIEXPORT void JNICALL
//Java_com_rl_ff_1face_1detection_1yj_MainActivity_1jni_faceDetection(
//        JNIEnv *env, jobject thiz, jlong matAddrGray, jlong matAddrRgba) {
//
//    Mat &mGr = *(Mat *) matAddrGray;
//    Mat &mRgb = *(Mat *) matAddrRgba;
//
//    // Load the cascade classifier
//    CascadeClassifier faceCascade;
//    faceCascade.load("/storage/emulated/0/Download/haarcascade_frontalface_default.xml");
//
//    // Detect faces
//    std::vector<Rect> faces;
//    faceCascade.detectMultiScale(mGr, faces, 1.1, 2, 0 | CASCADE_SCALE_IMAGE, Size(30, 30));
//
//    // Draw rectangles around detected faces
//    for (size_t i = 0; i < faces.size(); i++) {
//        rectangle(mRgb, faces[i], Scalar(255, 0, 0), 2, LINE_AA);
//    }
//}
//
//JNIEXPORT void JNICALL
//Java_com_rl_ff_1face_1detection_1yj_MainActivity_1jni_eyeDetection(
//        JNIEnv *env, jobject thiz, jlong matAddrGray, jlong matAddrRgba) {
//
//    Mat &mGr = *(Mat *) matAddrGray;
//    Mat &mRgb = *(Mat *) matAddrRgba;
//
//    // Load the cascade classifier
//    CascadeClassifier eyeCascade;
//    eyeCascade.load("/storage/emulated/0/Download/haarcascade_eye.xml");
//
//    // Detect eyes
//    std::vector<Rect> eyes;
//    eyeCascade.detectMultiScale(mGr, eyes, 1.1, 2, 0 | CASCADE_SCALE_IMAGE, Size(30, 30));
//
//    // Draw rectangles around detected eyes
//    for (size_t i = 0; i < eyes.size(); i++) {
//        rectangle(mRgb, eyes[i], Scalar(0, 255, 0), 2, LINE_AA);
//    }
//}
//}