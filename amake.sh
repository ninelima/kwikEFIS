#!/bin/bash

# ./gradlew clean
# rm h:/src/kwikEFIS/apk/kwik-efis.apk
# rm h:/src/kwikEFIS/apk/kwik-efis-datapac-world.apk
# rm h:/src/kwikEFIS/apk/kwik-efis-datapac-zar.aus.apk
# rm h:/src/kwikEFIS/apk/kwik-efis-datapac-usa.can.apk
# rm h:/src/kwikEFIS/apk/kwik-efis-datapac-eur.rus.apk

./gradlew build

cp h:/src/kwikEFIS/pfd/build/outputs/apk/pfd-debug.apk h:/src/kwikEFIS/apk/kwik-efis.apk
cp h:/src/kwikEFIS/mfd/build/outputs/apk/mfd-debug.apk h:/src/kwikEFIS/apk/kwik-dmap.apk
cp h:/src/kwikEFIS/data.zar.aus/build/outputs/apk/data.zar.aus-debug.apk h:/src/kwikEFIS/apk/kwik-efis-datapac-zar.aus.apk
cp h:/src/kwikEFIS/data.usa.can/build/outputs/apk/data.usa.can-debug.apk h:/src/kwikEFIS/apk/kwik-efis-datapac-usa.can.apk
cp h:/src/kwikEFIS/data.eur.rus/build/outputs/apk/data.eur.rus-debug.apk h:/src/kwikEFIS/apk/kwik-efis-datapac-eur.rus.apk
cp h:/src/kwikEFIS/data.sah.jap/build/outputs/apk/data.sah.jap-debug.apk h:/src/kwikEFIS/apk/kwik-efis-datapac-sah.jap.apk
cp h:/src/kwikEFIS/CHANGELOG.md h:/src/kwikEFIS/apk/CHANGELOG.md

cd h:/src/kwikEFIS/apk

#md5sum kwik-efis.apk > kwik-efis.apk.md5
#md5sum kwik-efis-datapac-world.apk > kwik-efis-datapac-world.apk.md5
#md5sum kwik-efis-datapac-zar.aus.apk > kwik-efis-datapac-zar.aus.apk.md5
#md5sum kwik-efis-datapac-usa.can.apk > kwik-efis-datapac-usa.can.apk.md5
#md5sum kwik-efis-datapac-eur.rus.apk > kwik-efis-datapac-eur.rus.apk.md5


#gsm nexus 7
#adb -s 015d3249295c160b install -r ./kwik-efis.apk
#adb -s 015d3249295c160b install -r ./kwik-efis-datapac-zar.aus.apk
#adb -s 015d3249295c160b install -r ./kwik-efis-datapac-usa.can.apk
#adb -s 015d3249295c160b install -r ./kwik-efis-datapac-eur.rus.apk

#wifi nexus 7
adb -s 015d2ea4a467ec11 install -r ./kwik-efis.apk
adb -s 015d2ea4a467ec11 install -r ./kwik-dmap.apk
#adb -s 015d2ea4a467ec11 install -r ./kwik-efis-datapac-zar.aus.apk

pskill java
