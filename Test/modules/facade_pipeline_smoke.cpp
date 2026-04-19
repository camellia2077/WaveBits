#include "test_std_support.h"
#include "test_framework.h"

import bag.common.config;
import bag.common.error_code;
import bag.pipeline;
import bag.transport.facade;

#include "facade_pipeline_smoke_support.h"

int main() {
    test::Runner runner;
    modules_facade_pipeline_smoke::RegisterTransportFacadeSmokeTests(runner);
    modules_facade_pipeline_smoke::RegisterPipelineSmokeTests(runner);
    return runner.Run();
}
