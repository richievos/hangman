package name.voses.hangman.resources;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The state of the game being played including information about guesses")
public class PlayState {
    public static enum GuessIneligibleReason {
        TOO_MANY_WRONG_GUESSES,
        REPEAT,
        ALREADY_WON
    }

    private int remainingWrongGuesses;
    private List<LetterState> maskedWord;
    private List<LetterState> missedGuesses;

    public static PlayState build(int maxWrongGuesses, String[] guesses, String wordBeingGuessed) {
        Set<Integer> guessLetterCodePoints = Arrays.stream(guesses)
                                                    .map(s -> s.codePointAt(0))
                                                    // LinkedHashSet to ensure order is kept
                                                    .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<Integer> missedGuesses = new LinkedHashSet<Integer>();

        LetterState[] maskedWord = new LetterState[wordBeingGuessed.length()];
        Arrays.fill(maskedWord, LetterState.EMPTY_STATE);

        Map<Integer, List<Integer>> codePointToWordLocations = new HashMap<>(wordBeingGuessed.length());

        for (int i = 0; i < wordBeingGuessed.length(); i++) {
            int codePoint = wordBeingGuessed.codePointAt(i);

            List<Integer> locations = codePointToWordLocations.get(codePoint);
            if (locations == null) {
                // LinkedList gives O(1) insert and we only ever sequentially access the list
                locations = new LinkedList<>();
                codePointToWordLocations.put(codePoint, locations);
            }
            locations.add(i);
        }

        for (Integer guess : guessLetterCodePoints) {
            List<Integer> locations = codePointToWordLocations.get(guess);
            if (locations != null) {
                final LetterState guessLetterState = LetterState.fromCodePoint(guess);
                locations.forEach((location) -> maskedWord[location] = guessLetterState);
            } else {
                missedGuesses.add(guess);
            }

            // account for us getting into a state of:
            // word = "abcd"
            // guesses = ["a", "b", "c", <too many bad guesses>, "d" ]
            if (missedGuesses.size() >= maxWrongGuesses) {
                break;
            }
        }

        int remainingWrongGuesses = maxWrongGuesses - missedGuesses.size();
        List<LetterState> missedGuessLetterStates =
                                missedGuesses.stream()
                                              .map(PlayState::codePointToString)
                                              .map(LetterState::new)
                                              .collect(Collectors.toList());

        return new PlayState(remainingWrongGuesses, missedGuessLetterStates, Arrays.asList(maskedWord));
    }

    private static boolean includesLetter(final List<LetterState> letters, final String letter) {
        return letters.stream().anyMatch((l) -> l.isForLetter(letter));
    }

    private static String codePointToString(int codePoint) {
        return new String(new int[] { codePoint }, 0, 1);
    }

    public PlayState(int remainingWrongGuesses, List<LetterState> missedGuesses, List<LetterState> maskedWord) {
        this.remainingWrongGuesses = remainingWrongGuesses;
        this.missedGuesses = missedGuesses;
        this.maskedWord = maskedWord;
    }

    @Schema(description = "How many more wrong guesses before the game would be failed")
    public int getRemainingWrongGuesses() {
        return this.remainingWrongGuesses;
    }

    @Schema(description = "The actual word being guessed, with placeholders for letters that have not yet been guessed")
    public List<LetterState> getMaskedWord() {
        return this.maskedWord;
    }

    @Schema(description = "An array of the letters which were guessed (possibly unicode)")
    public List<LetterState> getMissedGuesses() {
        return this.missedGuesses;
    }

    public GuessIneligibleReason ineligibleToGuessReason(String guessedLetter) {
        // TODO: if desired we could have the guessed letters stored in a set to speed up this check
        boolean alreadyGuessed = includesLetter(maskedWord, guessedLetter) ||
                                    includesLetter(missedGuesses, guessedLetter);

        if (alreadyGuessed) {
            return GuessIneligibleReason.REPEAT;
        } else if (remainingWrongGuesses < 1) {
            return GuessIneligibleReason.TOO_MANY_WRONG_GUESSES;
        } else if (isGameWon()) {
            return GuessIneligibleReason.ALREADY_WON;
        }

        return null;
    }

    private boolean isGameWon() {
        return this.getMaskedWord().stream().allMatch(LetterState::isFilled);
    }
}