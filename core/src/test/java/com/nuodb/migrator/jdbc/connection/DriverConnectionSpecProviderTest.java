package com.nuodb.migrator.jdbc.connection;

import com.nuodb.migrator.spec.DriverConnectionSpec;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Properties;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class DriverConnectionSpecProviderTest {

    private static final String URL = "jdbc:com.nuodb://localhost/test";

    private static final int TRANSACTION_ISOLATION = Connection.TRANSACTION_READ_COMMITTED;

    private DriverConnectionSpec connectionSpec;
    private Driver driver;
    private Connection connection;

    @BeforeMethod
    public void init() throws Exception {
        connection = mock(Connection.class);
        driver = mock(Driver.class);
        when(driver.connect(anyString(), any(Properties.class))).thenReturn(connection);
        when(driver.acceptsURL(URL)).thenReturn(true);

        connectionSpec = mock(DriverConnectionSpec.class);
        when(connectionSpec.getDriver()).thenReturn(driver);
        when(connectionSpec.getUrl()).thenReturn(URL);
        when(connectionSpec.getUsername()).thenReturn("username");
        when(connectionSpec.getPassword()).thenReturn("password");
    }

    @Test
    public void testGetConnection() throws Exception {
        DriverConnectionSpecProvider connectionProvider = new DriverConnectionSpecProvider(connectionSpec);
        connectionProvider.setTransactionIsolation(TRANSACTION_ISOLATION);
        connectionProvider.setAutoCommit(false);

        assertNotNull(connectionProvider.getConnection());
        verify(connectionSpec).getDriver();
        verify(connectionSpec).getDriverClassName();
        verify(connectionSpec).getUsername();
        verify(connectionSpec).getPassword();
        verify(connection).setTransactionIsolation(anyInt());
        verify(connection).setAutoCommit(false);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        DriverManager.deregisterDriver(driver);
    }
}
