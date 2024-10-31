import json
import os
import shutil

import plotchart


def extract_files(root_dir):
    # 创建目标文件夹
    result_dir = os.path.join(root_dir, 'download_result')
    local_dir = os.path.join(root_dir, 'extract_result')
    os.makedirs(result_dir, exist_ok=True)
    os.makedirs(local_dir, exist_ok=True)
    os.makedirs(os.path.join(root_dir, 'Result_Extract'), exist_ok=True)
    local_app_Num = 0
    local_app_size = []
    no_answer_Num = 0
    no_answer_size = []
    answer_Num = 0
    answer_size = []
    download_app_Num = 0
    download_app_size = []
    download_file_Num = 0
    download_firmware_Num = 0
    result = {}
    local_apps = {}
    download_apps = {}
    answer_apps = {}
    no_answer_apps = {}
    # 遍历根目录下的一级文件夹
    for folder_name in os.listdir(root_dir):
        folder_path = os.path.join(root_dir, folder_name)
        apk_path = os.path.join(apk_directory, folder_name)
        # 检查是否为文件夹
        if os.path.isdir(folder_path):
            download_folder = os.path.join(folder_path, '3_LLM_answer/download')
            localFile_folder = os.path.join(folder_path, 'localFiles')
            answer_folder = os.path.join(folder_path, '2_LLM_answer')
            # 检查download文件夹是否存在
            if os.path.exists(download_folder):
                # 遍历download文件夹中的文件
                extract_target = os.path.join(folder_path, 'downloadFiles')
                if len(os.listdir(download_folder)) > 0:
                    f_size = os.path.getsize(apk_path)
                    f_size = round(f_size / float(1024 * 1024), 2)
                    download_apps.update({folder_name: f_size})
                    download_app_size.append(f_size)
                    download_app_Num += 1
                    lst = []
                    for file_name in os.listdir(download_folder):
                        download_file_Num += 1
                        if not (file_name.endswith(".apk") or file_name.endswith(".mp3")
                                or file_name.endswith(".pdf") or file_name.endswith(".m3u")):
                            download_firmware_Num += 1
                            lst.append(file_name)
                            file_path = os.path.join(download_folder, file_name)

                            # 检查是否为文件
                            if os.path.isfile(file_path):
                                # 移动文件到目标文件夹
                                # shutil.rmtree(extract_target)
                                os.makedirs(extract_target, exist_ok=True)
                                shutil.copy(file_path, os.path.join(extract_target, file_name))
                                print(f"Copy: {file_path} to {extract_target}")
                    if len(lst) > 0:
                        result.update({folder_name: lst})

                    if os.path.exists(localFile_folder):
                        for file_name in os.listdir(localFile_folder):
                            file_path = os.path.join(localFile_folder, file_name)

                            # 检查是否为文件
                            if os.path.isfile(file_path):
                                # 移动文件到目标文件夹
                                shutil.copy(file_path, os.path.join(local_dir, file_name))
                                print(f"Copy: {file_path} to {local_dir}")

            if os.path.exists(apk_path):
                f_size = os.path.getsize(apk_path)
                f_size = round(f_size / float(1024 * 1024), 2)
                if not os.path.exists(answer_folder):
                    no_answer_size.append(f_size)
                    no_answer_Num += 1
                    no_answer_apps.update({folder_name: f_size})
                else:
                    answer_size.append(f_size)
                    answer_Num += 1
                    answer_apps.update({folder_name: f_size})

                if os.path.exists(localFile_folder):
                    if len(os.listdir(localFile_folder)) > 0:
                        local_app_Num += 1
                        local_app_size.append(f_size)
                        local_apps.update({folder_name: f_size})

    json_path = os.path.join(root_directory, "Result_Extract/download.json")
    with open(json_path, 'w') as file:
        json.dump(result, file, indent=4)
        print("Output download json to :", json_path)

    json_path = os.path.join(root_directory, "Result_Extract/answer_apps.json")
    with open(json_path, 'w') as file:
        json.dump(answer_apps, file, indent=4)
        print("Answer app json to :", json_path)

    json_path = os.path.join(root_directory, "Result_Extract/no_answer_apps.json")
    with open(json_path, 'w') as file:
        json.dump(no_answer_apps, file, indent=4)
        print("No Answer app json to :", json_path)

    json_path = os.path.join(root_directory, "Result_Extract/local_apps.json")
    with open(json_path, 'w') as file:
        json.dump(local_apps, file, indent=4)
        print("local app json to :", json_path)

    json_path = os.path.join(root_directory, "Result_Extract/download_apps.json")
    with open(json_path, 'w') as file:
        json.dump(download_apps, file, indent=4)
        print("download app json to :", json_path)

    list.sort(no_answer_size)
    list.sort(answer_size)
    list.sort(local_app_size)
    list.sort(download_app_size)

    print_distri("No Answer num: ", no_answer_Num, no_answer_size)
    print_distri("Answer num: ", answer_Num, answer_size)
    print_distri("local App num: ", local_app_Num, local_app_size)
    print_distri("Download App num: ", download_app_Num, download_app_size)
    # print("Answer num: ", answer_Num, " avg: ", round(sum(answer_size) / answer_Num, 2))
    # print("local App num: ", local_app_Num, " avg: ", round(sum(local_app_size) / local_app_Num, 2))
    # print("Download App num: ", download_app_Num, " avg: ", round(sum(download_app_size) / download_app_Num, 2))
    print("Download file num: ", download_file_Num)
    print("Download App Num Filter: ", len(result.keys()))
    print("Download firmware num: ", download_firmware_Num)
    plotchart.doPlot(no_answer_size, answer_size)


def print_distri(outStr, num, Size):
    print(outStr, num,
          " avg: ", round(sum(Size) / num, 2),
          " 10%: ", Size[len(Size) * 1 // 10],
          " 20%: ", Size[len(Size) * 2 // 10],
          " 30%: ", Size[len(Size) * 3 // 10],
          " 40%: ", Size[len(Size) * 4 // 10],
          " 50%: ", Size[len(Size) * 5 // 10],
          " 60%: ", Size[len(Size) * 6 // 10],
          " 70%: ", Size[len(Size) * 7 // 10],
          " 80%: ", Size[len(Size) * 8 // 10],
          " 90%: ", Size[len(Size) * 9 // 10],
          " 95%: ", Size[len(Size) * 95 // 100])

if __name__ == "__main__":
    apk_directory = ""
    all_apk = os.listdir(apk_directory)
    local_apk = os.listdir("")
    all_apk.extend(local_apk)
    all_apk = set(all_apk)
    all_apk_num = len(all_apk)
    root_directory = ''
    extract_files(root_directory)
