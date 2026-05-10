/*
 * libpairipcore.so stub — Bypasses pairip integrity verification VM.
 *
 * Exports:
 *   JNI_OnLoad  — Returns JNI_VERSION_1_6 (success)
 *   Java_com_pairip_VMRunner_executeVM — Returns null (skip VM execution)
 *
 * Without the real pairipcore native lib, the app crashes on launch because
 * VMRunner.<clinit> calls System.loadLibrary("pairipcore"). This stub lets
 * the app start, but all pairip-decrypted String fields remain null/empty.
 */

#include <jni.h>
#include <android/log.h>

#define LOG_TAG "pairipcore-stub"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGI("pairipcore stub loaded — VM execution disabled");
    return JNI_VERSION_1_6;
}

JNIEXPORT jobject Java_com_pairip_VMRunner_executeVM(JNIEnv *env, jclass clazz,
                                                       jbyteArray vmCode, jobjectArray args) {
    LOGI("executeVM called — returning null (VM bypass)");
    return NULL;
}