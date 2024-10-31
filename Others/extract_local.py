import json
import os
import shutil

base_dir = '/data/wenzhi/Androzoo/Result'
output_dir = '/data/wenzhi/Androzoo/Result/Result_Extract'

if not os.path.exists(output_dir):
    os.makedirs(output_dir)

selected_dirs = []
num = 0
nums = 0
for dir_name in os.listdir(base_dir):
    dir_path = os.path.join(base_dir, dir_name)
    if os.path.isdir(dir_path):
        output_subdir = os.path.join(dir_path, 'Output')
        output_filedir = os.path.join(dir_path, 'localFiles')
        if os.path.exists(output_filedir) and len(os.listdir(output_filedir)) == 0:
            os.removedirs(output_filedir)
        elif os.path.exists(output_filedir) and len(os.listdir(output_filedir)) > 0:
            selected_dirs.append(dir_name)
            num += 1
            nums += len(os.listdir(output_filedir))
        elif os.path.exists(output_subdir):
            for file_name in os.listdir(output_subdir):
                if file_name.endswith('.txt'):
                    file_path = os.path.join(output_subdir, file_name)
                    if os.path.getsize(file_path) > 0:
                        #selected_dirs.append(dir_name)
                        break

# for dir_name in selected_dirs:
#     src_dir = os.path.join(base_dir, dir_name)
#     dst_dir = os.path.join(output_dir, dir_name)
#     shutil.copytree(src_dir, dst_dir)
#     print("copy: " + dst_dir)
#
result = {}
for dir_name in selected_dirs:
    dir_path = os.path.join(base_dir, dir_name)
    dir_path = os.path.join(dir_path, "localFiles")
    lst = os.listdir(dir_path)
    result.update({dir_name: lst})

# print("all: ", selected_dirs)
# result_path = os.path.join(output_dir, "result.json")
# with open(result_path, 'w') as file:
#     json.dump(result, file, indent=4)
#     print("Output to :", result_path)

print(num, nums)
