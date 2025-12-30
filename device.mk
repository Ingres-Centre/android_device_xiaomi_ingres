#
# Copyright (C) 2025 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from xiaomi sm8450-common
$(call inherit-product, device/xiaomi/sm8450-common/common.mk)

# Inherit from the proprietary version
$(call inherit-product, vendor/xiaomi/ingres/ingres-vendor.mk)

# Inherit from the proprietary version
$(call inherit-product, device/xiaomi/miuicamera-ingres/device.mk)

# Audio configs
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/audio/mixer_paths_waipio_mtp.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio/sku_taro/mixer_paths_waipio_mtp.xml \
    $(LOCAL_PATH)/audio/resourcemanager_waipio_mtp.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio/sku_taro/resourcemanager_waipio_mtp.xml \
    $(LOCAL_PATH)/audio/usecaseKvManager.xml:$(TARGET_COPY_OUT_VENDOR)/etc/usecaseKvManager.xml

# Overlay
PRODUCT_PACKAGES += \
    SettingsProviderResIngres \
    WifiResIngres \
    FrameworksResIngres \
    SystemUIResIngres \
    ApertureResIngres \
    NfcResIngres

# Powershare
PRODUCT_PACKAGES += \
    vendor.lineage.powershare-service.default

# GameKeys
PRODUCT_PACKAGES += \
    vendor.lineage.gamekeys-service.default \
    vendor.lineage.touchinjector-service.default \
    GameKeys

# Soong namespaces
PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)
