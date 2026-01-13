package com.miniredis.miniredis.service;

import com.miniredis.miniredis.command.CommandResult;
import com.miniredis.miniredis.domain.RedisDataStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedisCommandServiceTest {

    private RedisDataStore dataStore;
    private RedisCommandService commandService;

    @BeforeEach
    void setUp() {
        dataStore = new RedisDataStore();
        commandService = new RedisCommandService(dataStore);
    }

    @AfterEach
    void tearDown() {
        dataStore.shutdown();
    }

    // ============== SET Command Tests ==============

    @Test
    @DisplayName("SET should return OK on success")
    void setSuccess() {
        CommandResult result = commandService.execute("SET mykey myvalue");
        assertInstanceOf(CommandResult.Ok.class, result);
    }

    @Test
    @DisplayName("SET with EX should return OK on success")
    void setWithExSuccess() {
        CommandResult result = commandService.execute("SET mykey myvalue EX 60");
        assertInstanceOf(CommandResult.Ok.class, result);
    }

    @Test
    @DisplayName("SET with wrong number of arguments should return error")
    void setWrongArgs() {
        CommandResult result = commandService.execute("SET mykey");
        assertInstanceOf(CommandResult.ErrorResult.class, result);
        assertTrue(result.toString().contains("wrong number of arguments"));
    }

    @Test
    @DisplayName("SET with invalid key characters should return error")
    void setInvalidKey() {
        CommandResult result = commandService.execute("SET my.key myvalue");
        assertInstanceOf(CommandResult.ErrorResult.class, result);
    }

    // ============== GET Command Tests ==============

    @Test
    @DisplayName("GET should return value when key exists")
    void getExisting() {
        commandService.execute("SET mykey myvalue");
        CommandResult result = commandService.execute("GET mykey");

        assertInstanceOf(CommandResult.StringResult.class, result);
        assertEquals("myvalue", ((CommandResult.StringResult) result).value());
    }

    @Test
    @DisplayName("GET should return nil when key doesn't exist")
    void getNonExisting() {
        CommandResult result = commandService.execute("GET nonexistent");
        assertInstanceOf(CommandResult.NilResult.class, result);
    }

    // ============== DEL Command Tests ==============

    @Test
    @DisplayName("DEL should return 1 when key is deleted")
    void delExisting() {
        commandService.execute("SET mykey myvalue");
        CommandResult result = commandService.execute("DEL mykey");

        assertInstanceOf(CommandResult.IntegerResult.class, result);
        assertEquals(1, ((CommandResult.IntegerResult) result).value());
    }

    @Test
    @DisplayName("DEL should return 0 when key doesn't exist")
    void delNonExisting() {
        CommandResult result = commandService.execute("DEL nonexistent");

        assertInstanceOf(CommandResult.IntegerResult.class, result);
        assertEquals(0, ((CommandResult.IntegerResult) result).value());
    }

    // ============== DBSIZE Command Tests ==============

    @Test
    @DisplayName("DBSIZE should return count of keys")
    void dbSize() {
        commandService.execute("SET key1 value1");
        commandService.execute("SET key2 value2");

        CommandResult result = commandService.execute("DBSIZE");

        assertInstanceOf(CommandResult.IntegerResult.class, result);
        assertEquals(2, ((CommandResult.IntegerResult) result).value());
    }

    // ============== INCR Command Tests ==============

    @Test
    @DisplayName("INCR should increment value and return new value")
    void incrSuccess() {
        commandService.execute("SET counter 5");
        CommandResult result = commandService.execute("INCR counter");

        assertInstanceOf(CommandResult.IntegerResult.class, result);
        assertEquals(6, ((CommandResult.IntegerResult) result).value());
    }

    @Test
    @DisplayName("INCR on new key should return 1")
    void incrNewKey() {
        CommandResult result = commandService.execute("INCR newcounter");

        assertInstanceOf(CommandResult.IntegerResult.class, result);
        assertEquals(1, ((CommandResult.IntegerResult) result).value());
    }

    @Test
    @DisplayName("INCR on non-integer should return error")
    void incrNonInteger() {
        commandService.execute("SET key notanumber");
        CommandResult result = commandService.execute("INCR key");

        assertInstanceOf(CommandResult.ErrorResult.class, result);
        assertTrue(result.toString().contains("not an integer"));
    }

    // ============== ZADD Command Tests ==============

    @Test
    @DisplayName("ZADD should return 1 when adding new member")
    void zaddNew() {
        CommandResult result = commandService.execute("ZADD myset 1.5 member1");

        assertInstanceOf(CommandResult.IntegerResult.class, result);
        assertEquals(1, ((CommandResult.IntegerResult) result).value());
    }

    @Test
    @DisplayName("ZADD should return 0 when updating existing member")
    void zaddUpdate() {
        commandService.execute("ZADD myset 1.5 member1");
        CommandResult result = commandService.execute("ZADD myset 2.5 member1");

        assertInstanceOf(CommandResult.IntegerResult.class, result);
        assertEquals(0, ((CommandResult.IntegerResult) result).value());
    }

    @Test
    @DisplayName("ZADD with invalid score should return error")
    void zaddInvalidScore() {
        CommandResult result = commandService.execute("ZADD myset notanumber member1");
        assertInstanceOf(CommandResult.ErrorResult.class, result);
    }

    // ============== ZCARD Command Tests ==============

    @Test
    @DisplayName("ZCARD should return cardinality")
    void zcard() {
        commandService.execute("ZADD myset 1 m1");
        commandService.execute("ZADD myset 2 m2");

        CommandResult result = commandService.execute("ZCARD myset");

        assertInstanceOf(CommandResult.IntegerResult.class, result);
        assertEquals(2, ((CommandResult.IntegerResult) result).value());
    }

    @Test
    @DisplayName("ZCARD on non-existent key should return 0")
    void zcardNonExistent() {
        CommandResult result = commandService.execute("ZCARD nonexistent");

        assertInstanceOf(CommandResult.IntegerResult.class, result);
        assertEquals(0, ((CommandResult.IntegerResult) result).value());
    }

    // ============== ZRANK Command Tests ==============

    @Test
    @DisplayName("ZRANK should return rank of member")
    void zrank() {
        commandService.execute("ZADD myset 3 three");
        commandService.execute("ZADD myset 1 one");
        commandService.execute("ZADD myset 2 two");

        CommandResult result = commandService.execute("ZRANK myset two");

        assertInstanceOf(CommandResult.IntegerResult.class, result);
        assertEquals(1, ((CommandResult.IntegerResult) result).value());
    }

    @Test
    @DisplayName("ZRANK on non-existent member should return nil")
    void zrankNonExistent() {
        commandService.execute("ZADD myset 1 one");
        CommandResult result = commandService.execute("ZRANK myset nonexistent");

        assertInstanceOf(CommandResult.NilResult.class, result);
    }

    // ============== ZRANGE Command Tests ==============

    @Test
    @DisplayName("ZRANGE should return members in order")
    void zrange() {
        commandService.execute("ZADD myset 3 three");
        commandService.execute("ZADD myset 1 one");
        commandService.execute("ZADD myset 2 two");

        CommandResult result = commandService.execute("ZRANGE myset 0 -1");

        assertInstanceOf(CommandResult.ListResult.class, result);
        var list = ((CommandResult.ListResult) result).values();
        assertEquals(3, list.size());
        assertEquals("one", list.get(0));
        assertEquals("two", list.get(1));
        assertEquals("three", list.get(2));
    }

    // ============== Unknown Command Tests ==============

    @Test
    @DisplayName("Unknown command should return error")
    void unknownCommand() {
        CommandResult result = commandService.execute("UNKNOWNCMD arg1");
        assertInstanceOf(CommandResult.ErrorResult.class, result);
        assertTrue(result.toString().contains("unknown command"));
    }

    // ============== Empty Command Tests ==============

    @Test
    @DisplayName("Empty command should return error")
    void emptyCommand() {
        CommandResult result = commandService.execute("");
        assertInstanceOf(CommandResult.ErrorResult.class, result);
    }

    @Test
    @DisplayName("Whitespace-only command should return error")
    void whitespaceCommand() {
        CommandResult result = commandService.execute("   ");
        assertInstanceOf(CommandResult.ErrorResult.class, result);
    }

}