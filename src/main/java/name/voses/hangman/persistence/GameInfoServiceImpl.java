package name.voses.hangman.persistence;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.devskiller.friendly_id.FriendlyId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import name.voses.hangman.resources.Game;
import name.voses.hangman.resources.PlayState;

@Service("gameInfoServiceImpl")
@ConfigurationProperties(prefix="games")
public class GameInfoServiceImpl implements GameInfoService {
    private static final String GAME_INFO_TABLE = "GameInfo";
    private static final String GUESS_KEY_PREFIX = "guess";

    // TODO: it's unclear if there's a way to do this sort of loading of a list
    //       config var, so instead currently using @ConfigurationProperties
    private List<String> possibleWords = new ArrayList<>();
    public List<String> getPossibleWords() { return this.possibleWords; }


    @Autowired
	private AmazonDynamoDB amazonDynamoDB;

    public Game createGame(int maxWrongGuesses) {
        Table table = getTable();

        String gameWord = randomWord();
        Game game = new Game(FriendlyId.createFriendlyId(),
                             maxWrongGuesses,
                             gameWord,
                             PlayState.build(maxWrongGuesses, new String[0], gameWord));

        Item gameItem = new Item().withPrimaryKey("game_id", game.getId())
                                  .withInt("max_wrong_guesses", game.getMaxWrongGuesses())
                                  .withString("word_being_guessed", game.getWordBeingGuessed())
                                  .with("created_at", new Date().getTime());
        table.putItem(gameItem);

        return game;
    }

    public Game storeGuess(Game game, String letter) {
        Table table = getTable();

        String attrName = guessAttributeName(new Date(), letter);

        UpdateItemSpec updateItemSpec =
            new UpdateItemSpec().withPrimaryKey("game_id", game.getId())
                                .withUpdateExpression("SET #attrName = :letter")
                                .withNameMap(Map.of("#attrName", attrName))
                                .withValueMap(Map.of(":letter", letter))
                                .withReturnValues(ReturnValue.ALL_NEW);
        UpdateItemOutcome updateOutcome = table.updateItem(updateItemSpec);

        return loadGame(game.getId(), updateOutcome.getItem());
    }

    public Game findGameWithGuesses(String gameId) {
        Table table = getTable();

        GetItemSpec getItemSpec = new GetItemSpec().withPrimaryKey("game_id", gameId);

        Item outcome = table.getItem(getItemSpec);
        return loadGame(gameId, outcome);
    }

    private String randomWord() {
        int randomElementIndex = ThreadLocalRandom.current().nextInt(possibleWords.size());
        return possibleWords.get(randomElementIndex);
    }

    private Table getTable() {
        DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);

        return dynamoDB.getTable(GAME_INFO_TABLE);
    }

    private Game loadGame(String gameId, Item gameOutcome) {
        int maxWrongGuesses = gameOutcome.getInt("max_wrong_guesses");
        String wordBeingGuessed = gameOutcome.getString("word_being_guessed");

        // grab all the attributes that are guesses
        // sort them by their timestamp
        // map them to their letter
        String[] guesses = StreamSupport.stream(gameOutcome.attributes().spliterator(), false)
                                .filter((entry) -> entry.getKey().startsWith(GUESS_KEY_PREFIX))
                                .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                                .map((entry) -> entry.getValue())
                                .collect(Collectors.toList()).toArray(new String[0]);
        PlayState playState = PlayState.build(maxWrongGuesses, guesses, wordBeingGuessed);

        Game game = new Game(gameId,
                             maxWrongGuesses,
                             wordBeingGuessed,
                             playState);

        return game;
    }

    private String guessAttributeName(Date date, String letter) {
        return GUESS_KEY_PREFIX + "|" + Long.toString(date.getTime()) + "|" + letter;
    }
}