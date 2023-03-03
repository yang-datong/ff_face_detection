cmake_minimum_required(VERSION 3.10.2)

project("ff_face_detection_yj")

include_directories(${CMAKE_SOURCE_DIR}/../jniLibs/include)

add_library(main SHARED main.cpp )

find_library(log-lib log )

message("${CMAKE_SOURCE_DIR}/../jniLibs/arm64-v8a")

file( GLOB mylib ../jniLibs/arm64-v8a/*.so)

target_link_libraries(main ${mylib}  ${log-lib} )
