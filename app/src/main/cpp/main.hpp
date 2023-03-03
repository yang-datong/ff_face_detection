#ifndef MAIN_HPP_VDJI1OUJ
#define MAIN_HPP_VDJI1OUJ

#include <iostream>
#include <locale>
#include <vector>
#include <fstream>
#include "opencv2/face/facerec.hpp"
#include "opencv2/opencv.hpp"

#include <jni.h>
#include <android/log.h>
#include <dirent.h>
#include <unistd.h>
#include <sys/stat.h>

#define TAG "TAG---->"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__ )
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,  TAG, __VA_ARGS__ )

using std::string;
using std::vector;

class FaceRecognizer{
	public:
		string LABEL_FILE = "/model/label_file.npy";
		FaceRecognizer(string folderPath,cv::Size newSize ,const string& cascadeFile,int arithmetic);
		void FillData(vector<cv::Mat>& images, vector<int>& labels);
		void FetchModel();
		cv::Ptr<cv::face::FaceRecognizer> getRecognizer();
		int getKeys() const;
		std::vector<string> getLabelsName();
		void setWorkPath(string p);

	private:
		string _workPath;
		string _folderPath;
		cv::Size _newSize;
		string _cascadeFile;
		cv::Ptr<cv::face::FaceRecognizer> _recognizer = nullptr;
		int _arithmetic;
		cv::CascadeClassifier _faceCascade;

		const int EIGEN_FACE_KEYS = 4500;
		const int LBPH_FACE_KEYS = 100;
		const int FISHER_FACE_KEYS = 4500;

		string _local_model_file = "/model/LBPH_model.yml";
		int _keys = LBPH_FACE_KEYS;
		std::vector<string> _labels_name;
};

void faceRecognize(const cv::Mat& face_roi,cv::Mat frame,cv::Rect face);

#endif /* end of include guard: MAIN_HPP_VDJI1OUJ */
