package com.miniredis.miniredis.command;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CommandParser {

    private static final Pattern VALID_KEY_VALUE_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    public static List<String> parse(String commandString) {
        if (commandString == null || commandString.isBlank()) {
            throw new IllegalArgumentException("ERR empty command");
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : commandString.trim().toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }

        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("ERR empty command");
        }

        return tokens;
    }


    public static void validateKeyOrValue(String keyOrValue, String type) {
        if (keyOrValue == null || keyOrValue.isEmpty()) {
            throw new IllegalArgumentException("ERR " + type + " cannot be empty");
        }
        if (!VALID_KEY_VALUE_PATTERN.matcher(keyOrValue).matches()) {
            throw new IllegalArgumentException("ERR " + type + " can only contain [a-zA-Z0-9-_]");
        }
    }

    public static long parseLong(String longStr) {
        try {
            return Long.parseLong(longStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ERR value is not an integer or out of range");
        }
    }

}
