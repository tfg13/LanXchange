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
package de.tobifleig.lxc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * This class is used to sign updates.
 * Note: For obvious reason, you cannot do that, because the private key is not distributed.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class Signer {

    /**
     * Sign the file named lxc.zip and exit.
     *
     * @param args ignored
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
	// Sign file

	KeyFactory fact = KeyFactory.getInstance("RSA");
	FileInputStream ins = new FileInputStream(new File("lxc_updates.priv"));
	byte[] b = new byte[ins.available()];
	ins.read(b);
	ins.close();
	PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec(b);
	PrivateKey priKey = fact.generatePrivate(priKeySpec);

	Signature sign = Signature.getInstance("SHA256withRSA");
	sign.initSign(priKey);

	FileInputStream in = new FileInputStream("lxc.zip");
	int bufSize = 1024;
	byte[] buffer = new byte[bufSize];
	int n = in.read(buffer, 0, bufSize);
	while (n != -1) {
	    sign.update(buffer, 0, n);
	    n = in.read(buffer, 0, bufSize);
	}
	in.close();
	FileOutputStream out = new FileOutputStream("lxc.sign");
	byte[] signb = sign.sign();
	out.write(signb);
	out.close();
    }
}
