# encoding:utf-8
import json
from openai import OpenAI
import openai
from request import Request_multi
import ast
import os

os.environ['ALL_PROXY'] = 'http://127.0.0.1:7888'
os.environ['all_proxy'] = 'http://127.0.0.1:7888'
openai.proxy = None

visited = []


def funccall_download(APP_dir):  # APP_dir=APP/APP1
    query_dir = APP_dir + '/2_LLM_answer'  # 第二步的输出就是第三步的输入
    prompt_dir = '3_prompt'
    answer_dir = APP_dir + '/3_LLM_answer'
    download_dir = APP_dir + '/3_LLM_answer' + '/download'
    client = OpenAI(
        api_key="sk-r42s0LzOFAEn5kxaB61c645bE7B840DaA56fF36bB81aF191",
        base_url="https://chatapi.nloli.xyz/v1"
    )

    if not os.path.exists(query_dir):
        print("None")
        return

    # ------------------------创建结果文件夹------------------------#
    mkdir_list = [answer_dir, download_dir]
    for folder_ in mkdir_list:
        # 检查文件夹是否存在
        if not os.path.exists(folder_):
            # 创建文件夹
            os.makedirs(folder_)
            # print(f"文件夹 '{folder_}' 创建成功！")
    # ------------------------读取prompt------------------------#
    # 用于区分网络请求是否完整
    with open(prompt_dir + '/prompt_split.txt', 'r', encoding='utf-8') as file1:
        prompt_split = file1.read()
    # 用于对完整的网络请求做外部函数调用
    with open(prompt_dir + '/prompt_functioncall.txt', 'r', encoding='utf-8') as file2:
        prompt_functioncall = file2.read()
    # 用于获取下载链接
    with open(prompt_dir + '/prompt_downloadlink.txt', 'r', encoding='utf-8') as file3:
        prompt_downloadlink = file3.read()

    # ------------------------外部函数库构建------------------------#
    request_multi = Request_multi(APP_dir)
    available_function = {
        "make_request_name": request_multi.make_request_multi,
    }
    make_request_function_description = {
        "name": "make_request_name",
        "description": "用于发送get或post网络请求的函数",
        "parameters": {
            "type": "object",
            "properties": {
                "method": {
                    "type": "number",
                    "enum": [0, 1],  # 限制方法为GET或POST
                    "description": "网络请求的方法,GET为0,POST为1"
                },
                "urls": {
                    "type": "array",
                    "items": {  # 定义数组元素的类型
                        "type": "string"  # 数组元素的类型为字符串
                    },
                    "description": "request请求可能的url列表"
                },
                "headers": {  # 一个字符串类型的字典
                    "type": "object",
                    "additionalProperties": {
                        "type": "string"  # 可以接收任意字符串类型的属性键和值。
                    },
                    "required": [],
                    "description": "request请求附带的header,需要为字典格式,且每个header对应一个具体值,忽略dynamic动态值,只保留一个具体值,如果没有则置为空"
                },
                "parameter": {
                    "oneOf": [
                        {
                            "type": "object",
                            "additionalProperties": {
                                "type": "string"
                            },
                            "required": [],
                            "description": "从param需要为字典格式,且每个header对应一个值; 忽略dynamic动态值,只保留一个具体值,如果没有则置为空"
                        },
                        {
                            "type": "string",
                            "description": "一条字符串数据"
                        }
                    ],
                    "description": "method为GET时，从param字段获取为字典；method为POST时，从body字段获取为字典或字符串"
                },
            },
            "required": ["method", "url", "parameter"]  # 强制要求method和url
        }
    }
    functions_description = [make_request_function_description]

    # ------------------------读取处理文件------------------------#
    for filename in os.listdir(query_dir):
        input_json_name = os.path.join(query_dir, filename)
        if os.path.isfile(input_json_name):  # 只处理文件
            print(filename[0:-5] + " 即将开始处理............")
            with open(input_json_name, 'r', encoding='utf-8') as file:
                questions = json.load(file)
                # ------------------------主体：网络请求处理------------------------#
                result0 = {}
                result1 = {}  # 存储不完整网络请求 1类
                result2 = {}  # 存储不完整网络请求 2类
                result3 = {}  # 存储不完整网络请求 3类
                # 依次处理每个问题
                for key, question in questions.items():
                    if question == "0":
                        continue
                    question_str = str(question)
                    messages_split = [
                        {"role": "system", "content": prompt_split},
                        {"role": "user", "content": question_str}
                    ]

                    # completion = client.chat.completions.create(
                    #     model="gpt-4o-mini-2024-07-18",
                    #     messages=messages_split,
                    #     temperature=0.8,
                    #     top_p=0.8
                    # )
                    completion = client.chat.completions.create(
                        model="gpt-4o-mini-2024-07-18",
                        messages=messages_split,
                        temperature=0.5,
                        timeout=180
                    )
                    response = completion.choices[0].message.content
                    if response == '1':
                        result1[key] = question
                        print('key:', key, '网络请求不完整，类别：1')
                        continue
                    if response == '2':
                        result2[key] = question
                        print('key:', key, '网络请求不完整，类别：2')
                        continue
                    if response == '3':
                        result3[key] = question
                        print('key:', key, '网络请求不完整，类别：3')
                        continue
                    if response == '0':  # 完整的网络请求，调用外部函数
                        result0[key] = question
                        print('key:', key, '网络请求完整！！！开始进行网络请求下载............')

                        # ------------------------1.执行函数调用------------------------#
                        messages_functioncall = [
                            {"role": "system", "content": prompt_functioncall},
                            {"role": "user", "content": question_str}
                        ]
                        response = client.chat.completions.create(
                            model="gpt-4o-mini-2024-07-18",
                            messages=messages_functioncall,
                            functions=functions_description,
                            function_call="auto",  # 自动选择外部函数库中的函数
                        )
                        response_message = response.choices[0].message
                        # print(response_message)
                        function_name = response_message.function_call.name
                        # "make_request"
                        function_to_call = available_function[function_name]  # 根据函数名字得到要运行的函数本体
                        function_args = json.loads(response_message.function_call.arguments)
                        print("大模型得出的函数调用参数function_args:  ", function_args)
                        function_response = function_to_call(**function_args)
                        print("make_request结果：  ", function_response)

                        # ------------------------2.从request返回结果中获取下载链接------------------------#
                        i = 0
                        for res in function_response:
                            print('++++++++++++++++对第' + str(i) + '个做下载处理++++++++++++++++')
                            results = startDownload(res, request_multi, client, prompt_downloadlink, function_args.get('urls')[i])
                            for result in results:
                                if "http" in str(result):
                                    try:
                                        results = startDownload(result, request_multi, client, prompt_downloadlink, "")
                                    finally:
                                        continue
                            print("File download result:", results)
                            i += 1

                # 将不完整的网络请求保存为 JSON 文件
                with open(answer_dir + '/complete_0_' + filename, 'w', encoding='utf-8') as file0:
                    json.dump(result0, file0, ensure_ascii=False, indent=4)
                with open(answer_dir + '/incomplete_1_' + filename, 'w', encoding='utf-8') as file1:
                    json.dump(result1, file1, ensure_ascii=False, indent=4)
                with open(answer_dir + '/incomplete_2_' + filename, 'w', encoding='utf-8') as file2:
                    json.dump(result2, file2, ensure_ascii=False, indent=4)
                with open(answer_dir + '/incomplete_3_' + filename, 'w', encoding='utf-8') as file3:
                    json.dump(result3, file3, ensure_ascii=False, indent=4)


def startDownload(res, request_multi, client, prompt_downloadLink, function_args=""):
    global visited
    messages_downloadLink = [
        {"role": "system", "content": prompt_downloadLink},
        {"role": "user",
         "content": "Request url: " + function_args + "\nResponse: " + str(res)}
    ]
    download_response = client.chat.completions.create(
        model="gpt-4o-mini-2024-07-18",
        messages=messages_downloadLink,
    )
    print(".........开始提取链接.........")
    downloadLink_list = download_response.choices[0].message.content
    downloadLink_list = ast.literal_eval(downloadLink_list)  # 从列表的str转为list
    downloadLink_list = list(set(downloadLink_list))
    print(downloadLink_list)

    # ------------------------3.根据下载链接列表下载文件------------------------#
    for url in downloadLink_list:
        if url in visited:
            downloadLink_list.remove(url)
        visited.append(url)
    # File download
    print(".........开始进行下载.........")
    print(downloadLink_list)
    result = request_multi.make_request_multi(0, downloadLink_list, download=True)
    print(".........下载结束.........")
    return result


# Example usage
if __name__ == "__main__":
    funccall_download('/data/wenzhi/Result/IoT-VER/com.hubbleconnected.vervelife.apk')
    # 输入'APP/APP1/2_LLM_answer/com.dc.dreamcatcherlife.json'
    # 输出'APP/APP1/'3_LLM_answer'，其中：
    # download_dir：放置下载文件
    # complete_0_dir：放置完整请求
    # incomplete_1_dir：放置不完整请求1类
    # incomplete_2_dir：放置不完整请求2类
    # incomplete_3_dir：放置不完整请求3类
