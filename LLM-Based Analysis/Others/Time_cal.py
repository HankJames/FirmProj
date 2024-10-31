import os


def get_runtime_from_file(file_path):
    try:
        with open(file_path, 'r') as file:
            line = file.readline()
            # 假设格式为 "[Time]: 100"
            time_value = float(line.split(":")[1].strip())
            return time_value
    except Exception as e:
        print(f"读取文件 {file_path} 时出错: {e}")
        return None


def collect_runtimes_from_directory(target_path):
    runtimes = []
    vsa_times = []
    llm_times = []

    for root in os.listdir(target_path):
        root_path = os.path.join(target_path, root)
        if not os.path.isdir(root_path):
            continue
        time_VSA = 0
        time_LLM = 0
        if 'Output' in os.listdir(root_path):
            runtime_file = os.path.join(root_path, 'LLM_Time.txt')

            if os.path.exists(runtime_file):
                # 获取运行时间并添加到列表
                runtime = get_runtime_from_file(runtime_file)
                if runtime is not None:
                    time_LLM = round(runtime, 2)

            local_time_file = os.path.join(root_path, 'local_time.txt')
            if os.path.exists(local_time_file):
                local_time = get_runtime_from_file(local_time_file)
                if local_time is not None:
                    time_LLM += round(local_time, 2)

            if time_LLM <= 0.01:
                print(root_path)
            llm_times.append(time_LLM)

        if 'Output' in os.listdir(root_path):

            output_path = os.path.join(root_path, 'Output')
            runtime_file = os.path.join(output_path, 'RunTime.txt')

            if os.path.exists(runtime_file):
                # 获取运行时间并添加到列表
                runtime = get_runtime_from_file(runtime_file)
                if runtime is not None:
                    time_VSA = round(runtime/1000, 2)
                    vsa_times.append(time_VSA)

        total_time = time_VSA + time_LLM
        if total_time > 0.1:
            runtimes.append(total_time)
    return llm_times, vsa_times, runtimes


def main(target_path):
    # 收集所有运行时间
    llm_times, vsa_times, runtimes = collect_runtimes_from_directory(target_path)

    if vsa_times:
        max_runtime = max(vsa_times)
        min_runtime = min(vsa_times)
        avg_runtime = sum(vsa_times) / len(vsa_times)
        list.sort(vsa_times)
        # print(runtimes)
        print("[VSA Time]: ")
        print(f"Max Time: {max_runtime:.2f} s")
        print(f"Min Time: {min_runtime:.2f} s")
        print(f"Avg Time: {avg_runtime:.2f} s")

    if llm_times:
        max_runtime = max(llm_times)
        min_runtime = min(llm_times)
        avg_runtime = sum(llm_times) / len(llm_times)
        # print(runtimes)
        print("[LLM Time]: ")
        print(f"Max Time: {max_runtime:.2f} s")
        print(f"Min Time: {min_runtime:.2f} s")
        print(f"Avg Time: {avg_runtime:.2f} s")

    if runtimes:
        max_runtime = max(runtimes)
        min_runtime = min(runtimes)
        avg_runtime = sum(runtimes) / len(runtimes)
        list.sort(runtimes)
        # print(runtimes)
        print("[Total Time]: ")
        print(f"Max Time: {max_runtime:.2f} s")
        print(f"Min Time: {min_runtime:.2f} s")
        print(f"Avg Time: {avg_runtime:.2f} s")
    else:
        print("未找到任何运行时间数据。")


if __name__ == "__main__":
    # 目标路径可以修改为你需要检查的目录
    target_path = '/data/wenzhi/Result/LOCAL_APK_1'
    main(target_path)
