#include <dlfcn.h>
#include <string.h>
#include <stdio.h>
#include <android/log.h>

#define LOG_TAG "dlclose_shim"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// On Android, LD_PRELOAD'd libraries need to use __loader_ prefixed 
// real functions or call through the linker directly.
// Using android_dlopen_ext and direct linker symbols is more reliable.

// Track the pairipcore handle
static void* pairipcore_handle = NULL;
static int pairipcore_blocked = 0;

// Use the real dlopen/dlclose from the dynamic linker
// On Android 64-bit, these are in the linker itself
// The LD_PRELOAD shim can intercept by defining these symbols,
// and calling the "next" version via dlsym(RTLD_NEXT, ...)

void* dlopen(const char* filename, int flags) {
    void* (*real_dlopen)(const char*, int);
    void* result;
    
    real_dlopen = (void* (*)(const char*, int))dlsym(RTLD_NEXT, "dlopen");
    if (!real_dlopen) {
        LOGW("dlopen: could not resolve real dlopen");
        return NULL;
    }
    
    result = real_dlopen(filename, flags);
    if (filename && strstr(filename, "pairipcore")) {
        pairipcore_handle = result;
        LOGI("dlopen: pairipcore loaded at %p (flags=0x%x)", result, flags);
    }
    return result;
}

// Also hook android_dlopen_ext which is what the Android linker actually uses
void* android_dlopen_ext(const char* filename, void* arg) {
    void* (*real_android_dlopen_ext)(const char*, void*);
    void* result;
    
    real_android_dlopen_ext = (void* (*)(const char*, void*))dlsym(RTLD_NEXT, "android_dlopen_ext");
    if (!real_android_dlopen_ext) {
        LOGW("android_dlopen_ext: could not resolve real function");
        return NULL;
    }
    
    result = real_android_dlopen_ext(filename, arg);
    if (filename && strstr(filename, "pairipcore")) {
        pairipcore_handle = result;
        LOGI("android_dlopen_ext: pairipcore loaded at %p", result);
    }
    return result;
}

int dlclose(void* handle) {
    int (*real_dlclose)(void*);
    
    if (handle == pairipcore_handle && pairipcore_handle != NULL) {
        LOGI("dlclose: BLOCKED for pairipcore at %p — keeping library loaded", handle);
        pairipcore_blocked = 1;
        return 0;  // pretend it succeeded
    }
    
    real_dlclose = (int (*)(void*))dlsym(RTLD_NEXT, "dlclose");
    if (!real_dlclose) {
        LOGW("dlclose: could not resolve real dlclose");
        return -1;
    }
    
    return real_dlclose(handle);
}