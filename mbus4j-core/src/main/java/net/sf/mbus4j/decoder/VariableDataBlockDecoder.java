package net.sf.mbus4j.decoder;

/*
 * #%L
 * mbus4j-core
 * %%
 * Copyright (C) 2009 - 2014 MBus4J
 * %%
 * mbus4j - Drivers for the M-Bus protocol - http://mbus4j.sourceforge.net/
 * Copyright (C) 2009-2014, mbus4j.sf.net, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * #L%
 */
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mbus4j.NotSupportedException;
import net.sf.mbus4j.dataframes.LongFrame;
import net.sf.mbus4j.dataframes.MBusMedium;
import net.sf.mbus4j.dataframes.datablocks.ByteDataBlock;
import net.sf.mbus4j.dataframes.datablocks.DataBlock;
import net.sf.mbus4j.dataframes.datablocks.DateAndTimeDataBlock;
import net.sf.mbus4j.dataframes.datablocks.DateDataBlock;
import net.sf.mbus4j.dataframes.datablocks.EnhancedIdentificationDataBlock;
import net.sf.mbus4j.dataframes.datablocks.IntegerDataBlock;
import net.sf.mbus4j.dataframes.datablocks.LongDataBlock;
import net.sf.mbus4j.dataframes.datablocks.RawDataBlock;
import net.sf.mbus4j.dataframes.datablocks.RealDataBlock;
import net.sf.mbus4j.dataframes.datablocks.ShortDataBlock;
import net.sf.mbus4j.dataframes.datablocks.StringDataBlock;
import net.sf.mbus4j.dataframes.datablocks.dif.DataFieldCode;
import net.sf.mbus4j.dataframes.datablocks.dif.FunctionField;
import net.sf.mbus4j.dataframes.datablocks.dif.VariableLengthType;
import net.sf.mbus4j.dataframes.datablocks.vif.ObjectAction;
import net.sf.mbus4j.dataframes.datablocks.vif.Vif;
import net.sf.mbus4j.dataframes.datablocks.vif.VifAscii;
import net.sf.mbus4j.dataframes.datablocks.vif.VifFB;
import net.sf.mbus4j.dataframes.datablocks.vif.VifFC;
import net.sf.mbus4j.dataframes.datablocks.vif.VifeFC;
import net.sf.mbus4j.dataframes.datablocks.vif.VifFD;
import net.sf.mbus4j.dataframes.datablocks.vif.VifManufacturerSpecific;
import net.sf.mbus4j.dataframes.datablocks.vif.VifPrimary;
import net.sf.mbus4j.dataframes.datablocks.vif.Vife;
import net.sf.mbus4j.dataframes.datablocks.vif.VifeError;
import net.sf.mbus4j.dataframes.datablocks.vif.VifeManufacturerSpecific;
import net.sf.mbus4j.dataframes.datablocks.vif.VifePrimary;
import net.sf.mbus4j.log.LogUtils;

/**
 *
 * @author arnep@users.sourceforge.net
 * @version $Id$
 */
public class VariableDataBlockDecoder {

    private void createDataBlock() {
        try {
            db = DataBlock.getDataBlockClass(vif, vifes, dfc, variableLengthType).newInstance();
        } catch (IllegalAccessException | InstantiationException ex) {
            throw new RuntimeException(ex);
        }
        db.setVif(vif);
        db.setDataFieldCode(dfc);
        if (vifes == null) {
            db.clearVifes();
        } else {
            for (Vife vife : vifes) {
                db.addVife(vife);
            }
        }
        db.setStorageNumber(storageNumber);
        db.setTariff(tariff);
        db.setSubUnit(subUnit);
        db.setFunctionField(functionField);
        db.setAction(objectAction);
    }

    public enum DecodeState {

        WAIT_FOR_INIT,
        DIF,
        DIFE,
        VIF,
        VIF_FB,
        VIF_FC,
        VIF_FD,
        VIFE,
        MAN_SPEC_VIFE,
        VIFE_FC,
        ASCII_VIF_LENGTH,
        ASCII_VIF_COLLECT,
        SET_VARIABLE_LENGTH,
        COLLECTING_VALUE,
        ERROR,
        RESULT_AVAIL;
    }
    private final static Logger LOG = LogUtils.getDecoderLogger();
    private DecodeState ds = DecodeState.WAIT_FOR_INIT;
    private int difePos;
    private ObjectAction objectAction;
    private DataFieldCode dfc;
    private VariableLengthType variableLengthType;
    private FunctionField functionField;
    private long storageNumber;
    private int tariff;
    private short subUnit;
    private Vif vif;
    private Vife[] vifes;
    private DataBlock db;
    private LongFrame frame;
    private final Stack stack = new Stack();

    public VariableDataBlockDecoder() {
        super();
    }

    public DecodeState addByte(final byte b, final int bytesLeft) {
        switch (ds) {
            case DIF:
                decodeDif(b, bytesLeft);
                difePos = 0;
                return ds;
            case DIFE:
                decodeDIFE(b, difePos++);
                return ds;
            case VIF:
                decodeVIF(b);
                break;
            case ASCII_VIF_LENGTH:
                stack.init(b & 0xFF);
                setState(DecodeState.ASCII_VIF_COLLECT);
                return ds;
            case ASCII_VIF_COLLECT:
                stack.push(b);

                if (stack.isFull()) {
                    ((VifAscii) vif).setValue(stack.popString());
                    startCollectingValue();
                }
                return ds;
            case VIF_FB:
                decodeVifExtention_FB(b);
                return ds;
            case VIF_FC:
                decodeVifExtention_FC(b);
                return ds;
            case VIF_FD:
                decodeVifExtention_FD(b);
                return ds;
            case VIFE:
                decodeVife(b);
                return ds;
            case MAN_SPEC_VIFE:
                decodeManSpecVife(b);
                return ds;
            case VIFE_FC:
                decodeVifeExtension_FC(b);
                return ds;
            case COLLECTING_VALUE:
                stack.push(b);
                if (stack.isFull()) {
                    decodeValueFromStack();
                }
                return ds;
            case SET_VARIABLE_LENGTH:
                if ((b & 0xFF) < 0xBF) {
                    variableLengthType = VariableLengthType.STRING;
                    startCollectingValue((b & 0xFF));
                } else if ((b & 0xFF) < 0xCF) {
                    variableLengthType = VariableLengthType.BIG_DECIMAL;
                    startCollectingValue((b & 0xFF) - 0xC0);
                } else if ((b & 0xFF) < 0xDF) {
                    variableLengthType = VariableLengthType.BIG_DECIMAL;
                    startCollectingValue((b & 0xFF) - 0xD0);
                } else if ((b & 0xFF) < 0xEF) {
                    throw new DecodeException("binary number ???? how to decode", frame);
                } else if ((b & 0xFF) < 0xFA) {
                    throw new DecodeException("floating point to be defined", frame);
                } else {
                    throw new DecodeException(String.format("reserved: 0x%02x ", b & 0xFF), frame);
                }
                return ds;
            default:
                LOG.log(Level.SEVERE, "Unknown state: {0}", ds);
                setState(DecodeState.ERROR);
        }
        return ds;
    }

    public void reset() {
        stack.clear();
        setState(DecodeState.WAIT_FOR_INIT);
    }

    private void decodeDif(final byte b, int bytesLeft) {
        switch (b & 0xFF) {
            case 0x0F:
                dfc = DataFieldCode.SPECIAL_FUNCTION_MAN_SPEC_DATA_LAST_PACKET;
                if (bytesLeft == 0) {
                    createDataBlock();
                    setState(DecodeState.RESULT_AVAIL);
                } else {
                    startCollectingValue(bytesLeft);
                }
                return;
            case 0x1F:
                dfc = DataFieldCode.SPECIAL_FUNCTION_MAN_SPEC_DATA_PACKETS_FOLLOWS;
                if (bytesLeft == 0) {
                    createDataBlock();
                    setState(DecodeState.RESULT_AVAIL);
                } else {
                    startCollectingValue(bytesLeft);
                }
                return;
            case 0x2F:
                // Skip idlefiller next byte is DIF
                return;
            case 0x3F:
            case 0x4F:
            case 0x5F:
            case 0x6F:
                setState(DecodeState.ERROR);
                throw new DecodeException(String.format("DIF reserved 0x%02x ", b & 0xFF), frame); // Reserverd,
            case 0x7F:
                dfc = DataFieldCode.SPECIAL_FUNCTION_GLOBAL_READOUT_REQUEST;
                if (bytesLeft == 0) {
                    createDataBlock();
                    setState(DecodeState.RESULT_AVAIL);
                    return;
                } else {
                    throw new DecodeException("SPECIAL_FUNCTION_GLOBAL_READOUT_REQUEST and not at the End!", frame);
                }
            default:

                // decode data field
                switch (b & 0x0F) {
                    case 0x00:
                        dfc = DataFieldCode.NO_DATA;
                        break;
                    case 0x01:
                        dfc = DataFieldCode._8_BIT_INTEGER;
                        break;
                    case 0x02:
                        dfc = DataFieldCode._16_BIT_INTEGER;
                        break;
                    case 0x03:
                        dfc = DataFieldCode._24_BIT_INTEGER;
                        break;
                    case 0x04:
                        dfc = DataFieldCode._32_BIT_INTEGER;
                        break;
                    case 0x05:
                        dfc = DataFieldCode._32_BIT_REAL;
                        break;
                    case 0x06:
                        dfc = DataFieldCode._48_BIT_INTEGER;
                        break;
                    case 0x07:
                        dfc = DataFieldCode._64_BIT_INTEGER;
                        break;
                    case 0x08:
                        dfc = DataFieldCode.SELECTION_FOR_READOUT;
                        break;
                    case 0x09:
                        dfc = DataFieldCode._2_DIGIT_BCD;
                        break;
                    case 0x0A:
                        dfc = DataFieldCode._4_DIGIT_BCD;
                        break;
                    case 0x0B:
                        dfc = DataFieldCode._6_DIGIT_BCD;
                        break;
                    case 0x0C:
                        dfc = DataFieldCode._8_DIGIT_BCD;
                        break;
                    case 0x0D:
                        dfc = DataFieldCode.VARIABLE_LENGTH;
                        break;
                    case 0x0E:
                        dfc = DataFieldCode._12_DIGIT_BCD;
                        break;
                    case 0x0F:
                        setState(DecodeState.ERROR);
                        throw new NotSupportedException("data field 0x0f not supported");
                    default:
                        setState(DecodeState.ERROR);
                        throw new NotSupportedException(String.format("data field of DIF 0x%02x not supported", b & 0xFF));
                }

                // decode function field
                switch (b & 0x30) {
                    case 0x00:
                        functionField = FunctionField.INSTANTANEOUS_VALUE;
                        break;
                    case 0x10:
                        functionField = FunctionField.MAXIMUM_VALUE;
                        break;
                    case 0x20:
                        functionField = FunctionField.MINIMUM_VALUE;
                        break;
                    case 0x30:
                        functionField = FunctionField.VALUE_DURING_ERROR_STATE;
                        break;
                    default:
                        throw new NotSupportedException("Function field");
                }
        }

        storageNumber = (b >> 6) & 0x01;

        if ((b & Decoder.EXTENTION_BIT) == Decoder.EXTENTION_BIT) {
            setState(DecodeState.DIFE);
        } else if (bytesLeft == 0) {
            createDataBlock();
            setState(DecodeState.RESULT_AVAIL);
        } else {
            setState(DecodeState.VIF);
        }
    }

    private void decodeDIFE(final byte b, int dFIEIndex) {
        storageNumber |= (long) (b & 0x0F) << (1 + (dFIEIndex * 4));
        tariff |= ((b >> 4) & 0x03) << (dFIEIndex * 2);
        subUnit |= (short) (((b >> 6) & 0x01) << dFIEIndex);

        if ((b & Decoder.EXTENTION_BIT) != Decoder.EXTENTION_BIT) {
            setState(DecodeState.VIF);
        }
    }

    private void decodeEnhancedIdentificationDataBlock(EnhancedIdentificationDataBlock db) {
        db.setMedium(MBusMedium.valueOf(stack.popByte()));
        db.setVersion(stack.popByte());
        db.setMan(stack.popMan());
        db.setId(stack.popBcdInteger(8));
    }

    private void decodeValueFromStack() {
        createDataBlock();
        switch (dfc) {
            case NO_DATA:
                break;

            case _8_BIT_INTEGER:
                ((ByteDataBlock) db).setValue(stack.popByte());

                break;

            case _2_DIGIT_BCD:
                ((ByteDataBlock) db).setBcdError(stack.peekBcdError(2));

                if (((ByteDataBlock) db).getBcdError() != null) {
                    stack.popBcdByte();
                } else {
                    ((ByteDataBlock) db).setValue(stack.popBcdByte());
                }

                break;

            case _16_BIT_INTEGER:

                if (db instanceof DateDataBlock) {
                    ((DateDataBlock) db).setValue(stack.popDate());
                } else {
                    ((ShortDataBlock) db).setValue(stack.popShort());
                }

                break;

            case _4_DIGIT_BCD:
                ((ShortDataBlock) db).setBcdError(stack.peekBcdError(4));

                if (((ShortDataBlock) db).getBcdError() != null) {
                    stack.popBcdShort(4);
                } else {
                    ((ShortDataBlock) db).setValue(stack.popBcdShort(4));
                }

                break;

            case _24_BIT_INTEGER:
                ((IntegerDataBlock) db).setValue(stack.popInteger(3));

                break;

            case _6_DIGIT_BCD:
                ((IntegerDataBlock) db).setBcdError(stack.peekBcdError(6));

                if (((IntegerDataBlock) db).getBcdError() != null) {
                    stack.popBcdInteger(6);
                } else {
                    ((IntegerDataBlock) db).setValue(stack.popBcdInteger(6));
                }

                break;

            case _32_BIT_INTEGER:

                if (db instanceof DateAndTimeDataBlock) {
                    DateAndTimeDataBlock d = ((DateAndTimeDataBlock) db);
                    d.setValid(stack.peekIsTimestampValid());
                    d.setSummerTime(stack.peekIsTimestampSummertime());
                    d.setRes1(stack.peekIsTimestampRes1());
                    d.setRes2(stack.peekIsTimestampRes2());
                    d.setRes3(stack.peekIsTimestampRes3());
                    d.setValue(stack.popTimeStamp());
                } else {
                    ((IntegerDataBlock) db).setValue(stack.popInteger());
                }

                break;

            case _8_DIGIT_BCD:

                if (db instanceof EnhancedIdentificationDataBlock) {
                    ((EnhancedIdentificationDataBlock) db).setId(stack.popBcdInteger(8));
                } else {
                    ((IntegerDataBlock) db).setBcdError(stack.peekBcdError(8));

                    if (((IntegerDataBlock) db).getBcdError() != null) {
                        stack.popBcdInteger(8);
                    } else {
                        ((IntegerDataBlock) db).setValue(stack.popBcdInteger(8));
                    }
                }

                break;

            case _32_BIT_REAL:
                ((RealDataBlock) db).setValue(stack.popFloat());

                break;

            case _48_BIT_INTEGER:
                ((LongDataBlock) db).setValue(stack.popLong(6));

                break;

            case _12_DIGIT_BCD:
                ((LongDataBlock) db).setBcdError(stack.peekBcdError(12));

                if (((LongDataBlock) db).getBcdError() != null) {
                    stack.popBcdInteger(12);
                } else {
                    ((LongDataBlock) db).setValue(stack.popBcdLong(12));
                }

                break;

            case _64_BIT_INTEGER:

                if (db instanceof EnhancedIdentificationDataBlock) {
                    decodeEnhancedIdentificationDataBlock((EnhancedIdentificationDataBlock) db);
                } else {
                    ((LongDataBlock) db).setValue(stack.popLong());
                }

                break;

            case VARIABLE_LENGTH:

                if (db instanceof RawDataBlock) {
                    ((RawDataBlock) db).setValue(stack.popBytes());
                } else if (db instanceof StringDataBlock) {
                    ((StringDataBlock) db).setValue(stack.popString());
                } else {
                    // TODO BIG DECIMAL and binray
                    throw new RuntimeException("decode variable lenght " + db.getClass().getName());
                }

                break;

            case SPECIAL_FUNCTION_MAN_SPEC_DATA_LAST_PACKET:
            case SPECIAL_FUNCTION_MAN_SPEC_DATA_PACKETS_FOLLOWS:
                ((RawDataBlock) db).setValue(stack.popBytes());

                break;

            default:
                throw new RuntimeException("decode data value" + db.getDataFieldCode());
        }

        stack.clear();
        setState(DecodeState.RESULT_AVAIL);
    }

    /**
     * see chapter 8.4.3
     *
     * @param b b will be expanded to int, so clear the sign, wich will be
     * nagative in the case extention bit is set
     */
    private void decodeVIF(final byte b) {
        switch (b & 0xFF) {
            case 0x7C:
                //Vif is ASCII coded (length in first/next byte)
                stack.clear();
                vif = new VifAscii();
                setState(DecodeState.ASCII_VIF_LENGTH);
                return;

            case 0x7E:
                vif = VifPrimary.READOUT_SELECTION;
                break;

            case 0x7F:
                vif = new VifManufacturerSpecific();
                break;

            case 0xFB:
                // decode vife table 8.4.4 b
                setState(DecodeState.VIF_FB);
                return;

            case 0xFC:
                setState(DecodeState.VIF_FC);
                return;

            case 0xFD:
                // decode vife table 8.4.4 a
                setState(DecodeState.VIF_FD);
                return;

            case 0xFF:
                vif = new VifManufacturerSpecific();
                setState(DecodeState.MAN_SPEC_VIFE);
                return;

            default:
                vif = VifPrimary.valueOfTableIndex((byte) (b & ~Decoder.EXTENTION_BIT));

        }

        goFromVifOrVife(b);
    }

    private void decodeManSpecVife(final byte b) {
        // Extended VID chapter 8.4.5
        switch (frame.getControlCode()) {
            case RSP_UD:
                Vife vife = new VifeManufacturerSpecific(b);

                if (vifes == null) {
                    vifes = new Vife[1];
                } else {
                    vifes = Arrays.copyOf(vifes, vifes.length + 1);
                }
                vifes[vifes.length - 1] = vife;

                break;

            case SND_UD:
                objectAction = ObjectAction.valueOf(b);

                break;

            default:
                setState(DecodeState.ERROR);
                throw new NotSupportedException(String.format(
                        "Dont know how to handele Control code %s ",
                        frame.getControlCode()));
        }

        goFromVifOrVifeManSpec(b);
    }

    private void decodeVife(final byte b) {
        // Extended VID chapter 8.4.5
        switch (frame.getControlCode()) {
            case RSP_UD:
                if (0xFC == (b & 0xFF)) {
                    setState(DecodeState.VIFE_FC);
                    return;
                }
                Vife vife;

                if ((b & ~Decoder.EXTENTION_BIT) > VifePrimary.CODE_OFFSET) {
                    vife = VifePrimary.valueOfTableIndex((byte) (b & ~Decoder.EXTENTION_BIT));
                } else {
                    vife = VifeError.valueOfTableIndex((byte) (b & ~Decoder.EXTENTION_BIT));
                }

                if (vifes == null) {
                    vifes = new Vife[1];
                } else {
                    vifes = Arrays.copyOf(vifes, vifes.length + 1);
                }

                vifes[vifes.length - 1] = vife;

                if (vife == VifePrimary.MAN_SPEC) {
                    goFromVifOrVifeManSpec(b);
                } else {
                    goFromVifOrVife(b);
                }

                break;

            case SND_UD:
                objectAction = ObjectAction.valueOf(b);
                goFromVifOrVife(b);

                break;

            default:
                setState(DecodeState.ERROR);
                throw new NotSupportedException(String.format(
                        "Dont know how to handele Control code %s ",
                        frame.getControlCode()));
        }

    }

    private void decodeVifeExtension_FC(final byte b) {
        // Extended VID chapter 8.4.5
        switch (frame.getControlCode()) {
            case RSP_UD:
                VifeFC vifeFC = VifeFC.valueOfTableIndex((byte) (b & ~Decoder.EXTENTION_BIT));

                if (vifes == null) {
                    vifes = new Vife[1];
                } else {
                    vifes = Arrays.copyOf(vifes, vifes.length + 1);
                }
                vifes[vifes.length - 1] = vifeFC;

                break;

            case SND_UD:
                objectAction = ObjectAction.valueOf(b);

                break;

            default:
                setState(DecodeState.ERROR);
                throw new NotSupportedException(String.format(
                        "Dont know how to handele Control code %s ",
                        frame.getControlCode()));
        }

        goFromVifOrVife(b);
    }

    private void decodeVifExtention_FB(final byte b) {
        // Extended VID chapter 8.4.4 table b
        vif = VifFB.valueOfTableIndex((byte) (b & ~Decoder.EXTENTION_BIT));
        goFromVifOrVife(b);
    }

    private void decodeVifExtention_FC(final byte b) {
        //new stuff first seen @ SINUS 
        vif = VifFC.valueOfTableIndex((byte) (b & ~Decoder.EXTENTION_BIT));
        goFromVifOrVife(b);
    }

    private void decodeVifExtention_FD(final byte b) {
        // Extended VID chapter 8.4.4 table a
        vif = VifFD.valueOfTableIndex((byte) (b & ~Decoder.EXTENTION_BIT));
        goFromVifOrVife(b);
    }

    public DataBlock getDataBlock() {
        return db;
    }

    public DecodeState getState() {
        return ds;
    }

    private void goFromVifOrVifeManSpec(final byte b) {
        if ((b & Decoder.EXTENTION_BIT) == Decoder.EXTENTION_BIT) {
            setState(DecodeState.MAN_SPEC_VIFE);
        } else if ((dfc == DataFieldCode.SELECTION_FOR_READOUT) || (dfc == DataFieldCode.SPECIAL_FUNCTION_GLOBAL_READOUT_REQUEST)) {
            createDataBlock();
            setState(DecodeState.RESULT_AVAIL);
        } else {
            startCollectingValue();
        }
    }

    private void goFromVifOrVife(final byte b) {
        if ((b & Decoder.EXTENTION_BIT) == Decoder.EXTENTION_BIT) {
            setState(DecodeState.VIFE);
        } else if ((dfc == DataFieldCode.SELECTION_FOR_READOUT) || (dfc == DataFieldCode.SPECIAL_FUNCTION_GLOBAL_READOUT_REQUEST)) {
            createDataBlock();
            setState(DecodeState.RESULT_AVAIL);
        } else {
            startCollectingValue();
        }
    }

    public void init(LongFrame frame) {

        functionField = null;
        storageNumber = 0;
        dfc = null;
        variableLengthType = null;
        objectAction = null;
        tariff = 0;
        subUnit = 0;
        vif = null;
        vifes = null;
        db = null;

        setState(DecodeState.DIF);
        this.frame = frame;
        stack.clear();
    }

    public void setState(DecodeState ds) {
        if ((ds == DecodeState.RESULT_AVAIL) && (db == null)) {
            throw new RuntimeException("DB IST NULL");
        }

        final DecodeState oldState = this.ds;
        this.ds = ds;

        LOG.log(Level.FINEST, "{0} => {1}", new Object[]{oldState, ds});
    }

    private void startCollectingValue() {
        switch (dfc) {
            case NO_DATA:
                stack.clear();
                createDataBlock();
                setState(DecodeState.RESULT_AVAIL);

                return;

            case _8_BIT_INTEGER:
            case _2_DIGIT_BCD:
                stack.init(1);

                break;

            case _16_BIT_INTEGER:
            case _4_DIGIT_BCD:
                stack.init(2);

                break;

            case _24_BIT_INTEGER:
            case _6_DIGIT_BCD:
                stack.init(3);

                break;

            case _32_BIT_INTEGER:
            case _8_DIGIT_BCD:
            case _32_BIT_REAL:
                stack.init(4);

                break;

            case _48_BIT_INTEGER:
            case _12_DIGIT_BCD:
                stack.init(6);

                break;

            case _64_BIT_INTEGER:
                stack.init(8);

                break;

            case VARIABLE_LENGTH:
                setState(DecodeState.SET_VARIABLE_LENGTH);

                return;

            default:
                throw new RuntimeException("START COLLECTING VALUE" + dfc);
        }

        setState(DecodeState.COLLECTING_VALUE);
    }

    private void startCollectingValue(int bytesLeft) {
        stack.init(bytesLeft);
        setState(DecodeState.COLLECTING_VALUE);
    }
}
