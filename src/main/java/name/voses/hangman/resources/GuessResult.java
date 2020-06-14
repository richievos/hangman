package name.voses.hangman.resources;

public class GuessResult {
    public static enum GuessResultState {
        MATCH,
        MISS,
        TOO_MANY_WRONG_GUESSES,
        REPEAT
    }

    private final GuessResultState resultState;
    private final PlayState playState;

    public GuessResult(GuessResultState resultState, PlayState playState) {
        this.resultState = resultState;
        this.playState = playState;
    }

    public GuessResultState getResultState() {
        return this.resultState;
    }

    public PlayState getPlayState() {
        return this.playState;
    }
}