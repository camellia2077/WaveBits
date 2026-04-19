#include "test_std_support.h"
#include "test_framework.h"

import bag.flash.phy_clean;
import bag.flash.signal;
import bag.flash.voicing;

#include "flash_voicing_test_support.h"

int main() {
    test::Runner runner;
    flash_voicing_test::RegisterFlashVoicingPayloadTests(runner);
    flash_voicing_test::RegisterFlashVoicingFormalTests(runner);
    flash_voicing_test::RegisterFlashVoicingShellTests(runner);
    return runner.Run();
}
