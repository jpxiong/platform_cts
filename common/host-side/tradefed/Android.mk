# Copyright (C) 2014 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Builds the cts tradefed host library

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

#LOCAL_JAVA_RESOURCE_DIRS := res

LOCAL_MODULE := cts-tradefed_v2

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := ddmlib-prebuilt tradefed-prebuilt hosttestlib cts-common-util-host_v2

include $(BUILD_HOST_JAVA_LIBRARY)

###############################################################################
# Builds the cts tradefed executable

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_PREBUILT_EXECUTABLES := cts-tradefed_v2

include $(BUILD_HOST_PREBUILT)

###############################################################################
# Builds the cts tradefed tests

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, tests)

LOCAL_MODULE := cts-tradefed-tests_v2

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := ddmlib-prebuilt tradefed-prebuilt cts-tradefed_v2

LOCAL_STATIC_JAVA_LIBRARIES := easymock

include $(BUILD_HOST_JAVA_LIBRARY)
