/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.utility;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author wkong
 */
public class TextProcessing {

    public static String clean(String text) {
        text = text.toLowerCase();
        // remove '
        text = text.replaceAll("'", "");
        text = text.replaceAll("’", "");
        text = text.replaceAll("[^a-z0-9]", " ");
        text = text.replaceAll("\\s+", " ");
        text = text.trim();
        return text;
    }

    public static int CountSubstring(String fullString, String subString) {
        int lastIndex = 0;
        int count = 0;
        while (lastIndex != -1) {
            lastIndex = fullString.indexOf(subString, lastIndex);
            if (lastIndex != -1) {
                count++;
            }
        }
        return count;
    }

    public static int countWords(String text) {
        return text.split("\\s+").length;
    }

    public static String join(List list, String delimiter) {
        if (list.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(list.get(0).toString());
        for (int i = 1; i < list.size(); i++) {
            sb.append(delimiter).append(list.get(i));
        }
        return sb.toString();
    }
    
    public static String join(Object [] list, String delimiter) {
        if (list.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder(list[0].toString());
        for (int i = 1; i < list.length; i++) {
            sb.append(delimiter).append(list[i]);
        }
        return sb.toString();
    }
    
    public static String join(int [] list, String delimiter) {
        if (list.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder(Integer.toString(list[0]));
        for (int i = 1; i < list.length; i++) {
            sb.append(delimiter).append(Integer.toString(list[i]));
        }
        return sb.toString();
    }

    public static int countPhraseFreq(String phrase, String textContent) {
        Pattern pattern = Pattern.compile("\\b" + phrase + "\\b");
        Matcher matcher = pattern.matcher(textContent);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}