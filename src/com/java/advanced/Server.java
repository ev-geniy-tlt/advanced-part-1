package com.java.advanced;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.io.File.separator;
import static java.math.BigInteger.ZERO;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class Server {

    public static final int SOCKET_PORT = 8000;
    public static final String REQUEST_REGEXP = "command=([a-zA-Z]+),\\s*argument=(.+),\\s*result_port=([0-9]+)";
    public static final Pattern commandPattern = Pattern.compile(REQUEST_REGEXP, CASE_INSENSITIVE);

    public static final String WORK_DIR = System.getProperty("java.io.tmpdir") + "advanced";

    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static final AtomicInteger threadCounter = new AtomicInteger(0);
    private static final ExecutorService ioPool = new ThreadPoolExecutor(64, 256, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            runnable -> new Thread(runnable, "Worker-" + threadCounter.getAndIncrement()));
    private static final ForkJoinPool cpuPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

    public static void main(String[] args) {
        boolean workDirCreatedOrExists = createWorkDirIfNotExists();
        if (!workDirCreatedOrExists) {
            throw new IllegalStateException(String.format("Cannot create WORK_DIR %s", WORK_DIR));
        }
        Runtime.getRuntime().addShutdownHook(new Thread(Server::shutdown));

        try (ServerSocket server = new ServerSocket(SOCKET_PORT)) {
            logger.info(String.format("Server started on socket %s", SOCKET_PORT));

            while (!server.isClosed()) { //infinitive loop
                Socket client = server.accept();
                logger.info("Client connected");
                ioPool.execute(() -> handle(client));
            }

            shutdown();
        } catch (IOException e) {
            logger.severe(e.toString());
        }
    }

    public static void handle(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

            String message = in.readLine();
            Matcher matcher = commandPattern.matcher(message);
            if (matcher.matches()) {
                String command = matcher.group(1);
                String argument = matcher.group(2);
                int port = Integer.parseInt(matcher.group(3));
                switch (command) {
                    case "SUM":
                        logger.info(String.format("Calculating sum for: %s", argument));
                        BigInteger sum = cpuPool.invoke(new SumRecursiveTask(ZERO, new BigInteger(argument)));
                        sendResult(client, port, String.format("Total sum: %s", sum));
                        break;
                    case "ZIP":
                        logger.info(String.format("Zipping %s", argument));
                        long createdZipFileSize = zipDir(argument);
                        sendResult(client, port, String.format("Total zip file size: %s", createdZipFileSize));
                        break;
                    case "DOWNLOAD":
                        String[] urls = argument.substring(argument.indexOf("[") + 1, argument.lastIndexOf("]")).split(",");
                        logger.info(String.format("Downloading %s", Arrays.toString(urls)));
                        long totalDownloadedBytes = downloadFiles(urls);
                        sendResult(client, port, String.format("Total downloaded files size: %s", totalDownloadedBytes));
                        break;
                    default:
                        logger.info(String.format("Unsupported command: %s", command));
                }
            } else {
                logger.info(String.format("Wrong client's message: %s", message));
            }
        } catch (Exception e) {
            logger.severe(String.format("Error on server: %s", e));
        }
    }

    private static long downloadFiles(String[] urls) throws ExecutionException, InterruptedException {
        HttpClient httpClient = HttpClient.newBuilder().executor(ioPool).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urls[0]))
                .setHeader("User-Agent", "Java 11 HttpClient")
                .build();

        List<CompletableFuture<Long>> downloads = new ArrayList<>(urls.length);
        for (String url : urls) {
            String fileName = getFileNameFromUrl(url);
            try {
                logger.info(String.format("Target filename: %s", fileName));
                downloads.add(httpClient.sendAsync(request,
                                HttpResponse.BodyHandlers.ofFile(Paths.get(fileName)))
                        .thenApply(pathHttpResponse -> {
                            logger.info(String.format("%s downloaded", fileName));
                            return pathHttpResponse.body().toFile().length();
                        }));
            } catch (Exception e) {
                logger.warning(String.format("Error during downloading the file %s: %s", fileName, e));
            }
        }

        final CompletableFuture<Void> commonProgress = CompletableFuture.allOf(downloads.toArray(CompletableFuture[]::new));
        commonProgress.get();
        long totalDownloadedBytes = 0;
        for (CompletableFuture<Long> download : downloads) {
            totalDownloadedBytes += download.get();
        }
        return totalDownloadedBytes;
    }

    private static String getFileNameFromUrl(String url) {
        return String.format("%s%s%s", WORK_DIR, separator, url.substring(url.lastIndexOf("/") + 1));
    }

    private static String getZipFileName(String srcDriName) {
        return String.format("%s%s%s.zip", WORK_DIR, separator, Paths.get(srcDriName).getFileName().toString());
    }

    private static boolean createWorkDirIfNotExists() {
        File directory = new File(WORK_DIR);
        if (!directory.exists()) {
            return directory.mkdir();
        }
        return true;
    }

    private static void sendResult(Socket client, Integer resultPort, String result) {
        try {
            logger.info(String.format("Result will be send to the port %s", resultPort));
            Socket callbackSocket = new Socket(client.getInetAddress(), resultPort);
            try (PrintWriter out = new PrintWriter(new OutputStreamWriter(callbackSocket.getOutputStream()), true)) {
                out.println(result);
            }
            callbackSocket.close();
            logger.info(String.format("Result successfully sent to the port %s", resultPort));
        } catch (IOException e) {
            logger.severe(String.format("Error on sending result %s", e));
        }
    }

    public static long zipDir(String sourceDirPath) throws IOException {
        String zipFilePath = getZipFileName(sourceDirPath);
        Path p = Paths.get(zipFilePath);

        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            Path pp = Paths.get(sourceDirPath);
            Files.walk(pp)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            logger.severe(String.format("Error on zipping: %s", e));
                        }
                    });
        }
        return new File(zipFilePath).length();
    }

    public static void shutdown() {
        logger.info("Shutting down...");
        ioPool.shutdown();
        try {
            boolean done = ioPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
            if (!done) {
                ioPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            ioPool.shutdownNow();
        }
    }
}

