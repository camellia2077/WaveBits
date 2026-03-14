namespace bag::flash {

using std::uint8_t;

ErrorCode EncodeTextToBytes(const std::string& text, std::vector<uint8_t>* out_bytes) {
    if (out_bytes == nullptr) {
        return ErrorCode::kInvalidArgument;
    }
    out_bytes->assign(text.begin(), text.end());
    return ErrorCode::kOk;
}

ErrorCode DecodeBytesToText(const std::vector<uint8_t>& bytes, std::string* out_text) {
    if (out_text == nullptr) {
        return ErrorCode::kInvalidArgument;
    }
    out_text->assign(bytes.begin(), bytes.end());
    return ErrorCode::kOk;
}

}  // namespace bag::flash
