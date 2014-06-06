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

package de.tobifleig.lxc.plaf.impl.textbased;

import de.tobifleig.lxc.data.LXCFile;
import java.util.HashMap;

/**
 * Provides a convinient number for LXCFiles and resolves that number back to the file.
 * 
 * @author Michael
 */
public class FileNumberTranslator {

    private HashMap<Integer, LXCFile> files;
    private HashMap<LXCFile, Integer> numbers;
    private int currentNumber = 0;

    public FileNumberTranslator() {
        files = new HashMap<>();
        numbers = new HashMap<>();
    }

    public LXCFile getFileForNumber(int number) {
        if (files.containsKey(number)) {
            return files.get(number);
        } else {
            return null;
        }

    }

    public int getNumberForFile(LXCFile file) {
        if (numbers.containsKey(file)) {
            return numbers.get(file);
        } else {
            numbers.put(file, currentNumber);
            files.put(currentNumber, file);
            return currentNumber++;
        }
    }
}
