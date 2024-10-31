# encoding:utf-8
import requests
import json
import os
from urllib.parse import urlparse


class Request_multi:
    def __init__(self, app_path=None):
        self.app_path = app_path

    proxies = {
        'http': None,
        'https': None
    }

    def make_request_multi(self, method, urls, headers=None, parameter=None, download=False):
        result_list = []
        visited = []
        for url in urls:
            if url in visited:
                continue
            result_list.append(self.make_request(method, url, headers, parameter, download))
            visited.append(url)
        return result_list

    def make_request(self, method, url, headers=None, parameter=None, download=False):
        """
        request函数，用于检验网络请求是否有效
        :param download: 可选参数， true则只返回文件名
        :param method: 必要参数，分为get或post，用0,1表示
        :param url: 必要参数，request请求使用的url，用字符串表示
        :param headers: 可选参数，request请求时使用的header，用字典表示
        :param parameter: 可选参数，在get请求中表示get的查询参数param，用字典表示,在post请求中表示post的data参数，用字符串表示
        :return: make_request函数request请求的响应结果，返回结果为表示为字符串格式
        """
        try:
            if method == 0:
                response = requests.get(url, params=parameter, headers=headers, proxies=self.proxies, timeout=10)
            elif method == 1:
                response = requests.post(url, data=parameter, headers=headers, proxies=self.proxies, timeout=10)
            else:
                return {"error": "Unsupported method. Use 'get' or 'post'."}

            # Check if the response was successful
            if response.status_code >= 400:
                return {
                    "error": f"HTTP Error {response.status_code}",
                    "status_code": response.status_code,
                    "content": response.text
                }

            content_type = response.headers.get('Content-Type', '').lower()
            filename = self.get_filename_from_url(url, response)

            flag = 0
            if 'application/json' in content_type:
                returnContent = {
                    "status_code": response.status_code,
                    "content": json.loads(response.text)
                }
                download = False

            elif 'text' in content_type or 'application/xml' in content_type:
                returnContent = {
                    "status_code": response.status_code,
                    "content": response.text
                }
                download = False

            elif 'octet-stream' in content_type or 'application/octet-stream' in content_type \
                    or 'application/zip' in content_type or 'audio' in content_type \
                    or 'video' in content_type \
                    or 'application' in content_type:
                if filename.endswith(".json"):
                    returnContent = {
                        "status_code": response.status_code,
                        "content": json.loads(response.text)
                    }
                    download = False
                else:
                    self.save_file(response.content, filename)
                    flag = 1
                    returnContent = {
                        "status_code": response.status_code,
                        "content": f"File saved as: {filename}"
                    }
            else:
                if int(response.headers.get('Content-Length', '0')) > 50000:
                    self.save_file(response.content, filename)
                    flag = 1
                    returnContent = {
                        "status_code": response.status_code,
                        "content": f"File saved as: {filename}"
                    }
                else:
                    returnContent = {
                        "status_code": response.status_code,
                        "content": f"Unsupported content type: {content_type}",
                        "raw_content": response.content
                    }
            if download:
                if flag < 1:
                    self.save_file(response.content, filename)
                return "file: " + filename
            return returnContent

        except requests.exceptions.RequestException as e:
            return {"error": f"An error occurred: {str(e)}"}

    def get_filename_from_url(self, url, response):
        # Try to get filename from Content-Disposition header
        content_disposition = response.headers.get('Content-Disposition')
        if content_disposition:
            import re
            filename = re.findall("filename=(.+)", content_disposition)
            if filename:
                return filename[0].strip('"')

        # If not found, use the last part of the URL
        parsed_url = urlparse(url)
        filename = os.path.basename(parsed_url.path)

        # If filename is still empty, use a default name
        if not filename:
            filename = 'downloaded_file'

        return filename

    def save_file(self, content, filename):
        save_p = self.app_path + '/3_LLM_answer/download/' + filename
        i = 0
        while os.path.exists(save_p):
            with open(save_p, 'rb') as f:
                content_f = f.read()
                h1 = hash(content_f)
                h2 = hash(content)
                if h1 != h2:
                    save_p += "_" + str(i)
                    i += 1
                else:
                    return
        with open(save_p, 'wb') as f:
            f.write(content)
            print("下载文件： ", filename)


# Example usage
if __name__ == "__main__":
    # Successful GET request example
    request_multi = Request_multi()
    result = request_multi.make_request_multi(0, ["http://bo.ruochanit.com:6088/v1/versioninfos"],
                                              headers={'User-Agent': 'Python Request'},
                                              parameter={'name': 'AndroidDeviceConfigDebug'})
    print("GET result:", result)

    # Successful POST request example
    post_data = {'key1': 'value1', 'key2': 'value2'}
    result = request_multi.make_request_multi(1, ["https://httpbin.org/post"], parameter=post_data)
    print("POST result:", result)

    # File download example
    result = request_multi.make_request_multi(0, ["http://avpro.global.yamaha.com/hpep/Yamaha_EP-E30A_0100.bin"])
    print("File download result:", result)

    # Error response example
    result = request_multi.make_request_multi(0, [
        "http://api.ruochanit.com:6090/v1/files/3034b1306f0511e9ba29cd7183803e53"])
    print("Error response result:", result)
