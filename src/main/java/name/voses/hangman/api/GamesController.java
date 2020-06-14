package name.voses.hangman.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import name.voses.hangman.resources.Game;
import name.voses.hangman.resources.GuessResult;
import name.voses.hangman.resources.PlayState;

@RestController
@RequestMapping(path = "/games")
@OpenAPIDefinition(info = @Info(title = "Game endpoints", version = "1"))
@ConfigurationProperties(prefix="games")
public class GamesController {
    private static class GameCreateOptions {
        @Schema(description = "How many wrong guesses to allow", required = false)
        private Integer maxWrongGuesses;

        public Integer getMaxWrongGuesses() { return this.maxWrongGuesses; }
        public void setMaxWrongGuesses(Integer maxWrongGuesses) { this.maxWrongGuesses = maxWrongGuesses; }
    }

    private static Map<String, Game> gameStore = new HashMap<>();

    private static Logger LOG = LoggerFactory.getLogger(GamesController.class);

    @Value("${games.defaultMaxWrongGuesses}")
    private int defaultMaxWrongGuesses;

    // TODO: it's unclear if there's a way to do this sort of loading of a list
    //       config var, so instead currently using @ConfigurationProperties
    private List<String> possibleWords = new ArrayList<>();
    public List<String> getPossibleWords() { return this.possibleWords; }

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

        Game game = new Game(UUID.randomUUID().toString(),
                             options.getMaxWrongGuesses(),
                             randomWord(),
                             PlayState.build(options.getMaxWrongGuesses(), new String[0], "carpool"));
        gameStore.put(game.getId(), game);
        logJSON(Map.of("action", "gameCreate",
                       "id", game.getId(),
                       "word", game.getWordBeingGuessed(),
                       "maxWrongGuesses", game.getMaxWrongGuesses()));

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                                             .path("/{id}") // TODO: full path?
                                             .buildAndExpand(game.getId())
                                              .toUri();

        return ResponseEntity.created(uri).body(Map.of("game", game));
    }

    @PutMapping("{gameId}/guesses/{letter}")
    @Timed
    @Operation(description = "Guess a letter")
    @ApiResponse(responseCode = "200", description = "Guess registered, returns the updated game. Idempotent on a re-guess of a letter.",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = Game.class)))
    @ApiResponse(responseCode = "404", description = "Game with given id not found")
    @ApiResponse(responseCode = "400", description = "Max wrong guesses already performed or invalid letter")
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

        Game game = gameStore.get(gameId);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        GuessResult guessResult = game.recordGuess(letter);
        game.setPlayState(guessResult.getPlayState());

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

    private String randomWord() {
        int randomElementIndex = ThreadLocalRandom.current().nextInt(possibleWords.size());
        return possibleWords.get(randomElementIndex);
    }
}