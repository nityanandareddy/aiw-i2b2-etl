/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.emory.cci.aiw.i2b2etl.table;

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
import edu.emory.cci.aiw.i2b2etl.metadata.InvalidConceptCodeException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.protempa.KnowledgeSource;
import org.protempa.proposition.Parameter;
import org.protempa.proposition.Proposition;
import org.protempa.proposition.TemporalProposition;
import org.protempa.proposition.UniqueId;
import org.protempa.proposition.value.AbsoluteTimeGranularityUtil;
import org.protempa.proposition.value.InequalityNumberValue;
import org.protempa.proposition.value.NominalValue;
import org.protempa.proposition.value.NumberValue;
import org.protempa.proposition.value.NumericalValue;
import org.protempa.proposition.value.Value;

/**
 *
 * @author arpost
 */
public abstract class FactHandler {

    private boolean inited = false;
    private int batchNumber = 0;
    private long ctr = 0L;
    private int counter = 0;
    private int batchSize = 1000;
    private int commitCounter = 0;
    private int commitSize = 10000;
    private PreparedStatement ps;
    private Timestamp importTimestamp;
    private final String startConfig;
    private final String finishConfig;
    private final String unitsPropertyName;
    private final String propertyName;

    public FactHandler(String propertyName, String startConfig, String finishConfig, String unitsPropertyName) {
        this.propertyName = propertyName;
        this.startConfig = startConfig;
        this.finishConfig = finishConfig;
        this.unitsPropertyName = unitsPropertyName;
    }

    public String getStartConfig() {
        return startConfig;
    }

    public String getFinishConfig() {
        return finishConfig;
    }

    public String getUnitsPropertyName() {
        return unitsPropertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public abstract void handleRecord(PatientDimension patient, VisitDimension visit, ProviderDimension provider, Proposition encounterProp, Map<Proposition, List<Proposition>> forwardDerivations, Map<Proposition, List<Proposition>> backwardDerivations, Map<UniqueId, Proposition> references, KnowledgeSource knowledgeSource, Set<Proposition> derivedPropositions, Connection cn) throws InvalidFactException;

    public final void clearOut(Connection cn) throws SQLException {
        Logger logger = TableUtil.logger();
        if (this.ps != null) {
            try {
                if (counter > 0) {
                    batchNumber++;
                    ps.executeBatch();
                    logger.log(Level.FINEST, "DB_OBX_BATCH={0}", batchNumber);
                }
                if (commitCounter > 0) {
                    cn.commit();
                }
                ps.close();
                ps = null;
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException ignore) {
                    }
                }
            }
        }
    }

    protected final void insert(ObservationFact obx, Connection cn) throws SQLException, InvalidConceptCodeException {
        Logger logger = TableUtil.logger();
        if (obx.isRejected()) {
            logger.log(Level.WARNING, "Rejected fact {0}", obx);
        } else {
            try {
                setParameters(cn, obx);

                ps.addBatch();
                counter++;
                commitCounter++;
                if (counter >= batchSize) {
                    this.importTimestamp =
                            new Timestamp(System.currentTimeMillis());
                    batchNumber++;
                    ps.executeBatch();
                    logger.log(Level.FINEST, "DB_OBX_BATCH={0}", batchNumber);
                    ps.clearBatch();
                    counter = 0;
                }
                if (commitCounter >= commitSize) {
                    cn.commit();
                    commitCounter = 0;
                }
                ps.clearParameters();
            } catch (SQLException e) {
                logger.log(Level.FINEST, "DB_OBX_BATCH_FAIL={0}", batchNumber);
                logger.log(Level.SEVERE, "Batch failed on ObservationFact. I2B2 will not be correct.", e);
                try {
                    ps.close();
                } catch (SQLException sqle) {
                }
                throw e;
            }
        }
    }

    protected final String handleUnits(Proposition prop) {
        String value;
        if (this.unitsPropertyName != null) {
            Value unitsVal = prop.getProperty(this.unitsPropertyName);
            if (unitsVal != null) {
                value = unitsVal.getFormatted();
            } else {
                value = null;
            }
        } else {
            value = null;
        }
        return value;
    }

    protected final Value handleValue(Proposition prop) {
        Value value = null;
        if (this.propertyName != null) {
            Value tvalCharVal = prop.getProperty(this.propertyName);
            if (tvalCharVal != null) {
                value = tvalCharVal;
            }
        } else if (prop instanceof Parameter) {
            value = ((Parameter) prop).getValue();
        } else {
            value = NominalValue.getInstance(prop.getId());
        }
        return value;
    }

    protected final Date handleStartDate(Proposition prop, Proposition encounterProp, Value propertyVal) throws InvalidFactException {
        Date start;
        if (prop instanceof TemporalProposition) {
            start = AbsoluteTimeGranularityUtil.asDate(((TemporalProposition) prop).getInterval().getMinStart());
        } else if (this.startConfig != null) {
            if (this.startConfig.equals("start")) {
                start = AbsoluteTimeGranularityUtil.asDate(((TemporalProposition) encounterProp).getInterval().getMinStart());
            } else if (this.startConfig.equals("finish")) {
                start = AbsoluteTimeGranularityUtil.asDate(((TemporalProposition) encounterProp).getInterval().getMinFinish());
            } else {
                start = null;
            }
        } else {
            start = null;
        }
        return start;
    }

    protected final Date handleFinishDate(Proposition prop, Proposition encounterProp, Value propertyVal) throws InvalidFactException {
        Date start;
        if (prop instanceof TemporalProposition) {
            start = AbsoluteTimeGranularityUtil.asDate(((TemporalProposition) prop).getInterval().getMinFinish());
        } else if (this.finishConfig != null) {
            if (this.finishConfig.equals("start")) {
                start = AbsoluteTimeGranularityUtil.asDate(((TemporalProposition) encounterProp).getInterval().getMinStart());
            } else if (this.finishConfig.equals("finish")) {
                start = AbsoluteTimeGranularityUtil.asDate(((TemporalProposition) encounterProp).getInterval().getMinFinish());
            } else {
                start = null;
            }
        } else {
            start = null;
        }
        return start;
    }

    private void setParameters(Connection cn, ObservationFact obx) throws SQLException, InvalidConceptCodeException {
        if (!inited) {
            ps = cn.prepareStatement("insert into OBSERVATION_FACT values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            inited = true;
        }
        ps.setLong(1, obx.getVisit().getEncounterNum());
        ps.setLong(2, obx.getPatient().getPatientNum());
        ps.setString(3, obx.getConcept().getConceptCode());
        ps.setString(4,
                TableUtil.setStringAttribute(obx.getProvider().getConcept().getConceptCode()));							//	seems coupled to 'reports'
        ps.setTimestamp(5,
                TableUtil.setTimestampAttribute(obx.getStartDate()));
        ps.setString(6, Long.toString(ctr++));								//	used for admitting, primary, secondary on ICD9Diag

        Value value = obx.getValue();
        if (value == null) {
            ps.setString(7, ValTypeCode.NO_VALUE.getCode());
            ps.setString(8, null);
            ps.setString(9, null);
        } else if (value instanceof NumericalValue) {
            ps.setString(7, ValTypeCode.NUMERIC.getCode());
            if (value instanceof NumberValue) {
                ps.setString(8, TValCharWhenNumberCode.EQUAL.getCode());
            } else {
                InequalityNumberValue inv = (InequalityNumberValue) value;
                TValCharWhenNumberCode tvalCode =
                        TValCharWhenNumberCode.codeFor(inv.getComparator());
                ps.setString(8, tvalCode.getCode());
            }
            ps.setObject(9, ((NumericalValue) value).getNumber());
        } else {
            ps.setString(7, ValTypeCode.TEXT.getCode());
            String tval = value.getFormatted();
            if (tval.length() > 255) {
                ps.setString(8, tval.substring(0, 255));
                TableUtil.logger().log(Level.WARNING, "Truncated text result to 255 characters: " + tval);
            } else {
                ps.setString(8, tval);
            }
            ps.setString(9, null);
        }
        ps.setString(10, obx.getValueFlagCode().getCode());
        ps.setObject(11, null);
        ps.setObject(12, null);
        ps.setString(13, obx.getUnits());
        ps.setTimestamp(14, TableUtil.setTimestampAttribute(obx.getEndDate()));
        ps.setString(15, null);
        ps.setObject(16, null);
        ps.setString(17, null);
        ps.setTimestamp(18, null);
        ps.setTimestamp(19, null);
        if (this.importTimestamp == null) {
            this.importTimestamp = new Timestamp(System.currentTimeMillis());
        }
        ps.setTimestamp(20, this.importTimestamp);
        ps.setString(21, obx.getSourceSystem());
        ps.setObject(22, null);
    }
}