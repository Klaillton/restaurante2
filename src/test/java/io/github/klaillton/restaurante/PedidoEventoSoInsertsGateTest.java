package io.github.klaillton.restaurante;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * Gate do ADR 0002, ponto 4: ligado como {@code app}, a tabela {@code pedido_evento} só aceita
 * INSERT e SELECT. Usa o script de roles real ({@code docker/postgres/init/01-roles.sh}) e a
 * migração Flyway real, corrida como {@code migracao}.
 *
 * <p>Só conta como verde se o erro for exatamente SQLSTATE 42501 (permissão negada). Se a tabela
 * não existir (42P01) ou o INSERT falhar, o teste falha.
 */
@Testcontainers
class PedidoEventoSoInsertsGateTest {

  private static final String PERMISSAO_NEGADA = "42501";
  private static final String MIGRACAO_PASSWORD = UUID.randomUUID().toString();
  private static final String APP_DB_PASSWORD = UUID.randomUUID().toString();

  @Container
  @SuppressWarnings("resource")
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("restaurante")
          .withEnv("MIGRACAO_PASSWORD", MIGRACAO_PASSWORD)
          .withEnv("APP_DB_PASSWORD", APP_DB_PASSWORD)
          .withCopyFileToContainer(
              MountableFile.forHostPath("docker/postgres/init/01-roles.sh", 0755),
              "/docker-entrypoint-initdb.d/01-roles.sh");

  private static final UUID PEDIDO_ID = UUID.randomUUID();
  private static final UUID EVENTO_ID = UUID.randomUUID();

  @BeforeAll
  static void migrarComoMigracaoEGravarUmEventoComoApp() throws SQLException {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), "migracao", MIGRACAO_PASSWORD)
        .schemas("restaurante")
        .createSchemas(false)
        .locations("classpath:db/migration")
        .load()
        .migrate();

    try (Connection app = ligarComoApp()) {
      try (PreparedStatement pedido =
          app.prepareStatement("INSERT INTO pedido (pedido_id, estado) VALUES (?, 'CRIADO')")) {
        pedido.setObject(1, PEDIDO_ID);
        assertThat(pedido.executeUpdate()).isEqualTo(1);
      }
      try (PreparedStatement evento =
          app.prepareStatement(
              "INSERT INTO pedido_evento (evento_id, pedido_id, tipo, payload, ocorrido_em, sequencia)"
                  + " VALUES (?, ?, 'CRIADO', '{}'::jsonb, now(), 1)")) {
        evento.setObject(1, EVENTO_ID);
        evento.setObject(2, PEDIDO_ID);
        assertThat(evento.executeUpdate()).isEqualTo(1);
      }
    }
  }

  @Test
  void appLeOEventoQueGravou() throws SQLException {
    try (Connection app = ligarComoApp();
        PreparedStatement select =
            app.prepareStatement("SELECT count(*) FROM pedido_evento WHERE evento_id = ?")) {
      select.setObject(1, EVENTO_ID);
      try (ResultSet rs = select.executeQuery()) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getLong(1)).isEqualTo(1L);
      }
    }
  }

  @Test
  void appNaoPodeFazerUpdateEmPedidoEvento() throws SQLException {
    assertPermissaoNegada(
        "UPDATE pedido_evento SET tipo = 'PRONTO' WHERE evento_id = '" + EVENTO_ID + "'");
  }

  @Test
  void appNaoPodeFazerDeleteEmPedidoEvento() throws SQLException {
    assertPermissaoNegada("DELETE FROM pedido_evento WHERE evento_id = '" + EVENTO_ID + "'");
  }

  @Test
  void appNaoPodeFazerTruncateEmPedidoEvento() throws SQLException {
    assertPermissaoNegada("TRUNCATE pedido_evento");
  }

  @Test
  void appNaoEDonoDaTabela() throws SQLException {
    assertPermissaoNegada("ALTER TABLE pedido_evento DISABLE TRIGGER ALL");
  }

  @Test
  void appNaoPodeAlterarIdentidadeDoPedido() throws SQLException {
    assertPermissaoNegada(
        "UPDATE pedido SET pedido_id = gen_random_uuid() WHERE pedido_id = '" + PEDIDO_ID + "'");
    assertPermissaoNegada(
        "UPDATE pedido SET criado_em = now() WHERE pedido_id = '" + PEDIDO_ID + "'");
  }

  @Test
  void appNaoPodeApagarPedido() throws SQLException {
    assertPermissaoNegada("DELETE FROM pedido WHERE pedido_id = '" + PEDIDO_ID + "'");
  }

  private static void assertPermissaoNegada(String sql) throws SQLException {
    try (Connection app = ligarComoApp(); Statement st = app.createStatement()) {
      // Numa transação desfeita no fim: se um GRANT a mais deixar o comando passar, este teste
      // falha sem apagar a linha que os outros testes leem.
      app.setAutoCommit(false);
      try {
        assertThatThrownBy(() -> st.execute(sql))
            .isInstanceOf(SQLException.class)
            .extracting(e -> ((SQLException) e).getSQLState())
            .isEqualTo(PERMISSAO_NEGADA);
      } finally {
        app.rollback();
      }
    }
    // E o evento continua lá, intacto.
    try (Connection app = ligarComoApp();
        PreparedStatement select =
            app.prepareStatement("SELECT tipo FROM pedido_evento WHERE evento_id = ?")) {
      select.setObject(1, EVENTO_ID);
      try (ResultSet rs = select.executeQuery()) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(1)).isEqualTo("CRIADO");
      }
    }
  }

  private static Connection ligarComoApp() throws SQLException {
    return DriverManager.getConnection(POSTGRES.getJdbcUrl(), "app", APP_DB_PASSWORD);
  }
}
