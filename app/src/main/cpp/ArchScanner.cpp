//
// Created by emang on 7/12/2025.
//
#include "ArchScanner.h"
#include <filesystem>
#include <fstream>
#include <sstream>
#include <iomanip>
#include <vector>
#include <jni.h>

namespace fs = std::filesystem;

namespace ArchScanner {

    struct LibraryInfo {
        std::string filename;
        std::string arch;
    };

    std::string classify_arch(uint16_t e_machine) {
        switch (e_machine) {
            case 0x28: return "armeabi-v7a";
            case 0xB7: return "arm64-v8a";
            case 3:    return "x86";
            case 62:   return "x86-64";
            case 8:    return "mips";
            default:   return "unknown";
        }
    }

    bool inspect_library(const fs::path& path, LibraryInfo& info) {
        if (!fs::is_regular_file(path) || path.extension() != ".so")
            return false;

        std::ifstream file(path, std::ios::binary);
        if (!file)
            return false;

        unsigned char ident[4];
        file.read(reinterpret_cast<char*>(ident), 4);
        if (file.gcount() != 4 || ident[0] != 0x7F || ident[1] != 'E' || ident[2] != 'L' || ident[3] != 'F')
            return false;

        file.seekg(18, std::ios::beg);
        uint16_t e_machine;
        file.read(reinterpret_cast<char*>(&e_machine), sizeof(e_machine));
        if (!file)
            return false;

        std::string arch = classify_arch(e_machine);
        if (arch == "unknown")
            return false;

        info.filename = path.filename().string();
        info.arch = arch;
        return true;
    }

    std::string scan(const std::string& folderPath) {
        std::ostringstream output;
        std::vector<LibraryInfo> libraries;

        fs::path folder(folderPath);
        if (!fs::exists(folder) || !fs::is_directory(folder)) {
            return "Invalid directory\n";
        }

        for (const auto& entry : fs::recursive_directory_iterator(folder)) {
            LibraryInfo info;
            if (inspect_library(entry.path(), info))
                libraries.push_back(info);
        }

        output << "Total number of libraries: " << libraries.size() << "\n\n";
        output << std::left << std::setw(25) << "File" << "ArchType\n";
        output << std::left << std::setw(25) << "====" << "========\n";
        for (const auto& lib : libraries)
            output << std::left << std::setw(25) << lib.filename << lib.arch << '\n';

        return output.str();
    }

}
