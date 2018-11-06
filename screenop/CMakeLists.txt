cmake_minimum_required(VERSION 3.4.1)

#yuv
include_directories(${CMAKE_SOURCE_DIR}/libs/yuv/include)
add_subdirectory(${CMAKE_SOURCE_DIR}/libs/yuv)

#rtmp
#这个可以对应Android.mk文件中的 -local_cfflags 对应C++中的define
add_definitions("-DNO_CRYPTO")
set(INC_DIR ${CMAKE_SOURCE_DIR}/libs/rtmp/librtmp)
set(LIB_SRC ${CMAKE_SOURCE_DIR}/libs/rtmp/librtmp)
include_directories(${INC_DIR})


#x264
set(LINK_X264_DIR ${CMAKE_SOURCE_DIR}/libs/x264/${ANDROID_ABI})
include_directories(${CMAKE_SOURCE_DIR}/libs/x264/include)
add_library(x264 SHARED IMPORTED)
set_target_properties(x264 PROPERTIES IMPORTED_LOCATION ${LINK_X264_DIR}/libx264.so)
message("API ")
message(${LINK_X264_DIR}/libx264.so)


add_library( # Sets the name of the library.
             native-lib
             SHARED
             ${LIB_SRC}/amf.c
             ${LIB_SRC}/hashswf.c
             ${LIB_SRC}/log.c
             ${LIB_SRC}/parseurl.c
             ${LIB_SRC}/rtmp.c
             src/main/cpp/native-lib.cpp

             src/main/cpp/X264Encoder.cpp
             src/main/cpp/X264Encoder.h
             src/main/cpp/Java_x264_X264Encoder.cpp
             src/main/cpp/log.h
              )

find_library(log-lib
              log )

target_link_libraries(native-lib
                       yuv
                       x264
                       ${log-lib}
                     )
