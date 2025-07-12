package com.example.myapplication

object NativeLib {
    init {
        System.loadLibrary("lib_arch_counter")   // matches CMake add_library(...)
    }
    external fun scanDirectory(path: String): String
}
