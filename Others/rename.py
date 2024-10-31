import os
import logging
import contextlib
from androguard.core.apk import APK
from androguard.misc import AnalyzeAPK
import argparse
import time

# 定义要扫描的目录
output = ""
logging.getLogger("androguard").setLevel(logging.ERROR)
logging.getLogger("androguard.core").setLevel(logging.ERROR)
logging.getLogger("androguard.core.axml").setLevel(logging.ERROR)


# 遍历目录下所有文件

def is_iot_app(apk, classes):
    # 检查常见的物联网相关权限
    iot_permissions = [
        'android.permission.INTERNET',
        'android.permission.ACCESS_NETWORK_STATE',
        'android.permission.ACCESS_WIFI_STATE',
        'android.permission.CHANGE_WIFI_STATE',
        'android.permission.ACCESS_FINE_LOCATION',
        'android.permission.ACCESS_COARSE_LOCATION'
    ]

    permissions = apk.get_permissions()
    iot_permission_count = sum(1 for perm in iot_permissions if perm in permissions)

    # 检查活动和服务名称中是否包含物联网相关关键词
    iot_keywords = ['checkota', 'otaupdate', 'firmwareupdate','startdfu','deviceinfo','firmware','device']
    activities = apk.get_activities()
    services = apk.get_services()

    keyword_count = sum(1 for item in activities + services + list(classes)
                        for keyword in iot_keywords
                        if keyword in item.lower())

    # 如果满足以下条件之一，认为可能是物联网应用：
    # 1. 包含至少3个物联网相关权限
    # 2. 活动或服务名称中包含至少2个物联网相关关键词
    return iot_permission_count >= 3 and keyword_count >= 2


def rename(filename):
    if filename.endswith(".apk"):
        apk_path = filename
        all_classes = set()
        try:
            # 使用Androguard获取APK信息
            apk_info, d, dx = AnalyzeAPK(filename)
            for dex in d:
                classes = dex.get_classes_names()
                all_classes.update(classes)
            if is_iot_app(apk_info, all_classes):
                package_name = apk_info.get_package()

                # 新的文件名（包名.apk）
                new_filename = package_name + ".apk"
                new_file_path = os.path.join(output, new_filename)

                # 检查新文件名是否已经存在
                if not os.path.exists(new_file_path):
                    # 重命名文件
                    os.rename(apk_path, new_file_path)
                    print(f"文件 {filename} 已重命名为 {new_filename}")
                else:
                    print(f"文件名 {new_filename} 已存在，跳过重命名。")
            else:
                print("NOT IOT APP: " + apk_path)
                time.sleep(2)
        except Exception as e:
            print(f"无法处理文件 {filename}: {e}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=str, required=True, help="APK NAME")
    args = parser.parse_args()
    rename(args.apk)
