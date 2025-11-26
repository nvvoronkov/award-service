package org.example;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Test
    void shouldHaveEmployeeAndAwardTables() {
        Mono<Boolean> check = Mono.usingWhen(
                connectionFactory.create(),
                conn -> Flux.from(conn.createStatement(
                                        """
                                           SELECT table_name 
                                           FROM information_schema.tables 
                                           WHERE table_name IN ('employee', 'award')
                                           """)
                                .execute())
                        .flatMap(result ->
                                Flux.from(result.map((row, meta) -> row.get("table_name", String.class))))
                        .collectList()
                        .map(list -> list.contains("employee") && list.contains("award")),
                conn -> conn.close()
        );

        assertThat(check.block()).isTrue();
    }
}