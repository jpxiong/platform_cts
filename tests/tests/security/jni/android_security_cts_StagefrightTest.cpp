/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * This code was provided to AOSP by Zimperium Inc and was
 * written by:
 *
 * Simone "evilsocket" Margaritelli
 * Joshua "jduck" Drake
 */

#define LOG_TAG "StagefrightTest"

#include <android/log.h>
#include <cutils/log.h>
#include <fcntl.h>
#include <jni.h>
#include <nativehelper/JNIHelp.h>
#include <nativehelper/ScopedUtfChars.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <string.h>
#include <errno.h>


typedef void (*StagefrightExtractMetadataWrapper_t)(int fd);


static inline bool resolve_sym(const char* filename, void *lib, void **paddr,
                               const char *sym)
{
    void *ret;
    const void *err;

    (void)dlerror();
    ret = dlsym(lib, sym);
    err = dlerror();

    if (err != NULL) {
        ALOGE("dlsym: %s unable to resolve %s: %s", filename, sym, err);
        return false;
    }
    *paddr = ret;
    return true;
}


static jboolean stagefright_test(JNIEnv *env, jobject, jstring jfilename)
{
    if (jfilename == NULL) {
        jniThrowNullPointerException(env, NULL);
        return false;
    }

    ScopedUtfChars filename(env, jfilename);
    const char *cfilename = filename.c_str();

    void *sflib = dlopen("libctsstagefright.so", RTLD_NOW);
    if (!sflib) {
        ALOGE("dlopen: %s: unable to load libstagefright: %s",
              cfilename, dlerror());
        return false;
    }

    StagefrightExtractMetadataWrapper_t StagefrightExtractMetadataWrapper;
    if (!resolve_sym("libctsstagefright.so", sflib,
                     (void **)&StagefrightExtractMetadataWrapper,
                     "StagefrightExtractMetadataWrapper")) {
        dlclose(sflib);
        return false;
    }

    int fd = open(cfilename, O_RDONLY | O_LARGEFILE | O_NOFOLLOW | O_CLOEXEC);
    if (fd == -1) {
        dlclose(sflib);
        ALOGE("open: %s: %s", cfilename, strerror(errno));
        return false;
    }

    StagefrightExtractMetadataWrapper(fd);
    close(fd);
    dlclose(sflib);
    return true;
}


static JNINativeMethod gMethods[] = {
    { (char*)"stagefrightTest",
      (char*)"(Ljava/lang/String;)Z", (void *)stagefright_test }
};

int register_android_security_cts_StagefrightTest(JNIEnv* env) {
    jclass clazz = env->FindClass("android/security/cts/StagefrightTest");
    return env->RegisterNatives(
            clazz, gMethods, sizeof(gMethods) / sizeof(JNINativeMethod));
}
