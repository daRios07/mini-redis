package com.miniredis.miniredis.service;

import com.miniredis.miniredis.command.CommandResult;
import com.miniredis.miniredis.command.CommandParser;
import com.miniredis.miniredis.domain.RedisDataStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RedisCommandService {

    private final RedisDataStore dataStore;

    public RedisCommandService(RedisDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public CommandResult execute(String commandString) {
        try {
            List<String> tokens = CommandParser.parse(commandString);
            String command = tokens.get(0).toUpperCase();

            return switch (command) {
                case "SET" -> executeSet(tokens);
                case "GET" -> executeGet(tokens);
                case "DEL" -> executeDel(tokens);
                case "DBSIZE" -> executeDbSize(tokens);
                case "INCR" -> executeIncr(tokens);
                case "ZADD" -> executeZadd(tokens);
                case "ZCARD" -> executeZcard(tokens);
                case "ZRANK" -> executeZrank(tokens);
                case "ZRANGE" -> executeZrange(tokens);
                default -> new CommandResult.ErrorResult("ERR unknown command '" + command + "'");
            };
        } catch (IllegalArgumentException e) {
            return new CommandResult.ErrorResult(e.getMessage());
        }
    }


    private CommandResult executeSet(List<String> tokens) {
        if (tokens.size() < 3) {
            return new CommandResult.ErrorResult("ERR wrong number of arguments for 'SET' command");
        }

        String key = tokens.get(1);
        String value = tokens.get(2);

        CommandParser.validateKeyOrValue(key, "key");
        CommandParser.validateKeyOrValue(value, "value");

        // Check for EX option
        if (tokens.size() >= 5) {
            if (tokens.get(3).equalsIgnoreCase("EX")) {
                long seconds = CommandParser.parseLong(tokens.get(4));
                if (seconds <= 0) {
                    return new CommandResult.ErrorResult("ERR invalid expire time in 'SET' command");
                }
                dataStore.setWithExpiry(key, value, seconds);
                return new CommandResult.Ok();
            }
        } else if (tokens.size() == 3) {
            dataStore.set(key, value);
            return new CommandResult.Ok();
        }

        return new CommandResult.ErrorResult("ERR syntax error");
    }

    private CommandResult executeGet(List<String> tokens) {
        if (tokens.size() != 2) {
            return new CommandResult.ErrorResult("ERR wrong number of arguments for 'GET' command");
        }

        String key = tokens.get(1);
        CommandParser.validateKeyOrValue(key, "key");

        Optional<String> value = dataStore.get(key);
        return value
                .map(CommandResult.StringResult::new)
                .map(r -> (CommandResult) r)
                .orElse(new CommandResult.NilResult());
    }


    private CommandResult executeDel(List<String> tokens) {
        if (tokens.size() != 2) {
            return new CommandResult.ErrorResult("ERR wrong number of arguments for 'DEL' command");
        }

        String key = tokens.get(1);
        CommandParser.validateKeyOrValue(key, "key");

        boolean deleted = dataStore.delete(key);
        return new CommandResult.IntegerResult(deleted ? 1 : 0);
    }

    private CommandResult executeDbSize(List<String> tokens) {
        if (tokens.size() != 1) {
            return new CommandResult.ErrorResult("ERR wrong number of arguments for 'DBSIZE' command");
        }

        return new CommandResult.IntegerResult(dataStore.dbSize());
    }

    /**
     * INCR key
     */
    private CommandResult executeIncr(List<String> tokens) {
        if (tokens.size() != 2) {
            return new CommandResult.ErrorResult("ERR wrong number of arguments for 'INCR' command");
        }

        String key = tokens.get(1);
        CommandParser.validateKeyOrValue(key, "key");

        try {
            long newValue = dataStore.incr(key);
            return new CommandResult.IntegerResult(newValue);
        } catch (IllegalArgumentException e) {
            return new CommandResult.ErrorResult(e.getMessage());
        }
    }

    /**
     * ZADD key score member
     */
    private CommandResult executeZadd(List<String> tokens) {
        if (tokens.size() != 4) {
            return new CommandResult.ErrorResult("ERR wrong number of arguments for 'ZADD' command");
        }

        String key = tokens.get(1);
        double score = CommandParser.parseScore(tokens.get(2));
        String member = tokens.get(3);

        CommandParser.validateKeyOrValue(key, "key");
        CommandParser.validateKeyOrValue(member, "member");

        try {
            int added = dataStore.zadd(key, score, member);
            return new CommandResult.IntegerResult(added);
        } catch (IllegalArgumentException e) {
            return new CommandResult.ErrorResult(e.getMessage());
        }
    }

    /**
     * ZCARD key
     */
    private CommandResult executeZcard(List<String> tokens) {
        if (tokens.size() != 2) {
            return new CommandResult.ErrorResult("ERR wrong number of arguments for 'ZCARD' command");
        }

        String key = tokens.get(1);
        CommandParser.validateKeyOrValue(key, "key");

        try {
            int cardinality = dataStore.zcard(key);
            return new CommandResult.IntegerResult(cardinality);
        } catch (IllegalArgumentException e) {
            return new CommandResult.ErrorResult(e.getMessage());
        }
    }

    /**
     * ZRANK key member
     */
    private CommandResult executeZrank(List<String> tokens) {
        if (tokens.size() != 3) {
            return new CommandResult.ErrorResult("ERR wrong number of arguments for 'ZRANK' command");
        }

        String key = tokens.get(1);
        String member = tokens.get(2);

        CommandParser.validateKeyOrValue(key, "key");
        CommandParser.validateKeyOrValue(member, "member");

        try {
            Optional<Integer> rank = dataStore.zrank(key, member);
            return rank
                    .map(r -> (CommandResult) new CommandResult.IntegerResult(r))
                    .orElse(new CommandResult.NilResult());
        } catch (IllegalArgumentException e) {
            return new CommandResult.ErrorResult(e.getMessage());
        }
    }

    /**
     * ZRANGE key start stop
     */
    private CommandResult executeZrange(List<String> tokens) {
        if (tokens.size() != 4) {
            return new CommandResult.ErrorResult("ERR wrong number of arguments for 'ZRANGE' command");
        }

        String key = tokens.get(1);
        int start = CommandParser.parseInt(tokens.get(2));
        int stop = CommandParser.parseInt(tokens.get(3));

        CommandParser.validateKeyOrValue(key, "key");

        try {
            List<String> members = dataStore.zrange(key, start, stop);
            return new CommandResult.ListResult(members);
        } catch (IllegalArgumentException e) {
            return new CommandResult.ErrorResult(e.getMessage());
        }
    }

    public void flushAll() {
        dataStore.flushAll();
    }



}
