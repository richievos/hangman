package name.voses.hangman.persistence;

import java.util.Arrays;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// TODO: switch this to a junit extension, or otherwise simplify using this
@Component
public class GameInfoDynamoDBManagement implements CommandLineRunner {
    private static Logger LOG = LoggerFactory.getLogger(GameInfoDynamoDBManagement.class);

    public static void tearDownDB(AmazonDynamoDB amazonDynamoDB, DynamoDBMapper mapper) throws Exception {
        if (amazonDynamoDB.listTables(new ListTablesRequest()).getTableNames().indexOf("GameInfo") <= 0) {
            DeleteTableRequest dtr = mapper.generateDeleteTableRequest(GameInfo.class);
            TableUtils.deleteTableIfExists(amazonDynamoDB, dtr);
            logJSON(Map.of("action", "tearDownDB",
                           "table", dtr.getTableName()));
    	}
    }

    public static void initDB(AmazonDynamoDB amazonDynamoDB, DynamoDBMapper mapper) throws Exception {
    	CreateTableRequest ctr = mapper.generateCreateTableRequest(GameInfo.class);
    	final ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput(5L, 5L);
    	ctr.setProvisionedThroughput(provisionedThroughput);
        // ctr.getGlobalSecondaryIndexes().forEach(v -> v.setProvisionedThroughput(provisionedThroughput));

        Boolean tableWasCreated = TableUtils.createTableIfNotExists(amazonDynamoDB, ctr);
        logJSON(Map.of("action", "initDB",
                       "operation", "created",
                       "tableWasCreated", tableWasCreated,
                       "table", ctr.getTableName()));

        TableUtils.waitUntilActive(amazonDynamoDB, ctr.getTableName());
        logJSON(Map.of("action", "initDB",
                       "operation", "tableActive",
                       "table", ctr.getTableName()));
    }

    private static void logJSON(Map<?, ?> message) throws JsonProcessingException {
        LOG.info(new ObjectMapper().writeValueAsString(message));
    }

    @Autowired
    private AmazonDynamoDB amazonDynamoDB;
    @Autowired
    private DynamoDBMapper mapper;

    @Override
    public void run(String... args) throws Exception {
        if (Arrays.stream(args).anyMatch("autosetupdb"::equals)) {
            initDB(amazonDynamoDB, mapper);
        }
    }
}