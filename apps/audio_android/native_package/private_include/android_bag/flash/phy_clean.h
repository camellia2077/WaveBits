#pragma once

#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "android_bag/common/error_code.h"
#include "android_bag/flash/signal.h"
#include "android_bag/flash/voicing.h"
#include "android_bag/transport/decoder.h"

namespace bag::flash {

ErrorCode EncodeTextToPcm16WithSignalProfileAndFlavor(const CoreConfig& config,
                                     const std::string& text,
                                     FlashSignalProfile signal_profile,
                                     FlashVoicingFlavor flavor,
                                     std::vector<std::int16_t>* out_pcm);
ErrorCode EncodeTextToPcm16WithSignalProfileAndFlavor(const CoreConfig& config,
                                     const std::string& text,
                                     FlashSignalProfile signal_profile,
                                     FlashVoicingFlavor flavor,
                                     std::vector<std::int16_t>* out_pcm,
                                     const EncodeProgressSink* progress_sink);
ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<std::int16_t>* out_pcm);
ErrorCode EncodeTextToPcm16(const CoreConfig& config,
                            const std::string& text,
                            std::vector<std::int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink);
ErrorCode DecodePcm16ToTextWithSignalProfileAndFlavor(const CoreConfig& config,
                                     const std::vector<std::int16_t>& pcm,
                                     FlashSignalProfile signal_profile,
                                     FlashVoicingFlavor flavor,
                                     std::string* out_text);
ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<std::int16_t>& pcm,
                            std::string* out_text);
std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config);

}  // namespace bag::flash
