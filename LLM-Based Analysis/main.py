# encoding:utf-8
import os.path
import time

from function_split import function_split
from upgrade_inform_extract import upgrade_inform_extract
from funccall_download import funccall_download
import argparse


def main(APP_path):
    timeFile = os.path.join(APP_path, "LLM_Time.txt")
    # if os.path.exists(timeFile):
    #     return
    start = time.time()
    try:
        print("==============================开始对jimple进行分类===============================")
        function_split(APP_path)
        print("================================开始提取固件升级信息===============================")
        upgrade_inform_extract(APP_path)
        print("=============================开始进行函数调用并下载文件=============================")
        funccall_download(APP_path)
    finally:
        end = time.time()

        with open(timeFile, "w") as f:
            f.write(f'[Time]: {end - start}')


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--app_dir", type=str, required=True, help="APP directory")
    args = parser.parse_args()
    query_dir = args.app_dir + '/2_LLM_answer'
    # if not os.path.exists(query_dir):
    main(args.app_dir)
    # 输入'APP/APP1/1_LLM_query/com.dc.dreamcatcherlife.json'
    # 输出'APP/APP1/'1_LLM_answer'

    # 输入'APP/APP1/'2_LLM_query'
    # 输出'APP/APP1/'2_LLM_answer'
    # 输出'APP/APP1/'3_LLM_answer'，其中：
    # download_dir：放置下载文件
    # conplete_0_xxx.json  完整网络请求0
    # inconplete_1_xxx.json  不完整网络请求1
    # inconplete_2_xxx.json  不完整网络请求2
    # inconplete_3_xxx.json  不完整网络请求3
