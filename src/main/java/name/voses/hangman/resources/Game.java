package name.voses.hangman.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import name.voses.hangman.resources.PlayState.GuessIneligibleReason;

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

    @Schema(description = "id used for interacting with the game")
    public String getId() {
        return this.id;
    }

    @Schema(description = "The maximum number of guesses allowed before the game is considered lost")
    public int getMaxWrongGuesses() {
        return this.maxWrongGuesses;
    }

    @Schema(description = "The state of the game, including information on the word itself")
    public PlayState getPlayState() {
        return this.playState;
    }

    @Schema(description = "How long the word being guessed is")
    public int getWordLength() {
        return this.wordBeingGuessed.length();
    }

    @JsonIgnore
    public String getWordBeingGuessed() {
        return this.wordBeingGuessed;
    }

	public void setPlayState(PlayState playState) {
        this.playState = playState;
	}

	public GuessIneligibleReason ineligibleToGuessReason(String letter) {
		return getPlayState().ineligibleToGuessReason(letter);
	}
}