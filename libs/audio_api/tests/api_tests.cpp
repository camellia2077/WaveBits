#include "bag_api.h"

#include "api_test_support.h"

int main() {
    test::Runner runner;
    api_tests::RegisterApiSyncTests(runner);
    api_tests::RegisterApiAsyncTests(runner);
    api_tests::RegisterApiFlashTests(runner);
    return runner.Run();
}
