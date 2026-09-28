package com.dazaike.ciderpatcher;

interface IInstallService {
    void destroy() = 16777114;

    String install(in ParcelFileDescriptor apk, long sizeBytes) = 1;

    String uninstall(String packageName) = 2;
}
