LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

#LOCAL_MODULE_TAGS := cts

LOCAL_SRC_FILES := \
	SpecProgressDoclet.java

LOCAL_CLASSPATH := \
	$(HOST_JDK_TOOLS_JAR)

LOCAL_MODULE:= spec-progress

include $(BUILD_HOST_JAVA_LIBRARY)
