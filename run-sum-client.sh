javac -cp src/ src/com/java/advanced/Client.java
java -cp src/ com.java.advanced.Client COMMAND=SUM,ARGUMENT=1000000000,RESULT_PORT=$((10000 + RANDOM))