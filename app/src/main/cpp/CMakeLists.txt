cmake_minimum_required(VERSION 3.22.1)
project("zerodrop_fsm")

add_library(
    score_fsm
    SHARED
    score_fsm.cpp
    score_jni.cpp
)

target_include_directories(score_fsm PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

find_library(
    log-lib
    log
)

target_link_libraries(
    score_fsm
    ${log-lib}
)
