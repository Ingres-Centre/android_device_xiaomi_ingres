#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2025 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.fixups_blob import (
    blob_fixup,
    blob_fixups_user_type,
)
from extract_utils.fixups_lib import (
    lib_fixups,
)
from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)

namespace_imports = [
    'device/xiaomi/sm8450-common',
    'hardware/qcom-caf/sm8450-6.6',
    'hardware/xiaomi',
    'vendor/qcom/opensource/commonsys-intf/display',
    'vendor/xiaomi/sm8450-common',
]

blob_fixups: blob_fixups_user_type = {
    'vendor/lib64/hw/camera.qcom.so': blob_fixup()
        .add_needed('libcamximageformatutils_shim.so')
        .binary_regex_replace(
            b'_ZN4CamX16ImageFormatUtils9GetFormatERNS_17GrallocPropertiesERNS_6FormatE',
            b'_ZN4CamX16ImageFormatUtils9GetXiaomiERNS_17GrallocPropertiesERNS_6FormatE',
        ),
    (
        'vendor/etc/camera/ingres_enhance_motiontuning.xml',
        'vendor/etc/camera/ingres_motiontuning.xml',
    ): blob_fixup().regex_replace('xml=version', 'xml version'),
    (
        'vendor/etc/camera/pureShot_parameter.xml',
        'vendor/etc/camera/pureView_parameter.xml',
    ): blob_fixup().regex_replace(r'=([0-9]+)>', r'="\1">'),
    (
        'vendor/lib64/hw/com.qti.chi.override.so',
        'vendor/lib64/libcamxcommonutils.so',
        'vendor/lib64/libmialgoengine.so',
    ): blob_fixup().add_needed('libprocessgroup_shim.so'),
    # Compile embedded color-conversion GLSL instead of opening its cached binary.
    # Replace BL fopen with MOV X0, XZR; the following CBZ selects the source path.
    'vendor/lib64/libTrueSight.so': blob_fixup()
    .binary_regex_replace(
        b'\xb5\x9e\xfd\x97\x00\x06\x00\xb4\xbf\x9e\xfd\x97',
        b'\xe0\x03\x1f\xaa\x00\x06\x00\xb4\xbf\x9e\xfd\x97',
    )
    .clear_symbol_version('AHardwareBuffer_allocate')
    .clear_symbol_version('AHardwareBuffer_describe')
    .clear_symbol_version('AHardwareBuffer_isSupported')
    .clear_symbol_version('AHardwareBuffer_lock')
    .clear_symbol_version('AHardwareBuffer_lockPlanes')
    .clear_symbol_version('AHardwareBuffer_release')
    .clear_symbol_version('AHardwareBuffer_unlock'),
    'vendor/lib64/libmorpho_ubwc.so': blob_fixup()
    .clear_symbol_version('AHardwareBuffer_allocate')
    .clear_symbol_version('AHardwareBuffer_describe')
    .clear_symbol_version('AHardwareBuffer_isSupported')
    .clear_symbol_version('AHardwareBuffer_lock')
    .clear_symbol_version('AHardwareBuffer_lockPlanes')
    .clear_symbol_version('AHardwareBuffer_release')
    .clear_symbol_version('AHardwareBuffer_unlock'),
    'vendor/lib64/libmialgo_pureShot.so': blob_fixup()
    .clear_symbol_version('remote_handle64_close')
    .clear_symbol_version('remote_handle64_invoke')
    .clear_symbol_version('remote_handle64_open')
    .clear_symbol_version('remote_register_buf_attr'),
}

module = ExtractUtilsModule(
    'ingres',
    'xiaomi',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
    check_elf=True,
    add_firmware_proprietary_file=False,
)

if __name__ == '__main__':
    utils = ExtractUtils.device_with_common(
        module, 'sm8450-common', module.vendor
    )
    utils.run()
