cmake_minimum_required(VERSION 3.20)
project(miron_backend LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(MINGW)
    get_filename_component(MINGW_ROOT "${CMAKE_CXX_COMPILER}/../.." ABSOLUTE)
    set(PostgreSQL_ROOT "${MINGW_ROOT}")
endif()

include(FetchContent)

set(BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(BUILD_CTL OFF CACHE BOOL "" FORCE)
set(BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(JWT_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(JWT_BUILD_TESTS OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
    drogon
    GIT_REPOSITORY https://github.com/drogonframework/drogon.git
    GIT_TAG v1.9.10
    GIT_SHALLOW TRUE
    GIT_SUBMODULES_RECURSE TRUE
)
FetchContent_Declare(
    jwt-cpp
    GIT_REPOSITORY https://github.com/Thalhammer/jwt-cpp.git
    GIT_TAG v0.7.1
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(drogon jwt-cpp)

find_path(SODIUM_INCLUDE_DIR sodium.h REQUIRED)
find_library(SODIUM_LIBRARY NAMES sodium libsodium REQUIRED)

file(GLOB_RECURSE SOURCES CONFIGURE_DEPENDS src/*.cc)

add_executable(miron_backend ${SOURCES})
target_include_directories(miron_backend PRIVATE src ${SODIUM_INCLUDE_DIR})
target_link_libraries(miron_backend PRIVATE drogon jwt-cpp::jwt-cpp ${SODIUM_LIBRARY})
