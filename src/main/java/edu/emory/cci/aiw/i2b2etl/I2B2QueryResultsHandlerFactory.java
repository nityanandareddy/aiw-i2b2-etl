package edu.emory.cci.aiw.i2b2etl;

/*
 * #%L
 * AIW i2b2 ETL
 * %%
 * Copyright (C) 2012 - 2013 Emory University
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

import java.io.File;
import org.protempa.KnowledgeSource;
import org.protempa.query.Query;
import org.protempa.query.handler.CollectStatisticsException;
import org.protempa.query.handler.QueryResultsHandler;
import org.protempa.query.handler.QueryResultsHandlerFactory;
import org.protempa.query.handler.QueryResultsHandlerInitException;
import org.protempa.query.handler.StatisticsCollector;
import org.protempa.query.handler.StatisticsCollectorInitException;

/**
 *
 * @author Andrew Post
 */
public class I2B2QueryResultsHandlerFactory implements QueryResultsHandlerFactory {
    private File confXML;
    private boolean inferPropositionIdsNeeded;
    
    /**
     * Creates a new query results handler that will use the provided
     * configuration file. It is the same as calling the two-argument
     * constructor with <code>inferPropositionIdsNeeded</code> set to
     * <code>true</code>.
     *
     * @param confXML an i2b2 query results handler configuration file. Cannot
     * be <code>null</code>.
     */
    public I2B2QueryResultsHandlerFactory(File confXML) {
        this(confXML, true);
    }
    
    /**
     * Creates a new query results handler that will use the provided
     * configuration file. This constructor, through the
     * <code>inferPropositionIdsNeeded</code> parameter, lets you control
     * whether proposition ids to be returned from the Protempa processing run
     * should be inferred from the i2b2 configuration file.
     *
     * @param confXML an i2b2 query results handler configuration file. Cannot
     * be <code>null</code>.
     * @param inferPropositionIdsNeeded <code>true</code> if proposition ids to
     * be returned from the Protempa processing run should include all of those
     * specified in the i2b2 configuration file, <code>false</code> if the
     * proposition ids returned should be only those specified in the Protempa
     * {@link Query}.
     */
    public I2B2QueryResultsHandlerFactory(File confXML,
            boolean inferPropositionIdsNeeded) { 
        if (confXML == null) {
            throw new IllegalArgumentException("confXML cannot be null");
        }
        this.confXML = confXML;
        this.inferPropositionIdsNeeded = inferPropositionIdsNeeded;
    }

    @Override
    public QueryResultsHandler getInstance(Query query, KnowledgeSource knowledgeSource) throws QueryResultsHandlerInitException {
       return new I2B2QueryResultsHandler(query, knowledgeSource, this.confXML, this.inferPropositionIdsNeeded);
    }

    @Override
    public StatisticsCollector getStatisticsCollector() throws StatisticsCollectorInitException {
        return new I2B2StatisticsCollector(this.confXML);
    }
    
}
