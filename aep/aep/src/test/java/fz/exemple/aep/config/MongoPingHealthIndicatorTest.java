package fz.exemple.aep.config;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MongoPingHealthIndicator - health check do Mongo")
class MongoPingHealthIndicatorTest {

    private MongoTemplate mongoTemplate;
    private MongoDatabase mongoDatabase;
    private MongoPingHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        mongoDatabase = mock(MongoDatabase.class);
        when(mongoTemplate.getDb()).thenReturn(mongoDatabase);
        when(mongoDatabase.getName()).thenReturn("poc_doacoes");
        indicator = new MongoPingHealthIndicator(mongoTemplate);
    }

    @Test
    @DisplayName("ping respondido deixa o status UP e informa o banco")
    void pingOkDeixaUp() {
        when(mongoTemplate.executeCommand(anyString())).thenReturn(new Document("ok", 1));

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("poc_doacoes", health.getDetails().get("database"));
    }

    @Test
    @DisplayName("falha na conexao deixa o status DOWN com a causa")
    void falhaDeixaDown() {
        when(mongoTemplate.executeCommand(anyString()))
                .thenThrow(new IllegalStateException("Mongo fora do ar"));

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("poc_doacoes", health.getDetails().get("database"));
        assertNotNull(health.getDetails().get("error"));
    }
}
