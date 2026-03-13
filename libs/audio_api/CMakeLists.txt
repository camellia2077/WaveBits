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

if(WAVEBITS_HOST_IMPORT_STD_PILOT_ENABLED)
    target_compile_features(bag_api PRIVATE cxx_std_23)
    target_compile_definitions(bag_api PRIVATE WAVEBITS_API_IMPORT_STD=1)
    set_property(TARGET bag_api PROPERTY CXX_MODULE_STD ON)
endif()
