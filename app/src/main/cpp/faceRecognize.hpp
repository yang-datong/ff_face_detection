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
#include <sys/unistd.h>
#include <utility>

#define TAG "TAG---->"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__ )
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,  TAG, __VA_ARGS__ )

using std::string;
using std::vector;

class FaceRecognizer{
	public:
		FaceRecognizer(string workPath,string folderPath,cv::Size newSize ,const string& cascadeFile,int arithmetic);
		void FillData(vector<cv::Mat>& images, vector<int>& labels);
		void FetchModel();
		cv::Ptr<cv::face::FaceRecognizer> getRecognizer();
		int getKeys() const;
		std::vector<string> getLabelsName();

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

		string _local_model_file ;
		string _labelFile;
		int _keys = LBPH_FACE_KEYS;
		std::vector<string> _labels_name;
};

#endif /* end of include guard: MAIN_HPP_VDJI1OUJ */
