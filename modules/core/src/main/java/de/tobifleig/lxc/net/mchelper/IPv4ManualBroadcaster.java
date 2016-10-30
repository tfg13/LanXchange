/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014 Tobias Fleig (tobifleig gmail com)
 *
 * All rights reserved.
 *
 * This file is part of LanXchange.
 *
 * LanXchange is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LanXchange is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LanXchange. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tobifleig.lxc.net.mchelper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * A MulticastHelper that emulates Multicasts by sending
 * UDP packets to all addresses within the /24 subnet.
 *
 * IPv4 only.
 *
 * @author Tobias Fleig <tobifleig@googlemail.com>
 */
public class IPv4ManualBroadcaster implements MulticastHelper {

    @Override
    public boolean supportsIPv4() {
        return true;
    }

    @Override
    public boolean supportsIPv6() {
        return false;
    }

    @Override
    public void helpIPv4(byte[] packet, NetworkInterface inter, DatagramSocket socket) {
        // get local ipv4 address of this interface
        InetAddress localAddress = null;
        Enumeration<InetAddress> inetAddresses = inter.getInetAddresses();
        while (inetAddresses.hasMoreElements()) {
            InetAddress testAddress = inetAddresses.nextElement();
            if (!testAddress.isLoopbackAddress() && testAddress instanceof Inet4Address) {
                // just pick this one
                localAddress = testAddress;
                break;
            }
        }
        // found one?
        if (localAddress != null) {
            byte[] localAddressBytes = localAddress.getAddress();
            int localSuffix = localAddressBytes[3];
            try {
                for (int i = 1; i < 255; i++) {
                    if (i == localSuffix) {
                        // don't ping self
                        continue;
                    }
                    localAddressBytes[3] = (byte) i;
                    DatagramPacket pack = new DatagramPacket(packet, packet.length, InetAddress.getByAddress(localAddressBytes), 27716);
                    socket.send(pack);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void helpIPv6(byte[] packet, NetworkInterface inter, DatagramSocket socket) {
        throw new UnsupportedOperationException("IPv4ManualBroadcaster does not support IPv6");
    }

    @Override
    public String getIdentifier() {
        return "v4_manual_broadcast";
    }
}
