package name.voses.hangman.persistence;

import java.io.Serializable;
import java.util.Date;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import org.springframework.data.annotation.Id;

@DynamoDBTable(tableName = "GameInfo")
public class GameInfo {
    // Composite key for the id
    // https://github.com/derjust/spring-data-dynamodb/wiki/Use-Hash-Range-keys
    public static class GameInfoId implements Serializable {
        private static final long serialVersionUID = 1L;

        private String gameId;
        private String sk;

        @DynamoDBHashKey
        public String getGameId() {
            return gameId;
        }

        public void setGameId(String gameId) {
            this.gameId = gameId;
        }

        @DynamoDBRangeKey
        public String getSK() {
            return sk;
        }

        public void setSK(String sk) {
            this.sk = sk;
        }

    }

    private String wordData;
    private Date createdAt;

    private Integer guessCountData;

    @Id
    private GameInfoId gameInfoId;

    public void makeGame(String gameId, Date createdAt, String word, int maxWrongGuesses) {
        this.setGameId(gameId);
        this.setSK("game#{" + createdAt.getTime() + "}");
        this.wordData = word;
        this.guessCountData = maxWrongGuesses;
        this.createdAt = createdAt;
    }

    public void makeGuess(String gameId, Date createdAt, String letter) {
        this.setGameId(gameId);
        // "guess#{timestamp}#{letter}"
        this.setSK("guess#{" + createdAt.getTime() + "}#{" + letter + "}");
        this.wordData = letter;
        this.createdAt = createdAt;
    }

    @DynamoDBIgnore
    public boolean isGame() {
        String sk = getSK();
        return sk != null && sk.startsWith("game");
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

    public void setWordData(String wordData) {
        this.wordData = wordData;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDBHashKey(attributeName = "game_id")
    public String getGameId() {
        return gameInfoId == null ? null : gameInfoId.getGameId();
    }

    public void setGameId(String gameId) {
        if (gameInfoId == null) {
			gameInfoId = new GameInfoId();
		}

        this.gameInfoId.setGameId(gameId);
    }

    @DynamoDBRangeKey(attributeName = "sk")
    public String getSK() {
        return gameInfoId == null ? null : gameInfoId.getSK();
    }

    public void setSK(String sk) {
        if (gameInfoId == null) {
			gameInfoId = new GameInfoId();
		}

        this.gameInfoId.setSK(sk);
    }

    public void setGuessCountData(Integer guessCountData) {
        this.guessCountData = guessCountData;
    }
}