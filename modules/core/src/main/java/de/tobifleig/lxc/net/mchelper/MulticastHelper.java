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

import java.net.DatagramSocket;
import java.net.NetworkInterface;

/**
 * Interface for Multicast Helpers.
 *
 * MulticastHelpers are used on platforms where regular multicasts do
 * not work reliably (enough) to provide useful device detection.
 *
 * MulticastHelpers enmulate multicast-messages by sending direct UDP
 * packets to certain addresses. How these addresses are determined
 * is entirely up to the helper implementation.
 *
 * Some helpers may use other techniques to get a list of potential
 * targets, or even send packets to all devices in a limited subnet.
 *
 * @author Tobias Fleig <tobifleig@googlemail.com>
 */
public interface MulticastHelper {

    /**
     * Whether this helper supports IPv4.
     *
     * @return true, if IPv4 supported, false otherwise
     */
    public boolean supportsIPv4();

    /**
     * Whether this helper supports IPv6.
     *
     * @return true, if IPv6 supported, false otherwise
     */
    public boolean supportsIPv6();

    /**
     * Helps sending the given packet over the given socket.
     * Will not be called if supportsIPv4() returns false
     *
     * @param packet outgoing data
     * @param inter the network interface
     * @param socket the outgoing socket
     */
    public void helpIPv4(byte[] packet, NetworkInterface inter, DatagramSocket socket);

    /**
     * Helps sending the given packet over the given socket.
     * Will not be called if supportsIPv6() return false
     *
     * @param packet outgoing data
     * @param inter the network interface
     * @param socket the outgoing socket
     */
    public void helpIPv6(byte[] packet, NetworkInterface inter, DatagramSocket socket);

    /**
     * Returns the identifier of this helper.
     * Every helper has a unique identifier.
     *
     * Platforms can be queued for a list of required identifiers.
     *
     * @return the identifier
     */
    public String getIdentifier();
}
