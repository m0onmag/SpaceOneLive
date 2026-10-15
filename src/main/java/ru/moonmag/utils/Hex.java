package ru.moonmag.utils;

import net.md_5.bungee.api.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Hex {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private Hex() {
    }

    public static String color(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.indexOf("&#") != -1) {
            Matcher matcher = HEX.matcher(text);
            StringBuffer sb = new StringBuffer(text.length() + 16);
            while (matcher.find()) {
                String replacement = ChatColor.of("#" + matcher.group(1)).toString();
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            text = sb.toString();
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String gradient(String text, String from, String to) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int start = Integer.parseInt(from.replace("#", ""), 16);
        int end = Integer.parseInt(to.replace("#", ""), 16);
        int length = text.length();
        StringBuilder sb = new StringBuilder(length * 15);
        for (int i = 0; i < length; i++) {
            double t = length == 1 ? 0 : (double) i / (length - 1);
            int r = lerp((start >> 16) & 0xFF, (end >> 16) & 0xFF, t);
            int g = lerp((start >> 8) & 0xFF, (end >> 8) & 0xFF, t);
            int b = lerp(start & 0xFF, end & 0xFF, t);
            sb.append(ChatColor.of(String.format("#%02x%02x%02x", r, g, b))).append(text.charAt(i));
        }
        return sb.toString();
    }

    private static int lerp(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }
}
