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
package de.tobifleig.lxc;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Represents the configuration.
 *
 * backed by HashMap<String, String>.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class Configuration {

    /**
     * the backing Hashmap
     */
    private static HashMap<String, String> map = new HashMap<String, String>();
    
    /**
     * private constructor
     */
    private Configuration() {
    }

    /**
     * Reads a Setting, returns its value as long.
     * @param setting the setting to read
     * @return the saved value, or 0 (default)
     */
    public static long getLongSetting(String setting) {
        long ret = 0;
        try {
            ret = Long.parseLong(map.get(setting).toString());
        } catch (Exception ex) {
        }
        return ret;
    }

    /**
     * Reads a Settings, returns its value as String
     * @param setting the setting to read
     * @return the saved value, or null (default)
     */
    public static String getStringSetting(String setting) {
        String ret = null;
        try {
            ret = map.get(setting).toString();
        } catch (Exception ex) {
        }
        return ret;
    }
    
    /**
     * Puts a long-setting.
     * overrides previous stored settings
     * @param setting the key
     * @param value the value
     */
    public static void putLongSetting(String setting, long value) {
        putStringSetting(setting, String.valueOf(value));
    }
    
    /**
     * Puts a String-setting.
     * overrides previous stored settings
     * @param setting the key
     * @param value the value
     */
    public static void putStringSetting(String setting, String value) {
        map.put(setting, value);
    }
    
    /**
     * Returns true, if the given setting is contained in this configuration.
     * @param setting the key
     * @return true, if set
     */
    public static boolean containsKey(String setting) {
        return map.containsKey(setting);
    }

    /**
     * Returns an Iterator over all keys contained in this configuration.
     * @return an Iterator over all keys contained in this configuration
     */
    public static Iterator<String> getKeyIterator() {
        return map.keySet().iterator();
    }
}
