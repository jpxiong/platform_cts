# Copyright (C) 2011 The Android Open Source Project
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

LOCAL_PATH := $(call my-dir)

#####################
# Build image analysis library

include $(CLEAR_VARS)
include external/stlport/libstlport.mk

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libcolorchecker

LOCAL_SRC_FILES += lib/colorchecker.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
LOCAL_SHARED_LIBRARIES := libandroid \
                          libstlport \
                          libcutils \
                          libutils

include $(BUILD_SHARED_LIBRARY)

#####################
# Build JNI library

include $(CLEAR_VARS)

LOCAL_MODULE := libcameraanalyzer_jni

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := jni/com_android_cts_cameraanalyzer_colorchecker.cpp

LOCAL_C_INCLUDES += $(LOCAL_PATH)/include $(JNI_H_INCLUDE)

LOCAL_SHARED_LIBRARIES := libandroid \
                          libcolorchecker \
                          libjnigraphics \
                          libcutils \
                          libutils

include $(BUILD_SHARED_LIBRARY)

#####################
# Build test app

include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := CameraAnalyzer
LOCAL_MODULE_TAGS := optional

LOCAL_SHARED_LIBRARIES := libcameraanalyzer_jni \
                          libcolorchecker

LOCAL_SRC_FILES := $(call all-subdir-java-files)

include $(BUILD_PACKAGE)
