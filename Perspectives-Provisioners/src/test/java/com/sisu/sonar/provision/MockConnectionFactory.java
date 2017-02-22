package com.sisu.sonar.provision;

import com.attivio.sdk.server.sonar.provision.ProvisionConnectionFactory;
import com.attivio.sdk.server.sonar.provision.ProvisionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.mockito.Mockito.*;


/**
 * Created by dave on 10/20/16.
 */
public class MockConnectionFactory implements ProvisionConnectionFactory {

    Logger log = LoggerFactory.getLogger(this.getClass());

    Connection conn;
    PreparedStatement stmt;
    ResultSet rs;

    @Override
    public Connection connect() throws ProvisionException {
        conn = mock(Connection.class);
        stmt = mock(PreparedStatement.class);
        rs = mock(ResultSet.class);


        try {
            when(conn.prepareStatement("SELECT * FROM ?")).thenReturn(stmt);
            when(stmt.executeQuery()).thenReturn(rs);
            when(rs.getString(0)).thenReturn("1");
            when(rs.getString(1)).thenReturn("Macbook Pro");
            when(rs.getFloat(2)).thenReturn(1500.0f);

        }catch (SQLException sql) {
            log.error("error in mock connection", sql.getCause());
        }


        return conn;
    }
}
