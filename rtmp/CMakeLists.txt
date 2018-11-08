cmake_minimum_required(VERSION 3.4.1)

add_definitions("-DNO_CRYPTO")
include_directories(${CMAKE_SOURCE_DIR}/libs/rtmp/librtmp)

#native-lib
file(GLOB PROJECT_SOURCES "${CMAKE_SOURCE_DIR}/libs/rtmp/librtmp/*.c")

add_library(rtmp-lib
        SHARED
        src/main/cpp/rtmp-hanlde.cpp
        ${PROJECT_SOURCES}
        )

find_library( # Sets the name of the path variable.
        log-lib
        log)

target_link_libraries( # Specifies the target library.
        rtmp-lib
        ${log-lib})
