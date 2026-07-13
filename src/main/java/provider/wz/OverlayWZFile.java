package provider.wz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.Data;
import provider.DataDirectoryEntry;
import provider.DataEntity;
import provider.DataProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Binary WZ provider that applies the same runtime patch order as kaentake:
 * Locale > Custom > original WZ.
 */
public class OverlayWZFile implements DataProvider {
    private static final Logger log = LoggerFactory.getLogger(OverlayWZFile.class);

    private final WZFiles wzFile;
    private final DataProvider original;
    private final DataProvider custom;
    private final DataProvider locale;

    public OverlayWZFile(WZFiles wzFile) {
        this.wzFile = wzFile;
        this.original = new BinaryWZFile(wzFile.getWzFilePath());
        this.custom = createCustomProvider();
        this.locale = createLocaleProvider(wzFile);
    }

    private static DataProvider createCustomProvider() {
        Path customPath = Path.of(WZFiles.getDirectory(), "Custom.wz");
        if (!Files.isRegularFile(customPath)) {
            return null;
        }
        log.info("Using WZ custom overlay: {}", customPath);
        return new BinaryWZFile(customPath.toString());
    }

    private static DataProvider createLocaleProvider(WZFiles wzFile) {
        String localeName = System.getProperty("wz-locale");
        if (localeName == null || localeName.isBlank()) {
            localeName = System.getProperty("locale");
        }
        if (localeName == null || localeName.isBlank()
                || "en".equalsIgnoreCase(localeName)
                || "en-US".equalsIgnoreCase(localeName)) {
            return null;
        }

        Path localePath = Path.of(WZFiles.getDirectory(), "Locale", localeName, wzFile.getFileName());
        if (!Files.isRegularFile(localePath)) {
            return null;
        }
        log.info("Using WZ locale overlay for {}: {}", wzFile.getRootName(), localePath);
        return new BinaryWZFile(localePath.toString());
    }

    @Override
    public Data getData(String path) {
        Data originalData = original.getData(path);
        Data customData = custom == null ? null : custom.getData(customPath(path));
        Data localeData = locale == null ? null : locale.getData(path);

        if (originalData == null && customData == null) {
            return null;
        }
        return OverlayData.merge(localeData, customData, originalData, null);
    }

    @Override
    public DataDirectoryEntry getRoot() {
        return original.getRoot();
    }

    private String customPath(String path) {
        return wzFile.getRootName() + "/" + path;
    }

    private static final class OverlayData implements Data {
        private final Data locale;
        private final Data custom;
        private final Data original;
        private final DataEntity parent;

        private OverlayData(Data locale, Data custom, Data original, DataEntity parent) {
            this.locale = locale;
            this.custom = custom;
            this.original = original;
            this.parent = parent;
        }

        private static Data merge(Data locale, Data custom, Data original, DataEntity parent) {
            if (locale == null && custom == null && original == null) {
                return null;
            }
            if (locale == null && custom == null && parent == null) {
                return original;
            }
            if (locale == null && original == null && parent == null) {
                return custom;
            }
            return new OverlayData(locale, custom, original, parent);
        }

        @Override
        public String getName() {
            return first().getName();
        }

        @Override
        public DataType getType() {
            return first().getType();
        }

        @Override
        public List<Data> getChildren() {
            Map<String, Data[]> children = new LinkedHashMap<>();
            addChildren(children, original, 2);
            addChildren(children, custom, 1);
            addChildren(children, locale, 0);

            List<Data> result = new ArrayList<>();
            for (Data[] layers : children.values()) {
                Data merged = merge(layers[0], layers[1], layers[2], this);
                if (merged != null) {
                    result.add(merged);
                }
            }
            return result;
        }

        private static void addChildren(Map<String, Data[]> children, Data layer, int index) {
            if (layer == null) {
                return;
            }
            for (Data child : layer.getChildren()) {
                children.computeIfAbsent(child.getName(), ignored -> new Data[3])[index] = child;
            }
        }

        @Override
        public Data getChildByPath(String path) {
            if (path == null || path.isEmpty()) {
                return this;
            }
            if (path.startsWith("..")) {
                if (parent instanceof Data parentData) {
                    int slash = path.indexOf('/');
                    return slash == -1 ? parentData : parentData.getChildByPath(path.substring(slash + 1));
                }
                return null;
            }

            int slash = path.indexOf('/');
            String childName = slash == -1 ? path : path.substring(0, slash);
            String rest = slash == -1 ? "" : path.substring(slash + 1);

            Data merged = merge(
                    child(locale, childName),
                    child(custom, childName),
                    child(original, childName),
                    this
            );
            return rest.isEmpty() || merged == null ? merged : merged.getChildByPath(rest);
        }

        private static Data child(Data parent, String childName) {
            return parent == null ? null : parent.getChildByPath(childName);
        }

        @Override
        public Object getData() {
            return first().getData();
        }

        @Override
        public DataEntity getParent() {
            return parent;
        }

        @Override
        public Iterator<Data> iterator() {
            return getChildren().iterator();
        }

        private Data first() {
            if (locale != null) {
                return locale;
            }
            if (custom != null) {
                return custom;
            }
            return original;
        }
    }
}
