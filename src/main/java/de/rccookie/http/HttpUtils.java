package de.rccookie.http;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

final class HttpUtils {

    private HttpUtils() { }


    public static final DecimalFormat NO_SCIENTIFIC_FLOATS = new DecimalFormat("0",DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    static {
        NO_SCIENTIFIC_FLOATS.setMaximumFractionDigits(340);
    }
}
