import os

def check_txt_files_in_subfolders(folder_path):
    """
    列出文件夹路径下所有一级子文件夹，检查每个子文件夹的 output 文件夹中的 txt 文件大小是否为 0。
    如果是，则记录该子文件夹名称。
    """
    recorded_folders = []

    fs = []
    fs1 = []
    # 获取所有一级子文件夹
    subfolders = [f.name for f in os.scandir(folder_path) if f.is_dir() and f.name.endswith(".apk")]


    for subfolder in subfolders:
        subfolder_path = os.path.join(folder_path, subfolder)
        output_folder = os.path.join(subfolder_path, 'Output')
        
        # 检查 output 文件夹是否存在
        if os.path.isdir(output_folder):
            # 获取 output 文件夹下的所有 txt 文件
            txt_files = [f for f in os.listdir(output_folder) if f.endswith('.txt')]

            if len(txt_files) == 1:
                txt_file_path = os.path.join(output_folder, txt_files[0])
                
                # 检查 txt 文件大小是否为 0
                if os.path.getsize(txt_file_path) == 0:
                    recorded_folders.append(subfolder)
                elif not os.path.exists(os.path.join(subfolder_path, "2_LLM_answer")):
                    fs.append(subfolder)
            else:
                if not os.path.exists(os.path.join(subfolder_path, "2_LLM_answer")):
                    fs1.append(subfolder)
                print(f"警告：文件夹 {output_folder} 中的 txt 文件数量不是 1。")
        else:
            print(f"警告：子文件夹 {subfolder_path} 中不存在 output 文件夹。")
    
    # 输出结果
    print("以下子文件夹中的 txt 文件大小为 0：")
    for folder in recorded_folders:
        print(f"- {folder}")

    print(f"sub: {fs}\n {len(fs)}")
    print(f"sub1: {fs1}\n {len(fs1)}")
    print(f"\nNO_Answer:：{len(recorded_folders)}")
    print(f"\nAll: {len(subfolders)}")

def main():
    # 替换为你的实际文件夹路径 A
    folder_path = '/data/wenzhi/Result/IoT-VER'
    check_txt_files_in_subfolders(folder_path)

if __name__ == "__main__":
    main()
