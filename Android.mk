LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := src/org/lineageos/eleven/IElevenService.aidl
LOCAL_SRC_FILES += $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, res)

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v8-renderscript \
    android-common \
    guava \
    junit

LOCAL_STATIC_ANDROID_LIBRARIES := \
    android-support-v4 \
    android-support-v7-cardview \
    android-support-v7-palette \
    android-support-v7-recyclerview

LOCAL_USE_AAPT2 := true

LOCAL_PACKAGE_NAME := Eleven
LOCAL_OVERRIDES_PACKAGES := Music

LOCAL_PRIVILEGED_MODULE := true

LOCAL_JNI_SHARED_LIBRARIES := librsjni

LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.cfg
ifeq ($(TARGET_BUILD_VARIANT),user)
    LOCAL_PROGUARD_ENABLED := obfuscation
else
    LOCAL_PROGUARD_ENABLED := disabled
endif

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

include $(BUILD_MULTI_PREBUILT)
