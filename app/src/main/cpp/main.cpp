#include "main.hpp"
#include <sys/unistd.h>

#include <utility>

int gArithmetic = 2; //Default use LBPHFaceRecognizer
std::string gDirectory = "/lfw";
std::string gWorkPath = "/data/user/0/com.rl.ff_face_detection_yj/files";
cv::Ptr<cv::face::FaceRecognizer> gRecognizer = nullptr;
int gKeys = -1;
std::vector<string> gLabelsName;

cv::Ptr<cv::face::FaceRecognizer> FaceRecognizer::getRecognizer(){
	return _recognizer;
}

int FaceRecognizer::getKeys() const{
	return _keys;
}

std::vector<string> FaceRecognizer::getLabelsName(){
	return _labels_name;
}

//void faceRecognize(cv::Mat& face_roi,cv::Mat frame,cv::Rect face){ TODO
void faceRecognize(const cv::Mat& face_roi,cv::Mat frame,cv::Rect face){
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

//FaceRecognizer::FaceRecognizer(string folderPath,cv::Size newSize,string cascadeFile,int arithmetic)
//		:_folderPath(folderPath),_newSize(newSize), _cascadeFile(cascadeFile), _arithmetic(arithmetic){
//	_faceCascade.load(cascadeFile);
//} TODO
FaceRecognizer::FaceRecognizer(string folderPath,cv::Size newSize,const string& cascadeFile,int arithmetic)
	:_folderPath(std::move(folderPath)),_newSize(newSize), _cascadeFile(cascadeFile), _arithmetic(arithmetic){
		_faceCascade.load(cascadeFile);
	}

void FaceRecognizer::setWorkPath(string p){
	//	_workPath = p; //TODO
	_workPath = std::move(p); //将 p 的资源所有权转移到 _workPath 中，避免了复制操作，从而提高了效率, p 成为了一个空对象
	_folderPath = _workPath + _folderPath;
	LABEL_FILE = _workPath + LABEL_FILE;
	_cascadeFile = _workPath + _cascadeFile;
}

void findImages(const string& folderPath, cv::CascadeClassifier& faceCascade, const cv::Size& newSize, vector<cv::Mat>& images, vector<int>& labels, vector<string>& labelsName, int& index) {
	DIR *dir;
	struct dirent *ent;
	if ((dir = opendir(folderPath.c_str())) != NULL) {
		while ((ent = readdir(dir)) != NULL) {
			if (ent->d_type == DT_REG) {  // 判断是否是普通文件
				string filename = ent->d_name;
				string filepath = folderPath + "/" + filename;
				cv::Mat img = imread(filepath, cv::IMREAD_GRAYSCALE);
				if (img.empty())
					continue;
				vector<cv::Rect> faces;
				faceCascade.detectMultiScale(img, faces, 1.1, 5);
				for (const auto& face : faces) {
					cv::Mat face_roi = img(face);
					if (face_roi.empty())
						continue;
					resize(face_roi, face_roi, newSize);
					images.push_back(face_roi);
					labels.push_back(index);
					labelsName.push_back(filename);
					index++;
				}
			} else if (ent->d_type == DT_DIR && strcmp(ent->d_name, ".") != 0 && strcmp(ent->d_name, "..") != 0) {  // 判断是否是文件夹
				string subFolderPath = folderPath + "/" + ent->d_name;
				findImages(subFolderPath, faceCascade, newSize, images, labels, labelsName, index);
			}
		}
		closedir(dir);
	} else {
		// 打开文件夹失败
	}
}

void FaceRecognizer::FillData(vector<cv::Mat>& images, vector<int>& labels) {
	//	int index = 0;
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
	int index = 0;
	findImages(_folderPath,_faceCascade,_newSize,images,labels,_labels_name,index);

	std::ofstream ofs(LABEL_FILE);
	for (const auto& lab : _labels_name)
		ofs << lab << std::endl;

	LOGE("\033[33m Into -> %s()\033[0m", __FUNCTION__ );
}

void FaceRecognizer::FetchModel(){
	switch (_arithmetic) {
		case 1:
			_recognizer = cv::face::EigenFaceRecognizer::create();
			_local_model_file = "/model/Eigen_model.yml";
			_keys = EIGEN_FACE_KEYS;
			break;
		case 2:
			_recognizer = cv::face::LBPHFaceRecognizer::create();
			_local_model_file = _workPath + "/model/LBPH_model.yml";
			_keys = LBPH_FACE_KEYS;
			break;
		case 3:
			_recognizer = cv::face::FisherFaceRecognizer::create();
			_local_model_file = "/model/Fisher_model.yml";
			_keys = FISHER_FACE_KEYS;
			break;
	}

	if (access(_local_model_file.c_str(), F_OK) != 0) {
		vector<cv::Mat> images;
		vector<int> labels;
		LOGE("Train done out model -> %s",_local_model_file.c_str());
		//		struct timespec start_time, end_time;
		struct timespec start_time{}, end_time{}; //TODO
		LOGE("Filling image ...");
		clock_gettime(CLOCK_REALTIME, &start_time);
		FillData(images,labels);
		clock_gettime(CLOCK_REALTIME, &end_time);
		LOGE("Fill done");
		LOGE("Number of seconds -> %ld",end_time.tv_sec - start_time.tv_sec);

		LOGE("Training ...");
		clock_gettime(CLOCK_REALTIME, &start_time);
		_recognizer->train(images,labels);
		clock_gettime(CLOCK_REALTIME, &end_time);
		LOGE("Number of seconds -> %ld",end_time.tv_sec - start_time.tv_sec);
		LOGE("Train done out model -> %s",_local_model_file.c_str());
		_recognizer->save(_local_model_file);
	}else{
		LOGE("Reading local mode ...");
		_recognizer->read(_local_model_file);
	}

	std::ifstream f(FaceRecognizer::LABEL_FILE);
	if (f.is_open()) {
		std::string line;
		while (std::getline(f, line)) {
			_labels_name.push_back(line);
		}
		f.close();
	}
	LOGE("\033[33m Into -> %s()\033[0m", __FUNCTION__ );
}

JNIEXPORT int JNICALL LoadModel(JNIEnv *env, jobject thi, jstring workPath,jint arithmetic,jstring directory) {
	gArithmetic = arithmetic;
	gDirectory = env->GetStringUTFChars(directory,nullptr);
	if(gArithmetic != 1 && gArithmetic != 2 && gArithmetic != 3){
		LOGE("Arithmetic format error. try '--help'");
		exit(1);
	}
	string _workPath = env->GetStringUTFChars(workPath,nullptr);
	string modeDir = _workPath + "/model";
	int err = -1 ;
	err = access(modeDir.c_str(), F_OK);
	if (err == -1){
		err = mkdir(modeDir.c_str(),0777);
	}
	if (err == -1){
		return -1;
	}
	gWorkPath = _workPath;
	FaceRecognizer face(gDirectory,cv::Size(92,112),_workPath + "/haarcascades/haarcascade_frontalface_default.xml",gArithmetic);
	face.setWorkPath(_workPath);
	face.FetchModel();
	gRecognizer = face.getRecognizer();
	gKeys = face.getKeys();
	gLabelsName = face.getLabelsName();
	return 0;
}

JNIEXPORT void JNICALL faceDetection(JNIEnv *env, jobject thi, jlong matAddressGray, jlong matAddressRgba) {
	cv::Mat &mGr = *(cv::Mat *) matAddressGray;
	cv::Mat &mRgb = *(cv::Mat *) matAddressRgba;

	// Load the cascade classifier
	cv::CascadeClassifier faceCascade;
	faceCascade.load(  gWorkPath+"/haarcascades/haarcascade_frontalface_default.xml");

	// Detect faces
	std::vector<cv::Rect> faces;
	faceCascade.detectMultiScale(mGr, faces, 1.1, 2, 0 | cv::CASCADE_SCALE_IMAGE, cv::Size(30, 30));

	// Draw rectangles around detected faces
	for (auto & face : faces) {
		rectangle(mRgb, face, cv::Scalar(255, 0, 0), 2, cv::LINE_AA);
		faceRecognize(mGr(face),mRgb,face);
	}
}

JNIEXPORT void JNICALL eyeDetection(JNIEnv *env, jobject thi, jlong matAddressGray, jlong matAddressRgba) {
	//	cv::Mat &mGr = *(cv::Mat *) matAddressGray;
	//	cv::Mat &mRgb = *(cv::Mat *) matAddressRgba;
	//	// Load the cascade classifier
	//	cv::CascadeClassifier eyeCascade;
	//	eyeCascade.load("/haarcascades/haarcascade_eye.xml");
	//
	//	// Detect eyes
	//	std::vector<cv::Rect> eyes;
	//	eyeCascade.detectMultiScale(mGr, eyes, 1.1, 2, 0 | cv::CASCADE_SCALE_IMAGE, cv::Size(30, 30));
	//
	//	// Draw rectangles around detected eyes
	//	for (auto & eye : eyes) {
	//		rectangle(mRgb, eye, cv::Scalar(0, 255, 0), 2, cv::LINE_AA);
	//	}
}

static JNINativeMethod nativeMethods[] = {
	{"faceDetection", "(JJ)V", (void*)faceDetection},
	{"eyeDetection", "(JJ)V", (void*)eyeDetection},
	{"LoadModel", "(Ljava/lang/String;ILjava/lang/String;)I", (void*)LoadModel}
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
