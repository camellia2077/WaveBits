#pragma once

#if !defined(FLIPBITS_TEST_IMPORT_STD)
#include <functional>
#include <iostream>
#include <stdexcept>
#include <string>
#include <vector>
#endif

namespace test {

class Runner {
public:
    using TestFn = std::function<void()>;

    void Add(const std::string& name, TestFn fn) {
        tests_.push_back({name, std::move(fn)});
    }

    int Run() const {
        int failed = 0;
        for (const auto& test : tests_) {
            try {
                test.fn();
                std::cout << "[PASS] " << test.name << "\n";
            } catch (const std::exception& ex) {
                ++failed;
                std::cerr << "[FAIL] " << test.name << " :: " << ex.what() << "\n";
            } catch (...) {
                ++failed;
                std::cerr << "[FAIL] " << test.name << " :: unknown error\n";
            }
        }
        std::cout << "Total: " << tests_.size() << ", Failed: " << failed << "\n";
        return failed == 0 ? 0 : 1;
    }

private:
    struct TestCase {
        std::string name;
        TestFn fn;
    };

    std::vector<TestCase> tests_;
};

inline void Fail(const std::string& message) {
    throw std::runtime_error(message);
}

template <typename T, typename U>
inline void AssertEq(const T& lhs, const U& rhs, const std::string& message) {
    if (!(lhs == rhs)) {
        Fail(message);
    }
}

inline void AssertTrue(bool value, const std::string& message) {
    if (!value) {
        Fail(message);
    }
}

inline void AssertContains(const std::string& haystack,
                           const std::string& needle,
                           const std::string& message) {
    if (haystack.find(needle) == std::string::npos) {
        Fail(message);
    }
}

template <typename Fn>
inline void AssertThrows(Fn&& fn, const std::string& message) {
    try {
        fn();
    } catch (...) {
        return;
    }
    Fail(message);
}

}  // namespace test
