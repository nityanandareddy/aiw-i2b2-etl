package edu.emory.cci.aiw.i2b2etl.ksb;

/*-
 * #%L
 * AIW i2b2 ETL
 * %%
 * Copyright (C) 2012 - 2016 Emory University
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

/**
 *
 * @author Andrew Post
 */
public class QueryConstructorUnionedMetadataQueryBuilder extends AbstractUnionedMetadataQueryBuilder {
    private QueryConstructor queryConstructor;
    
    public QueryConstructorUnionedMetadataQueryBuilder queryConstructor(QueryConstructor queryConstructor) {
        this.queryConstructor = queryConstructor;
        return this;
    }

    @Override
    protected void appendStatement(StringBuilder sql, String table) {
        this.queryConstructor.appendStatement(sql, table);
    }
    
}
