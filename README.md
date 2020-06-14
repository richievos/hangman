# Hangman

This implements an API for a variant of the [Hangman game](https://en.wikipedia.org/wiki/Hangman_(game)).

## Requirement Notes

In the interests of time, the current design of this system has the following simplifications:

* word list is a static configuration, versus being pulled from a data store
* who performed any given operation is not stored (no audit logging, no caller tracking)
* no client id requirements nor tracking
* no rate limiting
* no logging or metric tie-ins (though the endpoints will generate metrics if that was plugged in)
* failure cases of the API are not documented (400s / 500s)
* repeated guesses of the same letter are ignored. A letter submission is idempotent since that's usually how the game would be played, and it simplifies client-server interactions around retries and failure cases.

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

Game
    id
    created_at
    maxWrongGuesses
    word

    Guesses
        created_at
        letter

## API Notes

* when talking about characters, the API returns strings. JSON doesn't have a char type, so single length strings are returned. This also helps avoid unicode issues (some letters are multiple characters)
* the masked words are an array of objects instead of just an array as an attempt to avoid having an array with a series of `null` characters in it. The client can can either key off of `.letter` being null or the entry not having the letter key. It also allows for future expansion of the API, such as tracking who guessed the given letter.
    * an alternative solution could return an array where null or "" represent placeholders and guessed characters are filled in.
    * an alternative solution could return a total length and positions of already guessed letters ({ pos: 0, letter: "G" }), but that would seemingly complicate the client side implementation.
    * an alternative solution could return the state of the letter explicitly (eg filled=true/false in or not)
    * which alternative is more ideal is a discussion to have with the client team, weighing YAGNI versus ease of implementation. A GraphQL wrapper on this API could allow for a combination of fields


### API Docs
The APIs are documented with OpenAPI/Swagger and accessible at `http://localhost:8080/v3/api-docs`. A dump of that is also checked in as doc/openapi.json.

It's suggested to view the API you copy-paste that openapi schema to https://editor.swagger.io/.

## Development

Dependencies:

* Java 14+
* Maven (for building)
* docker (for running)

## Launching

This app uses Dynamodb storage, so the easiest way to run it is boot the app and dependencies via `docker-compose`.

```
# build the app
$ mvn clean package

# rebuild the app image
$ docker-compose build

# boot the app and all dependencies
$ docker-compose up

# hit the app (in a separate terminal)
$ curl -i http://localhost:8080/v3/api-docs


```
