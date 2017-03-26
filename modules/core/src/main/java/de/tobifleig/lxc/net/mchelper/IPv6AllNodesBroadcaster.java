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

import de.tobifleig.lxc.log.LXCLogBackend;
import de.tobifleig.lxc.log.LXCLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.NetworkInterface;
import java.net.UnknownHostException;

/**
 * Emulates Multicast by broadcasting to "all nodes" FF02::1
 * 
 * IPv6 only.
 * 
 * Android devices seem to receive this if they are active anyway.
 * When left alone, many stop receiving broadcasts.
 *
 * @author Tobias Fleig <tobifleig@googlemail.com>
 */
public class IPv6AllNodesBroadcaster implements MulticastHelper {

    private final LXCLogger logger;

    public IPv6AllNodesBroadcaster() {
        logger = LXCLogBackend.getLogger("v6all-broadcaster");
    }

    @Override
    public boolean supportsIPv4() {
        return false;
    }

    @Override
    public boolean supportsIPv6() {
        return true;
    }

    @Override
    public void helpIPv4(byte[] packet, NetworkInterface inter, DatagramSocket socket) {
        throw new UnsupportedOperationException("IPv6AllNodesBroadcaster does not support IPv4");
    }

    @Override
    public void helpIPv6(byte[] packet, NetworkInterface inter, DatagramSocket socket) {
        try {
            Inet6Address adr = Inet6Address.getByAddress("FF02::1", new byte[]{(byte) 0xff, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01}, inter);
            
            DatagramPacket dpack = new DatagramPacket(packet, packet.length, adr, 27716);
            socket.send(dpack);
        } catch (IOException ex) {
            logger.warn("Unable to send allnodes-broadcast", ex);
        }
    }

    @Override
    public String getIdentifier() {
        return "v6_allnodes_broadcast";
    }
}
