/*
 * Copyright 2009, 2010, 2011, 2012 Tobias Fleig (tobifleig gmail com)
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
package de.tobifleig.lxc.net.serv;

import java.net.InetAddress;

/**
 * Used to listen for PingServer-events
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public interface PingServerListener {
    
    /**
     * Called, when a ping is received.
     * @param data the data contained
     * @param host the origin of this ping
     */
    public void pingReceived(byte[] data, InetAddress host);
}
