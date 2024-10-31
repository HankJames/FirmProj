import os
import shutil


def rename_and_merge_folders(target_dir):
    """
    遍历目标文件夹下的所有文件夹，如果文件夹的名字不以 .apk 为结尾，
    则将其改为 .apk 结尾。如果已经存在同名的 .apk 文件夹，则合并二者的内容。

    :param target_dir: 要处理的目标文件夹路径
    """
    for root, dirs, files in os.walk(target_dir, topdown=False):
        for dir_name in dirs:
            dir_path = os.path.join(root, dir_name)

            # 如果文件夹名不以 .apk 结尾
            if os.path.isdir(dir_path) and dir_name.endswith('Output.apk'):
                new_dir_name = dir_name.replace('Output.apk', 'Output')
                new_dir_path = os.path.join(root, new_dir_name)

                # 如果目标文件夹已经存在
                if os.path.exists(new_dir_path):
                    print(f"Merging contents of {dir_path} into {new_dir_path}")

                    # 将原文件夹的内容移动到目标文件夹
                    for item in os.listdir(dir_path):
                        src_item = os.path.join(dir_path, item)
                        dest_item = os.path.join(new_dir_path, item)

                        if os.path.exists(dest_item):
                            if os.path.isdir(dest_item):
                                # 如果目标中已经存在同名文件夹，递归合并内容
                                shutil.copytree(src_item, dest_item, dirs_exist_ok=True)
                            else:
                                # 如果目标中已经存在同名文件，跳过或覆盖
                                print(f"File {dest_item} already exists, skipping...")
                        else:
                            shutil.move(src_item, dest_item)

                    # 删除原文件夹
                    shutil.rmtree(dir_path)
                else:
                    # 如果目标文件夹不存在，则直接重命名
                    print(f"Renaming {dir_path} to {new_dir_path}")
                    os.rename(dir_path, new_dir_path)


if __name__ == "__main__":
    target_dir = "/data/wenzhi/Result/IoT-VER"
    rename_and_merge_folders(target_dir)
