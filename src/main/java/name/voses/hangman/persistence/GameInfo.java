package name.voses.hangman.persistence;

import java.util.Date;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "GameInfo")
public class GameInfo {
    private String wordData;
    private Date createdAt;

    private Integer guessCountData;

    private String gameId;

    public void makeGame(String gameId, Date createdAt, String word, int maxWrongGuesses) {
        this.gameId = gameId;
        this.wordData = word;
        this.guessCountData = maxWrongGuesses;
        this.createdAt = createdAt;
    }

    @DynamoDBAttribute(attributeName = "word_data")
    public String getWordData() {
        return wordData;
    }

    @DynamoDBAttribute(attributeName = "created_at")
    public Date getCreatedAt() {
        return createdAt;
    }

    @DynamoDBAttribute(attributeName = "guess_count_data")
    public Integer getGuessCountData() {
        return guessCountData;
    }

    @DynamoDBHashKey(attributeName = "game_id")
    public String getGameId() {
        return this.gameId;
    }

    public void setWordData(String wordData) {
        this.wordData = wordData;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public void setGuessCountData(Integer guessCountData) {
        this.guessCountData = guessCountData;
    }
}