
find /data/wenzhi/Androzoo/Result/ -name "*.apk" ! -name "*.split.*" -maxdepth 1 -print | parallel -j 32 python3 /data/wenzhi/LLM_AUTO/4_终极流程/main.py --app_dir={}  \;


#while true
#do
#  sleep 3
#done
