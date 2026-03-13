#pragma once

namespace bag {

enum class ErrorCode {
    kOk = 0,
    kInvalidArgument = 1,
    kNotReady = 2,
    kNotImplemented = 3,
    kInternal = 4,
};

}  // namespace bag
