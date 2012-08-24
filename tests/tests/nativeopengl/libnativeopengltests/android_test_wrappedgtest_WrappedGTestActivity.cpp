/*
 * Copyright 2012 The Android Open Source Project
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
#include <android/log.h>
#include <gtest/gtest.h>
#include <jni.h>

using namespace testing;

class GTestListener : public EmptyTestEventListener {
public:
    GTestListener(JNIEnv *env, jobject activity)
        : mActivity(activity), mEnv(env) {

        jclass clazz = env->FindClass(
              "android/test/wrappedgtest/WrappedGTestActivity");
        mSendStatusID = env->GetMethodID(clazz, "sendStatus",
              "(Ljava/lang/String;)V");
        mMessageBuffer = new char[2048];
    }

    ~GTestListener() {
        delete[] mMessageBuffer;
    }

private:
    jobject   mActivity;
    JNIEnv *  mEnv;
    jmethodID mSendStatusID;
    char *    mMessageBuffer;

    virtual void OnTestIterationStart(const UnitTest& unit_test,
            int iteration) {
        snprintf(mMessageBuffer, sizeof(char) * 2048,
                "[==========] Running %i tests from %i test cases.",
                unit_test.test_to_run_count(),
                unit_test.test_case_to_run_count());

        mEnv->CallVoidMethod(mActivity, mSendStatusID,
                mEnv->NewStringUTF(mMessageBuffer));
    }

    virtual void OnTestStart(const TestInfo& test_info) {
        snprintf(mMessageBuffer, sizeof(char) * 2048, "[ RUN      ] %s.%s",
                test_info.test_case_name(), test_info.name());

        mEnv->CallVoidMethod(mActivity, mSendStatusID,
                mEnv->NewStringUTF(mMessageBuffer));
    }

    virtual void OnTestPartResult(const TestPartResult& result) {
        if (result.type() == TestPartResult::kSuccess) {
            return;
        }

        snprintf(mMessageBuffer, sizeof(char) * 2048, "%s:%i: Failure\n%s",
                result.file_name(), result.line_number(), result.message());

        mEnv->CallVoidMethod(mActivity, mSendStatusID,
                mEnv->NewStringUTF(mMessageBuffer));
    }

    virtual void OnTestEnd(const TestInfo& test_info) {
        const char * result = test_info.result()->Passed() ?
                "[       OK ] " : "[  FAILED  ] ";

        snprintf(mMessageBuffer, sizeof(char) * 2048, "%s%s.%s (%lli ms)",
                result, test_info.test_case_name(), test_info.name(),
                test_info.result()->elapsed_time());

        mEnv->CallVoidMethod(mActivity, mSendStatusID,
                mEnv->NewStringUTF(mMessageBuffer));
    }

    virtual void OnTestIterationEnd(const UnitTest& unit_test, int iteration) {
        snprintf(mMessageBuffer, sizeof(char) * 2048,
                "[==========] %i tests from %i test cases ran. (%lli ms total)",
                unit_test.test_to_run_count(),
                unit_test.test_case_to_run_count(), unit_test.elapsed_time());

        mEnv->CallVoidMethod(mActivity, mSendStatusID,
                mEnv->NewStringUTF(mMessageBuffer));
    }
};

static jboolean WrappedGTestActivity_runTests(JNIEnv *env, jobject obj,
        jobject activity) {
    // init gtest with no args
    int argc = 0;
    InitGoogleTest(&argc, (char**)NULL);

    // delete the default listener
    TestEventListeners& listeners = UnitTest::GetInstance()->listeners();
    delete listeners.Release(listeners.default_result_printer());

    // add custom listener
    GTestListener * listener = new GTestListener(env, activity);
    listeners.Append(listener);

    // run tests
    int result = RUN_ALL_TESTS();

    delete listener;
    return result;
};

static JNINativeMethod methods[] = {
    // name, signature, function
    { "runTests", "(Landroid/test/wrappedgtest/WrappedGTestActivity;)I", (void*)WrappedGTestActivity_runTests },
};

int register_WrappedGTestActivity(JNIEnv *env) {
    return env->RegisterNatives(
            env->FindClass("android/test/wrappedgtest/WrappedGTestActivity"),
            methods, sizeof(methods) / sizeof(JNINativeMethod));
};
