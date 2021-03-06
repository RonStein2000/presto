/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.jdbc;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;

public class PrestoDriver
        implements Driver, Closeable
{
    static final String DRIVER_NAME = "Presto JDBC Driver";
    static final String DRIVER_VERSION;
    static final int DRIVER_VERSION_MAJOR;
    static final int DRIVER_VERSION_MINOR;

    private static final DriverPropertyInfo[] DRIVER_PROPERTY_INFOS = {};

    private static final String DRIVER_URL_START = "jdbc:presto:";

    private static final String USER_PROPERTY = "user";

    private final QueryExecutor queryExecutor;

    static {
        String version = PrestoDriver.class.getPackage().getImplementationVersion();
        if (version == null) {
            DRIVER_VERSION = "unknown";
            DRIVER_VERSION_MAJOR = 0;
            DRIVER_VERSION_MINOR = 0;
        }
        else {
            DRIVER_VERSION = version;
            List<String> parts = Splitter.on(CharMatcher.anyOf(".-")).limit(3).splitToList(version);
            DRIVER_VERSION_MAJOR = parseInt(parts.get(0));
            DRIVER_VERSION_MINOR = parseInt(parts.get(1));
        }

        try {
            DriverManager.registerDriver(new PrestoDriver());
        }
        catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    public PrestoDriver()
    {
        this.queryExecutor = QueryExecutor.create(DRIVER_NAME + "/" + DRIVER_VERSION);
    }

    @Override
    public void close()
    {
        queryExecutor.close();
    }

    @Override
    public Connection connect(String url, Properties info)
            throws SQLException
    {
        if (!acceptsURL(url)) {
            return null;
        }

        String user = info.getProperty(USER_PROPERTY);
        if (isNullOrEmpty(user)) {
            throw new SQLException(format("Username property (%s) must be set", USER_PROPERTY));
        }

        return new PrestoConnection(new PrestoDriverUri(url), user, queryExecutor);
    }

    @Override
    public boolean acceptsURL(String url)
            throws SQLException
    {
        return url.startsWith(DRIVER_URL_START);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
            throws SQLException
    {
        return DRIVER_PROPERTY_INFOS;
    }

    @Override
    public int getMajorVersion()
    {
        return DRIVER_VERSION_MAJOR;
    }

    @Override
    public int getMinorVersion()
    {
        return DRIVER_VERSION_MINOR;
    }

    @Override
    public boolean jdbcCompliant()
    {
        // TODO: pass compliance tests
        return false;
    }

    @Override
    public Logger getParentLogger()
            throws SQLFeatureNotSupportedException
    {
        // TODO: support java.util.Logging
        throw new SQLFeatureNotSupportedException();
    }
}
