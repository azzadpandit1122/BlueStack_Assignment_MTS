# 📦 Library Architecture Determination (Android + NDK)

## 🔍 Overview

This native C++ tool (built with Android NDK) scans a directory for `.so` shared libraries and reports the CPU architecture of each valid ELF file.

**Supported architectures:**
- `armeabi-v7a`
- `arm64-v8a`
- `x86`
- `x86_64`
- `mips`

---

## 🛠️ Setup (Android NDK Environment)

### ✅ Requirements

- Android Studio with **NDK** support
- CMake or ndk-build
- JNI interface (for Java/Kotlin communication)
- Native code in `src/main/cpp/`

---

## 🏗️ Build Instructions

### Using **CMake** (`externalNativeBuild`)

1. Add your C++ source (e.g. `scan.cpp`) to `src/main/cpp/`.

2. Create a `CMakeLists.txt` file:

```cmake
cmake_minimum_required(VERSION 3.10.2)
project(libarchcounter)

add_library(
    archcounter
    SHARED
    scan.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    archcounter
    ${log-lib}
)

```base
android {
    ...
    defaultConfig {
        externalNativeBuild {
            cmake {
                cppFlags ""
            }
        }
    }

    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
        }
    }
}
```
### Output
```text
libfoo.so  → arm64-v8a  
libbar.so  → x86  
libbaz.so  → unsupported  
Total valid libraries: 2

```


[![Download APK](https://img.shields.io/badge/Download-APK-blue.svg?logo=android&logoColor=white)](https://github.com/azzadpandit1122/BlueStack_Assignment_MTS/raw/master/app/src/main/java/com/example/myapplication/release%20apk/app-debug%20(1).apk)


<img width="540" height="1206" alt="image" src="https://github.com/user-attachments/assets/983d2cf2-6a3d-4ef3-b438-5d28f279ac0b" />



