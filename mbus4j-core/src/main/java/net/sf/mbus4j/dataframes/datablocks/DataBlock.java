/*
 * mbus4j - Open source drivers for mbus protocol see <http://www.m-bus.com/ > - http://mbus4j.sourceforge.net/
 * Copyright (C) 2009  Arne Plöse
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/ >.
 */
package net.sf.mbus4j.dataframes.datablocks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import net.sf.mbus4j.dataframes.datablocks.dif.DataFieldCode;
import net.sf.mbus4j.dataframes.datablocks.dif.FunctionField;
import net.sf.mbus4j.dataframes.datablocks.vif.ObjectAction;
import net.sf.mbus4j.dataframes.datablocks.vif.SiPrefix;
import net.sf.mbus4j.dataframes.datablocks.vif.UnitOfMeasurement;
import net.sf.mbus4j.dataframes.datablocks.vif.Vif;
import net.sf.mbus4j.dataframes.datablocks.vif.VifAscii;
import net.sf.mbus4j.dataframes.datablocks.vif.VifFB;
import net.sf.mbus4j.dataframes.datablocks.vif.VifFD;
import net.sf.mbus4j.dataframes.datablocks.vif.VifManufacturerSpecific;
import net.sf.mbus4j.dataframes.datablocks.vif.VifStd;
import net.sf.mbus4j.dataframes.datablocks.vif.Vife;
import net.sf.mbus4j.dataframes.datablocks.vif.VifeError;
import net.sf.mbus4j.dataframes.datablocks.vif.VifeObjectAction;
import net.sf.mbus4j.dataframes.datablocks.vif.VifeStd;
import net.sf.mbus4j.json.JSONSerializable;

/**
 *
 * @author arnep@users.sourceforge.net
 * @version $Id$
 */
public abstract class DataBlock implements Serializable, JSONSerializable {

    private ObjectAction action;
    private Vif vif;
    /**
     * Extend paramdescription by VIFE value from table 8.4.5
     * @param vife
     */
    private List<Vife> vifes;
    private FunctionField functionField;
    private long storageNumber;
    private int tariff;
    private final DataFieldCode dataFieldCode;
    private short subUnit;

    public DataBlock(DataBlock dataBlock) {
        this(dataBlock.dataFieldCode);
        this.vif = dataBlock.getVif();
        this.vifes = dataBlock.getVifes();
        this.storageNumber = dataBlock.getStorageNumber();
        this.functionField = dataBlock.getFunctionField();
        this.subUnit = dataBlock.getSubUnit();
        this.tariff = dataBlock.getTariff();
    }

    public DataBlock(DataFieldCode dataFieldCode) {
        super();
        this.dataFieldCode = dataFieldCode;
    }

    public DataBlock(DataFieldCode dif, FunctionField functionField, short subUnit, int tariff, long storageNumber, Vif vif, Vife... vifes) {
        super();
        this.dataFieldCode = dif;
        this.functionField = functionField;
        this.subUnit = subUnit;
        this.tariff = tariff;
        this.storageNumber = storageNumber;
        this.vif = vif;
        if (vifes != null && vifes.length > 0) {
            this.vifes = new ArrayList<Vife>(vifes.length);
            for (Vife vife : vifes) {
                this.vifes.add(vife);
            }
        }
    }

    public DataBlock(DataFieldCode dif, Vif vif, Vife... vifes) {
        super();
        this.dataFieldCode = dif;
        this.vif = vif;
        if (vifes != null && vifes.length > 0) {
            this.vifes = new ArrayList<Vife>(vifes.length);
            for (Vife vife : vifes) {
                this.vifes.add(vife);
            }
        }
    }

    public boolean addVife(Vife vife) {
        if (vifes == null) {
            vifes = new ArrayList<Vife>(1);
        }
        return vifes.add(vife);
    }

    /**
     * @return the action
     */
    public ObjectAction getAction() {
        return action;
    }

    public DataFieldCode getDataFieldCode() {
        return dataFieldCode;
    }

    public Integer getExponent() {
        return vif != null ? vif.getExponent() : null;
    }

    public FunctionField getFunctionField() {
        return functionField;
    }

    public String getParamDescr() {
        StringBuilder sb = new StringBuilder();
        if (vif != null) {
            sb.append(vif.getLabel());
        }
        if (vifes != null) {
            for (Vife vife : vifes) {
                sb.append(" ").append(vife);
            }
        }
        return sb.toString();
    }

    public SiPrefix getSiPrefix() {
        return vif != null ? vif.getSiPrefix() : null;
    }

    public long getStorageNumber() {
        return storageNumber;
    }

    /**
     * @return the deviceUnit
     */
    public short getSubUnit() {
        return subUnit;
    }

    public int getTariff() {
        return tariff;
    }

    public UnitOfMeasurement getUnitOfMeasurement() {
        return vif != null ? vif.getUnitOfMeasurement() : null;
    }

    public abstract String getValueAsString();

    public Vif getVif() {
        return vif;
    }

    public List<Vife> getVifes() {
        return vifes;
    }

    /**
     * @param action the action to set
     */
    public void setAction(ObjectAction action) {
        this.action = action;
    }

    public void setFunctionField(FunctionField functionField) {
        this.functionField = functionField;
    }

    public void setObjectAction(ObjectAction action) {
        this.action = action;
    }

    public void setStorageNumber(long storageNumber) {
        this.storageNumber = storageNumber;
    }

    /**
     * @param deviceUnit the deviceUnit to set
     */
    public void setSubUnit(short subUnit) {
        this.subUnit = subUnit;
    }

    public void setTariff(int tariff) {
        this.tariff = tariff;
    }

    public void setVif(Vif vif) {
        this.vif = vif;
    }

    public void toString(StringBuilder sb, String inset) {
        if (action != null) {
            sb.append(inset).append("action = ").append(getAction()).append("\n");
        }
        sb.append(inset).append("dataType = ").append(getDataFieldCode()).append("\n");
        if (vif != null) {
            sb.append(inset).append("description = ").append(getParamDescr()).append("\n");
            if (getDataFieldCode().equals(DataFieldCode.NO_DATA) || getDataFieldCode().equals(DataFieldCode.SELECTION_FOR_READOUT)) {
                sb.append(inset).append("unit =");
            } else {
                sb.append(inset).append("value = ").append(getValueAsString());
            }
            if (getUnitOfMeasurement() != null) {
                if (getExponent() != null) {

                    if (getExponent() > 0) {
                        sb.append(" * 1");
                        for (int i = 0; i < getExponent(); i++) {
                            sb.append("0");
                        }
                    } else if (getExponent() < 0) {
                        sb.append(" * 0.");
                        for (int i = -1; i > getExponent(); i--) {
                            sb.append("0");
                        }
                        sb.append("1");

                    }
                }
                sb.append(" [");
                if (getSiPrefix() != null) {
                    sb.append(getSiPrefix());
                }
                sb.append(getUnitOfMeasurement()).append("]\n");
            } else {
                sb.append("\n");
            }
        }
        if (functionField != null) {
            sb.append(inset).append("tariff = ").append(getTariff()).append('\n');
            sb.append(inset).append("storageNumber = ").append(getStorageNumber()).append("\n");
            sb.append(inset).append("functionField = ").append(getFunctionField()).append("\n");
            sb.append(inset).append("subUnit = ").append(getSubUnit()).append("\n");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb, "");
        return sb.toString();
    }

    @Override
    public JSONObject toJSON(boolean isTemplate) {
        JSONObject result = new JSONObject();
        JSONObject drh = new JSONObject();
        JSONObject dib = new JSONObject();
        JSONObject vib = new JSONObject();

        if (getAction() != null) {
            dib.accumulate("action", getAction().getLabel());
        }
        dib.accumulate("dataFieldCode", getDataFieldCode().getLabel());
        if (dataFieldCode.VARIABLE_LENGTH.equals(getDataFieldCode())) {
            if (this instanceof StringDataBlock) {
                dib.accumulate("variableLengthType", "string");
            } else {
                throw new UnsupportedOperationException("Varoable datablock:" + this.getClass().getName());
            }
        }
        if (DataFieldCode.SPECIAL_FUNCTION_MAN_SPEC_DATA_LAST_PACKET.equals(dataFieldCode) || DataFieldCode.SPECIAL_FUNCTION_MAN_SPEC_DATA_PACKETS_FOLLOWS.equals(dataFieldCode) || DataFieldCode.SPECIAL_FUNCTION_GLOBAL_READOUT_REQUEST.equals(dataFieldCode)) {
            drh.accumulate("dib", dib);
        } else {
            dib.accumulate("functionField", getFunctionField().getLabel());
            dib.accumulate("storageNumber", getStorageNumber());
            dib.accumulate("subUnit", getSubUnit());
            dib.accumulate("tariff", getTariff());
            drh.accumulate("dib", dib);
            JSONObject jsonVif = new JSONObject();
            jsonVif.accumulate("vifType", vif.getVifTypeName());
            if (vif instanceof VifManufacturerSpecific) {
                VifManufacturerSpecific vifManSpec = (VifManufacturerSpecific) vif;
                jsonVif.accumulate("code", vifManSpec.getVife());
                if (vifManSpec.getVifes().length > 0) {
                    JSONArray jsonVifes = new JSONArray();
                    for (byte b : vifManSpec.getVifes()) {
                        jsonVifes.add(b);
                    }
                    jsonVif.accumulate("vifes", jsonVifes);
                }
            } else {
                jsonVif.accumulate("description", vif.getLabel());
            }
            if (vif.getUnitOfMeasurement() != null) {
                jsonVif.accumulate("unitOfMeasurement", vif.getUnitOfMeasurement().getLabel());
            }
            if (vif.getSiPrefix() != null) {
                jsonVif.accumulate("siPrefix", vif.getSiPrefix().getLabel());
            }
            if (vif.getExponent() != null) {
                jsonVif.accumulate("exponent", vif.getExponent());
            }

            vib.accumulate("vif", jsonVif);
            if (getVifes() != null) {
                JSONArray jsonVifes = new JSONArray();
                for (Vife value : vifes) {
                    JSONObject jsonVife = new JSONObject();
                    jsonVife.accumulate("vifeType", value.getVifeTypeName());
                    jsonVife.accumulate("name", value.getLabel());
                    jsonVifes.add(jsonVife);
                }
                vib.accumulate("vifes", jsonVifes);
            }
            drh.accumulate("vib", vib);
        }
        result.accumulate("drh", drh);
        return result;
    }

    public static Vife[] vifesFromJSON(JSONArray jsonVifes) {
        if (jsonVifes instanceof JSONArray) {
            Vife[] result = new Vife[jsonVifes.size()];
            for (int i = 0; i < jsonVifes.size(); i++) {
                String vifeType = jsonVifes.getJSONObject(i).getString("vifeType");
                String vifeName = jsonVifes.getJSONObject(i).getString("name");
                if (VifeStd.PRIMARY.equals(vifeType)) {
                    result[i] = VifeStd.fromLabel(vifeName);
                } else if (VifeError.ERROR.equals(vifeType)) {
                    result[i] = VifeError.fromLabel(vifeName);
                } else if (VifeObjectAction.OBJECT_ACTION.equals(vifeType)) {
                    result[i] = VifeObjectAction.fromLabel(vifeName);
                } else {
                    throw new IllegalArgumentException(vifeType);
                }
            }
            return result;
        } else {
            return new Vife[0];
        }
    }

    public static Vif vifFromJSON(JSONObject jsonVif) {
        String vifType = jsonVif.getString("vifType");
        String description = jsonVif.containsKey("description") ? jsonVif.getString("description") : null;
        SiPrefix siPrefix = jsonVif.containsKey("siPrefix") ? SiPrefix.fromLabel(jsonVif.getString("siPrefix")) : null;
        Integer exponent = jsonVif.containsKey("exponent") ? jsonVif.getInt("exponent") : null;
        UnitOfMeasurement unitOfMeasurement = jsonVif.containsKey("unitOfMeasurement") ? UnitOfMeasurement.fromLabel(jsonVif.getString("unitOfMeasurement")) : null;
        if (VifStd.PRIMARY.equals(vifType)) {
            return VifStd.assemble(jsonVif.getString("description"), unitOfMeasurement, siPrefix, exponent);
        } else if (VifFB.EXTENTION_FB.equals(vifType)) {
            return VifFB.assemble(description, unitOfMeasurement, siPrefix, exponent);
        } else if (VifFD.EXTENTION_FD.equals(vifType)) {
            return VifFD.assemble(description, unitOfMeasurement, siPrefix, exponent);
        } else if (VifAscii.ASCII.equals(vifType)) {
            return new VifAscii(description);
        } else if (VifManufacturerSpecific.MANUFACTURER_SPECIFIC.equals(vifType)) {
            VifManufacturerSpecific manSpec = new VifManufacturerSpecific((byte) jsonVif.getInt("code"));
            if (jsonVif.containsKey("vifes")) {
                JSONArray jsonVifes = jsonVif.getJSONArray("vifes");
                for (int i = 0; i < jsonVifes.size(); i++) {
                    manSpec.addVIFE((byte) jsonVifes.getInt(i));
                }
            }
            return manSpec;
        } else {
            throw new IllegalArgumentException(jsonVif.getString("vifType"));
        }
    }

    @Override
    public void fromJSON(JSONObject json) {
        JSONObject drh = json.getJSONObject("drh");
        JSONObject dib = drh.getJSONObject("dib");
        JSONObject vib = drh.getJSONObject("vib");
        if (dib.get("action") != null) {
            setAction(ObjectAction.fromLabel(dib.getString("action")));
        }

        if (DataFieldCode.SPECIAL_FUNCTION_MAN_SPEC_DATA_LAST_PACKET.equals(dataFieldCode) || DataFieldCode.SPECIAL_FUNCTION_MAN_SPEC_DATA_PACKETS_FOLLOWS.equals(dataFieldCode) || DataFieldCode.SPECIAL_FUNCTION_GLOBAL_READOUT_REQUEST.equals(dataFieldCode)) {
        } else {
            setFunctionField(FunctionField.fromLabel(dib.getString("functionField")));
            setStorageNumber(dib.getLong("storageNumber"));
            setSubUnit((short) dib.getInt("subUnit"));
            setTariff(dib.getInt("tariff"));
            if (!vib.isNullObject()) {
                JSONObject jsonVif = vib.getJSONObject("vif");
                vif = vifFromJSON(jsonVif);
                if (vib.containsKey("vifes")) {

                    Vife[] parsedVifes = vifesFromJSON(vib.getJSONArray("vifes"));
                    if (parsedVifes.length > 0) {

                        vifes = new ArrayList<Vife>(parsedVifes.length);
                        for (Vife vife : parsedVifes) {
                            vifes.add(vife);
                        }
                    } else {
                        vifes = null;
                    }
                } else {
                    vifes = null;
                }
            }
        }
    }
}
