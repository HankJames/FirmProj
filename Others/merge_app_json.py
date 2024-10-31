import json

def merge_json_files(a_path, b_path, output_path):
    """合并两个 JSON 文件，保留较大的值，并输出统计信息"""
    try:
        # 读取 A.json 文件
        with open(a_path, 'r', encoding='utf-8') as file_a:
            data_a = json.load(file_a)
    except Exception as e:
        print(f"读取文件 {a_path} 时出错: {e}")
        return

    try:
        # 读取 B.json 文件
        with open(b_path, 'r', encoding='utf-8') as file_b:
            data_b = json.load(file_b)
    except Exception as e:
        print(f"读取文件 {b_path} 时出错: {e}")
        return

    # 检查数据类型
    if not isinstance(data_a, dict) or not isinstance(data_b, dict):
        print("JSON 文件的顶层结构必须是字典（对象）。")
        return

    # 合并两个字典，保留较大的值
    merged_data = data_a.copy()
    for key, value in data_b.items():
        if key in merged_data:
            # 比较并保留较大的值
            try:
                merged_value = min(merged_data[key], value)
            except TypeError:
                print(f"键 '{key}' 的值不是数字，无法比较。")
                continue
            merged_data[key] = merged_value
        else:
            merged_data[key] = value

    # 保存合并后的数据到指定路径
    try:
        with open(output_path, 'w', encoding='utf-8') as output_file:
            json.dump(merged_data, output_file, ensure_ascii=False, indent=4)
    except Exception as e:
        print(f"写入文件 {output_path} 时出错: {e}")
        return

    # 统计信息
    values = merged_data.values()
    numeric_values = []

    for v in values:
        if isinstance(v, (int, float)):
            numeric_values.append(v)
        else:
            print(f"值 '{v}' 不是数字，已跳过统计。")

    if numeric_values:
        max_value = max(numeric_values)
        min_value = min(numeric_values)
        avg_value = sum(numeric_values) / len(numeric_values)
    else:
        print("没有有效的数字值进行统计。")
        return

    # 输出统计结果
    print(f"合并后的键的数量：{len(merged_data)}")
    print(f"值的最大值：{max_value}")
    print(f"值的最小值：{min_value}")
    print(f"值的平均值：{avg_value:.2f}")

def main():
    # 替换为你的实际文件路径
    a_path = ''
    b_path = ''
    output_path = ''

    merge_json_files(a_path, b_path, output_path)

if __name__ == "__main__":
    main()
