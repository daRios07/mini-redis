package com.miniredis.miniredis.cli;

import com.miniredis.miniredis.service.RedisCommandService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple socket server that allows CLI connections via netcat.
 * Enables: docker exec -it container redis-cli
 */
@Component
public class ShellServer {

    private static final Logger log = Logger.getLogger(ShellServer.class.getName());
    private static final int PORT = 6380;

    private final RedisCommandService commandService;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public ShellServer(RedisCommandService commandService) {
        this.commandService = commandService;
    }

    @PostConstruct
    public void start() {
        Thread thread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                log.info("Shell server listening on port " + PORT);

                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        new Thread(() -> handleClient(client)).start();
                    } catch (IOException e) {
                        if (running) log.log(Level.SEVERE, "Accept error", e);
                    }
                }
            } catch (IOException e) {
                log.log(Level.SEVERE, "Could not start shell server", e);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void handleClient(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.println("Mini Redis - Type 'help' for commands, 'exit' to quit");
            out.print("mini-redis> ");
            out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();

                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    out.println("Bye!");
                    break;
                }

                if (line.equalsIgnoreCase("help")) {
                    out.println("Commands: SET, GET, DEL, DBSIZE, INCR, ZADD, ZCARD, ZRANK, ZRANGE");
                } else if (!line.isEmpty()) {
                    out.println(commandService.execute(line));
                }

                out.print("mini-redis> ");
                out.flush();
            }
        } catch (IOException e) {
            // Client disconnected
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
    }
}