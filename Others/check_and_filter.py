import os
import shutil


def get_files_in_folder(folder_path):
    """获取给定文件夹内所有文件的名称"""
    return {name for name in os.listdir(folder_path) if os.path.isfile(os.path.join(folder_path, name))}


def check_overlap_and_merge_files(folders):
    """检查四个文件夹之间是否有重叠，并在有重叠时合并文件（保持重叠文件一份）"""
    merged_folder = 'YOUR DIR'
    all_files = set()
    overlapping_files = set()  # 用于存储重叠文件

    # 收集所有文件并检查重叠
    for folder in folders:
        files = get_files_in_folder(folder)

        # 检查与已存在的文件是否重叠
        current_overlap = all_files.intersection(files)
        overlapping_files.update(current_overlap)

        if current_overlap:
            print(f"{folder} Same APK exist: {current_overlap}")

        all_files.update(files)

    # 如果存在重叠的文件，才进行合并
    if overlapping_files:
        # os.makedirs(merged_folder, exist_ok=True)  # 创建合并文件夹

        files = get_files_in_folder(folders[0])

        for file in files:
            if file in overlapping_files:
                src = os.path.join(folders[0], file)
                # dst = os.path.join(merged_folder, file)
                # if os.path.exists(dst):  # 只在目标文件夹不存在时复制
                #     shutil.copy2(src, dst)  # 使用 copy2 保留文件的元数据
                os.remove(src)
                print("remove :" + src)

        # print("Output:", merged_folder)
        print("NUM:", len(overlapping_files))
    else:
        print("没有重叠文件，未进行合并。")


# 示例用法
# folders = ['APP1', 'APP2', 'APP3', 'APP4']

folders = ['', '']
check_overlap_and_merge_files(folders)
