/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package provider;

import provider.wz.OverlayWZFile;
import provider.wz.WZFiles;
import provider.wz.XMLWZFile;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public class DataProviderFactory {

    /**
     * Set {@code -Dwz-mode=binary} to read directly from {@code .wz} files
     * via libwz instead of pre-extracted XML directories.
     */
    private static final boolean USE_BINARY =
            "binary".equalsIgnoreCase(System.getProperty("wz-mode"));
    private static final Map<WZFiles, DataProvider> BINARY_PROVIDER_CACHE = new EnumMap<>(WZFiles.class);

    private static DataProvider getWZ(Path in) {
        return new XMLWZFile(in);
    }

    public static DataProvider getDataProvider(WZFiles in) {
        if (USE_BINARY) {
            synchronized (BINARY_PROVIDER_CACHE) {
                return BINARY_PROVIDER_CACHE.computeIfAbsent(in, OverlayWZFile::new);
            }
        }
        return getWZ(in.getFile());
    }
}
