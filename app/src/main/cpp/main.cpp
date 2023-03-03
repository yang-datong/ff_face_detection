#include "main.hpp"

int gArithmetic = 2; //Default use LBPHFaceRecognizer
std::string gDirectory = "/data/local/tmp/lfw";

cv::Ptr<cv::face::FaceRecognizer> gRecognizer = nullptr;
cv::Ptr<cv::face::FaceRecognizer> FaceRecognizer::getRecognizer(){
	return _recognizer;
}

int gKeys = -1;
int FaceRecognizer::getKeys(){
	return _keys;
}

std::vector<string> gLabelsName;
std::vector<string> FaceRecognizer::getLabelsName(){
	return _labels_name;
}

void faceRecognize(cv::Mat face_roi,cv::Mat frame,cv::Rect face){
	//resize(face_roi, face_roi, _newSize);
	//cv::namedWindow("Image");
	//cv::imshow("Image",face_roi);
	//cv::waitKey(1);
	int label = -1;
	double confidence = 0.0;
	gRecognizer->predict(face_roi,label,confidence);
	//	cv::putText(frame, std::to_string(confidence), cv::Point(face.x, face.y - 10), cv::FONT_HERSHEY_SIMPLEX, 0.9, cv::Scalar(0, 255, 0), 2);
	if(confidence < gKeys){
		string name = gLabelsName[label];
		cv::rectangle(frame, face, cv::Scalar(0, 255, 0), 2);
		cv::putText(frame, name, cv::Point(face.x, face.y - 10), cv::FONT_HERSHEY_SIMPLEX, 0.9, cv::Scalar(0, 255, 0), 2);
	}
	else{
		cv::rectangle(frame, face, cv::Scalar(0, 0, 255), 2);
		cv::putText(frame, "unknown", cv::Point(face.x, face.y - 10), cv::FONT_HERSHEY_SIMPLEX, 0.9, cv::Scalar(0, 0, 255), 2);
	}
}

FaceRecognizer::FaceRecognizer(string folderPath,cv::Size newSize,string cascadeFile,int arithmetic)
	:_folderPath(folderPath),_newSize(newSize), _cascadeFile(cascadeFile), _arithmetic(arithmetic){
		_faceCascade.load(cascadeFile);
	}

void FaceRecognizer::FillData(vector<cv::Mat>& images, vector<int>& labels) {
	int index = 0;
	//	for (const auto& entry : std::__fs::filesystem::recursive_directory_iterator(_folderPath)) {
	//		if (entry.is_regular_file()) {
	//			string file_path = entry.path().string();
	//			cv::Mat img = imread(file_path, cv::IMREAD_GRAYSCALE);
	//			if (img.empty())
	//				continue;
	//			vector<cv::Rect> faces;
	//			_faceCascade.detectMultiScale(img, faces, 1.1, 5);
	//			for (const auto& face : faces) {
	//				cv::Mat face_roi = img(face);
	//				if (face_roi.empty())
	//					continue;
	//				resize(face_roi, face_roi, _newSize);
	//				images.push_back(face_roi);
	//				labels.push_back(index);
	//				_labels_name.push_back(entry.path().filename().string());
	//				index++;
	//			}
	//		}
	//	}
	std::ofstream ofs(LABEL_FILE);
	for (const auto& lab : _labels_name)
		ofs << lab << std::endl;

	std::cout << "\033[33m Into -> " << __FUNCTION__ << "()\033[0m" << std::endl;
}

void FaceRecognizer::FetchModel(){
	switch (_arithmetic) {
		case 1:
			_recognizer = cv::face::EigenFaceRecognizer::create();
			_local_model_file = "Eigen_model.yml";
			_keys = EIGEN_FACE_KEYS;
			break;
		case 2:
			_recognizer = cv::face::LBPHFaceRecognizer::create();
			_local_model_file = "/data/local/tmp/model/LBPH_model.yml";
			_keys = LBPH_FACE_KEYS;
			break;
		case 3:
			_recognizer = cv::face::FisherFaceRecognizer::create();
			_local_model_file = "Fisher_model.yml";
			_keys = FISHER_FACE_KEYS;
			break;
	}

	//	if (!std::__fs::filesystem::exists(_local_model_file)) {
	//		vector<cv::Mat> images;
	//		vector<int> labels;
	//
	//		struct timespec start_time, end_time;
	//		std::cout << "Filling image ..." << std::endl;
	//		clock_gettime(CLOCK_REALTIME, &start_time);
	//		FillData(images,labels);
	//		clock_gettime(CLOCK_REALTIME, &end_time);
	//		std::cout << "Fill done" << std::endl;
	//		std::cout << "Number of seconds -> "  <<  end_time.tv_sec - start_time.tv_sec << std::endl;
	//
	//		std::cout << "Training ..." << std::endl;
	//		clock_gettime(CLOCK_REALTIME, &start_time);
	//		_recognizer->train(images,labels);
	//		clock_gettime(CLOCK_REALTIME, &end_time);
	//		std::cout << "Number of seconds -> "  <<  end_time.tv_sec - start_time.tv_sec << std::endl;
	//		std::cout << "Train done" << std::endl;
	//
	//		_recognizer->save(_local_model_file);
	//	}else{
	std::cout << "Reading local mode ..." << std::endl;
	_recognizer->read(_local_model_file);
	//	}

	std::ifstream f(FaceRecognizer::LABEL_FILE);
	if (f.is_open()) {
		std::string line;
		while (std::getline(f, line)) {
			_labels_name.push_back(line);
		}
		f.close();
	}
	std::cout << "\033[33m Into -> " << __FUNCTION__ << "()\033[0m" << std::endl;
}

JNIEXPORT void JNICALL LoadModel(JNIEnv *env, jobject thiz) {
	if(gArithmetic != 1 && gArithmetic != 2 && gArithmetic != 3){
		std::cout << "Arithmetic format error. try '--help'" << std::endl;
		exit(1);
	}
	FaceRecognizer face(gDirectory,cv::Size(92,112),"/data/local/tmp/haarcascades/haarcascade_frontalface_default.xml",gArithmetic);
	face.FetchModel();

	gRecognizer = face.getRecognizer();
	gKeys = face.getKeys();
	gLabelsName = face.getLabelsName();

	//if(!gFile.empty())
	//	face.PredictPhoto(gFile);
	//else
	//	face.PredictCamera();
}

JNIEXPORT void JNICALL faceDetection(JNIEnv *env, jobject thiz, jlong matAddrGray, jlong matAddrRgba) {
	cv::Mat &mGr = *(cv::Mat *) matAddrGray;
	cv::Mat &mRgb = *(cv::Mat *) matAddrRgba;

	// Load the cascade classifier
	cv::CascadeClassifier faceCascade;
	// adb push haarcascades /data/local/tmp/ && adb shell "su -c 'chmod -R 777 /data/local/tmp/haarcascades'"
	faceCascade.load("/data/local/tmp/haarcascades/haarcascade_frontalface_default.xml");

	// Detect faces
	std::vector<cv::Rect> faces;
	faceCascade.detectMultiScale(mGr, faces, 1.1, 2, 0 | cv::CASCADE_SCALE_IMAGE, cv::Size(30, 30));

	// Draw rectangles around detected faces
	for (auto & face : faces) {
		rectangle(mRgb, face, cv::Scalar(255, 0, 0), 2, cv::LINE_AA);
		faceRecognize(mGr(face),mRgb,face);
	}
}

JNIEXPORT void JNICALL eyeDetection(JNIEnv *env, jobject thiz, jlong matAddrGray, jlong matAddrRgba) {
        cv::Mat &mGr = *(cv::Mat *) matAddrGray;
        cv::Mat &mRgb = *(cv::Mat *) matAddrRgba;
	    // Load the cascade classifier
	    cv::CascadeClassifier eyeCascade;
	    eyeCascade.load("/data/local/tmp/haarcascades/haarcascade_eye.xml");

	    // Detect eyes
	    std::vector<cv::Rect> eyes;
	    eyeCascade.detectMultiScale(mGr, eyes, 1.1, 2, 0 | cv::CASCADE_SCALE_IMAGE, cv::Size(30, 30));

	    // Draw rectangles around detected eyes
	    for (auto & eye : eyes) {
	        rectangle(mRgb, eye, cv::Scalar(0, 255, 0), 2, cv::LINE_AA);
	    }
}

static JNINativeMethod nativeMethods[] = {
	{"faceDetection", "(JJ)V", (void*)faceDetection},
	{"eyeDetection", "(JJ)V", (void*)eyeDetection},
	{"LoadModel", "()V", (void*)LoadModel}
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
	JNIEnv* env = nullptr;
	jint result = -1;

	if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
		return -1;
	}

	jclass clazz = env->FindClass("com/rl/ff_face_detection_yj/MainActivity_jni");
	if (clazz == nullptr) {
		return -1;
	}

	if (env->RegisterNatives(clazz, nativeMethods, sizeof(nativeMethods) / sizeof(nativeMethods[0])) != JNI_OK) {
		return -1;
	}

	result = JNI_VERSION_1_6;

	return result;
}
