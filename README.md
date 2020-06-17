# Hangman

This implements an API for a variant of the [Hangman game](https://en.wikipedia.org/wiki/Hangman_(game)).

## Implementation Notes

Generally this is less about the game, and more about experimentation in implementing this system. In particular this implementation:

* is built on the latest Spring Boot (Lovelace-SR18)
* persists into Dynamodb
* is tested via junit5
* manages instances via Docker/docker-compose
* uses a lock-less data storage/management implementation

## Requirement Notes

In the interests of time, the current design of this system has the following idiosyncracies:

* the word list is a static configuration, versus being pulled from a data store
* no user management / audit logging / client tracking for who is using a game
* no client id requirements nor tracking
* no rate limiting
* no logging or metric tie-ins (though the endpoints will generate metrics if that was plugged in)
* failure cases of the API are not documented (400s / 500s)
* repeated guesses of the same letter are ignored (intentionally). A letter submission is idempotent since that's usually how the game would be played, and it simplifies client-server interactions around retries and failure cases.

### Future Extension Notes

#### Tracking Who Guessed
It's highly probable in the future the identity of "who" performed a given guess would want to be tracked.
Tracking that would add additional metadata to the letter tables in the data store, and the responses. Eg:

```
{
    "maskedWord": [{ "letter": "G", "who": "⛄️" }, { }, ... ]
}
```

## Data Model

General data model:

```
Game
    id
    created_at
    maxWrongGuesses
    word

    Guesses
        created_at
        letter
```

The Dynamodb data model implementing that follows. It stores a row for every single game, with the guesses as a list.

| key_name          | notes |
| ---               | --- |
| game_id           | partition key |
| max_wrong_guesses |
| word_being_guessed |
| created_at       | ms since epoch |
| guesses | [{letter}] the guesses are stored as an array of strings in order of their happening |

A future extension would be to add a `ttl` column that's an int of (`created_at + delta`) which would auto delete old games [via a TTL](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/TTL.html). It's likely old games aren't useful after a couple hours (minutes?) and that'd save costs.

## API Notes

* when talking about characters, the API returns strings. JSON doesn't have a char type, so single length strings are returned. This also helps avoid unicode issues (some letters are multiple characters)
* the masked words are an array of objects instead of just an array as an attempt to avoid having an array with a series of `null` characters in it. The client can key off of `.letter` being null. It also allows for future expansion of the API, such as tracking who guessed the given letter.
    * an alternative solution could return an array where null or "" represent placeholders and guessed characters are filled in.
    * an alternative solution could return a total length and positions of already guessed letters ({ pos: 0, letter: "G" }), but that would seemingly complicate the client side implementation.
    * an alternative solution could return the state of the letter explicitly (eg filled=true/false in or not)
    * which alternative is more ideal is a discussion to have with the client team, weighing YAGNI versus ease of implementation. A GraphQL wrapper on this API could allow for a combination of fields


### API Docs
The APIs are documented with OpenAPI/Swagger and accessible at `http://localhost:8080/v3/api-docs`. A dump of that is also checked in as [doc/openapi.json](doc/openapi.json).

It's suggested to view the API you copy-paste that openapi schema to https://editor.swagger.io/.

## Development

Dependencies:

* Java 14+
* Maven (for building)
* docker (for running)

## Launching

This app uses Dynamodb storage, so the easiest way to run it is boot the app and dependencies via `docker-compose`.

```bash
# build the app
$ mvn clean package

# rebuild the app image
$ docker-compose build

# boot the app and all dependencies
$ docker-compose up

# hit the app (in a separate terminal)
$ curl -i http://localhost:8080/v3/api-docs
```

Running tests

```bash
# build the app
$ mvn clean package

# boot the local dynamo instance
$ docker-compose up dynamodb-test

# hit the app (in a separate terminal)
$ mvn test
```

## Example Normal Interaction

```bash
# Create a game (actual word in this example is "staff")
$ curl -i -H "Content-Type: application/json" -X POST --data '{"maxWrongGuesses":5}' http://localhost:8080/games
HTTP/1.1 201 Created
Date: Wed, 17 Jun 2020 05:50:34 GMT
Location: http://localhost:8080/games/5Yd0mITK0Y3k1aIaI6ODVZ
Content-Type: application/json
Transfer-Encoding: chunked

{"game":{"id":"5Yd0mITK0Y3k1aIaI6ODVZ","maxWrongGuesses":5,"playState":{"remainingWrongGuesses":5,"maskedWord":[{"letter":null},{"letter":null},{"letter":null},{"letter":null},{"letter":null}],"missedGuesses":[]},"wordLength":5}}

# An incorrect guess
$ curl -i -H "Content-Type: application/json" -X PUT http://localhost:8080/games/5Yd0mITK0Y3k1aIaI6ODVZ/guesses/p
HTTP/1.1 200 OK
Date: Wed, 17 Jun 2020 05:51:25 GMT
Content-Type: application/json
Transfer-Encoding: chunked

{"game":{"id":"5Yd0mITK0Y3k1aIaI6ODVZ","maxWrongGuesses":5,"playState":{"remainingWrongGuesses":4,"maskedWord":[{"letter":null},{"letter":null},{"letter":null},{"letter":null},{"letter":null}],"missedGuesses":[{"letter":"p"}]},"wordLength":5}}

# A correct guess
$ curl -i -H "Content-Type: application/json" -X PUT http://localhost:8080/games/5Yd0mITK0Y3k1aIaI6ODVZ/guesses/s
HTTP/1.1 200 OK
Date: Wed, 17 Jun 2020 05:52:37 GMT
Content-Type: application/json
Transfer-Encoding: chunked

{"game":{"id":"5Yd0mITK0Y3k1aIaI6ODVZ","maxWrongGuesses":5,"playState":{"remainingWrongGuesses":4,"maskedWord":[{"letter":"s"},{"letter":null},{"letter":null},{"letter":null},{"letter":null}],"missedGuesses":[{"letter":"p"}]},"wordLength":5}}
```