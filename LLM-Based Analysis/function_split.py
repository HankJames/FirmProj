# encoding:utf-8
from openai import OpenAI
import os
import json
import openai

openai.proxy = None
os.environ['HTTP_PROXY'] = ''
os.environ['HTTPS_PROXY'] = ''
def function_split(APP_dir):  # APP_dir=APP/APP1
    query_dir = APP_dir + '/Output/LLM-Query'
    prompt_dir = '1_prompt'
    answer_dir = APP_dir + '/1_LLM_answer'
    if not os.path.exists(query_dir):
        # 创建文件夹
        os.makedirs(query_dir)
    # APP1
    #     1_LLM_qeury
    #        query_ruochan.json
    #     1_LLM_answer
    #        answer_ruochan.json
    # 读取提示文件
    with open(prompt_dir + '/prompt0.txt', 'r', encoding='utf-8') as file0:  # 分类1/2/3/4
        prompt0 = file0.read()
    with open(prompt_dir + '/prompt1.txt', 'r', encoding='utf-8') as file1:  # 加解密 1
        prompt1 = file1.read()
    with open(prompt_dir + '/prompt2.txt', 'r', encoding='utf-8') as file2:  # 字符串格式化 2
        prompt2 = file2.read()
    with open(prompt_dir + '/prompt3.txt', 'r', encoding='utf-8') as file3:  # http格式化 3
        prompt3 = file3.read()
    client = OpenAI(
        api_key="KEY",
        base_url="URL",
    )
    # 遍历query文件
    for filename in os.listdir(query_dir):
        if filename.endswith('.json'):
            with open(os.path.join(query_dir, filename), 'r', encoding='utf-8') as file:
                question_data = json.load(file)
                question = json.dumps(question_data)  # 将整个内容转换为字符串作为问题

            # Step 1: 使用 prompt0 进行分类
            messages_classify = [
                {"role": "system", "content": prompt0},
                {"role": "user", "content": question}
            ]

            completion = client.chat.completions.create(
                model="gpt-4o-mini-2024-07-18",
                messages=messages_classify,
                temperature=0.3,
                timeout=180
            )
            category_response = completion.choices[0].message.content
            category = int(category_response)  # 将响应转换为整数类别
            # print(f"Category for {filename}: {category}")

            # 根据分类结果选择对应的 prompt 处理
            prompt = None
            if category == 1:  # 加解密
                prompt = prompt1
            elif category == 2:  # 字符串格式化
                prompt = prompt2
            elif category == 3:  # http
                prompt = prompt3
            else:  # category == 4 其他类别
                # print("其他类别")
                continue  # 跳过其他类别

            # Step 2: 根据类别使用相应的 prompt 处理输入
            messages_process = [
                {"role": "system", "content": prompt},
                {"role": "user", "content": question}
            ]

            completion_ = client.chat.completions.create(
                model="gpt-4o-mini-2024-07-18",
                messages=messages_process,
                temperature=0.1,
            )
            response = completion_.choices[0].message.content
            # 打印最终响应
            # print(f"Response for {filename}: {response}")
            # print("----------------------------------------------------------------------------------------")
            # 检查文件夹是否存在
            if not os.path.exists(answer_dir):
                # 创建文件夹
                os.makedirs(answer_dir)
                # print(f"文件夹 '{answer_dir}' 创建成功！")
            # 保存结果到answer文件
            answer_file_path = os.path.join(answer_dir, f'answer_{filename[6:-5]}.txt')  # 去掉.json后缀
            with open(answer_file_path, 'w') as answer_file:
                answer_file.write(response)


# Example usage
if __name__ == "__main__":
    function_split('APP/APP1')  # 生成'APP/APP1/1_LLM_answer/answer_ruochan.json'
