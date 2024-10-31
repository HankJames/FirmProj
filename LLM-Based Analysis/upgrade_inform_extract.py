# encoding:utf-8
import json
import time
import os
from openai import OpenAI


def upgrade_inform_extract(APP_dir):  # APP_dir=APP/APP1
    query_dir = APP_dir + '/Output'
    prompt_dir = '2_prompt'
    answer_dir = APP_dir + '/2_LLM_answer'
    # 读取 prompt 和问题
    with open(prompt_dir + '/prompt.txt', 'r', encoding='utf-8') as file:
        prompt = file.read()
    client = OpenAI(
        api_key="sk-r42s0LzOFAEn5kxaB61c645bE7B840DaA56fF36bB81aF191",
        base_url="https://chatapi.nloli.xyz/v1"
    )
    # 遍历所有待处理文件
    for filename in os.listdir(query_dir):
        if filename.endswith('.json'):
            input_json_name = os.path.join(query_dir, filename)
            print(filename[0:-5] + " 即将开始处理............")
            with open(input_json_name, 'r', encoding='utf-8') as file:
                question = file.read()
            if question == '{}' or len(question) < 10:
                print(f"{filename} Empty query!")
                continue
            num = 0
            while num < 4:  # 最多重复跑4次，防止未知APP导致的死循环
                try:
                    num += 1
                    # 依次处理每个问题
                    messages = [
                        {"role": "system", "content": prompt},
                        {"role": "user", "content": question}
                    ]
                    completion = client.chat.completions.create(
                        model="gpt-4o-mini-2024-07-18",
                        messages=messages,
                        temperature=0.5,
                        timeout=180
                    )
                    response = completion.choices[0].message.content
                    # print(response)
                    # 将字符串解析为 JSON 对象
                    # data = json.loads(response, strict=False)
                    data = json.loads(response)
                    # 检查文件夹是否存在
                    if not os.path.exists(answer_dir):
                        # 创建文件夹
                        os.makedirs(answer_dir)
                        # print(f"文件夹 '{answer_dir}' 创建成功！")
                    with open(answer_dir + '/' + filename, 'w',
                              encoding='utf-8') as output_file:
                        json.dump(data, output_file, ensure_ascii=False, indent=4)
                    del response
                    print(filename[0:-5] + " 结果已保存............")
                    break
                except Exception as e:
                    print("出错了，重新分析此App文件")


# Example usage
if __name__ == "__main__":
    upgrade_inform_extract('APP/APP1')  # 生成'APP/APP1/2_LLM_answer/com.dc.dreamcatcherlife.json'
