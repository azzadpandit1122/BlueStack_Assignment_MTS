cmake_minimum_required(VERSION 3.18.1)
project(lib_arch_counter)

# 🟢 1.  Enable C++17 so <string>, <filesystem>, … resolve
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 🟢 2.  Build the JNI library you’ll load via System.loadLibrary("lib_arch_counter")
add_library(lib_arch_counter SHARED
        lib_arch_counter.cpp        # JNI wrapper
        ArchScanner.cpp)            # core logic you split out

# (If ArchScanner.cpp/.hpp sit in a sub‑folder, add its path)
# target_include_directories(lib_arch_counter PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

# 🟢 3.  Link against Android’s logcat library if you ever call __android_log_print
find_library(log-lib log)
#target_link_libraries(lib_arch_counter ${log-lib})

target_include_directories(lib_arch_counter
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include)


## For more information about using CMake with Android Studio, read the
## documentation: https://d.android.com/studio/projects/add-native-code.html
#
## Sets the minimum version of CMake required to build the native library.
#
#cmake_minimum_required(VERSION 3.18.1)
#
## Declares and names the project.
#
#project("myapplication")
#
## Creates and names a library, sets it as either STATIC
## or SHARED, and provides the relative paths to its source code.
## You can define multiple libraries, and CMake builds them for you.
## Gradle automatically packages shared libraries with your APK.
#
#add_library( # Sets the name of the library.
#        myapplication
#
#        # Sets the library as a shared library.
#        SHARED
#
#        # Provides a relative path to your source file(s).
#        lib_arch_counter.cpp)
#
## Searches for a specified prebuilt library and stores the path as a
## variable. Because CMake includes system libraries in the search path by
## default, you only need to specify the name of the public NDK library
## you want to add. CMake verifies that the library exists before
## completing its build.
#
#find_library( # Sets the name of the path variable.
#        log-lib
#
#        # Specifies the name of the NDK library that
#        # you want CMake to locate.
#        log)
#
## Specifies libraries CMake should link to your target library. You
## can link multiple libraries, such as libraries you define in this
## build script, prebuilt third-party libraries, or system libraries.
#
#target_link_libraries( # Specifies the target library.
#        myapplication
#
#        # Links the target library to the log library
#        # included in the NDK.
#        ${log-lib})