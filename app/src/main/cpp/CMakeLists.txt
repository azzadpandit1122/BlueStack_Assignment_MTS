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
