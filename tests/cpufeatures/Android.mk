LOCAL_PATH := $(call my-dir)

# If targetting ARM CPUs, build the ARMv7 detection
# function. It will only be called by the main
# library if we detect ARMv7 through a system property.
#
ifneq ($(TARGET_SIMULATOR),true)
include $(CLEAR_VARS)
LOCAL_MODULE := cpufeatures
LOCAL_SRC_FILES := cpu-features.c
include $(BUILD_STATIC_LIBRARY)
endif
