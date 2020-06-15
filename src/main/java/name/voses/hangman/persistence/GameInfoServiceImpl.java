package name.voses.hangman.persistence;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.devskiller.friendly_id.FriendlyId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import name.voses.hangman.resources.Game;
import name.voses.hangman.resources.PlayState;

@Service("gameInfoServiceImpl")
@ConfigurationProperties(prefix="games")
public class GameInfoServiceImpl implements GameInfoService {
    @Autowired
    private GameInfoRepository gameInfoRepository;


    // TODO: it's unclear if there's a way to do this sort of loading of a list
    //       config var, so instead currently using @ConfigurationProperties
    private List<String> possibleWords = new ArrayList<>();
    public List<String> getPossibleWords() { return this.possibleWords; }


    public Game createGame(int getMaxWrongGuesses) {
        String gameWord = randomWord();
        Game game = new Game(FriendlyId.createFriendlyId(),
                             getMaxWrongGuesses,
                             gameWord,
                             PlayState.build(getMaxWrongGuesses, new String[0], gameWord));
        storeGame(game);
        return game;
    }

    public void storeGuess(Game game, String letter) {
        GameInfo gameInfo = new GameInfo();
        gameInfo.makeGuess(game.getId(), new Date(), letter);
        gameInfoRepository.save(gameInfo);
    }

    public Game findGameWithGuesses(String gameId) {
        // TODO: I think the game would always be returned as element 0, but I'm not 100% sure of that in dynamo
        List<GameInfo> gameAndGuesses = gameInfoRepository.findAllByGameId(gameId);

        GameInfo actualGameInfo = null;
        List<String> letters = new LinkedList<>();

        for (GameInfo gameInfo : gameAndGuesses) {
            if (gameInfo.isGame()) {
                actualGameInfo = gameInfo;
            } else {
                letters.add(gameInfo.getWordData());
            }
        }

        PlayState playState = PlayState.build(actualGameInfo.getGuessCountData(),
                                              letters.toArray(new String[0]),
                                              actualGameInfo.getWordData());

        Game game = new Game(actualGameInfo.getGameId(),
                             actualGameInfo.getGuessCountData(),
                             actualGameInfo.getWordData(),
                             playState);

        return game;
    }

    private void storeGame(Game game) {
        GameInfo gameInfo = new GameInfo();
        gameInfo.makeGame(game.getId(), new Date(), game.getWordBeingGuessed(), game.getMaxWrongGuesses());
        gameInfoRepository.save(gameInfo);
    }

    private String randomWord() {
        int randomElementIndex = ThreadLocalRandom.current().nextInt(possibleWords.size());
        return possibleWords.get(randomElementIndex);
    }
}