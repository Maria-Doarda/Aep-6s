package fz.exemple.aep.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Health check do MongoDB usado em /actuator/health.
 *
 * O indicador padrao do Spring Boot 4 lista todos os bancos visiveis e executa
 * { hello: 1 } em cada um deles - inclusive "local". No MongoDB Atlas o usuario
 * da aplicacao nao tem permissao nesse banco, entao o health virava DOWN mesmo
 * com a API funcionando (atrapalhando o health check do Render).
 *
 * Aqui o ping roda apenas no banco da aplicacao, que e o que interessa.
 */
@Component("mongoHealth")
public class MongoPingHealthIndicator implements HealthIndicator {

    private final MongoTemplate mongoTemplate;

    public MongoPingHealthIndicator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Health health() {
        String banco = mongoTemplate.getDb().getName();
        try {
            mongoTemplate.executeCommand("{ ping: 1 }");
            return Health.up().withDetail("database", banco).build();
        } catch (RuntimeException ex) {
            return Health.down(ex).withDetail("database", banco).build();
        }
    }
}
