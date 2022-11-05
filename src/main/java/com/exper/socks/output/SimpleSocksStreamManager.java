package com.exper.socks.output;

import com.exper.socks.net.IPv4Address;

import javax.net.SocketFactory;
import java.io.IOException;

public class SimpleSocksStreamManager implements SocksStreamManager {
    @Override
    public SocksStream openExitStreamTo(String hostname, int port) throws IOException {
        return new SocketStream(SocketFactory.getDefault().createSocket(hostname, port));
    }

    @Override
    public SocksStream openExitStreamTo(IPv4Address address, int port) throws IOException {
        return new SocketStream(SocketFactory.getDefault().createSocket(address.toInetAddress(), port));
    }
}
