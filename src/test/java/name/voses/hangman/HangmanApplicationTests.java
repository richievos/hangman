package name.voses.hangman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@SuppressWarnings("unchecked")
public class HangmanApplicationTests {
    // private static final String CONFIG_PATH =
    // ResourceHelpers.resourceFilePath("test-example.yml");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static HttpHeaders headers;


    // public static final DropwizardAppExtension<HangmanConfiguration> RULE =
    // new DropwizardAppExtension<>(
    // HangmanApplication.class, CONFIG_PATH);

    @BeforeAll
    public static void runBeforeAllTestMethods() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    /******************************************
     * Basic create tests
     ******************************************/
    @Test
    public void createsNewGame() throws Exception {
        ResponseEntity<String> response = postCreateGame(5);
        assertEquals(201, response.getStatusCodeValue());

        Map<String, Object> game = readGame(response);
        assertNotNull(game);

        assertNotNull(game.get("id"));
        assertEquals(5, game.get("maxWrongGuesses"));

        Map<String, Object> playState = (Map<String, Object>) game.get("playState");
        assertNotNull(playState);
        assertEquals(new ArrayList<Object>(), playState.get("missedGuesses"));
        assertEquals(5, playState.get("remainingWrongGuesses"));
    }

    @Test
    public void createsGameWithDefaultNumGuesses() throws Exception {
        ResponseEntity<String> response = postCreateGame(null);
        assertEquals(201, response.getStatusCodeValue());

        Map<String, Object> game = readGame(response);

        assertEquals(10, game.get("maxWrongGuesses"));
    }

    /******************************************
     * Guessing tests
     ******************************************/
    @Test
    public void gamesDecrementOnBadGuesses() throws Exception {
        ResponseEntity<String> createResponse = postCreateGame(3);
        Map<String, Object> game = readGame(createResponse);

        // Using random unicode chars to avoid having to stub the string (currently the
        // words don't include unicode)
        ResponseEntity<String> badGuessResponse = registerGuess((String) game.get("id"), "☃");
        assertEquals(200, badGuessResponse.getStatusCodeValue());
        game = readGame(badGuessResponse);
        Map<String, Object> playState = (Map<String, Object>) game.get("playState");
        assertEquals(2, playState.get("remainingWrongGuesses"));

        badGuessResponse = registerGuess((String) game.get("id"), "☠");
        assertEquals(200, badGuessResponse.getStatusCodeValue());
        game = readGame(badGuessResponse);
        playState = (Map<String, Object>) game.get("playState");
        assertEquals(1, playState.get("remainingWrongGuesses"));

        badGuessResponse = registerGuess((String) game.get("id"), "☣");
        assertEquals(200, badGuessResponse.getStatusCodeValue());
        game = readGame(badGuessResponse);
        playState = (Map<String, Object>) game.get("playState");
        assertEquals(0, playState.get("remainingWrongGuesses"));
    }

    @Test
    public void gamesDontDecrementOnCorrectGuesses() throws Exception {
        ResponseEntity<String> createResponse = postCreateGame(3);
        Map<String, Object> game = readGame(createResponse);

        // Word is "abruptly" (TODO: stub that versus relying on the config)
        ResponseEntity<String> badGuessResponse = registerGuess((String) game.get("id"), "a");
        assertEquals(200, badGuessResponse.getStatusCodeValue());
        game = readGame(badGuessResponse);
        Map<String, Object> playState = (Map<String, Object>) game.get("playState");
        assertEquals(3, playState.get("remainingWrongGuesses"));
        List<String> letters = getLetters(playState);
        assertIterableEquals(Arrays.asList("a", null, null, null, null, null, null, null),
                             letters);
    }


    @Test
    public void gamesDontAllowMoreGuessesAfterLost() throws Exception {
        ResponseEntity<String> createResponse = postCreateGame(1);
        Map<String, Object> game = readGame(createResponse);

        ResponseEntity<String> badGuessResponse = registerGuess((String) game.get("id"), "☃");
        assertEquals(200, badGuessResponse.getStatusCodeValue());

        badGuessResponse = registerGuess((String) game.get("id"), "👍");
        assertEquals(400, badGuessResponse.getStatusCodeValue());
    }

    @Test
    public void badGuessesAreIdempotent() throws Exception {
        ResponseEntity<String> createResponse = postCreateGame(5);
        Map<String, Object> game = readGame(createResponse);

        for (int i = 0; i < 3; i++) {
            // Using random unicode chars to avoid having to stub the string (currently the
            // words don't include unicode)
            ResponseEntity<String> badGuessResponse = registerGuess((String) game.get("id"), "☃");
            assertEquals(200, badGuessResponse.getStatusCodeValue());
            game = readGame(badGuessResponse);

            Map<String, Object> playState = (Map<String, Object>) game.get("playState");
            assertEquals(4, playState.get("remainingWrongGuesses"));
        }
    }

    /******************************************
     * Helpers
     *
     * @throws JSONException
     ******************************************/
    private ResponseEntity<String> postCreateGame(Integer maxWrongGuesses) throws JSONException {
        JSONObject game = new JSONObject();
        game.put("maxWrongGuesses", maxWrongGuesses);

        HttpEntity<String> request = new HttpEntity<String>(game.toString(), headers);

        ResponseEntity<String> response =
            this.restTemplate.postForEntity("http://localhost:" + port + "/games",
                                            request,
                                            String.class);

        return response;
    }

    private ResponseEntity<String> registerGuess(String gameId, String guess) throws UnsupportedEncodingException {
        ResponseEntity<String> response =
            // this.restTemplate.exchange("http://localhost:" + port + "/games/{gameId}/guesses/{letter}",
            //                             HttpMethod.PUT,
            //                             null,
            //                             String.class,
            //                             Map.of("gameId", gameId, "letter", guess));
            this.restTemplate.exchange("http://localhost:" + port + "/games/" + gameId + "/guesses/" + guess,
                                       HttpMethod.PUT,
                                       null,
                                       String.class);

        return response;
    }

    private Map<String, Object> readGame(ResponseEntity<String> response) throws JsonMappingException, JsonProcessingException {
        Map<String, Object> responseContents = new ObjectMapper().readValue(response.getBody(), Map.class);

        Map<String, Object> game = (Map<String, Object>) responseContents.get("game");

        return game;
    }

    private List<String> getLetters(Map<String, Object> playState) {
        List<Map<String, Object>> maskedWords = (List<Map<String, Object>>) playState.get("maskedWord");
        List<String> result = maskedWords
                     .stream()
                     .map((l) -> (String) l.get("letter"))
                     .collect(Collectors.toList());
        return result;
    }
}
