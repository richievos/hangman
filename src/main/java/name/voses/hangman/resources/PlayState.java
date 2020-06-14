package name.voses.hangman.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import name.voses.hangman.resources.GuessResult.GuessResultState;

@Schema(description = "The state of the game being played including information about guesses")
public class PlayState {
    private int remainingWrongGuesses;
    private List<LetterState> maskedWord;
    private List<LetterState> missedGuesses;
    private Map<Integer, List<Integer>> codePointToWordLocations;

    public static PlayState build(int maxWrongGuesses, String[] guesses, String wordBeingGuessed) {
        Set<Integer> guessLetterCodePoints = Arrays.stream(guesses)
                                                    .map(s -> s.codePointAt(0))
                                                    .collect(Collectors.toCollection(TreeSet::new));
        Set<Integer> missedGuesses = new TreeSet<>(guessLetterCodePoints);

        List<LetterState> maskedWord = new ArrayList<>(wordBeingGuessed.length());

        Map<Integer, List<Integer>> codePointToWordLocations = new HashMap<>();

        for (int i = 0; i < wordBeingGuessed.length(); i++) {
            int codePoint = wordBeingGuessed.codePointAt(i);

            List<Integer> locations = codePointToWordLocations.get(codePoint);
            if (locations == null) {
                locations = new LinkedList<>();
                codePointToWordLocations.put(codePoint, locations);
            }
            locations.add(i);

            if (guessLetterCodePoints.contains(codePoint)) {
                maskedWord.add(new LetterState(codePointToString(codePoint)));
                missedGuesses.remove(codePoint);
            } else {
                maskedWord.add(new LetterState());
            }
        }

        // NOTE: it's possible that remainingWrongGuesses could be negative, if we allowed decreasing
        // the max miss count after guesses have been registered
        int remainingWrongGuesses = maxWrongGuesses - missedGuesses.size();
        List<LetterState> missedGuessLetterStates =
                                missedGuesses.stream()
                                              .map(PlayState::codePointToString)
                                              .map(LetterState::new)
                                              .collect(Collectors.toList());

        return new PlayState(remainingWrongGuesses, missedGuessLetterStates, maskedWord, codePointToWordLocations);
    }

    private static boolean includesLetter(final List<LetterState> letters, final String letter) {
        return letters.stream().anyMatch((l) -> l.isForLetter(letter));
    }

    private static String codePointToString(int codePoint) {
        return new String(new int[] { codePoint }, 0, 1);
    }

    public PlayState(int remainingWrongGuesses, List<LetterState> missedGuesses, List<LetterState> maskedWord, Map<Integer, List<Integer>> codePointToWordLocations) {
        this.remainingWrongGuesses = remainingWrongGuesses;
        this.missedGuesses = missedGuesses;
        this.maskedWord = maskedWord;
        this.codePointToWordLocations = codePointToWordLocations;
    }

    @JsonProperty
    @Schema(description = "How many more wrong guesses before the game would be failed")
    public int getRemainingWrongGuesses() {
        return this.remainingWrongGuesses;
    }

    @JsonProperty
    @Schema(description = "The actual word being guessed, with placeholders for letters that have not yet been guessed")
    public List<LetterState> getMaskedWord() {
        return this.maskedWord;
    }

    @JsonProperty
    @Schema(description = "An array of the letters which were guessed (possibly unicode)")
    public List<LetterState> getMissedGuesses() {
        return this.missedGuesses;
    }

    public GuessResult recordGuess(String guessedLetter) {
        boolean alreadyGuessed = includesLetter(maskedWord, guessedLetter) ||
                                    includesLetter(missedGuesses, guessedLetter);
        if (alreadyGuessed) {
            return new GuessResult(GuessResultState.REPEAT, this);
        } else if (remainingWrongGuesses < 1) {
            return new GuessResult(GuessResultState.TOO_MANY_WRONG_GUESSES, this);
        }

        int codePoint = guessedLetter.codePointAt(0);
        List<Integer> codePointLocations = this.codePointToWordLocations.get(codePoint);
        if (codePointLocations == null) {
            return new GuessResult(GuessResultState.MISS,
                                   cloneWithExtraMiss(guessedLetter));
        } else {
            return new GuessResult(GuessResultState.MATCH,
                                   cloneWithHitFilledIn(guessedLetter, codePointLocations));
        }
    }

    private PlayState cloneWithExtraMiss(String newMiss) {
        List<LetterState> newMissedGuesses = new ArrayList<>(missedGuesses);
        newMissedGuesses.add(new LetterState(newMiss));

        return new PlayState(remainingWrongGuesses - 1,
                             newMissedGuesses,
                             maskedWord,
                             codePointToWordLocations);

    }

    private PlayState cloneWithHitFilledIn(String letter, List<Integer> codePointLocations) {
        List<LetterState> newMaskedWord = new ArrayList<>(this.maskedWord);
        for (int index : codePointLocations) {
            newMaskedWord.set(index, new LetterState(letter));
        }

        return new PlayState(remainingWrongGuesses,
                             missedGuesses,
                             newMaskedWord,
                             codePointToWordLocations);
    }
}