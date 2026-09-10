package caprou.app.impl.util.keyboard;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public final class ClipboardManager {
    public void copy(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        getClipboard().setContents(new StringSelection(text), null);
    }

    public String paste() {
        try {
            Object value = getClipboard().getData(DataFlavor.stringFlavor);
            return value instanceof String ? (String) value : "";
        } catch (UnsupportedFlavorException e) {
            return "";
        } catch (IOException e) {
            return "";
        }
    }

    private Clipboard getClipboard() {
        return Toolkit.getDefaultToolkit().getSystemClipboard();
    }
}
