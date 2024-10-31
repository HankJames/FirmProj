import json

def count_JSON(a_path):
    filenum = 0
    files = []
    try:
        with open(a_path, 'r', encoding='utf-8') as f:
            data_a = json.load(f)
    except Exception as e:
        print(f"读取文件A时出错: {e}")
        return
    if not isinstance(data_a, dict):
        return
    num = len(data_a.keys())
    for key in data_a:
        lst = data_a.get(key)
        lst = [x for x in lst if not ".xml" in x and not ".json" in x]
        filenum += len(lst)
        files.extend(lst)
    return num, filenum, files


def merge_json_files(a_path, b_path, c_path):
    """将B合并到A中，取值的并集，并输出指定的信息"""
    try:
        # 读取A.json文件
        with open(a_path, 'r', encoding='utf-8') as file_a:
            data_a = json.load(file_a)
    except Exception as e:
        print(f"读取文件A时出错: {e}")
        return

    try:
        # 读取B.json文件
        with open(b_path, 'r', encoding='utf-8') as file_b:
            data_b = json.load(file_b)
    except Exception as e:
        print(f"读取文件B时出错: {e}")
        return

    # 检查键和值的类型
    if not isinstance(data_a, dict) or not isinstance(data_b, dict):
        print("JSON文件的顶层结构必须是字典（对象）。")
        return

    # B中有但A中没有的键
    keys_in_b_not_in_a = [key for key in data_b.keys() if key not in data_a]

    # A和B都有但值不同的键
    keys_with_different_values = []
    for key in data_b.keys():
        if key in data_a:
            # 对于相同的key，取两个列表的并集

            if data_a[key] != data_b[key]:
                keys_with_different_values.append(key)
                # 合并两个列表并去重
                merged_list = list(set(data_a[key]).union(set(data_b[key])))
                data_a[key] = merged_list
        else:
            # 如果key不在A中，直接添加
            data_a[key] = data_b[key]

    # try:
    #     # 将合并后的数据保存到C.json
    #     with open(c_path, 'w', encoding='utf-8') as file_c:
    #         json.dump(data_a, file_c, ensure_ascii=False, indent=4)
    # except Exception as e:
    #     print(f"写入文件C时出错: {e}")
    #     return

    # 输出结果
    print("B中有但A中没有的键：")
    print(len(keys_in_b_not_in_a))
    # for key in keys_in_b_not_in_a:
    #     print(f"- {key}")

    print("\nA和B都有但值不同的键：")
    print(len(keys_with_different_values))
    # for key in keys_with_different_values:
    #     print(f"- {key}")


def main():
    # 替换为实际的文件路径
    a_path = ''
    b_path = ''
    c_path = ''

    merge_json_files(a_path, b_path, c_path)
    # num, filenum, files = count_JSON(c_path)
    # print(files)
    # print(num, filenum)

if __name__ == "__main__":
    main()
