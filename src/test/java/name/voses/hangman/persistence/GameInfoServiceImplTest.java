package name.voses.hangman.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import name.voses.hangman.HangmanApplication;
import name.voses.hangman.resources.Game;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {PropertyPlaceholderAutoConfiguration.class, HangmanApplication.class})
// @TestPropertySource(properties = {
//     "amazon.dynamodb.endpoint=http://localhost:8000/",
//     "amazon.aws.accesskey=test1",
//     "amazon.aws.secretkey=test231" })
public class GameInfoServiceImplTest {
	@Autowired
    private GameInfoService gameInfoService;

    /******************************************
     * DB Setup
     /******************************************/
    @Autowired
	private AmazonDynamoDB amazonDynamoDB;

	@Autowired
	private DynamoDBMapper mapper;

    @BeforeEach
    public void setupDB() throws Exception { GameInfoDynamoDBManagement.initDB(amazonDynamoDB, mapper); }

    @AfterEach
    public void tearDownDB() throws Exception { GameInfoDynamoDBManagement.tearDownDB(amazonDynamoDB, mapper); }

    /******************************************
     * Tests
     /******************************************/
    @Test
    public void createsAndRetrievesGame() {
        Game originalGame = gameInfoService.createGame(10);

        Game foundGame = gameInfoService.findGameWithGuesses(originalGame.getId());
        assertEquals(originalGame.getId(), foundGame.getId());
        assertEquals(originalGame.getMaxWrongGuesses(), foundGame.getMaxWrongGuesses());
        assertEquals(originalGame.getWordBeingGuessed(), foundGame.getWordBeingGuessed());
    }

    @Test
    public void associatesGuessWithGame() {
        Game originalGame = gameInfoService.createGame(10);

        gameInfoService.storeGuess(originalGame, "z");

        assertNotNull(gameInfoService);
        Game foundGame = gameInfoService.findGameWithGuesses(originalGame.getId());

        assertEquals(9, foundGame.getPlayState().getRemainingWrongGuesses());

        List<String> missedLetters = foundGame.getPlayState()
                                              .getMissedGuesses()
                                              .stream()
                                              .map((l) -> l.getLetter())
                                              .collect(Collectors.toList());
        assertIterableEquals(List.of("z"), missedLetters);
    }
}