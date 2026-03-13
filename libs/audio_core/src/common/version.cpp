#if !defined(WAVEBITS_MODULE_IMPL_WRAPPER)
#if __cplusplus >= 202002L
module;

module bag.common.version;
#endif
#endif

namespace bag {

const char* CoreVersion() {
    return "0.3.1";
}

}  // namespace bag
