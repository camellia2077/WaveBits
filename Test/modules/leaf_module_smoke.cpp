#include "test_std_support.h"
#include "test_framework.h"

import audio_io.wav;
import bag.common.error_code;
import bag.common.version;
import bag.flash.codec;
import bag.flash.signal;
import bag.flash.voicing;
import bag.flash.phy_clean;
import bag.pro.codec;
import bag.transport.compat.frame_codec;
import bag.ultra.codec;

#include "leaf_module_smoke_support.h"

int main() {
    test::Runner runner;
    modules_leaf_smoke::RegisterLeafAudioIoTests(runner);
    modules_leaf_smoke::RegisterLeafFlashTests(runner);
    modules_leaf_smoke::RegisterLeafTransportTests(runner);
    return runner.Run();
}
