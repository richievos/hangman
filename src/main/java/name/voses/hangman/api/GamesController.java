package name.voses.hangman.api;

import java.net.URI;
import java.util.Map;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import name.voses.hangman.persistence.GameInfoService;
import name.voses.hangman.resources.Game;
import name.voses.hangman.resources.PlayState.GuessIneligibleReason;

@RestController
@RequestMapping(path = "/games")
@OpenAPIDefinition(info = @Info(title = "Hangman Game endpoints", version = "1"))
public class GamesController {
    private static class GameCreateOptions {
        @Schema(description = "How many wrong guesses to allow", required = false)
        private Integer maxWrongGuesses;

        public Integer getMaxWrongGuesses() { return this.maxWrongGuesses; }
        public void setMaxWrongGuesses(Integer maxWrongGuesses) { this.maxWrongGuesses = maxWrongGuesses; }
    }

    private static Logger LOG = LoggerFactory.getLogger(GamesController.class);

    @Autowired
    private GameInfoService gameInfoService;

    @Value("${games.defaultMaxWrongGuesses}")
    private int defaultMaxWrongGuesses;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Operation(description = "Start a new game")
    @ApiResponse(responseCode = "201", description = "New game created",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = Game.class)))
    public ResponseEntity<Map<String, Game>> createGame(@RequestBody GameCreateOptions options)
            throws JsonProcessingException {
        if (options.getMaxWrongGuesses() == null)
            options.setMaxWrongGuesses(defaultMaxWrongGuesses);

        Game game = gameInfoService.createGame(options.getMaxWrongGuesses());

        logJSON(Map.of("action", "gameCreate",
                       "id", game.getId(),
                       "data", Map.of("word", game.getWordBeingGuessed(),
                       "maxWrongGuesses", game.getMaxWrongGuesses())));

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                                             .path("/{id}")
                                             .buildAndExpand(game.getId())
                                             .toUri();

        return ResponseEntity.created(uri).body(Map.of("game", game));
    }

    @GetMapping("{gameId}")
    @Timed
    @Operation(description = "Retrieve an existing game")
    @ApiResponse(responseCode = "200", description = "Game retrieved",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = Game.class)))
    @ApiResponse(responseCode = "404", description = "Game with given id not found",
                 content = @Content())
    public ResponseEntity<Map<String, Game>> getGame(@PathVariable String gameId) {
        Game game = gameInfoService.findGameWithGuesses(gameId);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("game", game));
    }

    @PutMapping("{gameId}/guesses/{letter}")
    @Timed
    @Operation(description = "Guess a letter")
    @ApiResponse(responseCode = "200", description = "Guess registered, returns the current (updated) state of the game. Idempotent on a re-guess of a letter.",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = Game.class)))
    @ApiResponse(responseCode = "404", description = "Game with given id not found",
                 content = @Content())
    @ApiResponse(responseCode = "400", description = "Game finished or invalid letter",
                 content = @Content())
    public ResponseEntity<Map<String, Game>> guessLetter(
                                    @PathVariable("gameId")
                                    String gameId,

                                    @Schema(type = "string", minLength = 1, maxLength = 1)
                                    @PathVariable("letter")
                                    String letter) throws JsonProcessingException {
        if (letter.length() != 1) {
            logGuessResult(gameId, letter, "bad_length", Map.of("letterLength", letter.length(),
                                                                "codePoints", letter.codePoints().toArray()));
            // TODO: could return why this is a bad request in a message
            return ResponseEntity.badRequest().build();
        }

        Game game = gameInfoService.findGameWithGuesses(gameId);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }


        // To avoid having to lock the game record (while still avoiding race conditions) this code:
        // 1. checks if the current guess is eligible to play at all, if not fail fast
        // 2. stores the new guess
        // 3. reloads the game and its guesses
        // 4. returns the new state of the game
        //
        // This avoids having to do any locks, but does mean the data loading logic needs to
        // account for duplicate guesses being registered and possibly more guesses having
        // occurred then the game was expected to allow.
        //
        // As currently designed this means we can't tell the player if they got a hit or a miss,
        // however that could be straightforwardly done just by passing an ID along with the
        // letter we store, and comparing after the data is reloaded.

        // exit early if the game has already ended
        GuessIneligibleReason ineligibleReason = game.ineligibleToGuessReason(letter);
        if (ineligibleReason != null) {
            logGuessResult(gameId, letter, "ineligible_reason", Map.of("ineligibleToGuessReason", ineligibleReason.name()));
        }

        if (ineligibleReason == null) {
            // store that guess and reload to see where we ended up
            logGuessResult(gameId, letter, "recording_guess", Map.of("word", game.getWordBeingGuessed()));
            gameInfoService.storeGuess(game, letter);
            game = gameInfoService.findGameWithGuesses(gameId);
        } else if (ineligibleReason == GuessIneligibleReason.REPEAT) {
            // no-op, just act idempotently
        } else {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(Map.of("game", game));
    }

    private void logGuessResult(String gameId, String letter, String result, Map<?, ?> data) throws JsonProcessingException {
        logJSON(Map.of("action", "guess",
                       "gameId", gameId,
                       "letter", letter,
                       "result", result,
                       "data", data));
    }

    private void logJSON(Map<?, ?> message) throws JsonProcessingException {
        LOG.info(new ObjectMapper().writeValueAsString(message));
    }
}