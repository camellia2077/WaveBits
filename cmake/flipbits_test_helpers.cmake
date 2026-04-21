function(flipbits_add_host_test target_name)
    set(options MODULE_TEST)
    set(one_value_args NAME)
    set(multi_value_args SOURCES LIBS COMMAND_ARGS DEPENDS)
    cmake_parse_arguments(FLIPBITS_TEST "${options}" "${one_value_args}" "${multi_value_args}" ${ARGN})

    if(NOT FLIPBITS_TEST_NAME)
        message(FATAL_ERROR "flipbits_add_host_test requires NAME.")
    endif()
    if(NOT FLIPBITS_TEST_SOURCES)
        message(FATAL_ERROR "flipbits_add_host_test requires SOURCES.")
    endif()

    add_executable(${target_name}
        ${FLIPBITS_TEST_SOURCES}
    )
    target_include_directories(${target_name}
        PRIVATE
            ${PROJECT_SOURCE_DIR}/Test/include
            ${FLIPBITS_CORE_GENERATED_INCLUDE_DIR}
    )
    if(FLIPBITS_TEST_LIBS)
        target_link_libraries(${target_name}
            PRIVATE
                ${FLIPBITS_TEST_LIBS}
        )
    endif()
    if(FLIPBITS_TEST_DEPENDS)
        add_dependencies(${target_name}
            ${FLIPBITS_TEST_DEPENDS}
        )
    endif()
    if(FLIPBITS_TEST_MODULE_TEST)
        flipbits_configure_module_test(${target_name})
    endif()
    add_test(
        NAME ${FLIPBITS_TEST_NAME}
        COMMAND ${target_name} ${FLIPBITS_TEST_COMMAND_ARGS}
    )
endfunction()

function(flipbits_configure_module_test target_name)
    target_compile_features(${target_name} PRIVATE cxx_std_23)
    target_compile_definitions(${target_name} PRIVATE FLIPBITS_TEST_IMPORT_STD=1)
    set_property(TARGET ${target_name} PROPERTY CXX_MODULE_STD ON)
endfunction()
