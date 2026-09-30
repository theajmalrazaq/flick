set -e
cd "$(dirname "$0")"
JAR=${ANDROID_JAR:-android.jar}
[ -f "$JAR" ] || curl -sL -o "$JAR" https://github.com/Sable/android-platforms/raw/master/android-34/android.jar
rm -rf out && mkdir -p out/classes
if [ ! -f flick.jks ]; then
  keytool -genkeypair -keystore flick.jks -storepass flick123 -keypass flick123 -alias flick -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Flick" >/dev/null 2>&1
fi
aapt package -f -M AndroidManifest.xml -S res -A assets -I "$JAR" -F out/base.apk
javac -source 8 -target 8 -bootclasspath "$JAR" -d out/classes src/com/flick/walls/Main.java 2>&1 | grep -v "warning" || true
dalvik-exchange --dex --min-sdk-version=24 --output=out/classes.dex out/classes
cd out && aapt add base.apk classes.dex >/dev/null && cd ..
zipalign -f 4 out/base.apk out/aligned.apk
apksigner sign --ks flick.jks --ks-pass pass:flick123 --key-pass pass:flick123 --out flick.apk out/aligned.apk
apksigner verify --verbose flick.apk
