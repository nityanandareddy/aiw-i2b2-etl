package edu.emory.cci.aiw.i2b2etl.ksb;

/*
 * #%L
 * AIW i2b2 ETL
 * %%
 * Copyright (C) 2012 - 2015 Emory University
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import edu.emory.cci.aiw.i2b2etl.util.RecordHandler;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.arp.javautil.sql.DatabaseProduct;

/**
 *
 * @author Andrew Post
 */
public class UniqueIdTempTableHandler extends RecordHandler<String> {

    public UniqueIdTempTableHandler(Connection connection) throws SQLException {
        super(connection, "INSERT INTO EK_TEMP_UNIQUE_IDS VALUES (?)");
        createTempTableIfNeeded();
    }
    
    public UniqueIdTempTableHandler(Connection connection, boolean commit) throws SQLException {
        super(connection, "INSERT INTO EK_TEMP_UNIQUE_IDS VALUES (?)", commit);
        createTempTableIfNeeded();
    }
    
    @Override
    protected void setParameters(PreparedStatement statement, String record) throws SQLException {
        statement.setString(1, record);
    }
    
    private void createTempTableIfNeeded() throws SQLException {
        Connection cn = getConnection();
        
        switch (DatabaseProduct.fromMetaData(cn.getMetaData())) {
            case POSTGRESQL:
                try (Statement stmt = cn.createStatement()) {
                    stmt.execute("CREATE IF NOT EXISTS GLOBAL TEMPORARY TABLE EK_TEMP_UNIQUE_IDS (UNIQUE_ID VARCHAR(700)) ON COMMIT DELETE ROWS");
                }
                break;
            default:
                break;
        }
    }

}
