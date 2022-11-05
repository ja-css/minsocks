package com.exper.socks.output;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketStream implements SocksStream {
    private final Socket socket;

    public SocketStream(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getInputStream() {
        try {
            return socket.getInputStream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OutputStream getOutputStream() {
        try {
            return socket.getOutputStream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
