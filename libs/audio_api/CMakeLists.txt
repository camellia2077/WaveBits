add_library(bag_api STATIC
    src/bag_api.cpp
)

target_compile_features(bag_api PUBLIC cxx_std_20)
target_include_directories(bag_api
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
    PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/../audio_core/include
)
target_link_libraries(bag_api
    PUBLIC
        $<LINK_ONLY:bag_core>
)

target_compile_features(bag_api PRIVATE cxx_std_23)
set_property(TARGET bag_api PROPERTY CXX_MODULE_STD ON)

if(BUILD_TESTING)
    flipbits_add_host_test(test_api
        NAME api_tests
        SOURCES
            tests/api_tests.cpp
            tests/api_test_support.cpp
            tests/api_sync_tests.cpp
            tests/api_async_tests.cpp
            tests/api_flash_tests.cpp
        LIBS bag_api
    )
endif()
