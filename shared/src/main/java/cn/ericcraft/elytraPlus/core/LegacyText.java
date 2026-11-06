package cn.ericcraft.elytraPlus.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Splits legacy section-sign colors into explicit, non-inheriting native text runs. */
public final class LegacyText {
    private LegacyText() { }
    public static final class Run {
        public final String text;
        public final String formats;
        Run(String text, String formats) { this.text = text; this.formats = formats; }
    }
    public static List<Run> parse(String text) {
        List<Run> runs = new ArrayList<>();
        StringBuilder content = new StringBuilder();
        String formats = "";
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if ("0123456789abcdefklmnor".indexOf(code) >= 0) {
                    if (content.length() > 0) { runs.add(new Run(content.toString(), formats)); content.setLength(0); }
                    if (code == 'r') formats = "";
                    else if ("0123456789abcdef".indexOf(code) >= 0) formats = String.valueOf(code);
                    else if (formats.indexOf(code) < 0) formats += code;
                    i++;
                    continue;
                }
            }
            content.append(c);
        }
        if (content.length() > 0) runs.add(new Run(content.toString(), formats));
        return Collections.unmodifiableList(runs);
    }
}
