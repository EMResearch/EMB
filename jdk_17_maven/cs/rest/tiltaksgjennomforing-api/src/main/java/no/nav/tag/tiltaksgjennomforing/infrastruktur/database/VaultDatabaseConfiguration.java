package no.nav.tag.tiltaksgjennomforing.infrastruktur.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;
import no.nav.vault.jdbc.hikaricp.VaultError;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile({ Miljø.DEV_FSS, Miljø.PROD_FSS })
public class VaultDatabaseConfiguration {
    private final DatabaseProperties config;

    @Autowired
    public VaultDatabaseConfiguration(DatabaseProperties config) {
        this.config = config;
    }

    @Bean
    public DataSource userDataSource() {
        return dataSource("user");
    }

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            Flyway.configure()
                    .dataSource(dataSource("admin"))
                    .initSql(String.format("SET ROLE \"%s\"", dbRole("admin")))
                    .load()
                    .migrate();
        };
    }

    private HikariDataSource dataSource(String user) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getDatabaseUrl());
        if (config.getMaximumPoolSize() != null) {
            hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
        }
        if (config.getMinimumIdle() != null) {
            hikariConfig.setMinimumIdle(config.getMinimumIdle());
        }
        if (config.getMaxLifetime() != null) {
            hikariConfig.setMaxLifetime(config.getMaxLifetime());
        }
        try {
            return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(hikariConfig, config.getVaultSti(), dbRole(user));
        } catch (VaultError vaultError) {
            throw new BeanCreationException("Feil ved henting av credentials fra Vault: " + user, vaultError);
        }
    }

    private String dbRole(String role) {
        return config.getDatabaseNavn() + "-" + role;
    }
}
