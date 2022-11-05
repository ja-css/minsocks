package com.exper.socks;

import com.exper.socks.config.SocksConfig;
import com.exper.socks.net.SocksException;
import com.exper.socks.net.SocksRequestException;
import com.exper.socks.output.SocksStream;
import com.exper.socks.output.SocksStreamManager;
import com.exper.socks.protocol.Socks4Request;
import com.exper.socks.protocol.Socks5Request;
import com.exper.socks.protocol.SocksRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

public class SocksClientTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SocksClientTask.class);
	
    private final SocksConfig config;
    private final Socket inputSocket;
    private final SocksStreamManager socksStreamManager;

    SocksClientTask(SocksConfig config, Socket inputSocket, SocksStreamManager socksStreamManager) {
        this.config = config;
        this.inputSocket = inputSocket;
        this.socksStreamManager = socksStreamManager;
    }

    @Override
    public void run() {
        final int version = readByte();
        dispatchRequest(version);
        closeSocket();
    }

    private int readByte() {
        try {
            return inputSocket.getInputStream().read();
        } catch (IOException e) {
            logger.warn("IO error reading version byte: "+ e.getMessage());
            return -1;
        }
    }

    private void dispatchRequest(int versionByte) {
        switch(versionByte) {
        case 'H':
        case 'G':
        case 'P':
            sendHttpPage();
            break;
        case 4:
            processRequest(new Socks4Request(config, inputSocket));
            break;
        case 5:
            processRequest(new Socks5Request(config, inputSocket));
            break;
        default:
            // fall through, do nothing
            break;
        }	
    }

    private void processRequest(SocksRequest request) {
        try {
            request.readRequest();
            if(!request.isConnectRequest()) {
                logger.warn("Non connect command ("+ request.getCommandCode() + ")");
                request.sendError(true);
                return;
            }

            try {
                final SocksStream stream = openConnectStream(request);
                logger.debug("SOCKS CONNECT to "+ request.getTarget()+ " completed");
                request.sendSuccess();
                runOpenConnection(stream);
            } catch (InterruptedException e) {
                logger.info("SOCKS CONNECT to "+ request.getTarget() + " was thread interrupted");
                Thread.currentThread().interrupt();
                request.sendError(false);
            } catch (TimeoutException e) {
                logger.info("SOCKS CONNECT to "+ request.getTarget() + " timed out");
                request.sendError(false);
            } catch (Exception e) {
                logger.info("SOCKS CONNECT to "+ request.getTarget() + " failed: "+ e.getMessage());
                request.sendConnectionRefused();
            }
        } catch (SocksRequestException e) {
            logger.warn("Failure reading SOCKS request: "+ e.getMessage());
            try {
                request.sendError(false);
                inputSocket.close();
            } catch (Exception ignore) {
                //swallow
            }
        } 
    }

    private void runOpenConnection(SocksStream stream) throws IOException {
        SocksStreamConnection.runConnection(inputSocket, stream);
    }

    private SocksStream openConnectStream(SocksRequest request) throws InterruptedException, TimeoutException, IOException {
        if (request.getHostname() != null) {
            logger.debug("SOCKS CONNECT request to "+ request.getHostname() +":"+ request.getPort());
            return socksStreamManager.openExitStreamTo(request.getHostname(), request.getPort());
        } else if (request.getAddress() != null) {
            logger.debug("SOCKS CONNECT request to "+ request.getAddress() +":"+ request.getPort());
            return socksStreamManager.openExitStreamTo(request.getAddress(), request.getPort());
        } else {
            throw new SocksException("Request should have either address or hostname");
        }
    }

    private void sendHttpPage() {
        throw new SocksException("Returning HTTP page not implemented");
    }

    private void closeSocket() {
        try {
            inputSocket.close();
        } catch (Exception e) {
            logger.warn("Error closing SOCKS socket: "+ e.getMessage());
        }
    }
}
