/*
 * dlclose_shim_v4.c — LD_PRELOAD shim to prevent dlclose of libpairipcore.so
 * 
 * This version resolves the real functions at runtime using __loader_ symbols
 * which are provided by the Android bionic linker for LD_PRELOAD use.
 */
#include <dlfcn.h>
#include <string.h>
#include <android/log.h>

#define LOG_TAG "dlclose_shim"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// Function pointer types
typedef void* (*dlopen_fn)(const char*, int);
typedef int (*dlclose_fn)(void*);

// Resolved real function pointers (set in .init)
static dlopen_fn real_dlopen = NULL;
static dlclose_fn real_dlclose = NULL;

static void* pairipcore_handle = NULL;

__attribute__((constructor))
static void init_shim(void) {
    // On Android bionic, the real implementations are available via dlsym
    // using the special RTLD_NEXT, but only after our library is loaded.
    // The __loader_ symbols are exported by the linker for our use.
    real_dlopen = (dlopen_fn)dlsym(RTLD_NEXT, "dlopen");
    real_dlclose = (dlclose_fn)dlsym(RTLD_NEXT, "dlclose");
    
    if (real_dlopen) LOGI("init: resolved real dlopen at %p", real_dlopen);
    else LOGI("init: WARNING - could not resolve real dlopen");
    
    if (real_dlclose) LOGI("init: resolved real dlclose at %p", real_dlclose);
    else LOGI("init: WARNING - could not resolve real dlclose");
}

void* dlopen(const char* filename, int flags) {
    if (!real_dlopen) {
        // Fallback: try to resolve now
        real_dlopen = (dlopen_fn)dlsym(RTLD_NEXT, "dlopen");
    }
    
    void* result = real_dlopen ? real_dlopen(filename, flags) : NULL;
    
    if (filename && strstr(filename, "pairipcore")) {
        pairipcore_handle = result;
        LOGI("dlopen: pairipcore loaded at %p (flags=0x%x)", result, flags);
    }
    return result;
}

int dlclose(void* handle) {
    if (!real_dlclose) {
        real_dlclose = (dlclose_fn)dlsym(RTLD_NEXT, "dlclose");
    }
    
    if (handle != NULL && handle == pairipcore_handle) {
        LOGI("dlclose: BLOCKED for pairipcore at %p — keeping loaded", handle);
        return 0;
    }
    
    return real_dlclose ? real_dlclose(handle) : -1;
}