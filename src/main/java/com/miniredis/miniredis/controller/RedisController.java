package com.miniredis.miniredis.controller;

import com.miniredis.miniredis.command.CommandResult;
import com.miniredis.miniredis.service.RedisCommandService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class RedisController {

    private final RedisCommandService commandService;

    public RedisController(RedisCommandService commandService) {
        this.commandService = commandService;
    }

    /**
     * Query parameter style endpoint
     * Example: GET /?cmd=SET%20mykey%20myvalue
     */
    @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> executeCommand(@RequestParam("cmd") String cmd) {
        CommandResult result = commandService.execute(cmd);
        return formatResponse(result);
    }

    /**
     * REST-style GET - retrieves a key value
     * Example: GET /mykey
     */
    @GetMapping(value = "/{key}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getValue(@PathVariable String key) {
        CommandResult result = commandService.execute("GET " + key);
        return formatResponse(result);
    }

    /**
     * REST-style PUT - sets a key value
     * Example: PUT /mykey with body "myvalue"
     */
    @PutMapping(value = "/{key}",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> setValue(@PathVariable String key, @RequestBody String value) {
        CommandResult result = commandService.execute("SET " + key + " " + value.trim());
        return formatResponse(result);
    }

    /**
     * REST-style PUT with expiry
     * Example: PUT /mykey?ex=60 with body "myvalue"
     */
    @PutMapping(value = "/{key}",
            params = "ex",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> setValueWithExpiry(
            @PathVariable String key,
            @RequestParam("ex") long expirySeconds,
            @RequestBody String value) {
        CommandResult result = commandService.execute("SET " + key + " " + value.trim() + " EX " + expirySeconds);
        return formatResponse(result);
    }

    /**
     * REST-style DELETE - deletes a key
     * Example: DELETE /mykey
     */
    @DeleteMapping(value = "/{key}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deleteKey(@PathVariable String key) {
        CommandResult result = commandService.execute("DEL " + key);
        return formatResponse(result);
    }

    /**
     * DBSIZE endpoint
     */
    @GetMapping(value = "/db/size", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> dbSize() {
        CommandResult result = commandService.execute("DBSIZE");
        return formatResponse(result);
    }

    /**
     * INCR endpoint
     */
    @PostMapping(value = "/{key}/incr", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> incr(@PathVariable String key) {
        CommandResult result = commandService.execute("INCR " + key);
        return formatResponse(result);
    }

    /**
     * ZADD endpoint
     * Example: POST /myset/zadd with body "1.0 member1"
     */
    @PostMapping(value = "/{key}/zadd",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> zadd(@PathVariable String key, @RequestBody String scoreAndMember) {
        CommandResult result = commandService.execute("ZADD " + key + " " + scoreAndMember.trim());
        return formatResponse(result);
    }

    /**
     * ZCARD endpoint
     */
    @GetMapping(value = "/{key}/zcard", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> zcard(@PathVariable String key) {
        CommandResult result = commandService.execute("ZCARD " + key);
        return formatResponse(result);
    }

    /**
     * ZRANK endpoint
     */
    @GetMapping(value = "/{key}/zrank/{member}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> zrank(@PathVariable String key, @PathVariable String member) {
        CommandResult result = commandService.execute("ZRANK " + key + " " + member);
        return formatResponse(result);
    }

    /**
     * ZRANGE endpoint
     */
    @GetMapping(value = "/{key}/zrange", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> zrange(
            @PathVariable String key,
            @RequestParam("start") int start,
            @RequestParam("stop") int stop) {
        CommandResult result = commandService.execute("ZRANGE " + key + " " + start + " " + stop);
        return formatResponse(result);
    }

    /**
     * Format the command result for HTTP response
     */
    private ResponseEntity<String> formatResponse(CommandResult result) {
        return switch (result) {
            case CommandResult.Ok ok -> ResponseEntity.ok("OK");
            case CommandResult.StringResult sr -> ResponseEntity.ok(sr.value());
            case CommandResult.IntegerResult ir -> ResponseEntity.ok(String.valueOf(ir.value()));
            case CommandResult.NilResult nr -> ResponseEntity.ok("(nil)");
            case CommandResult.ListResult lr -> ResponseEntity.ok(lr.toSpaceDelimited());
            case CommandResult.ErrorResult er -> ResponseEntity.badRequest().body(er.message());
        };
    }
}
