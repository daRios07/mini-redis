package com.miniredis.miniredis.command;

import java.util.List;

public sealed interface CommandResult permits
        CommandResult.Ok,
        CommandResult.StringResult,
        CommandResult.IntegerResult,
        CommandResult.NilResult,
        CommandResult.ListResult,
        CommandResult.ErrorResult {

    record Ok() implements CommandResult {
        @Override
        public String toString() {
            return "OK";
        }
    }
    record StringResult(String value) implements CommandResult {
        @Override
        public String toString() {
            return value;
        }
    }

    record ErrorResult(String message) implements CommandResult {
        @Override
        public String toString() {
            return "(error) " + message;
        }
    }

    record NilResult() implements CommandResult {
        @Override
        public String toString() {
            return "(nil)";
        }
    }

    record IntegerResult(long value) implements CommandResult {
        @Override
        public String toString() {
            return "(integer) " + value;
        }
    }

    /**
     * List response (for ZRANGE)
     */
    record ListResult(List<String> values) implements CommandResult {
        @Override
        public String toString() {
            if (values.isEmpty()) {
                return "(empty list or set)";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append(i + 1).append(") \"").append(values.get(i)).append("\"");
            }
            return sb.toString();
        }

        /**
         * Space-delimited format for HTTP responses
         */
        public String toSpaceDelimited() {
            return String.join(" ", values);
        }
    }
}
