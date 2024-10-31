
find [path] -name "*.apk" ! -name "*.split.*" -maxdepth 1 -print | parallel -j 32 python3 [pathToMain.py] --app_dir={}  \;


#while true
#do
#  sleep 3
#done
