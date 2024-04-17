# TODO change to Android.bp.
# currently only support build check on aml device.
ifeq ($(AMLOGIC_PRODUCT),true)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MULTILIB := both
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_SRC_FILES += src/com/droidlogic/tvinput/services/ITvScanService.aidl
LOCAL_SRC_FILES += src/com/droidlogic/tvinput/services/IUpdateUiCallbackListener.aidl
LOCAL_SRC_FILES += src/com/android/tv/droidlogic/tvtest/ITvTestService.aidl
LOCAL_SRC_FILES += src/com/android/tv/droidlogic/tvtest/ITvTestCallbackListener.aidl

LOCAL_CERTIFICATE := platform
LOCAL_PACKAGE_NAME := DroidLogicTvInput

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_REQUIRED_MODULES := droidlogic.software.core droidlogic.tv.software.core
LOCAL_JAVA_LIBRARIES := droidlogic.software.core droidlogic.tv.software.core
LOCAL_USES_LIBRARIES := droidlogic.software.core droidlogic.tv.software.core

LOCAL_STATIC_JAVA_LIBRARIES := \
    vendor.amlogic.hardware.tvserver-V1.0-java \
    androidx-constraintlayout_constraintlayout

LOCAL_JNI_SHARED_LIBRARIES := \
    libjnidtvepgscanner \
    libjnifont

LOCAL_REQUIRED_MODULES := libsubtitlemanager_jni
LOCAL_VENDOR_MODULE := true

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))

endif
