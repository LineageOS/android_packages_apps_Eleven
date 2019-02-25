LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := src/org/lineageos/eleven/IElevenService.aidl
LOCAL_SRC_FILES += $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, res)

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v8-renderscript \
    android-common \
    constraint-layout-solver

LOCAL_STATIC_JAVA_AAR_LIBRARIES := \
    constraint-layout

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

########################

### FAKE RULES FOR ANDROID STUDIO SUPPORT

include $(CLEAR_VARS)
LOCAL_MODULE := eleven-fake-icu4j
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under,../../../external/icu/android_icu4j/src/main/java)
LOCAL_JAVA_RESOURCE_DIRS := $(LOCAL_PATH)/../../../external/icu/android_icu4j/resources
LOCAL_JAVA_LANGUAGE_VERSION := 1.8
include $(BUILD_JAVA_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := GenerateStudioFiles-Eleven
LOCAL_MODULE_CLASS := FAKE
LOCAL_MODULE_SUFFIX := -timestamp

system_deps_icu4j := $(call java-lib-deps,eleven-fake-icu4j)

system_libs_eleven_path := $(abspath $(LOCAL_PATH))/system_libs

include $(BUILD_SYSTEM)/base_rules.mk

.PHONY: copy_eleven_system_deps
copy_eleven_system_deps: $(system_deps_icu4j)
	$(hide) mkdir -p $(system_libs_eleven_path)
	$(hide) rm -rf $(system_libs_eleven_path)/*.jar
	$(hide) cp $(system_deps_icu4j) $(system_libs_eleven_path)/icu4j.jar

$(LOCAL_BUILT_MODULE): copy_eleven_system_deps
	$(hide) echo "Fake: $@"
	$(hide) mkdir -p $(dir $@)
	$(hide) touch $@
