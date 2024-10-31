import os
from collections import defaultdict


def find_duplicate_filenames(folder_path):
    """检查文件夹中是否有重名文件（忽略大小写），并返回重名文件的名字数组"""
    # 存储小写文件名的字典
    file_map = defaultdict(list)

    # 遍历文件夹
    for filename in os.listdir(folder_path):
        file_path = os.path.join(folder_path, filename)

        if os.path.isfile(file_path):
            # 将文件名转换为小写以便比较
            lower_filename = filename.lower()
            # 记录原始文件名
            file_map[lower_filename].append(filename)

    # 找出重名文件
    duplicates = {name: files for name, files in file_map.items() if len(files) > 1}

    return duplicates


# 示例用法
folder_path = 'Merged_Files'  # 请替换为你的文件夹路径
duplicates = find_duplicate_filenames(folder_path)

# 输出重名文件的名字数组
for lower_name, files in duplicates.items():
    print(f"重名文件: {files}")
    print(f"NUM: {len(files)}")
