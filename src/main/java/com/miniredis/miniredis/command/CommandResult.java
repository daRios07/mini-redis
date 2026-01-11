package com.miniredis.miniredis.command;

public sealed interface CommandResult permits
        CommandResult.Ok,
        CommandResult.StringResult,
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
}
