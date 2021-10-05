javac -cp src/ src/com/java/advanced/Client.java
java -cp src/ com.java.advanced.Client COMMAND=DOWNLOAD,ARGUMENT="[https://download.bell-sw.com/java/8u302+8/bellsoft-jdk8u302+8-windows-amd64.msi,https://download.bell-sw.com/java/11.0.12+7/bellsoft-jdk11.0.12+7-windows-amd64.msi]",RESULT_PORT=$((10000 + RANDOM))
