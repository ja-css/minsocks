package com.exper.socks.output;

import com.exper.socks.net.IPv4Address;

import java.io.IOException;

public interface SocksStreamManager {
    SocksStream openExitStreamTo(String hostname, int port) throws IOException;
    SocksStream openExitStreamTo(IPv4Address address, int port) throws IOException;
}
