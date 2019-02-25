LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := src/org/lineageos/eleven/IElevenService.aidl
LOCAL_SRC_FILES += $(call all-java-files-under, src)
LOCAL_SRC_FILES += $(call all-java-files-under, src_aosp)

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, res)

LOCAL_STATIC_ANDROID_LIBRARIES := \
    androidx.appcompat_appcompat \
    androidx.cardview_cardview \
    androidx.core_core \
    androidx.legacy_legacy-support-v4 \
    androidx.palette_palette \
    androidx.recyclerview_recyclerview \
    androidx-constraintlayout_constraintlayout

LOCAL_USE_AAPT2 := true

LOCAL_PACKAGE_NAME := Eleven
LOCAL_OVERRIDES_PACKAGES := Music

LOCAL_PRIVATE_PLATFORM_APIS := true

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
