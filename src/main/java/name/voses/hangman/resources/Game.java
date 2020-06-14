package name.voses.hangman.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Information about a game")
public class Game {
    private String id;
    private int maxWrongGuesses;

    private PlayState playState;

    private String wordBeingGuessed;

    public Game() {}

    public Game(String id, int maxWrongGuesses, String wordBeingGuessed, PlayState playState) {
        this.id = id;
        this.maxWrongGuesses = maxWrongGuesses;
        this.playState = playState;
        this.wordBeingGuessed = wordBeingGuessed;
    }

    @JsonProperty
    @Schema(description = "id used for interacting with the game")
    public String getId() {
        return this.id;
    }

    @JsonProperty
    @Schema(description = "The maximum number of guesses allowed before the game is considered lost")
    public int getMaxWrongGuesses() {
        return this.maxWrongGuesses;
    }

    @JsonProperty
    @Schema(description = "The state of the game, including information on the word itself")
    public PlayState getPlayState() {
        return this.playState;
    }

    @JsonIgnore
    public String getWordBeingGuessed() {
        return this.wordBeingGuessed;
    }

    public GuessResult recordGuess(String letter) {
        return playState.recordGuess(letter);
    }

	public void setPlayState(PlayState playState) {
        this.playState = playState;
	}
}