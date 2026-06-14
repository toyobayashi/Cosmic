package provider.wz;

import io.github.toyobayashi.libwz.WzBinaryProperty;
import io.github.toyobayashi.libwz.WzCanvasProperty;
import io.github.toyobayashi.libwz.WzImage;
import io.github.toyobayashi.libwz.WzImageProperty;
import io.github.toyobayashi.libwz.WzObject;
import io.github.toyobayashi.libwz.WzPngProperty;
import io.github.toyobayashi.libwz.WzPropertyCollection;
import io.github.toyobayashi.libwz.WzSubProperty;
import io.github.toyobayashi.libwz.WzVectorProperty;
import io.github.toyobayashi.libwz.WzUOLProperty;
import provider.Data;
import provider.DataEntity;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * {@link Data} implementation backed by a libwz {@link WzImageProperty}
 * or {@link WzImage}.  An image is treated as a {@link DataType#PROPERTY}
 * node (like {@code <imgdir>} in XML), mirroring {@link XMLDomMapleData}.
 */
public class BinaryWZMapleData implements Data {

    private final WzObject root;

    /** Wrap a single WZ image (root of a {@code .img} entry). */
    public BinaryWZMapleData(WzImage image) {
        this.root = image;
    }

    /** Wrap a single WZ property (leaf or subtree node). */
    public BinaryWZMapleData(WzImageProperty property) {
        this.root = property;
    }

    // ---- delegation helpers -----------------------------------------------

    private boolean isImage() { return root instanceof WzImage; }

    private WzImage asImage() { return (WzImage) root; }

    private WzImageProperty asProperty() { return (WzImageProperty) root; }

    // ---- Data -------------------------------------------------------------

    @Override
    public String getName() {
        return root.getName();
    }

    @Override
    public DataType getType() {
        if (isImage()) return DataType.PROPERTY;  // image = imgdir
        return switch (asProperty().getPropertyType()) {
            case NULL   -> DataType.IMG_0x00;
            case SHORT  -> DataType.SHORT;
            case INT    -> DataType.INT;
            case FLOAT  -> DataType.FLOAT;
            case DOUBLE -> DataType.DOUBLE;
            case STRING -> DataType.STRING;
            case SUB    -> DataType.PROPERTY;
            case CANVAS -> DataType.CANVAS;
            case VECTOR -> DataType.VECTOR;
            case CONVEX -> DataType.CONVEX;
            case SOUND  -> DataType.SOUND;
            case UOL    -> DataType.UOL;
            default     -> DataType.UNKNOWN_TYPE;
        };
    }

    @Override
    public synchronized Data getChildByPath(String path) {
        if (path.startsWith("..")) {
            DataEntity parent = getParent();
            if (parent instanceof Data data) {
                int slashIdx = path.indexOf('/');
                String rest = slashIdx == -1 ? "" : path.substring(slashIdx + 1);
                return rest.isEmpty() ? data : data.getChildByPath(rest);
            }
            return null;
        }

        WzImageProperty child = isImage()
                ? asImage().getFromPath(path)
                : asProperty().getFromPath(path);
        if (child == null) return null;

        return new BinaryWZMapleData(child);
    }

    @Override
    public synchronized List<Data> getChildren() {
        List<Data> result = new ArrayList<>();
        WzPropertyCollection wpc = isImage()
                ? asImage().wzProperties()
                : asProperty().wzProperties();
        if (wpc != null) {
            for (WzImageProperty child : wpc) {
                result.add(new BinaryWZMapleData(child));
            }
        }
        return result;
    }

    @Override
    public synchronized Object getData() {
        if (isImage()) return null;  // container node, no scalar value
        return switch (asProperty().getPropertyType()) {
            case SHORT  -> asProperty().getShort();
            case INT    -> asProperty().getInt();
            case LONG   -> (int) asProperty().getLong();
            case FLOAT  -> asProperty().getFloat();
            case DOUBLE -> asProperty().getDouble();
            case STRING -> asProperty().getString();
            case UOL    -> ((WzUOLProperty)asProperty()).getValue();
            case VECTOR -> {
                WzVectorProperty vec = (WzVectorProperty) asProperty();
                yield new Point(vec.getX(), vec.getY());
            }
            case NULL, CANVAS, SUB, CONVEX, SOUND, PNG, LUA, RAW -> null;
        };
    }

    public synchronized boolean saveCanvasToFile(java.io.File file) {
        if (isImage()) return false;
        WzImageProperty prop = asProperty();
        while (prop instanceof WzSubProperty sub) {
            var children = sub.wzProperties();
            if (children != null && children.iterator().hasNext()) {
                WzImageProperty child = children.iterator().next();
                if (child instanceof WzCanvasProperty canvas) {
                    WzPngProperty png = canvas.getPngProperty();
                    return png != null && png.saveToFile(file.getAbsolutePath());
                }
                prop = child;
            } else {
                return false;
            }
        }
        if (prop instanceof WzCanvasProperty canvas) {
            WzPngProperty png = canvas.getPngProperty();
            return png != null && png.saveToFile(file.getAbsolutePath());
        }
        return false;
    }

    public synchronized byte[] getSoundData() {
        if (isImage()) return null;
        WzImageProperty prop = asProperty();
        if (prop instanceof WzBinaryProperty binary) {
            return binary.getBytes();
        }
        return null;
    }

    @Override
    public DataEntity getParent() {
        if (isImage()) return null;  // image root has no parent in Data tree
        WzObject parent = root.getParent();
        if (parent instanceof WzImageProperty parentProp) {
            return new BinaryWZMapleData(parentProp);
        }
        return null;
    }

    @Override
    public Iterator<Data> iterator() {
        return getChildren().iterator();
    }
}
