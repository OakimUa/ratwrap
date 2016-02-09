package de.zalando.mass.ratwrap.util;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.regex.Pattern;

@Ignore
public class UtilTest {
    @Test
    public void testPath() throws Exception {
        String path = "/api/sites/erfurt/locations/BC-12345";
        String pathDef = "/{node}/sites/{site}/locations/{barcode}";
        final Pattern pattern = toRegexp(pathDef);
//        final Pattern pattern = Pattern.compile("/api/sites/[a-zA-Z0-9_-]*/locations/[a-zA-Z0-9_-]*");
        System.out.println(pattern.matcher(path).matches());
        System.out.println(Arrays.toString(pattern.split(path)));

        System.out.println(extract("node", pathDef, path));
        System.out.println(extract("site", pathDef, path));
        System.out.println(extract("barcode", pathDef, path));
    }

    private String extract(String var, String pathDef, String path) {
        final String[] split = pathDef.split("\\{" + var + "\\}");
        String res = path;
        for (int i=0; i<split.length; i++) {
            res = res.replaceFirst(toRegexpStr(split[i]), "");
        }
        return res;
    }

    @Test
    public void testPatternEquals() throws Exception {
        System.out.println(toRegexp("/api/sites/{site}/locations/{barcode}").pattern().equals(toRegexp("/api/sites/{site}/locations/{barcode}").pattern()));

    }

    private Pattern toRegexp(String pathDef) {
        return Pattern.compile(toRegexpStr(pathDef));
    }

    private String toRegexpStr(String pathDef) {
//        if (!pathDef.contains("{"))
            return pathDef;
//        final String s = pathDef.replaceAll("\\}\\{", "");
//        final String[] split = s.split("(\\{|\\})");
//        int seed = s.startsWith("{") ? 1 : 0;
//        for (int i = seed; i < split.length; i++) {
//            if (i % 2 != 0) {
//                split[i] = "[a-zA-Z0-9_-]*";
//            }
//        }
//        return String.join("", split);
    }
}
