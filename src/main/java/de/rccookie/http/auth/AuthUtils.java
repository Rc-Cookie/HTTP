package de.rccookie.http.auth;

import java.util.Map;

import de.rccookie.util.BoolWrapper;
import org.jetbrains.annotations.Contract;

final class AuthUtils {

    private AuthUtils() { }


    static String toString(String scheme, String token, Map<String, String> params) {
        StringBuilder str = new StringBuilder();
        str.append(scheme);

        if(token != null)
            return str.append(' ').append(token).toString();

        BoolWrapper first = new BoolWrapper(true);
        params.forEach((k,v) -> {
            if(!first.value)
                str.append(',');
            else first.value = false;
            str.append(' ');
            str.append(k.toLowerCase());
            str.append('=');
            enquote(v, str);
        });
        return str.toString();
    }

    static void enquote(String s, StringBuilder out) {
        out.append('"');
        for(int i=0, len=s.length(); i<len; i++) {
            char c = s.charAt(i);
            if(c == '"')
                out.append("\\\"");
            else if(Character.isWhitespace(c))
                out.append(' ');
            else out.append(c);
        }
        out.append('"');
    }

    @Contract("null->null;!null->!null")
    static String decode(String escaped) {
        if(escaped == null)
            return null;

        int len = escaped.length();
        if(escaped.length() < 2)
            return escaped;

        char quote = escaped.charAt(0);
        if(quote != '"' && quote != '\'' || escaped.charAt(len-1) != quote)
            return escaped;

        StringBuilder str = new StringBuilder(escaped.length());
        for(int i=1; i<len-1; i++) {
            char c = escaped.charAt(i);
            if(c == '\\' && ++i != len - 1)
                c = str.charAt(i);
            str.append(c);
        }
        return str.toString();
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
