# -*- coding: utf-8 -*-
import os
import time
import zipfile
import json

import requests.exceptions
from openai import OpenAI
import openai
import re

os.environ['HTTP_PROXY'] = ''
os.environ['HTTPS_PROXY'] = ''
openai.proxy = None
pattern = r'^res/.{3}\.bin$'
pattern1 = r'/\d+x\d+/'


def write_dict_to_json_file(data_dict, file_path):
    """
    将字典转换为 JSON 格式并写入到指定路径的文件中。

    :param data_dict: 要转换为 JSON 的字典
    :param file_path: 目标文件路径
    """
    # 确保目标目录存在
    os.makedirs(os.path.dirname(file_path), exist_ok=True)

    # 将字典转换为 JSON 格式并写入文件
    with open(file_path, 'wb') as json_file:
        json.dump(data_dict, json_file, ensure_ascii=False, indent=4)
        print(f"Dictionary written to {file_path}")

def check_black(file_name):
    for word in blackwords:
        if word in file_name:
            return False
    return True

def extract_bin_files(apk_path, output_dir):
    """
        遍历指定路径中的所有apk文件，搜索并提取.apk中的.bin文件到指定的输出目录中。
    """
    client = OpenAI(
        api_key="KEY",
        base_url="URL"
    )
    with open('prompt_scanbin.txt', 'r', encoding='utf-8') as file:
        prompt = file.read()

    # 确保输出目录存在
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    hard_dict = {}

    # 遍历指定目录下的所有文件
    for root, dirs, files in os.walk(apk_path):
        level = root.count(os.sep) - 1
        if level == depth:
            dirs.clear()
            continue
        for file in files:
            if file.endswith('.apk'):
                start = time.time()
                hard_dict[file] = []
                apk_file_path = os.path.join(root, file)
                print(f'处理APK文件: {apk_file_path}')

                # 创建以APK文件名命名的输出目录

                apk_output_dir = os.path.join(output_dir, file + '/localFiles')
                if os.path.exists(apk_output_dir):
                    continue
                # 打开apk文件（其实是zip格式）
                with zipfile.ZipFile(apk_file_path, 'r') as apk:
                    # 遍历apk中的所有文件
                    for apk_file in apk.namelist():
                        if apk.getinfo(apk_file).file_size > 10240:
                            if re.match(pattern, apk_file) or re.search(pattern1, apk_file):
                                continue
                            if '/' in apk_file:
                                apk_file_name = apk_file.rsplit('/', 1)[1]
                            else:
                                apk_file_name = apk_file
                            if check_black(apk_file) and FirmwareType(apk_file) and notInlist(apk_file_name):
                                # Step 1: 使用 prompt0 进行分类
                                messages_classify = [
                                    {"role": "system", "content": prompt},
                                    {"role": "user", "content": apk_file}
                                ]
                                try:
                                    completion = client.chat.completions.create(
                                        model="gpt-4o-mini-2024-07-18",
                                        messages=messages_classify,
                                        temperature=0.3,
                                        timeout=10
                                    )
                                    category_response = completion.choices[0].message.content  # 固件文件返回1，非固件文件返回0
                                    print("File: " + apk_file + " Anwser: " + category_response)
                                    if category_response == '1':
                                        if not os.path.exists(apk_output_dir):
                                            os.makedirs(apk_output_dir)
                                        target_file_path = os.path.join(apk_output_dir, apk_file_name)
                                        with open(target_file_path, 'wb') as target_file:
                                            # 从 APK 中读取文件并写入目标文件
                                            target_file.write(apk.read(apk_file))
                                        hard_dict[file].append(apk_file_name)
                                        print(f'提取文件: {apk_file} 到 {target_file_path}')
                                except requests.exceptions.Timeout:
                                    continue
                if not hard_dict[file]:
                    hard_dict.pop(file)
                end = time.time()
                outPath = os.path.join(output_dir, file)
                if not os.path.exists(outPath):
                    continue
                timeFile = os.path.join(outPath, "local_time.txt")
                with open(timeFile, "w") as f:
                    f.write(f'[Time]: {end - start}')
        return hard_dict


def FirmwareType(str):
    for end in ends:
        if str.endswith(end):
            return True
    return False


def notInlist(str):
    if ('/' in str):
        return str.rsplit('/', 1)[1] not in namelist
    else:
        return str not in namelist


# 常见的固件文件后缀
ends = ["zip", "img", "bin", "rar", "gz", "fw", "rom", "ota", "e2", "dfu", "eeprom"]
# 常见的非固件的二进制文件
namelist = ["default_config_eng.bin", "default_config.bin", "publicsuffixes.gz", "icudt46l,zip", "DebugProbesKt.bin",
            "signed.bin", "af.bin", "AssetManifest.bin", "anchors.bin", "data.bin", "md.bin", "font.bin", "gbk.bin",
            "unicode.bin", "data.bin", "res.zip", "libqs_arm64-v8a.zip", "libqs_armeabi-v7a.zip", "libqs_armeabi.zip",
            "libqs_mips.zip", "libqs_mips64.zip", "libqs_x86.zip", "libqs_x86_64.zip", "changeplay.bin"]
# 遍历的目录深度
depth = 0
# 确定的固件关键词
whitewords = ["fw", "flash", "ota", "firmware", "v1.0", "dfu"]
# 确定的非固件关键词
blackwords = ["config", "default", "datafile", "debug", "128x128\online", "model", "plugin", "cache", "ui_biz", "fonts", "images", "changeplay.bin"]

if __name__ == '__main__':
    apk_path = ''  # APK文件所在目录
    output_dir = ''  # 提取文件的输出目录
    hard_dict = extract_bin_files(apk_path, output_dir)
    json_str = json.dumps(hard_dict, indent=4)
    with open(output_dir + '/firmware_info.json', 'w') as json_file:
        json_file.write(json_str)
    print("固件信息：", hard_dict)
    # print(hard_dict)
    # write_dict_to_json_file(hard_dict, output_dir)
