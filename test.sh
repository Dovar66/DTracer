# 启动关闭应用100次
# shellcheck disable=SC2034
for i in $(seq 1 100)
do
   adb shell am force-stop com.dovar.demo
   sleep 1
   adb shell am start com.dovar.demo/.MainActivity | grep "TotalTime" | cut -d ' ' -f 2
done
