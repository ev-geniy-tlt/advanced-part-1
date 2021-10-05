package com.java.advanced;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;
import java.util.regex.Matcher;

public class Client {

    private static final Logger logger = Logger.getLogger(Client.class.getName());

    public static void main(String[] args) {
        int port = getPort(args[0]);
        try {
            Socket socket = new Socket("localhost", Server.SOCKET_PORT);
            logger.info("Client connected");

            try (PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
                logger.info("Sending command...");
                out.println(args[0]);
            }
            socket.close();
            ServerSocket serverSocket = new ServerSocket(port);
            logger.info(String.format("Waiting result on port %s ...", port));
            final Socket result = serverSocket.accept();
            logger.info("Result port connected");
            try (BufferedReader in = new BufferedReader(new InputStreamReader(result.getInputStream()))) {
                String serverResult = in.readLine();
                logger.info(String.format("Result: %s", serverResult));
            }
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int getPort(String arg) {
        Matcher matcher = Server.commandPattern.matcher(arg);
        if (!matcher.matches()) {
            throw new IllegalStateException("Specify the result port, e.g. result_port=5555");
        }
        return Integer.parseInt(matcher.group(3));
    }
}