/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.shardingscaling.core.job.preparer.splitter;

import lombok.SneakyThrows;
import org.apache.shardingsphere.shardingscaling.core.config.DataSourceConfiguration;
import org.apache.shardingsphere.shardingscaling.core.config.JDBCDataSourceConfiguration;
import org.apache.shardingsphere.shardingscaling.core.config.RdbmsConfiguration;
import org.apache.shardingsphere.shardingscaling.core.config.SyncConfiguration;
import org.apache.shardingsphere.shardingscaling.core.datasource.DataSourceManager;
import org.apache.shardingsphere.shardingscaling.core.job.task.ScalingTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class InventoryDataTaskSplitterTest {
    
    private static String dataSourceUrl = "jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MySQL";
    
    private static String userName = "root";
    
    private static String password = "password";
    
    private SyncConfiguration syncConfiguration;
    
    private DataSourceManager dataSourceManager;
    
    private InventoryDataTaskSplitter inventoryDataTaskSplitter;
    
    @Before
    public void setUp() {
        RdbmsConfiguration dumperConfig = mockDumperConfig();
        RdbmsConfiguration importerConfig = new RdbmsConfiguration();
        Map<String, String> tableMap = new HashMap<>();
        tableMap.put("t_order", "t_order");
        syncConfiguration = new SyncConfiguration(3, tableMap,
            dumperConfig, importerConfig);
        dataSourceManager = new DataSourceManager();
        inventoryDataTaskSplitter = new InventoryDataTaskSplitter();
    }
    
    @After
    public void tearDown() {
        dataSourceManager.close();
    }
    
    @Test
    public void assertSplitInventoryDataWithIntPrimary() {
        initIntPrimaryEnvironment(syncConfiguration.getDumperConfiguration());
        Collection<ScalingTask> actual = inventoryDataTaskSplitter.splitInventoryData(syncConfiguration, dataSourceManager);
        assertNotNull(actual);
        assertThat(actual.size(), is(3));
    }
    
    @Test
    public void assertSplitInventoryDataWithCharPrimary() {
        initCharPrimaryEnvironment(syncConfiguration.getDumperConfiguration());
        Collection<ScalingTask> actual = inventoryDataTaskSplitter.splitInventoryData(syncConfiguration, dataSourceManager);
        assertNotNull(actual);
        assertThat(actual.size(), is(1));
    }
    
    @Test
    public void assertSplitInventoryDataWithUnionPrimary() {
        initUnionPrimaryEnvironment(syncConfiguration.getDumperConfiguration());
        Collection<ScalingTask> actual = inventoryDataTaskSplitter.splitInventoryData(syncConfiguration, dataSourceManager);
        assertNotNull(actual);
        assertThat(actual.size(), is(1));
    }
    
    @Test
    public void assertSplitInventoryDataWithoutPrimary() {
        initNoPrimaryEnvironment(syncConfiguration.getDumperConfiguration());
        Collection<ScalingTask> actual = inventoryDataTaskSplitter.splitInventoryData(syncConfiguration, dataSourceManager);
        assertNotNull(actual);
        assertThat(actual.size(), is(1));
    }
    
    @SneakyThrows
    private void initIntPrimaryEnvironment(final RdbmsConfiguration dumperConfig) {
        DataSource dataSource = dataSourceManager.getDataSource(dumperConfig.getDataSourceConfiguration());
        try (Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS t_order");
            statement.execute("CREATE TABLE t_order (id INT PRIMARY KEY, user_id VARCHAR(12))");
            statement.execute("INSERT INTO t_order (id, user_id) VALUES (1, 'xxx'), (999, 'yyy')");
        }
    }
    
    @SneakyThrows
    private void initCharPrimaryEnvironment(final RdbmsConfiguration dumperConfig) {
        DataSource dataSource = dataSourceManager.getDataSource(dumperConfig.getDataSourceConfiguration());
        try (Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS t_order");
            statement.execute("CREATE TABLE t_order (id CHAR(3) PRIMARY KEY, user_id VARCHAR(12))");
            statement.execute("INSERT INTO t_order (id, user_id) VALUES ('1', 'xxx'), ('999', 'yyy')");
        }
    }
    
    @SneakyThrows
    private void initUnionPrimaryEnvironment(final RdbmsConfiguration dumperConfig) {
        DataSource dataSource = dataSourceManager.getDataSource(dumperConfig.getDataSourceConfiguration());
        try (Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS t_order");
            statement.execute("CREATE TABLE t_order (id INT, user_id VARCHAR(12), PRIMARY KEY (id, user_id))");
            statement.execute("INSERT INTO t_order (id, user_id) VALUES (1, 'xxx'), (999, 'yyy')");
        }
    }
    
    @SneakyThrows
    private void initNoPrimaryEnvironment(final RdbmsConfiguration dumperConfig) {
        DataSource dataSource = dataSourceManager.getDataSource(dumperConfig.getDataSourceConfiguration());
        try (Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS t_order");
            statement.execute("CREATE TABLE t_order (id INT, user_id VARCHAR(12))");
            statement.execute("INSERT INTO t_order (id, user_id) VALUES (1, 'xxx'), (999, 'yyy')");
        }
    }
    
    private RdbmsConfiguration mockDumperConfig() {
        DataSourceConfiguration dataSourceConfiguration = new JDBCDataSourceConfiguration(dataSourceUrl, userName, password);
        RdbmsConfiguration result = new RdbmsConfiguration();
        result.setDataSourceConfiguration(dataSourceConfiguration);
        return result;
    }
}
