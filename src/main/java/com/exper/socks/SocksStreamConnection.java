package com.exper.socks;

import com.exper.socks.output.SocksStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocksStreamConnection {
    private static final Logger logger = LoggerFactory.getLogger(SocksStreamConnection.class);
	
    public static void runConnection(Socket inputSocket, SocksStream outputStream) {
        SocksStreamConnection ssc = new SocksStreamConnection(inputSocket, outputStream);
        ssc.run();
    }
    private final static int TRANSFER_BUFFER_SIZE = 4096;
    private final SocksStream outputStream;
    private final InputStream torInputStream;
    private final OutputStream torOutputStream;
    private final Socket inputSocket;
    private final Thread incomingThread;
    private final Thread outgoingThread;
    private final Object lock = new Object();
    private volatile boolean outgoingClosed;
    private volatile boolean incomingClosed;

    private SocksStreamConnection(Socket inputSocket, SocksStream outputStream) {
        this.inputSocket = inputSocket;
        this.outputStream = outputStream;
        torInputStream = this.outputStream.getInputStream();
        torOutputStream = this.outputStream.getOutputStream();

        incomingThread = createIncomingThread();
        outgoingThread = createOutgoingThread();
    }

    private void run() {
        incomingThread.start();
        outgoingThread.start();
        synchronized(lock) {
            while(!(outgoingClosed && incomingClosed)) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            try {
                inputSocket.close();
            } catch (Exception e) {
                logger.warn("IOException on SOCKS socket close(): "+ e.getMessage());
            }
            closeStream(torInputStream);
            closeStream(torOutputStream);
        }
    }

    private Thread createIncomingThread() {
        return new Thread(() -> {
            try {
                incomingTransferLoop();
            } catch (IOException e) {
                logger.debug("System error on incoming stream IO  "+ outputStream +" : "+ e.getMessage());
            } finally {
                synchronized(lock) {
                    incomingClosed = true;
                    lock.notifyAll();
                }
            }
        });
    }

    private Thread createOutgoingThread() {
        return new Thread(() -> {
            try {
                outgoingTransferLoop();
            } catch (IOException e) {
                logger.debug("System error on outgoing stream IO "+ outputStream +" : "+ e.getMessage());
            } finally {
                synchronized(lock) {
                    outgoingClosed = true;
                    lock.notifyAll();
                }
            }
        });
    }

    private void incomingTransferLoop() throws IOException {
        final byte[] incomingBuffer = new byte[TRANSFER_BUFFER_SIZE];
        while(true) {
            final int n = torInputStream.read(incomingBuffer);
            if(n == -1) {
                logger.debug("EOF on TOR input stream "+ outputStream);
                inputSocket.shutdownOutput();
                return;
            } else if(n > 0) {
                logger.debug("Transferring "+ n +" bytes from "+ outputStream +" to SOCKS socket");
                if(!inputSocket.isOutputShutdown()) {
                    inputSocket.getOutputStream().write(incomingBuffer, 0, n);
                    inputSocket.getOutputStream().flush();
                } else {
                    closeStream(torInputStream);
                    return;
                }
            }
        }
    }

    private void outgoingTransferLoop() throws IOException {
        final byte[] outgoingBuffer = new byte[TRANSFER_BUFFER_SIZE];
        while(true) {
            //TODO: ???
            //outputStream.waitForSendWindow();
            final int n = inputSocket.getInputStream().read(outgoingBuffer);
            if(n == -1) {
                torOutputStream.close();
                logger.debug("EOF on SOCKS socket connected to "+ outputStream);
                return;
            } else if(n > 0) {
                logger.debug("Transferring "+ n +" bytes from SOCKS socket to "+ outputStream);
                torOutputStream.write(outgoingBuffer, 0, n);
                torOutputStream.flush();
            }
        }
    }

    private void closeStream(Closeable c) {
        try {
            c.close();
        } catch (IOException e) {
            logger.warn("Close failed on "+ c + " : "+ e.getMessage());
        }	
    }
}
