package io.github.squidecim.genialtcg.network;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class LobbyCode {

    public static String generateCode(String ip) {
        String[] parts = ip.split("\\.");
        long ipLong = (Long.parseLong(parts[0]) << 24)
            | (Long.parseLong(parts[1]) << 16)
            | (Long.parseLong(parts[2]) << 8)
            |  Long.parseLong(parts[3]);
        return Long.toString(ipLong, 36).toUpperCase();
    }

    public static String decodeCode(String code) {
        long ipLong = Long.parseLong(code.toLowerCase(), 36);
        long a = (ipLong >> 24) & 0xFF;
        long b = (ipLong >> 16) & 0xFF;
        long c = (ipLong >> 8)  & 0xFF;
        long d =  ipLong        & 0xFF;
        return a + "." + b + "." + c + "." + d;
    }

    public static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getHostAddress().contains(":")) continue;
                    return addr.getHostAddress();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }
}
