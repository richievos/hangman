package name.voses.hangman.resources;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Information about a guessed or not yet guessed letter in the game")
public class LetterState {
    public static LetterState EMPTY_STATE = new LetterState();

    private String letter;


    public static LetterState fromCodePoint(Integer guess) {
		return new LetterState(new String(new int[] { guess }, 0, 1));
    }


    public LetterState() { }

    public LetterState(String letter) {
        this.letter = letter;
    }

    @Schema(description = "The letter that was guessed. This may be unicode, and will be null for a 'placeholder' letter")
    public String getLetter() {
        return letter;
    }

    public boolean isForLetter(String letter) {
        return Optional.ofNullable(this.letter).equals(Optional.ofNullable(letter));
    }

    @JsonIgnore
    public boolean isFilled() {
        return letter != null;
    }
}