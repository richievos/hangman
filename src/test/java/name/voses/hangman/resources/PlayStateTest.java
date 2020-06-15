package name.voses.hangman.resources;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class PlayStateTest {
    @Test
    public void testBuildWithNoGuesses() {
        PlayState state = PlayState.build(10, new String[0], "myword");

        assertIterableEquals(Collections.EMPTY_LIST, state.getMissedGuesses());
        assertEquals(10, state.getRemainingWrongGuesses());

        List<String> maskedWordLetters = getLetters(state.getMaskedWord());
        assertIterableEquals(asList(null, null, null, null, null, null),
                             maskedWordLetters);
    }

    @Test
    public void testCountsWrongGuesses() {
        PlayState state = PlayState.build(10, new String[] { "m", "z" }, "mywordmy");

        assertIterableEquals(List.of("z"), getLetters(state.getMissedGuesses()));
        assertEquals(9, state.getRemainingWrongGuesses());
    }

    @Test
    public void testMasksAllHitsInBuild() {
        PlayState state = PlayState.build(10, new String[] { "m", "o" }, "mywordmy");

        assertIterableEquals(Collections.EMPTY_LIST, state.getMissedGuesses());
        assertEquals(10, state.getRemainingWrongGuesses());

        List<String> maskedWordLetters = getLetters(state.getMaskedWord());
        assertIterableEquals(asList("m", null, null, "o", null, null, "m", null),
                             maskedWordLetters);
    }

    @Test
    public void testStopsAfterEnoughWrongGuesses() {
        PlayState state = PlayState.build(1, new String[] { "m", "z", "o" }, "mywordmy");

        assertIterableEquals(List.of("z"), getLetters(state.getMissedGuesses()));
        assertEquals(0, state.getRemainingWrongGuesses());

        List<String> maskedWordLetters = getLetters(state.getMaskedWord());
        assertIterableEquals(asList("m", null, null, null, null, null, "m", null),
                             maskedWordLetters);
    }

    private List<String> getLetters(Collection<LetterState> letterStates) {
        return letterStates.stream()
                            .map((ls) -> ls.getLetter())
                            .collect(Collectors.toList());
    }
}