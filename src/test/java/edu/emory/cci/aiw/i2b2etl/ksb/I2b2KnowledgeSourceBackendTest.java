package edu.emory.cci.aiw.i2b2etl.ksb;

/*
 * #%L
 * Protempa BioPortal Knowledge Source Backend
 * %%
 * Copyright (C) 2012 - 2014 Emory University
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

import org.junit.Test;
import org.protempa.KnowledgeSourceReadException;
import org.protempa.PropositionDefinition;
import org.protempa.SourceFactory;
import org.protempa.bconfigs.ini4j.INIConfigurations;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.AfterClass;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.protempa.KnowledgeSource;
import org.protempa.ProtempaException;
import org.protempa.SourceCloseException;
import org.protempa.backend.ksb.KnowledgeSourceBackend;

/**
 *
 */
public class I2b2KnowledgeSourceBackendTest {

    private static final String ICD9_250_ID = "ICD9:250";
    private static KnowledgeSource ks;
    private static KnowledgeSourceBackend ksb;
    /*
     * Binding for the BioPortal H2 database connection pool
     */
    private static InitialContext initialContext;
    
    @BeforeClass
    public static void setUpCls() throws IOException, SQLException, NamingException, ProtempaException {
        // Create the bioportal ksb database
        File ksbBioportalDb = File.createTempFile("i2b2-dsb", ".db");
        try (Connection connection = DriverManager.getConnection("jdbc:h2:" + ksbBioportalDb.getAbsolutePath() +  ";INIT=RUNSCRIPT FROM 'src/test/resources/i2b2.sql'")) {
            
        }
        
        // set up a data source/connection pool for accessing the BioPortal H2 database
        BasicDataSource bds = new BasicDataSource();
        bds.setDriverClassName("org.h2.Driver");
        bds.setUrl("jdbc:h2:" + ksbBioportalDb.getAbsolutePath());
        bds.setMinIdle(1);
        bds.setMaxIdle(5);
        bds.setMaxTotal(5);
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.naming.java.javaURLContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");
        initialContext = new InitialContext();
        initialContext.createSubcontext("java:");
        initialContext.createSubcontext("java:/comp");
        initialContext.createSubcontext("java:/comp/env");
        initialContext.createSubcontext("java:/comp/env/jdbc");
        initialContext.bind("java:/comp/env/jdbc/I2b2DS", bds);
        SourceFactory sf = new SourceFactory(new INIConfigurations(new File("src/test/resources")),
                "i2b2-test-config");
        ks = sf.newKnowledgeSourceInstance();
        ksb = ks.getBackends()[0];
    }
    
    @AfterClass
    public static void tearDown() throws NamingException, SourceCloseException {
        ks.close();
        // Tear down the context binding to the BioPortal h2 connection pool
        initialContext.unbind("java:/comp/env/jdbc/I2b2DS");
        initialContext.destroySubcontext("java:/comp/env/jdbc");
        initialContext.destroySubcontext("java:/comp/env");
        initialContext.destroySubcontext("java:/comp");
        initialContext.destroySubcontext("java:");
        initialContext.close();
    }

    @Test
    public void testReadPropositionDefinition() throws KnowledgeSourceReadException {
        PropositionDefinition propDef = ksb.readPropositionDefinition(ICD9_250_ID);
        assertNotNull(propDef);
        assertEquals("ICD9:250", propDef.getId());
        assertEquals("Diabetes mellitus due to insulin receptor antibodies", propDef.getDisplayName());
        assertEquals("", propDef.getAbbreviatedDisplayName());
        assertTrue(propDef.getInDataSource());
    }

    @Test
    public void testReadPropositionDefinitionInverseIsA() throws KnowledgeSourceReadException {
        PropositionDefinition propDef = ksb.readPropositionDefinition(ICD9_250_ID);
        assertEquals(10, propDef.getChildren().length);
        Set<String> expectedChildren = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            expectedChildren.add(ICD9_250_ID + "." + i);
        }
        Set<String> actualChildren = new HashSet<>();
        for (String child : propDef.getChildren()) {
            actualChildren.add(child);
        }
        assertEquals(expectedChildren, actualChildren);
    }

    @Test
    public void testReadIsA() throws KnowledgeSourceReadException {
        String[] isa = ksb.readIsA(ICD9_250_ID + ".1");
        assertArrayEquals(new String[]{ICD9_250_ID}, isa);
    }

    @Test
    public void testGetKnowledgeSourceSearchResults() throws KnowledgeSourceReadException {
        Set<String> results = ksb.getKnowledgeSourceSearchResults("diabetes");
        assertEquals(61, results.size());
        assertTrue(results.contains(ICD9_250_ID));

        // uppercase "Diabetes"
        assertTrue(results.contains("ICD9:250.93"));

        // lowercase "diabetes"
        assertTrue(results.contains("ICD9:250.00"));
    }

    @Test
    public void testReadAbstractionDefinition() throws KnowledgeSourceReadException {
        assertNull(ksb.readAbstractionDefinition(ICD9_250_ID));
    }

    @Test
    public void testReadContextDefinition() throws KnowledgeSourceReadException {
        assertNull(ksb.readContextDefinition(ICD9_250_ID));
    }

    @Test
    public void testReadTemporalPropositionDefinition() throws KnowledgeSourceReadException {
        assertNotNull(ksb.readTemporalPropositionDefinition(ICD9_250_ID));
    }

    @Test
    public void testReadAbstractedInto() throws KnowledgeSourceReadException {
        assertArrayEquals(new String[0], ksb.readAbstractedInto(ICD9_250_ID));
    }

    @Test
    public void testReadInduces() throws KnowledgeSourceReadException {
        assertArrayEquals(new String[0], ksb.readInduces(ICD9_250_ID));
    }

    @Test
    public void testReadSubContextsOf() throws KnowledgeSourceReadException {
        assertArrayEquals(new String[0], ksb.readSubContextOfs(ICD9_250_ID));
    }
}
