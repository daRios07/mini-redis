# Mini Redis

A lightweight, thread-safe implementation of Redis core commands using Java 21 and Spring Boot. This project implements a subset of Redis commands with full atomicity guarantees, HTTP REST API, and CLI support.

## Features

- **Core Redis Commands**: SET, GET, DEL, DBSIZE, INCR, ZADD, ZCARD, ZRANK, ZRANGE
- **Key Expiration**: SET with EX seconds option
- **Thread-Safe**: All operations are atomic with proper locking
- **HTTP API**: Query parameter and REST-style endpoints
- **CLI Interface**: Interactive shell for direct command execution
- **Docker Support**: Run in containers with docker-compose

## Implemented Commands

| Command | Description |
|---------|-------------|
| `SET key value` | Set the string value of a key |
| `SET key value EX seconds` | Set value with expiration time |
| `GET key` | Get the value of a key |
| `DEL key` | Delete a key |
| `DBSIZE` | Return the number of keys in the database |
| `INCR key` | Increment the integer value of a key by 1 |
| `ZADD key score member` | Add a member with score to a sorted set |
| `ZCARD key` | Get the number of members in a sorted set |
| `ZRANK key member` | Get the rank of a member in a sorted set |
| `ZRANGE key start stop` | Get a range of members from a sorted set by index |

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker (optional)
- netcat (Optional for CLI on local)

### Build

```bash
# Build the project
mvn clean package

# Run tests
mvn test

# Run integration tests
mvn verify
```


### Run Locally

```bash
# Using Maven
mvn spring-boot:run

# Using JAR
java -jar target/mini-redis-1.0.0.jar
```

```# Test CLI locally on windows
# download and install netcat 
# https://nmap.org/download.html

ncat --version

ncat 127.0.0.1 6380

```


### Docker
```bash
docker stop mini-redis && docker rm mini-redis
docker build -t mini-redis .
docker run -d -p 8080:8080 --name mini-redis mini-redis
docker exec -it mini-redis redis-cli
```

### Docker compose
```bash
# build image and run container
docker-compose up -d --build

# CLI (for  other terminals)
docker exec -it mini-redis redis-cli

# HTTP
curl "localhost:8080/?cmd=SET%20key%20value"

# Stop 
docker-compose down
```

## HTTP API Usage

### Query Parameter Style

```bash
# SET
curl "localhost:8080/?cmd=SET%20mykey%20cool-value"
# Output: OK

# GET
curl "localhost:8080/?cmd=GET%20mykey"
# Output: cool-value

# DEL
curl "localhost:8080/?cmd=DEL%20mykey"
# Output: 1

# GET deleted key
curl "localhost:8080/?cmd=GET%20mykey"
# Output: (nil)

# SET with expiry
curl "localhost:8080/?cmd=SET%20tempkey%20tempvalue%20EX%2060"
# Output: OK

# INCR
curl "localhost:8080/?cmd=INCR%20counter"
# Output: 1

# ZADD
curl "localhost:8080/?cmd=ZADD%20myset%201.5%20member1"
# Output: 1

# ZRANGE
curl "localhost:8080/?cmd=ZRANGE%20myset%200%20-1"
# Output: member1
```

### REST Style

```bash
# PUT (SET)
curl -d "cool-value" -X PUT localhost:8080/mykey
# Output: OK

# GET
curl localhost:8080/mykey
# Output: cool-value

# DELETE
curl -X DELETE localhost:8080/mykey
# Output: 1

# GET deleted key
curl localhost:8080/mykey
# Output: (nil)

# PUT with expiry
curl -d "tempvalue" -X PUT "localhost:8080/tempkey?ex=60"
# Output: OK

# INCR
curl -X POST localhost:8080/counter/incr
# Output: 1

# ZADD
curl -d "1.5 member1" -X POST localhost:8080/myset/zadd
# Output: 1

# ZCARD
curl localhost:8080/myset/zcard
# Output: 1

# ZRANK
curl localhost:8080/myset/zrank/member1
# Output: 0

# ZRANGE
curl "localhost:8080/myset/zrange?start=0&stop=-1"
# Output: member1

# DBSIZE
curl localhost:8080/dbsize
# Output: 2
```

## CLI Commands

| Command | Description |
|---------|-------------|
| `set <key> <value> [--ex <seconds>]` | Set a key-value pair |
| `get <key>` | Get the value of a key |
| `del <key>` | Delete a key |
| `dbsize` | Get the number of keys |
| `incr <key>` | Increment a key |
| `zadd <key> <score> <member>` | Add to sorted set |
| `zcard <key>` | Get sorted set cardinality |
| `zrank <key> <member>` | Get member rank |
| `zrange <key> <start> <stop>` | Get range from sorted set |
| `help` | Show available commands |
| `exit` | Exit the shell |



## Thread Safety

The implementation uses:

- `ConcurrentHashMap` for the main data store
- `ReadWriteLock` per key for fine-grained locking
- `ConcurrentSkipListMap` for sorted set members
- All operations are atomic and thread-safe

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run all tests with verbose output
mvn test -Dtest.verbose=true
```

The test suite includes:
- Unit tests for all Redis commands
- Concurrent access tests with multiple threads
- Integration tests via HTTP endpoints
- Stress tests for atomicity verification

## Key Constraints

- Keys and values: Only `[a-zA-Z0-9-_]` characters allowed
- Scores: Any valid double value
- No persistence: Data is in-memory only

## License

MIT License