cmake_minimum_required(VERSION 3.22.1)
project(audiovis_native)

add_library(audiovis_native SHARED
    native-lib.cpp
    FFTProcessor.cpp
)
find_library(log-lib log)
target_link_libraries(audiovis_native ${log-lib})
