/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author wkong
 */
public class TextTokenizer {

    ArrayList<String> tokens;
    String text;
    int position;
    int lastSplit;

    static Pattern splitCharPattern = Pattern.compile("^[^\\p{L}\\p{N}'’.]$"); // \u2019 also recongized as an apostrophe

    enum StringStatus {

        Clean,
        NeedsSimpleFix,
        NeedsComplexFix,
        NeedsAcronymProcessing
    }

    public TextTokenizer() {
        tokens = new ArrayList<>();
    }

    public List<String> tokenize(String text) {
        reset();
        assert (text != null);
        this.text = text;

        for (; position < text.length(); position++) {
            char c = text.charAt(position);

            if (isSplitChar(c)) {
                onSplit();
            }
        }

        onSplit();

        return new ArrayList<>(tokens);
    }

    private void reset() {
        tokens.clear();
        position = 0;
        lastSplit = -1;
    }

    private static boolean isSplitChar(char c) {
        Matcher matcher = splitCharPattern.matcher(String.valueOf(c));
        return matcher.find();
    }

    private void onSplit() {
        if (position - lastSplit > 1) {
            int start = lastSplit + 1;
            String token = text.substring(start, position);
            StringStatus status = checkTokenStatus(token);

            switch (status) {
                case NeedsSimpleFix:
                    token = tokenSimpleFix(token);
                    break;

                case NeedsComplexFix:
                    token = tokenComplexFix(token);
                    break;

                case NeedsAcronymProcessing:
                    tokenAcronymProcessing(token, start, position);
                    break;

                case Clean:
                    // do nothing
                    break;
            }

            if (status != StringStatus.NeedsAcronymProcessing) {
                addToken(token, start, position);
            }
        }
        lastSplit = position;
    }

    /**
     * Adds a token to the document object. This method currently drops tokens
     * longer than 100 bytes long right now.
     *
     * @param token The token to add.
     * @param start The starting byte offset of the token in the document text.
     * @param end The ending byte offset of the token in the document text.
     */
    protected void addToken(final String token, int start, int end) {
        final int maxTokenLength = 100;
        // zero length tokens aren't interesting
        if (token.length() <= 0) {
            return;
        }
        // we want to make sure the token is short enough that someone
        // might actually type it.  UTF-8 can expand one character to 6 bytes.
        if (token.length() > maxTokenLength / 6
                && Utility.fromString(token).length >= maxTokenLength) {
            return;
        }
        tokens.add(token);
    }

    /**
     * This method scans the token, looking for uppercase characters and special
     * characters. If the token contains only numbers and lowercase letters, it
     * needs no further processing, and it returns Clean. If it also contains
     * uppercase letters or apostrophes, it returns NeedsSimpleFix. If it
     * contains special characters (especially Unicode characters), it returns
     * NeedsComplexFix. Finally, if any periods are present, this returns
     * NeedsAcronymProcessing.
     */
    private StringStatus checkTokenStatus(final String token) {
        StringStatus status = StringStatus.Clean;
        char[] chars = token.toCharArray();

        for (char c : chars) {
            boolean isAsciiLowercase = (c >= 'a' && c <= 'z');
            boolean isAsciiNumber = (c >= '0' && c <= '9');

            if (isAsciiLowercase || isAsciiNumber) {
                continue;
            }
            boolean isAsciiUppercase = (c >= 'A' && c <= 'Z');
            boolean isPeriod = (c == '.');
            boolean isApostrophe = isApostrophe(c);

            if ((isAsciiUppercase || isApostrophe) && status == StringStatus.Clean) {
                status = StringStatus.NeedsSimpleFix;
            } else if (!isPeriod) {
                status = StringStatus.NeedsComplexFix;
            } else {
                status = StringStatus.NeedsAcronymProcessing;
                break;
            }
        }

        return status;
    }

    public static boolean isApostrophe(char c) {
        return (c == '\'' || c == '’');
    }

    /**
     * If the text will be split at the position. [0, position-1], [position,
     * size).
     *
     * @param text
     * @param position
     * @return
     */
    public static boolean isSplitPoint(String text, int position) {
        if (position > 0 && position < text.length()) {
            // text = ... c1 c2 ...
            char c1 = text.charAt(position - 1);
            char c2 = text.charAt(position);

            if (isSplitChar(c1) || isSplitChar(c2)) {
                // either c1, c2 is a split char then this is a split point.
                return true;
            } else if (c1 == '.' || c2 == '.') {
                // if the two parts are connected by a period.
                // Check if they are acronymConnected, meaning 
                // they are parts of a acronym and should be connected.

                // get the token that spans the period
                int begin = position - 2;
                for (; begin >= 0; begin--) {
                    if (isSplitChar(text.charAt(begin))) {
                        break;
                    }
                }
                begin++;

                int end = position + 1;
                for (; end < text.length(); end++) {
                    if (isSplitChar(text.charAt(end))) {
                        break;
                    }
                }
                return !isAcronymConnected(text.substring(begin, end));
            } else {
                return false;
            }

        }

        return true;
    }

    /**
     * Scans through the token, removing apostrophes and converting uppercase to
     * lowercase letters.
     *
     * @param token
     * @return
     */
    protected static String tokenSimpleFix(String token) {
        char[] chars = token.toCharArray();
        int j = 0;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            boolean isAsciiUppercase = (c >= 'A' && c <= 'Z');
            boolean isApostrophe = isApostrophe(c);

            if (isAsciiUppercase) {
                chars[j] = (char) (chars[i] + 'a' - 'A');
            } else if (isApostrophe) {
                // it's an apostrophe, skip it
                j--;
            } else {
                chars[j] = chars[i];
            }

            j++;
        }

        token = new String(chars, 0, j);
        return token;
    }

    protected static String tokenComplexFix(String token) {
        token = tokenSimpleFix(token);
        token = token.toLowerCase();

        return token;
    }

    /**
     * If the token which contains a period is connected or split during
     * tokenization. Should be consistent with tokenAcronymProcessing.
     *
     * @param token
     * @return
     */
    private static boolean isAcronymConnected(String token) {
        // remove start and ending periods
        while (token.startsWith(".")) {
            token = token.substring(1);
        }

        while (token.endsWith(".")) {
            token = token.substring(0, token.length() - 1);
        }

        // does the token have any periods left?
        if (token.indexOf('.') >= 0) {
            // is this an acronym?  then there will be periods
            // at odd positions
            for (int pos = 1; pos < token.length(); pos += 2) {
                if (token.charAt(pos) != '.') {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * This method does three kinds of processing:
     * <ul>
     * <li>If the token contains periods at the beginning or the end, they are
     * removed.</li>
     * <li>If the token contains single letters followed by periods, such as
     * I.B.M., C.I.A., or U.S.A., the periods are removed.</li>
     * <li>If, instead, the token contains longer strings of text with periods
     * in the middle, the token is split into smaller tokens ("umass.edu"
     * becomes {"umass", "edu"}). Notice that this means ("ph.d." becomes {"ph",
     * "d"}).</li>
     * </ul>
     *
     * @param token
     * @param start
     * @param end
     */
    protected void tokenAcronymProcessing(String token, int start, int end) {
        token = tokenComplexFix(token);

        // remove start and ending periods
        while (token.startsWith(".")) {
            token = token.substring(1);
            start = start + 1;
        }

        while (token.endsWith(".")) {
            token = token.substring(0, token.length() - 1);
            end -= 1;
        }

        // does the token have any periods left?
        if (token.indexOf('.') >= 0) {
            // is this an acronym?  then there will be periods
            // at odd positions:
            boolean isAcronym = token.length() > 0;
            for (int pos = 1; pos < token.length(); pos += 2) {
                if (token.charAt(pos) != '.') {
                    isAcronym = false;
                }
            }

            if (isAcronym) {
                token = token.replace(".", "");
                addToken(token, start, end);
            } else {
                int s = 0;
                for (int e = 0; e < token.length(); e++) {
                    if (token.charAt(e) == '.') {
                        if (e - s > 1) {
                            String subtoken = token.substring(s, e);
                            addToken(subtoken, start + s, start + e);
                        }
                        s = e + 1;
                    }
                }

                if (token.length() - s >= 1) {
                    String subtoken = token.substring(s);
                    addToken(subtoken, start + s, end);
                }
            }
        } else {
            addToken(token, start, end);
        }
    }

}
