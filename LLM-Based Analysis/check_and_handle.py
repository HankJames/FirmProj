# encoding:utf-8
import os
import time
import json
import requests

llm_server_url = "http://10.102.104.22:5000/generate"  # 更新后的服务器地址


def llm_client(json_file):
    begin = time.time()

    with open('txt/prompt0.txt', 'r', encoding='utf-8') as file:
        prompt0 = file.read()

    # 读取 prompt 和问题
    with open('txt/prompt1.txt', 'r', encoding='utf-8') as file:
        prompt = file.read()

    # 读取指定的 JSON 文件
    with open(json_file, 'r', encoding='utf-8') as file:
        questions = json.load(file)

    results = {}
    num = 0

    # 依次处理每个问题
    for key, question in questions.items():
        num += 1

        # Step 1: 使用 prompt0 进行分类
        messages_classify = f"{prompt0}\n{question}"  # 将 prompt0 和问题组合成一个字符串

        # 使用 requests 调用分类接口
        response = requests.post(llm_server_url, json={"prompt": messages_classify}).json()
        category_response = response['response']  # 假设此处为返回的分类结果

        category = int(category_response)  # 将响应转换为整数类别

        if category == 1:  # 与固件升级检查过程相关
            print("固件相关：", key)
            messages = f"{prompt}\n{question}"  # 将 prompt 和问题组合成一个字符串

            # 使用 requests 调用生成结果接口
            response = requests.post(llm_server_url, json={"prompt": messages}).json()
            response_text = response['response']

            # 尝试将 response 转换为 JSON 格式
            try:
                response_json = json.loads(response_text.strip('\"'))
                results[key] = response_json  # 将解析后的 JSON 存入结果
            except json.JSONDecodeError:
                results[key] = response_text

    # 将结果保存为 JSON 文件
    output_file = f'results_{json_file.split("/")[-1]}'
    with open(output_file, 'w', encoding='utf-8') as file:
        json.dump(results, file, ensure_ascii=False, indent=4)

    print(f"结果已保存到 {output_file} 文件中")
    end = time.time()
    print("平均处理时间:", (end - begin) / num, "秒/输入")


def check_handle(directory):  # 实时检查文件夹，若有新文件则送入大模型服务器处理
    processed_set = set()
    while True:
        # 获取文件夹中的所有文件
        files = set(os.listdir(directory))

        for json_name in files:
            json_path = os.path.join(directory, json_name)
            # 检查每个文件是否已经处理过
            if json_name not in processed_set:
                # 执行文件处理
                llm_client(json_path)
                # 将处理过的文件添加到 set 中
                processed_set.add(json_name)
        # 等待一段时间再检查
        time.sleep(1)


if __name__ == "__main__":
    folder = "path_to_your_folder"  # 替换为你的文件夹路径
    check_handle(folder)
