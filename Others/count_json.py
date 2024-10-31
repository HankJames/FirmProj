import json
import os
import statistics

complete_0 = set()
no_complete_0 = set()

def analyze_json_complete_0(folder_path):
    """
    读取 JSON 文件，统计每个 key 对应的 value 的数量，并计算统计信息
    :param file_path: JSON 文件路径
    """
    for folder in os.listdir(folder_path):
        if folder.endswith(".apk"):
            apk_path = os.path.join(folder_path, folder)
            if "3_LLM_answer" in os.listdir(apk_path):
                llm_path = os.path.join(apk_path, "3_LLM_answer")
                for file in os.listdir(llm_path):
                    if file.startswith("incomplete_3") or file.startswith("incomplete_2"):
                        c0_file = os.path.join(llm_path, file)
                        if os.path.getsize(c0_file) > 10:
                            complete_0.add(folder)
                            if folder in no_complete_0:
                                no_complete_0.remove(folder)
                        elif folder not in complete_0:
                            no_complete_0.add(folder)


def analyze_json_values(file_path):
    """
    读取 JSON 文件，统计每个 key 对应的 value 的数量，并计算统计信息
    :param file_path: JSON 文件路径
    """
    try:
        # 读取 JSON 文件
        with open(file_path, 'r', encoding='utf-8') as file:
            data = json.load(file)
    except Exception as e:
        print(f"读取文件 {file_path} 时出错: {e}")
        return

    # 检查数据类型
    if not isinstance(data, dict):
        print("JSON 文件的顶层结构必须是字典。")
        return

    # 统计每个 key 的 value 数量
    value_counts = []
    max1_file = []
    max_file = ""
    for key, value in data.items():
        if isinstance(value, (list, set, tuple)):  # 只对列表、集合或元组进行计数
            value_counts.append(len(value))
            if len(value) >= max(value_counts):
                max_file = key
            if len(value) > 50:
                max1_file.append(key)
        else:
            print(f"警告：键 '{key}' 的值不是可迭代对象，已跳过。")

    # 如果没有有效的值数量，返回
    if not value_counts:
        print("没有有效的值进行统计。")
        return

    # 计算统计信息
    min_value = min(value_counts)
    max_value = max(value_counts)
    avg_value = sum(value_counts) / len(value_counts)
    median_value = statistics.median(value_counts)

    list.sort(value_counts)
    print(value_counts)
    # 输出统计结果
    print(f"最小值：{min_value}")
    print(f"最大值：{max_value}, file: {max_file}, {max1_file}")
    print(f"平均值：{avg_value:.2f}")
    print(f"中位数：{median_value}")
    print(len(value_counts), sum(value_counts))

def main():
    # 替换为你的实际文件路径
    file_path = ''
    folder_path = ''
    folder_path1 = ''
    folder_path2 = ''
    #analyze_json_values(file_path)

    analyze_json_complete_0(folder_path)
    analyze_json_complete_0(folder_path1)
    analyze_json_complete_0(folder_path2)
    print(len(complete_0), len(no_complete_0))

if __name__ == "__main__":
    main()
