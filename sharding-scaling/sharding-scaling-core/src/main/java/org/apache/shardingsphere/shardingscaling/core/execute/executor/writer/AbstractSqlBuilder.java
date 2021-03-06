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

package org.apache.shardingsphere.shardingscaling.core.execute.executor.writer;

import com.google.common.collect.Collections2;
import org.apache.shardingsphere.shardingscaling.core.execute.executor.record.Column;
import org.apache.shardingsphere.shardingscaling.core.execute.executor.record.DataRecord;
import org.apache.shardingsphere.shardingscaling.core.execute.executor.record.RecordUtil;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Abstract SQL builder.
 */
public abstract class AbstractSqlBuilder {
    
    private static final String INSERT_SQL_CACHE_KEY_PREFIX = "INSERT_";
    
    private static final String UPDATE_SQL_CACHE_KEY_PREFIX = "UPDATE_";
    
    private static final String DELETE_SQL_CACHE_KEY_PREFIX = "DELETE_";
    
    private final ConcurrentMap<String, String> sqlCacheMap = new ConcurrentHashMap<>();
    
    /**
     * Get left identifier quote string.
     *
     * @return string
     */
    protected abstract String getLeftIdentifierQuoteString();
    
    /**
     * Get right identifier quote string.
     *
     * @return string
     */
    protected abstract String getRightIdentifierQuoteString();
    
    /**
     * Build insert SQL.
     *
     * @param dataRecord data record
     * @return insert SQL
     */
    public String buildInsertSQL(final DataRecord dataRecord) {
        String sqlCacheKey = INSERT_SQL_CACHE_KEY_PREFIX + dataRecord.getTableName();
        if (!sqlCacheMap.containsKey(sqlCacheKey)) {
            sqlCacheMap.put(sqlCacheKey, buildInsertSQLInternal(dataRecord.getTableName(), dataRecord.getColumns()));
        }
        return sqlCacheMap.get(sqlCacheKey);
    }
    
    private String buildInsertSQLInternal(final String tableName, final List<Column> columns) {
        StringBuilder columnsLiteral = new StringBuilder();
        StringBuilder holder = new StringBuilder();
        for (Column each : columns) {
            columnsLiteral.append(String.format("%s%s%s,", getLeftIdentifierQuoteString(), each.getName(), getRightIdentifierQuoteString()));
            holder.append("?,");
        }
        columnsLiteral.setLength(columnsLiteral.length() - 1);
        holder.setLength(holder.length() - 1);
        return String.format("INSERT INTO %s%s%s(%s) VALUES(%s)", getLeftIdentifierQuoteString(), tableName, getRightIdentifierQuoteString(), columnsLiteral.toString(), holder.toString());
    }
    
    /**
     * Build update SQL.
     *
     * @param dataRecord data record
     * @return update SQL
     */
    public String buildUpdateSQL(final DataRecord dataRecord) {
        String sqlCacheKey = UPDATE_SQL_CACHE_KEY_PREFIX + dataRecord.getTableName();
        if (!sqlCacheMap.containsKey(sqlCacheKey)) {
            sqlCacheMap.put(sqlCacheKey, buildUpdateSQLInternal(dataRecord.getTableName(), RecordUtil.extractPrimaryColumns(dataRecord)));
        }
        StringBuilder updatedColumnString = new StringBuilder();
        for (Column each : extractUpdatedColumns(dataRecord.getColumns())) {
            updatedColumnString.append(String.format("%s%s%s = ?,", getLeftIdentifierQuoteString(), each.getName(), getRightIdentifierQuoteString()));
        }
        updatedColumnString.setLength(updatedColumnString.length() - 1);
        return String.format(sqlCacheMap.get(sqlCacheKey), updatedColumnString.toString());
    }
    
    private String buildUpdateSQLInternal(final String tableName, final Collection<Column> extractPrimaryColumns) {
        StringBuilder where = new StringBuilder();
        for (Column each : extractPrimaryColumns) {
            where.append(String.format("%s%s%s = ?,", getLeftIdentifierQuoteString(), each.getName(), getRightIdentifierQuoteString()));
        }
        where.setLength(where.length() - 1);
        return String.format("UPDATE %s%s%s SET %%s WHERE %s", getLeftIdentifierQuoteString(), tableName, getRightIdentifierQuoteString(), where.toString());
    }
    
    private Collection<Column> extractUpdatedColumns(final Collection<Column> columns) {
        return Collections2.filter(columns, Column::isUpdated);
    }
    
    /**
     * Build delete SQL.
     *
     * @param dataRecord data record
     * @return delete SQL
     */
    public String buildDeleteSQL(final DataRecord dataRecord) {
        String sqlCacheKey = DELETE_SQL_CACHE_KEY_PREFIX + dataRecord.getTableName();
        if (!sqlCacheMap.containsKey(sqlCacheKey)) {
            sqlCacheMap.put(sqlCacheKey, buildDeleteSQLInternal(dataRecord.getTableName(), RecordUtil.extractPrimaryColumns(dataRecord)));
        }
        return sqlCacheMap.get(sqlCacheKey);
    }
    
    private String buildDeleteSQLInternal(final String tableName, final Collection<Column> primaryColumns) {
        StringBuilder where = new StringBuilder();
        for (Column each : primaryColumns) {
            where.append(String.format("%s%s%s = ?,", getLeftIdentifierQuoteString(), each.getName(), getRightIdentifierQuoteString()));
        }
        where.setLength(where.length() - 1);
        return String.format("DELETE FROM %s%s%s WHERE %s", getLeftIdentifierQuoteString(), tableName, getRightIdentifierQuoteString(), where.toString());
    }
}
