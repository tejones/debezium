/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.postgresql;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;

import io.debezium.config.CommonConnectorConfig;
import io.debezium.connector.AbstractSourceInfoStructMaker;

public class PostgresSourceInfoStructMaker extends AbstractSourceInfoStructMaker<SourceInfo> {

    public static final String TIMESTAMP_KEY = "ts_ms";

    private final Schema schema;

    public PostgresSourceInfoStructMaker(String connector, String version, CommonConnectorConfig connectorConfig) {
        super(connector, version, connectorConfig);
        schema = commonSchemaBuilder()
                .name("io.debezium.connector.postgresql.Source")
                .field(TIMESTAMP_KEY, Schema.OPTIONAL_INT64_SCHEMA)
                .field(SourceInfo.DB_NAME_KEY, Schema.STRING_SCHEMA)
                .field(SourceInfo.TXID_KEY, Schema.OPTIONAL_INT64_SCHEMA)
                .field(SourceInfo.LSN_KEY, Schema.OPTIONAL_INT64_SCHEMA)
                .field(SourceInfo.SCHEMA_NAME_KEY, Schema.OPTIONAL_STRING_SCHEMA)
                .field(SourceInfo.TABLE_NAME_KEY, Schema.OPTIONAL_STRING_SCHEMA)
                .field(SourceInfo.SNAPSHOT_KEY, SchemaBuilder.bool().optional().defaultValue(false).build())
                .field(SourceInfo.XMIN_KEY, Schema.OPTIONAL_INT64_SCHEMA)
                .build();
    }

    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    public Struct struct(SourceInfo sourceInfo) {
        assert sourceInfo.dbName() != null
                && sourceInfo.schemaName() != null
                && sourceInfo.tableName() != null;

        Struct result = super.commonStruct();
        result.put(SourceInfo.DB_NAME_KEY, sourceInfo.dbName());
        result.put(SourceInfo.SCHEMA_NAME_KEY, sourceInfo.schemaName());
        result.put(SourceInfo.TABLE_NAME_KEY, sourceInfo.tableName());
        // use the offset information without the snapshot part (see below)
        sourceInfo.offset().forEach((k, v) -> {
            if (SourceInfo.TIMESTAMP_KEY.equals(k) && v != null) {
                result.put(TIMESTAMP_KEY, ((Long) v) / 1_000);
            }
            else if (SourceInfo.LAST_SNAPSHOT_RECORD_KEY.equals(k)) {
                return;
            }
            else {
                result.put(k, v);
            }
        });
        return result;
    }
}
