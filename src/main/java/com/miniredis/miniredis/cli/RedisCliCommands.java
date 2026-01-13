package com.miniredis.miniredis.cli;


import com.miniredis.miniredis.command.CommandResult;
import com.miniredis.miniredis.service.RedisCommandService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;


@ShellComponent
public class RedisCliCommands {

    private final RedisCommandService commandService;


    public RedisCliCommands(RedisCommandService commandService) {
        this.commandService = commandService;
    }

    @ShellMethod(value = "Execute a raw Redis command", key = "exec")
    public String exec(@ShellOption(help = "The Redis command to execute") String command) {
        CommandResult result = commandService.execute(command);
        return result.toString();
    }

    @ShellMethod(value = "SET key value [EX seconds]", key = {"set", "SET"})
    public String set(
            @ShellOption(help = "The key") String key,
            @ShellOption(help = "The value") String value,
            @ShellOption(value = {"--ex"}, defaultValue = "0", help = "Expiry in seconds") long ex) {
        String command = ex > 0
                ? String.format("SET %s %s EX %d", key, value, ex)
                : String.format("SET %s %s", key, value);
        return commandService.execute(command).toString();
    }

    @ShellMethod(value = "GET key", key = {"get", "GET"})
    public String get(@ShellOption(help = "The key to retrieve") String key) {
        return commandService.execute("GET " + key).toString();
    }

    @ShellMethod(value = "DEL key", key = {"del", "DEL"})
    public String del(@ShellOption(help = "The key to delete") String key) {
        return commandService.execute("DEL " + key).toString();
    }

    @ShellMethod(value = "DBSIZE - Get the number of keys", key = {"dbsize", "DBSIZE"})
    public String dbsize() {
        return commandService.execute("DBSIZE").toString();
    }

    @ShellMethod(value = "INCR key", key = {"incr", "INCR"})
    public String incr(@ShellOption(help = "The key to increment") String key) {
        return commandService.execute("INCR " + key).toString();
    }

    @ShellMethod(value = "ZADD key score member", key = {"zadd", "ZADD"})
    public String zadd(
            @ShellOption(help = "The sorted set key") String key,
            @ShellOption(help = "The score") double score,
            @ShellOption(help = "The member") String member) {
        return commandService.execute(String.format("ZADD %s %f %s", key, score, member)).toString();
    }

    @ShellMethod(value = "ZCARD key", key = {"zcard", "ZCARD"})
    public String zcard(@ShellOption(help = "The sorted set key") String key) {
        return commandService.execute("ZCARD " + key).toString();
    }

    @ShellMethod(value = "ZRANK key member", key = {"zrank", "ZRANK"})
    public String zrank(
            @ShellOption(help = "The sorted set key") String key,
            @ShellOption(help = "The member") String member) {
        return commandService.execute("ZRANK " + key + " " + member).toString();
    }

    @ShellMethod(value = "ZRANGE key start stop", key = {"zrange", "ZRANGE"})
    public String zrange(
            @ShellOption(help = "The sorted set key") String key,
            @ShellOption(help = "Start index") int start,
            @ShellOption(help = "Stop index") int stop) {
        return commandService.execute(String.format("ZRANGE %s %d %d", key, start, stop)).toString();
    }

    @ShellMethod(value = "FLUSHALL - Clear all data", key = {"flushall", "FLUSHALL"})
    public String flushall() {
        commandService.flushAll();
        return "OK";
    }

    @ShellMethod(value = "Close application", key = {"quit","QUIT","exit", "EXIT"})
    public void exit() {
        System.exit(0);
    }

}
