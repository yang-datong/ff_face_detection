#include "main.hpp"

#include <utility>

int gArithmetic = 2; //Default use LBPHFaceRecognizer
std::string gDirectory = "lfw";
std::string gWorkPath = "/data/user/0/com.rl.ff_face_detection_yj/files";
std::string gCascadeFile = "haarcascades/haarcascade_frontalface_default.xml";
cv::Ptr<cv::face::FaceRecognizer> gRecognizer = nullptr;
std::vector<string> gLabelsName;
int gKeys = -1;

int CheckFilePath(){
    int err;
    string modeDir = gWorkPath + "/" + "model";
    err = access(modeDir.c_str(), F_OK);
    if (err == -1)
        err = mkdir(modeDir.c_str(),0777);

    if (err == -1){
        LOGE("Create directory failed");
        return -1;
    }

    err = access(gDirectory.c_str(), F_OK);
    if (err == -1){
        LOGE("Don't find %s",gDirectory.c_str());
        return -1;
    }

    err = access(gCascadeFile.c_str(), F_OK);
    if (err == -1){
        LOGE("Don't find %s",gCascadeFile.c_str());
        return -1;
    }

    if(gArithmetic != 1 && gArithmetic != 2 && gArithmetic != 3){
        LOGE("Arithmetic format error");
        return -1;
    }
    return 0;
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

FaceRecognizer::FaceRecognizer(string workPath,string folderPath,cv::Size newSize,const string& cascadeFile,int arithmetic)
	:_workPath(std::move(workPath)),_folderPath(std::move(folderPath)),_newSize(newSize), _cascadeFile(cascadeFile), _arithmetic(arithmetic){
		_faceCascade.load(cascadeFile);
	}

cv::Ptr<cv::face::FaceRecognizer> FaceRecognizer::getRecognizer(){
    return _recognizer;
}

std::vector<string> FaceRecognizer::getLabelsName(){
    return _labels_name;
}

void FaceRecognizer::FillData(vector<cv::Mat>& images, vector<int>& labels) {
	int index = 0;
	findImages(_folderPath,_faceCascade,_newSize,images,labels,_labels_name,index);

	std::ofstream ofs(_labelFile);
	for (const auto& lab : _labels_name)
		ofs << lab << std::endl;

	LOGE("\033[33m Into -> %s()\033[0m", __FUNCTION__ );
}

void FaceRecognizer::FetchModel(){
	switch (_arithmetic) {
		case 1:
			_recognizer = cv::face::EigenFaceRecognizer::create();
			_local_model_file = _workPath + "/model/Eigen_model.yml";
			_labelFile = _workPath + "/model/Eigen_labels.npy";
			_keys = EIGEN_FACE_KEYS;
			break;
		case 2:
			_recognizer = cv::face::LBPHFaceRecognizer::create();
			_local_model_file = _workPath + "/model/LBPH_model.yml";
			_labelFile = _workPath + "/model/LBPH_labels.npy";
			_keys = LBPH_FACE_KEYS;
			break;
		case 3:
			_recognizer = cv::face::FisherFaceRecognizer::create();
			_local_model_file = _workPath + "/model/Fisher_model.yml";
			_labelFile = _workPath + "/model/Fisher_labels.npy";
			_keys = FISHER_FACE_KEYS;
			break;
	}

	if (access(_local_model_file.c_str(), F_OK) != 0) {
		vector<cv::Mat> images;
		vector<int> labels;
		LOGE("Train done out model -> %s",_local_model_file.c_str());
		struct timespec start_time{}, end_time{};
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

	std::ifstream f(_labelFile);
	if (f.is_open()) {
		std::string line;
		while (std::getline(f, line)) {
			_labels_name.push_back(line);
		}
		f.close();
	}
	LOGE("\033[33m Into -> %s()\033[0m", __FUNCTION__ );
}

int FaceRecognizer::getKeys() const{
    return _keys;
}

JNIEXPORT int JNICALL JNI_Initialization(JNIEnv *env, jobject thi, jstring workPath,jstring dataDirectory,jstring cascadeFile,jint useArithmetic) {
	gWorkPath = env->GetStringUTFChars(workPath,nullptr);
	gDirectory = gWorkPath + "/" + env->GetStringUTFChars(dataDirectory,nullptr);
	gCascadeFile = gWorkPath + "/" + env->GetStringUTFChars(cascadeFile,nullptr);
	gArithmetic = useArithmetic;

	if(CheckFilePath()){
		return -1;
	}

	FaceRecognizer face(gWorkPath,gDirectory,cv::Size(92,112),gCascadeFile,gArithmetic);
	//	face.setWorkPath(gWorkPath);
	face.FetchModel();
	gRecognizer = face.getRecognizer();
	gKeys = face.getKeys();
	gLabelsName = face.getLabelsName();
	return 0;
}

JNIEXPORT void JNICALL JNI_FaceDetection(JNIEnv *env, jobject thi, jlong matAddressGray, jlong matAddressRgba) {
	cv::Mat &mGr = *(cv::Mat *) matAddressGray;
	cv::Mat &mRgb = *(cv::Mat *) matAddressRgba;

	// Load the cascade classifier
	cv::CascadeClassifier faceCascade;
	faceCascade.load(gCascadeFile);

	// Detect faces
	std::vector<cv::Rect> faces;
	faceCascade.detectMultiScale(mGr, faces, 1.1, 2, 0 | cv::CASCADE_SCALE_IMAGE, cv::Size(30, 30));

	// Draw rectangles around detected faces
	for (auto & face : faces) {
		rectangle(mRgb, face, cv::Scalar(255, 0, 0), 2, cv::LINE_AA);
        //resize(face_roi, face_roi, _newSize);
        int label = -1;
        double confidence = 0.0;
        gRecognizer->predict(mGr(face),label,confidence);
        //cv::putText(frame, std::to_string(confidence), cv::Point(face.x, face.y - 10), cv::FONT_HERSHEY_SIMPLEX, 0.9, cv::Scalar(0, 255, 0), 2);
        if(confidence < gKeys){
            string name = gLabelsName[label];
            cv::rectangle(mRgb, face, cv::Scalar(0, 255, 0), 2);
            cv::putText(mRgb, name, cv::Point(face.x, face.y - 10), cv::FONT_HERSHEY_SIMPLEX, 0.9, cv::Scalar(0, 255, 0), 2);
        }
        else{
            cv::rectangle(mRgb, face, cv::Scalar(0, 0, 255), 2);
            cv::putText(mRgb, "unknown", cv::Point(face.x, face.y - 10), cv::FONT_HERSHEY_SIMPLEX, 0.9, cv::Scalar(0, 0, 255), 2);
        }
	}
}

JNIEXPORT void JNICALL JNI_EyeDetection(JNIEnv *env, jobject thi, jlong matAddressGray, jlong matAddressRgba) {
	cv::Mat &mGr = *(cv::Mat *) matAddressGray;
	cv::Mat &mRgb = *(cv::Mat *) matAddressRgba;
	// Load the cascade classifier
	//cv::CascadeClassifier eyeCascade;
	//eyeCascade.load("/haarcascades/haarcascade_eye.xml");
	//
	//// Detect eyes
	//std::vector<cv::Rect> eyes;
	//eyeCascade.detectMultiScale(mGr, eyes, 1.1, 2, 0 | cv::CASCADE_SCALE_IMAGE, cv::Size(30, 30));
	//
	//// Draw rectangles around detected eyes
	//for (auto & eye : eyes) {
	//	rectangle(mRgb, eye, cv::Scalar(0, 255, 0), 2, cv::LINE_AA);
	//}
}

static JNINativeMethod nativeMethods[] = {
	{"JNI_FaceDetection", "(JJ)V", (void*)JNI_FaceDetection},
	{"JNI_EyeDetection", "(JJ)V", (void*)JNI_EyeDetection},
	{"JNI_Initialization", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)I", (void*)JNI_Initialization}
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
