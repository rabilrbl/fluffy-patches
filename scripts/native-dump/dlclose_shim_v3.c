/*
 * dlclose_shim_v3.c — LD_PRELOAD shim to prevent dlclose of libpairipcore.so
 * 
 * This version only hooks dlclose, not dlopen, to avoid crashing the VM startup.
 * It checks the filename of the library being closed and blocks dlclose if it
 * matches pairipcore. Since dlclose only gets a handle (not filename), we 
 * track dlopen handles via the linker namespace.
 *
 * Actually, simpler approach: on Android bionic, dlclose is in the linker.
 * We can use __dl__dlclose as the real symbol. But the simplest working
 * approach: only override dlclose and use dlopen to check filenames.
 *
 * Key insight: On Android, the real dlclose is accessible via dlvsym or
 * by calling the function through the linker directly.
 * For bionic, we can use the __loader_ prefix for the real function.
 */

#include <dlfcn.h>
#include <string.h>
#include <android/log.h>
#include <stdio.h>

#define LOG_TAG "dlclose_shim"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// Android bionic exposes these as __loader_ prefixed symbols for use by LD_PRELOAD
// This avoids the circular call issue with dlsym(RTLD_NEXT, ...)
int __loader_dlclose(void* handle);
void* __loader_dlopen(const char* filename, int flags);

static void* pairipcore_handle = NULL;

void* dlopen(const char* filename, int flags) {
    void* result = __loader_dlopen(filename, flags);
    if (filename && strstr(filename, "pairipcore")) {
        pairipcore_handle = result;
        LOGI("dlopen: pairipcore loaded at %p (flags=0x%x)", result, flags);
    }
    return result;
}

int dlclose(void* handle) {
    if (handle != NULL && handle == pairipcore_handle) {
        LOGI("dlclose: BLOCKED for pairipcore at %p — keeping library loaded", handle);
        return 0;  // pretend success
    }
    return __loader_dlclose(handle);
}