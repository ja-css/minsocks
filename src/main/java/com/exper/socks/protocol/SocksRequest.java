package com.exper.socks.protocol;

import com.exper.socks.config.SocksConfig;
import com.exper.socks.net.SocksRequestException;
import com.exper.socks.net.IPv4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.Socket;

public abstract class SocksRequest {
    private static final Logger logger = LoggerFactory.getLogger(SocksRequest.class);
	
    private final SocksConfig config;
    private final Socket socket;

    @Nullable private IPv4Address address;
    @Nullable private String hostname;
    private int port;

    private long lastWarningTimestamp = 0;

    protected SocksRequest(SocksConfig config, Socket socket) {
        this.config = config;
        this.socket = socket;
    }

    abstract public void readRequest() throws SocksRequestException;
    abstract public int getCommandCode();
    abstract public boolean isConnectRequest();
    public abstract void sendError(boolean isUnsupportedCommand) throws SocksRequestException;
    public abstract void sendSuccess() throws SocksRequestException;
    public abstract void sendConnectionRefused() throws SocksRequestException;

    public int getPort() {
        return port;
    }

    @Nullable
    public IPv4Address getAddress() {
        return address;
    }

    @Nullable
    public String getHostname() {
        return hostname;
    }

    public String getTarget() {
        if(config.getSafeLogging()) {
            return "[scrubbed]:"+ port;
        }
        if(hostname != null) {
            return hostname + ":" + port;
        } else {
            return address + ":" + port;
        }
    }

    protected void setPortData(byte[] data) throws SocksRequestException {
        if(data.length != 2){
            throw new SocksRequestException();
        }
        port = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
    }

    protected void setIPv4AddressData(byte[] data) throws SocksRequestException {
        logUnsafeSOCKS();

        if(data.length != 4)
            throw new SocksRequestException();

        int addressValue = 0;
        for(byte b: data) {
            addressValue <<= 8;
            addressValue |= (b & 0xFF);
        }
        address = new IPv4Address(addressValue);
    }

    private boolean testRateLimit() {
        final long now = System.currentTimeMillis();
        final long diff = now - lastWarningTimestamp;
        lastWarningTimestamp = now;
        return diff > 5000;
    }

    private void logUnsafeSOCKS() throws SocksRequestException {
        if((config.getWarnUnsafeSocks() || config.getSafeSocks()) && testRateLimit()) {
            logger.warn("Your application is giving Orchid only "+
                        "an IP address.  Applications that do DNS "+
                        "resolves themselves may leak information. "+
                        "Consider using Socks4a (e.g. via privoxy or socat) "+
                        "instead.  For more information please see "+
                        "https://wiki.torproject.org/TheOnionRouter/TorFAQ#SOCKSAndDNS");
        }
        if(config.getSafeSocks()) {
            throw new SocksRequestException("Rejecting unsafe SOCKS request");
        }		
    }

    protected void setHostname(String name) {
        hostname = name;
    }

    protected byte[] readPortData() throws SocksRequestException {
        final byte[] data = new byte[2];
        readAll(data, 0, 2);
        return data;
    }

    protected byte[] readIPv4AddressData() throws SocksRequestException {
        final byte[] data = new byte[4];
        readAll(data);
        return data;
    }

    protected byte[] readIPv6AddressData() throws SocksRequestException {
        final byte[] data = new byte[16];
        readAll(data);
        return data;
    }

    protected String readNullTerminatedString() throws SocksRequestException {
        try {
            final StringBuilder sb = new StringBuilder();
            while(true) {
                final int c = socket.getInputStream().read();
                if(c == -1){
                    throw new SocksRequestException();
                }
                if(c == 0){
                    return sb.toString();
                }
                char ch = (char) c;
                sb.append(ch);
            }
        } catch (IOException e) {
            throw new SocksRequestException(e);
        }
    }

    protected int readByte() throws SocksRequestException {
        try {
            final int n = socket.getInputStream().read();
            if(n == -1){
                throw new SocksRequestException();
            }
            return n;
        } catch (IOException e) {
            throw new SocksRequestException(e);
        }
    }

    protected void readAll(byte[] buffer) throws SocksRequestException {
        readAll(buffer, 0, buffer.length);
    }

    protected void readAll(byte[] buffer, int offset, int length) throws SocksRequestException {
        try {
            while(length > 0) {
                int n = socket.getInputStream().read(buffer, offset, length);
                if(n == -1){
                    throw new SocksRequestException();
                }
                offset += n;
                length -= n;
            }
        } catch (IOException e) {
            throw new SocksRequestException(e);
        }
    }

    protected void socketWrite(byte[] buffer) throws SocksRequestException {
        try {
            socket.getOutputStream().write(buffer);
        } catch(IOException e) {
            throw new SocksRequestException(e);
        }
    }
}
