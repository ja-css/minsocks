package com.exper.socks;

import com.exper.socks.config.SocksConfig;
import com.exper.socks.out.SimpleSocksStreamManager;
import com.exper.socks.out.SocksStreamManager;

public class SocksMain {
    public static void main(String[] args) {
        SocksConfig config = new SocksConfig() {
            @Override public boolean getWarnUnsafeSocks() { return true; }
            @Override public boolean getSafeSocks() { return false; }
            @Override public boolean getSafeLogging() { return false; }
        };

        SocksStreamManager manager = new SimpleSocksStreamManager();

        SocksPortListener socksListener = new SocksPortListener(config, manager);
        socksListener.addListeningPort(9150);
    }
}
