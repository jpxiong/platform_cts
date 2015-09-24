# Copyright 2012 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# This is the shared library included by the JNI test app.
#

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libnativeopengltests

# Don't include this package in any configuration by default.
LOCAL_MODULE_TAGS := optional

my_src_dir := ../standalone/jni

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/$(my_src_dir) \
    external/gtest/include

# The sources are in ../standalone/jni/
LOCAL_SRC_FILES := $(addprefix $(my_src_dir)/, \
        register.cpp \
        GLTestHelper.cpp \
        tests/GLTest_test.cpp \
        tests/EGLCleanup_test.cpp \
        tests/EGLCreateContext_test.cpp \
        )

LOCAL_CXX_STL := libc++
LOCAL_SHARED_LIBRARIES := libEGL \
                          libGLESv2 \
                          libandroid \
                          liblog \

LOCAL_STATIC_LIBRARIES := libgtest

my_src_dir :=

include $(BUILD_SHARED_LIBRARY)
