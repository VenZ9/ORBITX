cmake_minimum_required(VERSION 3.22.1)
project(orbitx_bridge C)

# OrbitX native bridge: GLFW/EGL context plumbing for the bundled LWJGL build,
# following the Boardwalk/PojavLauncher approach (EGL surface owned by the app,
# handed to LWJGL through the GL bridge entry point).

add_library(orbitx_bridge SHARED orbitx_egl_bridge.c)

target_link_libraries(orbitx_bridge
        android
        EGL
        GLESv2
        log)

target_compile_options(orbitx_bridge PRIVATE -Wall -Wextra -fvisibility=default)
