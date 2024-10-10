#
# Copyright (C) 2022-2024 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from ingres device
$(call inherit-product, device/xiaomi/ingres/device.mk)

# Inherit from common lineage configuration
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

PRODUCT_NAME := lineage_ingres
PRODUCT_DEVICE := ingres
PRODUCT_BRAND := Xiaomi
PRODUCT_MODEL := Redmi K50G
PRODUCT_MANUFACTURER := xiaomi

PRODUCT_SYSTEM_NAME := ingres_global
PRODUCT_SYSTEM_DEVICE := ingres

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi
