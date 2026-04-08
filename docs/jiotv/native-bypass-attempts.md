# Session 2026-04-05 (Continued): Native Library Reverse Engineering and Mock Library Attempts

## Attempt: ARM64 Mock Library Creation

Built ARM64 and x86_64 mock libraries using Android NDK:
```bash
NDK=/home/rabil/Android/ndk/27.0.12077973
$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android21-clang++ \
    -shared -fPIC -Wl,-soname,libpairipcore.so -static-libstdc++ \
    -o /tmp/libpairipcore_arm64_mock.so mock_pairipcore.cpp
```

### Mock Library Code
```cpp
JNIEXPORT jbyteArray JNICALL Java_com_pairip_VMRunner_executeVM(
    JNIEnv* env, jobject thiz, jbyteArray bytecode, jobjectArray args) {
    jbyteArray result = env->NewByteArray(0);
    return result;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    return JNI_VERSION_1_6;
}
```

### Results
- **Library loads successfully** on x86_64 AVD (no UnsatisfiedLinkError)
- **Same ExceptionInInitializerError** at BaseActivity.onCreate
- Mock library provides no Kotlin metadata initialization

## Root Cause Confirmed

The native library `libpairipcore.so` does **critical runtime initialization** that cannot be replicated via smali patches or simple mock libraries:

1. **Kotlin Metadata Patching**: The library appears to patch Kotlin runtime metadata at load time
2. **ClassReference Initialization**: Kotlin's ClassReference class requires data from the native library
3. **Static Field Initialization**: The IklIsnnNWteL class with 49 static String fields requires native initialization

Without the original library's initialization:
- `ExceptionInInitializerError` occurs in any class that uses Kotlin reflection
- NullPointerException on `String.length()` calls on uninitialized fields

## Key Discovery

The original library's `ExecuteProgram` function (at offset 0x592e0) does more than just "execute bytecode":
- Reads encrypted bytecode from assets
- Decrypts and "interprets" it 
- The bytecode likely contains Kotlin metadata patches that are applied at runtime

## Conclusion

**For x86_64 AVD**: Smali patches + mock native library insufficient
**For ARM64 Physical Device**: Original library fails integrity check

**True bypass requires**:
1. Deep reverse-engineering of `ExecuteProgram` to understand bytecode format
2. Creating mock bytecode that provides equivalent initialization
3. Or finding/updating the Snailsoft bypass library for pairip v404

## Files Created

- `/tmp/libpairipcore_arm64_mock.so` - ARM64 mock library
- `/tmp/libpairipcore_x86_64_mock.so` - x86_64 mock library
- `/tmp/mock_pairipcore.cpp` - Mock source code
- `/tmp/jiotv-mock2-aligned-debugSigned.apk` - APK with mock library

## Next Steps

1. **Disassemble ExecuteProgram function** to understand bytecode format
2. **Extract bytecode** from assets and analyze format
3. **Create valid mock bytecode** that initializes Kotlin metadata properly
4. **OR** find updated Snailsoft/unpairip library for v404
