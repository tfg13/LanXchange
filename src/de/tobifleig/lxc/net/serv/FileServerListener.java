/*
 * Copyright 2009, 2010, 2011, 2012, 2013 Tobias Fleig (tobifleig gmail com)
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

import de.tobifleig.lxc.data.LXCFile;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;

/**
 * Used to listen for FileServer-events
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public interface FileServerListener {

    /**
     * Called upon download-requests.
     * Connection is already established, Seeder should be startet.
     *
     * @param file the requested LXCFile
     * @param outStream the outputStream
     * @param inStream the inputStream
     * @param address the remote address
     * @param transVersion protocol version to use
     */
    public void downloadRequest(LXCFile file, ObjectOutputStream outStream, ObjectInputStream inStream, InetAddress address, int transVersion);
}
