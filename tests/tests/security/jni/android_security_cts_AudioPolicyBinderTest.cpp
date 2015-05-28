/*
 * Copyright (C) 2014 The Android Open Source Project
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
 */

#define LOG_TAG "AudioPolicyBinderTest-JNI"

#include <jni.h>
#include <binder/IServiceManager.h>
#include <media/IAudioPolicyService.h>
#include <media/AudioSystem.h>
#include <system/audio.h>
#include <utils/Log.h>
#include <utils/SystemClock.h>

using namespace android;

/*
 * Native methods used by
 * cts/tests/tests/security/src/android/security/cts/AudioPolicyBinderTest.java
 */

static bool init(sp<IAudioPolicyService>& aps, audio_io_handle_t *output, int *session)
{
    aps = 0;
    if (output != NULL) {
        *output = 0;
    }
    if (session != NULL) {
        *session = 0;
    }

    int64_t startTime = 0;
    sp<IServiceManager> sm = defaultServiceManager();
    while (aps == 0) {
        sp<IBinder> binder = defaultServiceManager()->checkService(String16("media.audio_policy"));
        if (binder == 0) {
            if (startTime == 0) {
                startTime = uptimeMillis();
            } else if ((uptimeMillis()-startTime) > 10000) {
                ALOGE("timeout while getting audio policy service");
                return false;
            }
            sleep(1);
        } else {
            aps = interface_cast<IAudioPolicyService>(binder);
        }
    }

    if (output != NULL) {
        // get a valid output. Any use case will do.
        for (int stream = 0; stream < AUDIO_STREAM_CNT; stream++) {
            *output = AudioSystem::getOutput((audio_stream_type_t)stream);
            if (*output != 0) {
                break;
            }
        }
        if (*output == 0) {
            ALOGE("cannot get valid audio output");
            return false;
        }
    }
    if (session != NULL) {
        *session = 10000;
    }
    return true;
}

/*
 * Checks that IAudioPolicyService::startOutput() cannot be called with an
 * invalid stream type.
 */
jboolean android_security_cts_AudioPolicy_test_startOutput(JNIEnv* env __unused,
                                                           jobject thiz __unused)
{
    sp<IAudioPolicyService> aps;
    audio_io_handle_t output;
    int session;

    if (!init(aps, &output, &session)) {
        return false;
    }

    status_t status = aps->startOutput(output, (audio_stream_type_t)(-1), session);
    if (status == NO_ERROR) {
        return false;
    }
    status = aps->startOutput(output, (audio_stream_type_t)AUDIO_STREAM_CNT, session);
    if (status == NO_ERROR) {
        return false;
    }
    return true;
}

/*
 * Checks that IAudioPolicyService::stopOutput() cannot be called with an
 * invalid stream type.
 */
jboolean android_security_cts_AudioPolicy_test_stopOutput(JNIEnv* env __unused,
                                                           jobject thiz __unused)
{
    sp<IAudioPolicyService> aps;
    audio_io_handle_t output;
    int session;

    if (!init(aps, &output, &session)) {
        return false;
    }

    status_t status = aps->stopOutput(output, (audio_stream_type_t)(-1), session);
    if (status == NO_ERROR) {
        return false;
    }
    status = aps->stopOutput(output, (audio_stream_type_t)AUDIO_STREAM_CNT, session);
    if (status == NO_ERROR) {
        return false;
    }
    return true;
}

/*
 * Checks that IAudioPolicyService::isStreamActive() cannot be called with an
 * invalid stream type.
 */
jboolean android_security_cts_AudioPolicy_test_isStreamActive(JNIEnv* env __unused,
                                                           jobject thiz __unused)
{
    sp<IAudioPolicyService> aps;

    if (!init(aps, NULL, NULL)) {
        return false;
    }

    status_t status = aps->isStreamActive((audio_stream_type_t)(-1), 0);
    if (status == NO_ERROR) {
        return false;
    }
    status = aps->isStreamActive((audio_stream_type_t)AUDIO_STREAM_CNT, 0);
    if (status == NO_ERROR) {
        return false;
    }
    return true;
}

static JNINativeMethod gMethods[] = {
    {  "native_test_startOutput", "()Z",
            (void *) android_security_cts_AudioPolicy_test_startOutput },
    {  "native_test_stopOutput", "()Z",
                (void *) android_security_cts_AudioPolicy_test_stopOutput },
    {  "native_test_isStreamActive", "()Z",
                (void *) android_security_cts_AudioPolicy_test_isStreamActive },
};

int register_android_security_cts_AudioPolicyBinderTest(JNIEnv* env)
{
    jclass clazz = env->FindClass("android/security/cts/AudioPolicyBinderTest");
    return env->RegisterNatives(clazz, gMethods,
            sizeof(gMethods) / sizeof(JNINativeMethod));
}
