package au.com.venilia.switching.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import au.com.venilia.switching.domain.Switch;
import au.com.venilia.switching.exception.SwitchNotFoundException;
import au.com.venilia.switching.service.SwitchService.Circuit;
import au.com.venilia.switching.service.SwitchService.CircuitState;

public class SwitchDao {

    private static final Logger LOG = LoggerFactory.getLogger(SwitchDao.class);

    private final Connection connection;

    public SwitchDao(final Connection connection) {

        this.connection = connection;
    }

    public void createOrUpdate(final Switch swich) {

        if (swich.getId() == null) {

            LOG.debug("Inserting new Switch record - {}", swich);

            try (final PreparedStatement pstmt =
                    connection.prepareStatement("INSERT INTO swich(circuit, state) VALUES (?, ?)")) {

                pstmt.setString(1, swich.getCircuit().name());

                pstmt.setString(2, swich.getState().name());

                if (pstmt.executeUpdate() != 1)
                    throw new SQLException("No rows updated");
            } catch (final SQLException e) {

                throw new RuntimeException(e);
            }
        } else {

            LOG.debug("Updating Switch record - {}", swich);

            try (final PreparedStatement pstmt =
                    connection.prepareStatement("UPDATE swich SET circuit = ?, state = ? WHERE id = ?")) {

                pstmt.setString(1, swich.getCircuit().name());

                pstmt.setString(2, swich.getState().name());

                pstmt.setInt(3, swich.getId());

                if (pstmt.executeUpdate() != 1)
                    throw new RuntimeException(new SwitchNotFoundException(swich.getId()));
            } catch (final SQLException e) {

                throw new RuntimeException(e);
            }
        }
    }

    public Switch get(final int id) throws SwitchNotFoundException {

        LOG.debug("Fetching Switch record {}", id);

        try (final Statement stmt = connection.createStatement()) {

            final ResultSet rs = stmt
                    .executeQuery("SELECT circuit, state FROM swich WHERE id = " + id);

            while (rs.next()) {

                try {

                    final Circuit circuit = Circuit.valueOf(rs.getString(1));
                    return new Switch(id, circuit, CircuitState.valueOf(rs.getString(2)));
                } catch (final IllegalArgumentException e) {

                    LOG.warn(String.format("No Circuit type named %s - %s", rs.getString(1), e.getMessage()));
                    delete(rs.getString(1));
                }
            }

            throw new SwitchNotFoundException(id);
        } catch (final SQLException e) {

            throw new RuntimeException(e);
        }
    }

    public Switch getByCircuit(final Circuit circuit) {

        LOG.debug("Fetching Switch record {}", circuit);

        try (final Statement stmt = connection.createStatement()) {

            final ResultSet rs = stmt
                    .executeQuery("SELECT id, state FROM swich WHERE circuit = '" + circuit.name() + "'");

            while (rs.next())
                return new Switch(rs.getInt(1), circuit, CircuitState.valueOf(rs.getString(2)));

            return null;
        } catch (final SQLException e) {

            throw new RuntimeException(e);
        }
    }

    public Set<Switch> list() {

        LOG.debug("Fetching all Switch records");

        final Set<Switch> switches = Sets.newHashSet();

        try (final Statement stmt = connection.createStatement()) {

            final ResultSet rs = stmt
                    .executeQuery("SELECT id, circuit, state FROM swich");

            while (rs.next()) {

                try {

                    final Circuit circuit = Circuit.valueOf(rs.getString(2));
                    switches.add(new Switch(rs.getInt(1), circuit, CircuitState.valueOf(rs.getString(3))));
                } catch (final IllegalArgumentException e) {

                    LOG.warn(String.format("No Circuit type named %s - %s", rs.getString(1), e.getMessage()));
                    delete(rs.getString(2));
                }
            }
        } catch (final SQLException e) {

            throw new RuntimeException(e);
        }

        return switches;
    }

    private void delete(final String circuitConstant) {

        LOG.debug("Deleting Switch record {}", circuitConstant);

        try (final Statement stmt = connection.createStatement()) {

            final int rowsUpdated = stmt
                    .executeUpdate("DELETE FROM swich WHERE circuit = '" + circuitConstant + "'");

            LOG.trace("%d rows updated", rowsUpdated);
        } catch (final SQLException e) {

            throw new RuntimeException(e);
        }
    }
}
