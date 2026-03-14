import bag.common.version;

extern "C" int wavebits_android_modules_smoke_anchor() {
    const char* version = bag::CoreVersion();
    if (version == nullptr || version[0] == '\0') {
        return -1;
    }

    int size = 0;
    for (const char* current = version; *current != '\0'; ++current) {
        ++size;
    }
    return size;
}
