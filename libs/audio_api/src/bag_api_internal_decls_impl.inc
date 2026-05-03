inline constexpr const char kModeFlash[] = "flash";
inline constexpr const char kModePro[] = "pro";
inline constexpr const char kModeUltra[] = "ultra";
inline constexpr const char kModeMini[] = "mini";

std::string FormatRawBytesHex(const std::vector<std::uint8_t>& bytes);
std::string FormatRawBitsBinary(const std::vector<std::uint8_t>& bytes);
bag_decode_content_status CopyStringToApiBuffer(std::string_view input,
                                                char* buffer,
                                                std::size_t buffer_size);
std::string JoinTextTokens(const std::vector<std::string>& tokens);
std::size_t FormattedRawBytesHexSize(const std::vector<std::uint8_t>& bytes);
std::size_t FormattedRawBitsBinarySize(const std::vector<std::uint8_t>& bytes);
std::size_t JoinedTextTokensSize(const std::vector<std::string>& tokens);
bag_decode_content_status CopyRawBytesHexToApi(
    const std::vector<std::uint8_t>& bytes, char* buffer,
    std::size_t buffer_size);
bag_decode_content_status CopyRawBitsBinaryToApi(
    const std::vector<std::uint8_t>& bytes, char* buffer,
    std::size_t buffer_size);
bag_decode_content_status CopyJoinedTextTokensToApi(
    const std::vector<std::string>& tokens, char* buffer,
    std::size_t buffer_size);
void FillEncodeResultLayout(const bag::EncodedPcmFollowResult& result,
                            bag_encode_result_layout* out_layout);
bag_error_code BuildEncodeFollowDataResult(const bag::CoreConfig& core_config,
                                           const std::string& text,
                                           bag::EncodedPcmFollowResult* result);
bag_error_code EnsureEncodeJobFollowResult(bag_encode_job* job);
