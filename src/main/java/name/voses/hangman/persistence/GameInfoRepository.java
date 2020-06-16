package name.voses.hangman.persistence;

import java.util.List;

import com.codahale.metrics.annotation.Timed;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

@Timed
@EnableScan
public interface GameInfoRepository extends CrudRepository<GameInfo, String> {
    List<GameInfo> findAllByGameId(String id);
}