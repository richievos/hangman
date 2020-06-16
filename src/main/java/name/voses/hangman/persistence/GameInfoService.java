package name.voses.hangman.persistence;

import com.codahale.metrics.annotation.Timed;

import org.springframework.stereotype.Component;

import name.voses.hangman.resources.Game;

@Timed
@Component
public interface GameInfoService {
    public Game createGame(int maxWrongGuesses);

    public Game storeGuess(Game game, String letter);

    public Game findGameWithGuesses(String gameId);
}