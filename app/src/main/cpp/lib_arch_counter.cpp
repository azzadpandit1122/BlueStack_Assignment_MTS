#include <jni.h>
#include <string>
#include "ArchScanner.h"   // keep .h if that’s the actual header

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_myapplication_NativeLib_scanDirectory(   // ▼ package path here ▼
        JNIEnv* env, jobject /* this */, jstring dirPathJ) {

    const char* dirPath = env->GetStringUTFChars(dirPathJ, nullptr);
    if (dirPath == nullptr)           // safety check
        return nullptr;

    std::string result = ArchScanner::scan(dirPath);
    env->ReleaseStringUTFChars(dirPathJ, dirPath);

    return env->NewStringUTF(result.c_str());
}
//#include <jni.h>
//#include <string>
//#include "ArchScanner.h"
//
//extern "C"
//[[maybe_unused]] JNIEXPORT jstring JNICALL
//Java_com_example_libarchcounter_NativeLib_scanDirectory(
//        JNIEnv* env, jobject, jstring dirPathJ) {
//
//    const char* dirPath = env->GetStringUTFChars(dirPathJ, nullptr);
//
//    if (dirPath == nullptr)            // out‑of‑memory check
//        return nullptr;
//
//    std::string result = ArchScanner::scan(dirPath);
//    env->ReleaseStringUTFChars(dirPathJ, dirPath);
//    return env->NewStringUTF(result.c_str());
//}

//#include <jni.h>
//#include <string>
//
//extern "C" JNIEXPORT jstring JNICALL
//Java_com_example_myapplication_MainActivity_stringFromJNI(
//        JNIEnv* env,
//        jobject /* this */) {
//    std::string hello = "Hello from C++";
//    return env->NewStringUTF(hello.c_str());
//}