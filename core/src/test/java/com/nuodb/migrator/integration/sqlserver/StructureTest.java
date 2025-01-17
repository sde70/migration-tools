/**
 * Copyright (c) 2015, NuoDB, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NuoDB, Inc. nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NUODB, INC. BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nuodb.migrator.integration.sqlserver;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.nuodb.migrator.integration.MigrationTestBase;
import com.nuodb.migrator.integration.precision.MSSQLServerPrecision1;
import com.nuodb.migrator.integration.precision.MSSQLServerPrecision2;
import com.nuodb.migrator.integration.precision.MSSQLServerPrecisions;
import com.nuodb.migrator.integration.types.SQLServerTypes;

/**
 * Test to make sure all the Tables, Constraints, Views, Triggers etc have been
 * migrated.
 * 
 * @author Krishnamoorthy Dhandapani
 */
@Test(groups = { "sqlserverintegrationtest" }, dependsOnGroups = { "dataloadperformed" })
public class StructureTest extends MigrationTestBase {

    /*
     * test if all the Tables are migrated with the right columns
     */
    public void testTables() throws Exception {
        String sqlStr1 = "select TABLE_NAME from INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='BASE TABLE' and "
                + "TABLE_CATALOG = ?";
        String sqlStr2 = "select tablename from system.TABLES where TYPE = 'TABLE' and schema = ?";
        PreparedStatement stmt1 = null, stmt2 = null;
        ResultSet rs1 = null, rs2 = null;
        ArrayList<String> list1 = new ArrayList<String>();
        ArrayList<String> list2 = new ArrayList<String>();
        try {
            stmt1 = sourceConnection.prepareStatement(sqlStr1);
            stmt1.setString(1, sourceConnection.getCatalog());
            rs1 = stmt1.executeQuery();

            Assert.assertNotNull(rs1);
            boolean sourceFound = false;
            while (rs1.next()) {
                sourceFound = true;
                list1.add(rs1.getString(1).toUpperCase());
            }
            Assert.assertTrue(sourceFound);
            Assert.assertFalse(list1.isEmpty());

            stmt2 = nuodbConnection.prepareStatement(sqlStr2);
            stmt2.setString(1, nuodbSchemaUsed);
            rs2 = stmt2.executeQuery();

            Assert.assertNotNull(rs2);
            boolean targetFound = false;
            while (rs2.next()) {
                targetFound = true;
                list2.add(rs2.getString(1).toUpperCase());
            }
            Assert.assertTrue(targetFound);
            Assert.assertFalse(list2.isEmpty());

            for (String tname : list1) {
                Assert.assertTrue(list2.contains(tname));
                verifyTableColumns(tname);
            }

        } finally {
            closeAll(rs1, stmt1, rs2, stmt2);
        }
    }

    /*
     * TODO: Need to add check for complex data types with scale and precision
     */
    private void verifyTableColumns(String tableName) throws Exception {
        String sqlStr1 = "select * from information_schema.COLUMNS where TABLE_CATALOG = ?  and TABLE_NAME = ? order "
                + "by ORDINAL_POSITION";
        String sqlStr2 = "select * from  system.FIELDS F inner join system.DATATYPES D on "
                + "F.DATATYPE = D.ID and F.SCHEMA = ? and F.TABLENAME = ? order by F.FIELDPOSITION";
        String[] colNames = new String[] { "COLUMN_NAME", "ORDINAL_POSITION", "COLUMN_DEFAULT", "IS_NULLABLE",
                "DATA_TYPE", "CHARACTER_MAXIMUM_LENGTH", "NUMERIC_PRECISION", "NUMERIC_SCALE", "CHARACTER_SET_NAME",
                "COLLATION_NAME", "DATETIME_PRECISION" };
        PreparedStatement stmt1 = null, stmt2 = null;
        ResultSet rs1 = null, rs2 = null;
        HashMap<String, HashMap<String, String>> tabColMap = new HashMap<String, HashMap<String, String>>();
        try {
            stmt1 = sourceConnection.prepareStatement(sqlStr1);
            stmt1.setString(1, sourceConnection.getCatalog());
            stmt1.setString(2, tableName);
            rs1 = stmt1.executeQuery();

            Assert.assertNotNull(rs1);
            boolean sourceFound = false;
            while (rs1.next()) {
                sourceFound = true;
                HashMap<String, String> tabColDetailsMap = new HashMap<String, String>();
                for (String colName : colNames) {
                    tabColDetailsMap.put(colName, rs1.getString(colName));
                }
                Assert.assertFalse(tabColDetailsMap.isEmpty(), tableName + " column details empty at source");

                tabColMap.put(tabColDetailsMap.get(colNames[0]), tabColDetailsMap);
            }
            Assert.assertTrue(sourceFound);
            Assert.assertFalse(tabColMap.isEmpty(), tableName + " column details map empty at source");

            stmt2 = nuodbConnection.prepareStatement(sqlStr2);
            stmt2.setString(1, nuodbSchemaUsed);
            stmt2.setString(2, tableName);
            rs2 = stmt2.executeQuery();

            Assert.assertNotNull(rs2);
            boolean targetFound = false;
            while (rs2.next()) {
                targetFound = true;
                String colName = rs2.getString("FIELD");
                HashMap<String, String> tabColDetailsMap = tabColMap.get(colName);
                Assert.assertNotNull(tabColDetailsMap);
                Assert.assertEquals(colName, tabColDetailsMap.get(colNames[0]),
                        "Column name " + colName + " of table " + tableName + " did not match");
                int srcJdbcType = rs2.getInt("JDBCTYPE");
                int tarJdbcType = SQLServerTypes.getMappedJDBCType(tabColDetailsMap.get(colNames[4]),
                        tabColDetailsMap.get(colNames[6]));

                Assert.assertEquals(srcJdbcType, tarJdbcType,
                        "JDBCTYPE of column " + colName + " of table " + tableName + " did not match");
                String srcLength = rs2.getString("LENGTH");
                String tarLength = SQLServerTypes.getMappedLength(tabColDetailsMap.get(colNames[4]),
                        tabColDetailsMap.get(colNames[5]), tabColDetailsMap.get(colNames[6]),
                        tabColDetailsMap.get(colNames[7]), tabColDetailsMap.get(colNames[10]));
                Assert.assertEquals(srcLength, tarLength,
                        "LENGTH of column " + colName + " of table " + tableName + " did not match");
                // TBD
                // String val = tabColDetailsMap.get(colNames[7]);
                // Assert.assertEquals(rs2.getInt("SCALE"), val == null ? 0
                // : Integer.parseInt(val));

                // Assert.assertEquals(rs2.getString("PRECISION"),
                // tabColDetailsMap.get(colNames[6]));

                // String val = tabColDetailsMap.get(colNames[2]);
                Assert.assertEquals(rs2.getString("DEFAULTVALUE"),
                        SQLServerTypes.getMappedDefault(tabColDetailsMap.get(colNames[4]),
                                tabColDetailsMap.get(colNames[2])),
                        "DEFAULTVALUE of column " + colName + " of table " + tableName + " did not match");
            }
            Assert.assertTrue(targetFound);
        } finally {
            closeAll(rs1, stmt1, rs2, stmt2);
        }
    }

    /*
     * test if all the Views are migrated
     */
    @Test(groups = { "disabled" })
    public void testViews() throws Exception {
        // MYSQL Views are not migrated yet.
    }

    /*
     * test if all the Primary Constraints are migrated
     */
    public void testPrimaryKeyConstraints() throws Exception {
        String sqlStr1 = "select CU.TABLE_NAME,CU.COLUMN_NAME from INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                + "as TC join INFORMATION_SCHEMA.KEY_COLUMN_USAGE as CU on CU.CONSTRAINT_SCHEMA = TC.CONSTRAINT_SCHEMA "
                + "and CU.CONSTRAINT_NAME = TC.CONSTRAINT_NAME and CU.TABLE_SCHEMA = TC.TABLE_SCHEMA and "
                + "CU.TABLE_NAME = TC.TABLE_NAME where TC.CONSTRAINT_TYPE IN ( 'PRIMARY KEY' ) AND TC.TABLE_CATALOG = ?";
        String sqlStr2 = "SELECT FIELD FROM SYSTEM.INDEXES INNER JOIN SYSTEM.INDEXFIELDS ON "
                + "INDEXES.SCHEMA=INDEXFIELDS.SCHEMA AND " + "INDEXES.TABLENAME=INDEXFIELDS.TABLENAME AND "
                + "INDEXES.INDEXNAME=INDEXFIELDS.INDEXNAME WHERE SCHEMA=? AND TABLENAME=? AND INDEXNAME LIKE "
                + "'%PRIMARY_KEY%'";
        PreparedStatement stmt1 = null, stmt2 = null;
        ResultSet rs1 = null, rs2 = null;
        try {
            stmt1 = sourceConnection.prepareStatement(sqlStr1);
            stmt1.setString(1, sourceConnection.getCatalog());
            rs1 = stmt1.executeQuery();

            Assert.assertNotNull(rs1);
            boolean sourceFound = false;
            while (rs1.next()) {
                sourceFound = true;
                String tName = rs1.getString("TABLE_NAME");
                String cName = rs1.getString("COLUMN_NAME");
                stmt2 = nuodbConnection.prepareStatement(sqlStr2);
                stmt2.setString(1, nuodbSchemaUsed);
                stmt2.setString(2, tName);
                rs2 = stmt2.executeQuery();
                boolean found = false;
                while (rs2.next()) {
                    found = true;
                    String tarCol = rs2.getString(1);
                    Assert.assertEquals(tarCol, cName, "Source column name " + cName + " of table " + tName
                            + " did not match with target column name ");
                }
                Assert.assertTrue(found);
                rs2.close();
                stmt2.close();
            }
            Assert.assertTrue(sourceFound);
        } finally {
            closeAll(rs1, stmt1, rs2, stmt2);
        }
    }

    /*
     * test if all the Unique Key Constraints are migrated
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testUniqueKeyConstraints() throws Exception {
        String sqlStr1 = "select CU.TABLE_NAME,CU.COLUMN_NAME from INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                + "as TC join INFORMATION_SCHEMA.KEY_COLUMN_USAGE as CU on CU.CONSTRAINT_SCHEMA = TC.CONSTRAINT_SCHEMA "
                + "and CU.CONSTRAINT_NAME = TC.CONSTRAINT_NAME and CU.TABLE_SCHEMA = TC.TABLE_SCHEMA and "
                + "CU.TABLE_NAME = TC.TABLE_NAME where TC.CONSTRAINT_TYPE IN ( 'UNIQUE' ) AND TC.TABLE_CATALOG = ?";
        String sqlStr2 = "SELECT FIELD FROM SYSTEM.INDEXES INNER JOIN SYSTEM.INDEXFIELDS ON "
                + "INDEXES.SCHEMA=INDEXFIELDS.SCHEMA AND " + "INDEXES.TABLENAME=INDEXFIELDS.TABLENAME AND "
                + "INDEXES.INDEXNAME=INDEXFIELDS.INDEXNAME WHERE SCHEMA=? AND TABLENAME=? AND INDEXNAME LIKE '%UNIQUE%'";
        PreparedStatement stmt1 = null, stmt2 = null;
        ResultSet rs1 = null, rs2 = null;
        HashMap map = new HashMap();
        try {
            stmt1 = sourceConnection.prepareStatement(sqlStr1);
            stmt1.setString(1, sourceConnection.getCatalog());
            rs1 = stmt1.executeQuery();
            Assert.assertNotNull(rs1);
            boolean sourceFound = false;
            while (rs1.next()) {
                sourceFound = true;
                String tName = rs1.getString("TABLE_NAME");
                String cName = rs1.getString("COLUMN_NAME");
                if (!map.containsKey(tName)) {
                    ArrayList list = new ArrayList();
                    list.add(cName);
                    map.put(tName, list);
                } else {
                    ArrayList list = (ArrayList) map.get(tName);
                    list.add(cName);
                    map.put(tName, list);
                }
            }
            Assert.assertTrue(sourceFound);
            Iterator<Entry<String, ArrayList>> uniKey = map.entrySet().iterator();
            while (uniKey.hasNext()) {
                Entry<String, ArrayList> pairs = uniKey.next();
                String srcTname = pairs.getKey();
                ArrayList<String> srcColList = pairs.getValue();
                ArrayList<String> tarColList = new ArrayList<String>();
                stmt2 = nuodbConnection.prepareStatement(sqlStr2);
                stmt2.setString(1, nuodbSchemaUsed);
                stmt2.setString(2, srcTname);
                rs2 = stmt2.executeQuery();
                boolean found = false;
                while (rs2.next()) {
                    found = true;
                    tarColList.add(rs2.getString(1));
                }
                Assert.assertTrue(found);
                Assert.assertEquals(srcColList, tarColList);
                rs2.close();
                stmt2.close();
            }

        } finally {
            closeAll(rs1, stmt1, rs2, stmt2);
        }
    }

    /*
     * test if all the Check Constraints are migrated
     */
    @Test(groups = { "disabled" })
    public void testCheckConstraints() throws Exception {
        // MYSQL Does not have any implementations for CHECK constraints
    }

    /*
     * test if all the Foreign Key Constraints are migrated
     */
    public void testForeignKeyConstraints() throws Exception {
        String sqlStr1 = "SELECT  OBJECT_NAME(fk.parent_object_id) 'Parent table',c1.name 'Parent column',"
                + "OBJECT_NAME(fk.referenced_object_id) "
                + "'Referenced table',c2.name 'Referenced column' FROM sys.foreign_keys fk INNER JOIN sys"
                + ".foreign_key_columns fkc ON "
                + "fkc.constraint_object_id = fk.object_id INNER JOIN sys.columns c1 ON fkc.parent_column_id = "
                + "c1.column_id AND "
                + "fkc.parent_object_id = c1.object_id INNER JOIN sys.columns c2 ON fkc.referenced_column_id = "
                + "c2.column_id AND" + " fkc.referenced_object_id = c2.object_id ";

        String sqlStr2 = "SELECT PRIMARYTABLE.SCHEMA AS PKTABLE_SCHEM, PRIMARYTABLE.TABLENAME AS PKTABLE_NAME, "
                + " PRIMARYFIELD.FIELD AS PKCOLUMN_NAME, FOREIGNTABLE.SCHEMA AS FKTABLE_SCHEM, "
                + " FOREIGNTABLE.TABLENAME AS FKTABLE_NAME, FOREIGNFIELD.FIELD AS FKCOLUMN_NAME, "
                + " FOREIGNKEYS.POSITION+1 AS KEY_SEQ, FOREIGNKEYS.UPDATERULE AS UPDATE_RULE, "
                + " FOREIGNKEYS.DELETERULE AS DELETE_RULE, FOREIGNKEYS.DEFERRABILITY AS DEFERRABILITY "
                + "FROM SYSTEM.FOREIGNKEYS "
                + "INNER JOIN SYSTEM.TABLES PRIMARYTABLE ON PRIMARYTABLEID=PRIMARYTABLE.TABLEID "
                + "INNER JOIN SYSTEM.FIELDS PRIMARYFIELD ON PRIMARYTABLE.SCHEMA=PRIMARYFIELD.SCHEMA "
                + "AND PRIMARYTABLE.TABLENAME=PRIMARYFIELD.TABLENAME "
                + "AND FOREIGNKEYS.PRIMARYFIELDID=PRIMARYFIELD.FIELDID "
                + "INNER JOIN SYSTEM.TABLES FOREIGNTABLE ON FOREIGNTABLEID=FOREIGNTABLE.TABLEID "
                + "INNER JOIN SYSTEM.FIELDS FOREIGNFIELD ON FOREIGNTABLE.SCHEMA=FOREIGNFIELD.SCHEMA "
                + "AND FOREIGNTABLE.TABLENAME=FOREIGNFIELD.TABLENAME "
                + "AND FOREIGNKEYS.FOREIGNFIELDID=FOREIGNFIELD.FIELDID "
                + "WHERE FOREIGNTABLE.SCHEMA=? AND FOREIGNTABLE.TABLENAME=? ORDER BY PKTABLE_SCHEM, PKTABLE_NAME, "
                + "KEY_SEQ ASC";

        PreparedStatement stmt1 = null, stmt2 = null;
        ResultSet rs1 = null, rs2 = null;
        try {
            stmt1 = sourceConnection.prepareStatement(sqlStr1);
            // stmt1.setString(1, sourceConnection.getCatalog());
            rs1 = stmt1.executeQuery();

            Assert.assertNotNull(rs1);
            boolean sourceFound = false;
            while (rs1.next()) {
                sourceFound = true;
                String tName = rs1.getString("Parent table");
                String cName = rs1.getString("Parent column");
                String rtName = rs1.getString("Referenced table");
                String rcName = rs1.getString("Referenced column");

                stmt2 = nuodbConnection.prepareStatement(sqlStr2);
                stmt2.setString(1, nuodbSchemaUsed);
                stmt2.setString(2, tName);
                rs2 = stmt2.executeQuery();
                boolean found = false;
                while (rs2.next()) {
                    found = true;
                    Assert.assertEquals(rs2.getString("FKTABLE_SCHEM"), rs2.getString("PKTABLE_SCHEM"),
                            "Foreign key and Primary key Schema did not match");
                    Assert.assertEquals(rs2.getString("FKTABLE_NAME"), tName, "Foreign key table name did not match");
                    Assert.assertEquals(rs2.getString("FKCOLUMN_NAME"), cName,
                            "Foreign key column name" + rs2.getString("FKCOLUMN_NAME") + "of table"
                                    + rs2.getString("FKTABLE_NAME") + "did not match");
                    Assert.assertEquals(rs2.getString("PKTABLE_NAME"), rtName, "Primary key table name did not match");
                    Assert.assertEquals(rs2.getString("PKCOLUMN_NAME"), rcName,
                            "Primary key column name" + rs2.getString("PKCOLUMN_NAME") + "of table"
                                    + rs2.getString("PKTABLE_NAME") + "did not match");
                }
                Assert.assertTrue(found);
                rs2.close();
                stmt2.close();
            }
            Assert.assertTrue(sourceFound);
        } finally {
            closeAll(rs1, stmt1, rs2, stmt2);
        }
    }

    /*
     * test if all the auto increment settings are migrated
     */
    @Test(groups = { "disabled" })
    public void testAutoIncrement() throws Exception {
        String sqlStr1 = "SELECT  t.TABLE_NAME,c.COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                + "AS c JOIN INFORMATION_SCHEMA.TABLES AS t ON t.TABLE_NAME = c.TABLE_NAME "
                + "WHERE COLUMNPROPERTY(OBJECT_ID(c.TABLE_NAME),c.COLUMN_NAME,'IsIdentity') = 1 AND"
                + " t.TABLE_TYPE = 'Base Table' AND t.TABLE_NAME NOT LIKE 'dt%' AND t.TABLE_NAME "
                + "NOT LIKE 'MS%' AND t.TABLE_NAME NOT LIKE 'syncobj_%' AND t.TABLE_CATALOG= ?";
        String sqlStr2 = "SELECT S.SEQUENCENAME FROM SYSTEM.SEQUENCES S "
                + "INNER JOIN SYSTEM.FIELDS F ON S.SCHEMA=F.SCHEMA "
                + "WHERE F.SCHEMA=? AND F.TABLENAME=? AND F.FIELD=?";
        PreparedStatement stmt1 = null, stmt2 = null;
        ResultSet rs1 = null, rs2 = null;
        try {
            stmt1 = sourceConnection.prepareStatement(sqlStr1);
            stmt1.setString(1, sourceConnection.getCatalog());
            rs1 = stmt1.executeQuery();

            Assert.assertNotNull(rs1);
            boolean sourceFound = false;
            while (rs1.next()) {
                sourceFound = true;
                String tName = rs1.getString("TABLE_NAME");
                String cName = rs1.getString("COLUMN_NAME");
                // long ai = rs1.getLong("AUTO_INCREMENT");
                stmt2 = nuodbConnection.prepareStatement(sqlStr2);
                stmt2.setString(1, nuodbSchemaUsed);
                stmt2.setString(2, tName);
                stmt2.setString(3, cName);
                rs2 = stmt2.executeQuery();
                boolean found = false;
                while (rs2.next()) {
                    found = true;
                    String seqName = rs2.getString("SEQUENCENAME");
                    Assert.assertNotNull(seqName);
                    if (seqName.equals(nuodbSchemaUsed + "$" + "IDENTITY_SEQUENCE")) {
                        continue;
                    }
                    Assert.assertEquals(seqName.substring(0, 4), "SEQ_");
                    // TODO: Need to check start value - Don't know how yet
                }
                Assert.assertTrue(found);
                rs2.close();
                stmt2.close();
            }
            Assert.assertTrue(sourceFound);
        } finally {
            closeAll(rs1, stmt1, rs2, stmt2);
        }
    }

    /*
     * test if all the Indexes are migrated
     */
    public void testIndexes() throws Exception {
        String sqlStr1 = "SELECT ind.name ,col.name,t.name FROM sys.indexes ind INNER JOIN sys.index_columns ic ON  "
                + "ind.object_id = ic.object_id and ind.index_id = ic.index_id INNER JOIN sys.columns col ON "
                + "ic.object_id = col.object_id and ic.column_id = col.column_id INNER JOIN sys.tables t ON "
                + "ind.object_id = t.object_id WHERE (1=1) AND ind.is_primary_key = 0 AND ind.is_unique = 0 AND "
                + "ind.is_unique_constraint = 0 AND t.is_ms_shipped = 0 "
                + "ORDER BY t.name, ind.name, ind.index_id, ic.index_column_id ";
        String sqlStr2 = "SELECT I.INDEXNAME FROM SYSTEM.INDEXES I "
                + "INNER JOIN SYSTEM.INDEXFIELDS F ON I.SCHEMA=F.SCHEMA AND I.TABLENAME=F.TABLENAME AND I.INDEXNAME=F.INDEXNAME "
                + "WHERE I.INDEXTYPE=2 AND F.SCHEMA=? AND F.TABLENAME=? AND F.FIELD=?";
        PreparedStatement stmt1 = null, stmt2 = null;
        ResultSet rs1 = null, rs2 = null;
        try {
            stmt1 = sourceConnection.prepareStatement(sqlStr1);
            // stmt1.setString(1, sourceConnection.getCatalog());
            rs1 = stmt1.executeQuery();

            Assert.assertNotNull(rs1);
            boolean sourceFound = false;
            while (rs1.next()) {
                sourceFound = true;
                // String iName = rs1.getString(1);
                String cName = rs1.getString(2);
                String tName = rs1.getString(3);
                stmt2 = nuodbConnection.prepareStatement(sqlStr2);
                stmt2.setString(1, nuodbSchemaUsed);
                stmt2.setString(2, tName);
                stmt2.setString(3, cName);
                rs2 = stmt2.executeQuery();
                boolean found = false;
                while (rs2.next()) {
                    found = true;
                    String idxName = rs2.getString("INDEXNAME");
                    Assert.assertNotNull(idxName);
                    Assert.assertEquals(idxName.substring(0, 4), "IDX_");
                }
                Assert.assertTrue(found);
                rs2.close();
                stmt2.close();
            }
            Assert.assertTrue(sourceFound);
        } finally {
            closeAll(rs1, stmt1, rs2, stmt2);
        }
    }

    /*
     * test if all the Triggers are migrated
     */
    @Test(groups = { "disabled" })
    public void testTriggers() throws Exception {
        // MYSQL Triggers are not migrated yet.
    }

    /*
     * test the precisions and scale are migrated properly
     */
    public void testPrecisions() throws Exception {
        // String sqlStr2 =
        // "select
        // p1.c1,p1.c2,p1.c3,p1.c4,p1.c5,p2.c1,p2.c2,p2.c3,p2.c4,p2.c5,p2.c6,p2.c7
        // from precision1 as p1,precision2 as p2";
        // String sqlStr2 = "select count(*) from \"datatypes1\"";
        String sqlStr1 = "select * from precision1";
        String sqlStr2 = "select * from precision2";
        PreparedStatement stmt1 = null, stmt2 = null;
        ResultSet rs1 = null, rs2 = null;
        try {
            try {
                stmt2 = nuodbConnection.prepareStatement(sqlStr1);
                rs2 = stmt2.executeQuery();
                Collection<MSSQLServerPrecision1> expList = new ArrayList<MSSQLServerPrecision1>();
                Collection<MSSQLServerPrecision1> actList = MSSQLServerPrecisions.getMSSQLServerPrecision1();
                while (rs2.next()) {
                    MSSQLServerPrecision1 obj = new MSSQLServerPrecision1(rs2.getInt(1), rs2.getLong(2), rs2.getInt(3),
                            rs2.getInt(4));
                    expList.add(obj);
                }
                Assert.assertEquals(actList, expList);
                rs2.close();
                stmt2.close();

            } catch (SQLException e) {

            }
            stmt2 = nuodbConnection.prepareStatement(sqlStr2);
            rs2 = stmt2.executeQuery();
            Collection<MSSQLServerPrecision2> expList = new ArrayList<MSSQLServerPrecision2>();
            Collection<MSSQLServerPrecision2> actList = MSSQLServerPrecisions.getMSSQLServerPrecision2();
            while (rs2.next()) {
                MSSQLServerPrecision2 obj = new MSSQLServerPrecision2(rs2.getString(1), rs2.getString(2),
                        rs2.getDouble(3), rs2.getString(4), rs2.getInt(5), rs2.getDouble(6), rs2.getDouble(7),
                        rs2.getString(8));
                expList.add(obj);
            }
            Assert.assertEquals(actList, expList);
            rs2.close();
            stmt2.close();
        } finally {
            closeAll(rs2, stmt2);
        }
    }
}
