javac -cp src/ src/com/java/advanced/Client.java
java -cp src/ com.java.advanced.Client COMMAND=ZIP,ARGUMENT="C:\Temp\\advanced",RESULT_PORT=$((10000 + RANDOM))