cmake_minimum_required(VERSION 3.22.1)

include_directories(libyuv/include)
add_subdirectory(libyuv ./build)

add_library(
        libyuv
        SHARED
        libyuv_jni.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        libyuv
        ${log-lib}
        yuv)