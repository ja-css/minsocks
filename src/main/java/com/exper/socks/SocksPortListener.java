package com.exper.socks;

import com.exper.socks.config.SocksConfig;
import com.exper.socks.net.SocksException;
import com.exper.socks.output.SocksStreamManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocksPortListener {
    private static final Logger logger = LoggerFactory.getLogger(SocksPortListener.class);
    private final Set<Integer> listeningPorts = new HashSet<>();
    private final Map<Integer, AcceptTask> acceptThreads = new HashMap<>();
    private final SocksConfig config;
    private final SocksStreamManager circuitManager;
    private final ExecutorService executor;
    private boolean isStopped;

    public SocksPortListener(SocksConfig config, SocksStreamManager circuitManager) {
        this.config = config;
        this.circuitManager = circuitManager;
        executor = Executors.newCachedThreadPool();
    }

    public void addListeningPort(int port) {
        if(port <= 0 || port > 65535) {
            throw new SocksException("Illegal listening port: "+ port);
        }

        synchronized(listeningPorts) {
            if(isStopped) {
                throw new IllegalStateException("Cannot add listening port because Socks proxy has been stopped");
            }
            if(listeningPorts.contains(port))
                return;
            listeningPorts.add(port);
            try {
                startListening(port);
                logger.debug("Listening for SOCKS connections on port "+ port);
            } catch (IOException e) {
                listeningPorts.remove(port);
                throw new SocksException("Failed to listen on port "+ port +" : "+ e.getMessage());
            }
        }

    }

    //@Override
    public void stop() {
        synchronized (listeningPorts) {
            for(AcceptTask t: acceptThreads.values()) {
                t.stop();
            }
            executor.shutdownNow();
            isStopped = true;
        }
    }

    private void startListening(int port) throws IOException {
        final AcceptTask task = new AcceptTask(port);
        acceptThreads.put(port, task);
        executor.execute(task);
    }

    private Runnable newClientSocket(final Socket s) {
        return new SocksClientTask(config, s, circuitManager);
    }

    private class AcceptTask implements Runnable {
        private final ServerSocket socket;
        private final int port;
        private volatile boolean stopped;

        AcceptTask(int port) throws IOException {
            this.socket = new ServerSocket(port);
            this.port = port;
        }

        void stop() {
            stopped = true;
            try {
                socket.close();
            } catch (IOException e) { 
                //swallow
            }
        }

        @Override
        public void run() {
            try {
                runAcceptLoop();
            } catch (IOException e) {
                if(!stopped) {
                    logger.warn("System error accepting SOCKS socket connections: "+ e.getMessage());
                }
            } finally {
                synchronized (listeningPorts) {
                    listeningPorts.remove(port);
                    acceptThreads.remove(port);
                }
            }
        }

        private void runAcceptLoop() throws IOException {
            while(!Thread.interrupted() && !stopped) {
                final Socket s = socket.accept();
                executor.execute(newClientSocket(s));
            }
        }
    }
}
