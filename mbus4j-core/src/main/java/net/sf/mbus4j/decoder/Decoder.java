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
package net.sf.mbus4j.decoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;

import net.sf.mbus4j.NotSupportedException;
import net.sf.mbus4j.dataframes.ApplicationReset;
import net.sf.mbus4j.dataframes.Frame;
import net.sf.mbus4j.dataframes.GeneralApplicationError;
import net.sf.mbus4j.dataframes.LongFrame;
import net.sf.mbus4j.dataframes.MBusMedium;
import net.sf.mbus4j.dataframes.PrimaryAddress;
import net.sf.mbus4j.dataframes.RequestClassXData;
import net.sf.mbus4j.dataframes.SelectionOfSlaves;
import net.sf.mbus4j.dataframes.SendInitSlave;
import net.sf.mbus4j.dataframes.SendUserData;
import net.sf.mbus4j.dataframes.SendUserDataManSpec;
import net.sf.mbus4j.dataframes.SetBaudrate;
import net.sf.mbus4j.dataframes.SingleCharFrame;
import net.sf.mbus4j.dataframes.SynchronizeAction;
import net.sf.mbus4j.dataframes.UserDataResponse;

import net.sf.mbus4j.dataframes.datablocks.vif.VifAscii;
import net.sf.mbus4j.dataframes.datablocks.vif.VifManufacturerSpecific;
import net.sf.mbus4j.dataframes.datablocks.vif.Vif;
import net.sf.mbus4j.dataframes.datablocks.vif.VifFB;
import net.sf.mbus4j.dataframes.datablocks.vif.VifFD;
import net.sf.mbus4j.dataframes.datablocks.vif.VifStd;
import net.sf.mbus4j.dataframes.datablocks.vif.Vife;
import net.sf.mbus4j.dataframes.datablocks.vif.VifeError;
import net.sf.mbus4j.dataframes.datablocks.vif.VifeStd;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author arnep@users.sourceforge.net
 * @version $Id$
 */
public class Decoder {

    public static boolean compareVif(Vif vif, String label, String unitOfMeasurement, String siPrefix, Integer exponent) {
        boolean result = vif.getLabel() == null ? label == null : vif.getLabel().equals(label);
        result &= vif.getUnitOfMeasurement() == null ? unitOfMeasurement == null : vif.getUnitOfMeasurement().name().equals(unitOfMeasurement);
        result &= vif.getSiPrefix() == null ? siPrefix == null : vif.getSiPrefix().name().equals(siPrefix);
        result &= vif.getExponent() == null ? exponent == null : vif.getExponent().equals(exponent);
        return result;
    }

    public static boolean compareVife(Vife vife, String label) {
        return vife.getLabel() == null ? label == null : vife.getLabel().equals(label);
    }

    public static Vif getVif(String label, String unitOfMeasurement, String siPrefix, Integer exponent) {
        for (Vif vif : VifStd.values()) {
            if (compareVif(vif, label, unitOfMeasurement, siPrefix, exponent)) {
                return vif;
            }
        }
        for (Vif vif : VifFB.values()) {
            if (compareVif(vif, label, unitOfMeasurement, siPrefix, exponent)) {
                return vif;
            }
        }
        for (Vif vif : VifFD.values()) {
            if (compareVif(vif, label, unitOfMeasurement, siPrefix, exponent)) {
                return vif;
            }
        }
        if ((siPrefix == null) && (exponent == null) && (unitOfMeasurement == null)) {
            if (VifManufacturerSpecific.isManufacturerSecific(label)) {
                return VifManufacturerSpecific.fromLabel(label);
            } else {
                return new VifAscii(label);
            }
        }
        throw new IllegalArgumentException("Could not find Vif");
    }

    public static Vife getVife(String label) {
        for (Vife vife : VifeError.values()) {
            if (compareVife(vife, label)) {
                return vife;
            }
        }
        for (Vife vife : VifeStd.values()) {
            if (compareVife(vife, label)) {
                return vife;
            }
        }
        throw new IllegalArgumentException("Could not find Vife");
    }

    public static MBusMedium getMBusMedium(String label) {
                for (MBusMedium medium : MBusMedium.StdMedium.values()) {
            if (compareMedium(medium, label)) {
                return medium;
            }
        }
        return MBusMedium.UnknownMBusMedium.fromLabel(label);
    }

    private static boolean compareMedium(MBusMedium medium, String label) {
        return medium.getLabel() == null ? label == null : medium.getLabel().equals(label);
    }

    public enum DecodeState {

        EXPECT_START,
        LONG_LENGTH_1,
        LONG_LENGTH_2,
        START_LONG_PACK,
        C_FIELD,
        A_FIELD,
        CI_FIELD,
        APPLICATION_RESET_SUBCODE,
        GENERAL_APPLICATION_ERRORCODE,
        IDENT_NUMBER,
        MANUFACTURER,
        VERSION,
        MEDIUM,
        ACCESS_NUMBER,
        STATUS,
        SIGNATURE,
        VARIABLE_DATA_BLOCK,
        CHECKSUM,
        END_SIGN,
        ERROR;
    }
    private final static Logger log = LoggerFactory.getLogger(Decoder.class);
    public static final byte EXTENTIONS_BIT = (byte) 0x80;
    public static final byte EXTENTIONS_BIT_MASK = 0x7F;

    public static byte[] ascii2Bytes(String s) {
        byte[] result = new byte[s.length() / 2];
        for (int i = 0; i < s.length() / 2; i++) {
            result[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2),
                    16);
        }
        return result;
    }

    public static String bytes2Ascii(byte[] byteArray) {
        StringBuilder sb = new StringBuilder(byteArray.length);
        for (byte b : byteArray) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private int expectedLengt;
    private byte checksum;
    private Frame parsingFrame;
    private Stack stack = new Stack();
    private int dataPos;
    private byte start;
    private VariableDataBlockDecoder vdbd = new VariableDataBlockDecoder();
    private DecodeState state = DecodeState.EXPECT_START;

    public Decoder() {
    }

    public Frame addByte(final byte b) {
        checksum += b;
        dataPos++;
        if (DecodeState.ERROR.equals(state)) {
            if (b == 0x10 || b == 0x68 || b == 0xE5) {
                state = DecodeState.EXPECT_START;
            } //try to recover
        }
        switch (state) {
            case EXPECT_START:
                if (b == 0x68) {
                    parsingFrame = null;
                    start = b;
                    setState(DecodeState.LONG_LENGTH_1);
                } else if (b == 0x10) {
                    parsingFrame = null;
                    start = b;
                    setState(DecodeState.C_FIELD);
                } else if ((b & 0xFF) == 0xE5) {
                    parsingFrame = SingleCharFrame.SINGLE_CHAR_FRAME;
                    return parsingFrame;
                } else {
                    log.info(String.format("State: %s expect package start (0x68|0x10|0xE5), but found: 0x%02x", state, b));
                }
                break;
            case LONG_LENGTH_1:
                expectedLengt = b & 0xFF;
                setState(DecodeState.LONG_LENGTH_2);
                break;
            case LONG_LENGTH_2:
                if (expectedLengt != (b & 0xFF)) {
                    setState(DecodeState.ERROR);
                    throw new DecodeException(String.format("expected length: 0x%02x found: 0x%02x", expectedLengt, b));
                } else {
                    setState(DecodeState.START_LONG_PACK);
                    break;
                }
            case START_LONG_PACK:
                dataPos = 0;
                if (b == 0x68) {
                    setState(DecodeState.C_FIELD);
                    break;
                } else {
                    setState(DecodeState.ERROR);
                    throw new DecodeException(String.format("expected Long package end (0x68) but found: (0x%02x)", b));
                }
            case C_FIELD:
                checksum = b;
                switch (start) {
                    case 0x10:
                        if (b == 0x40) {
                            parsingFrame = new SendInitSlave();
                        } else if (b == 0x5B || b == 0x7B) {
                            parsingFrame = new RequestClassXData((b & 0x20) == 0x20, (b & 0x10) == 0x10, Frame.ControlCode.REQ_UD2);
                        } else if (b == 0x5A || b == 0x7A) {
                            parsingFrame = new RequestClassXData((b & 0x20) == 0x20, (b & 0x10) == 0x10, Frame.ControlCode.REQ_UD1);
                        } else {
                            setState(DecodeState.ERROR);
                            throw new NotSupportedException(String.format("C-Field = 0x%02X", b));
                        }
                        break;
                    case 0x68:
                        if (b == 0x53 || b == 0x73) {
                            parsingFrame = new SendUserData((b & 0x20) == 0x20);
                        } else if (b == 0x08 || b == 0x18 || b == 0x28 || b == 0x38) {
                            parsingFrame = new UserDataResponse((b & 0x20) == 0x20, (b & 0x10) == 0x10);
                        } else {
                            setState(DecodeState.ERROR);
                            throw new NotSupportedException(String.format("C-Field = 0x02X", b));
                        }
                        break;
                    default:
                }
                setState(DecodeState.A_FIELD);
                break;
            case A_FIELD:
                if (parsingFrame instanceof PrimaryAddress) {
                    ((PrimaryAddress) parsingFrame).setAddress(b);
                } else {
                    setState(DecodeState.ERROR);
                    throw new NotSupportedException("Cant set Address!");
                }

                switch (start) {
                    case 0x10:
                        setState(DecodeState.CHECKSUM);
                        break;
                    case 0x68:
                        setState(DecodeState.CI_FIELD);
                        break;
                    default:
                        setState(DecodeState.ERROR);
                        throw new NotSupportedException("A Field dont know where to go!");
                }
                break;
            case CI_FIELD:
                if (parsingFrame instanceof SendUserData) {
                    decodeCiSendUserData(b & 0xFF);
                } else if (parsingFrame instanceof UserDataResponse) {
                    decodeCiUserDataResponse(b & 0xFF);
                } else {
                    setState(DecodeState.ERROR);
                    throw new NotSupportedException(String.format("CI Field expected: 0x51 | 0x72, but found: 0x%02x | %s", b, parsingFrame.getClass().getName()));
                }
                break;
            case GENERAL_APPLICATION_ERRORCODE:
                if (!(parsingFrame instanceof GeneralApplicationError)) {
                    setState(DecodeState.ERROR);
                    throw new NotSupportedException("General Application Error Expected");
                }
                ((GeneralApplicationError) parsingFrame).setError(b);
                setState(DecodeState.CHECKSUM);
                break;
            case APPLICATION_RESET_SUBCODE:
                if (!(parsingFrame instanceof ApplicationReset)) {
                    setState(DecodeState.ERROR);
                    throw new NotSupportedException("Application Reset Expected");
                }
                ((ApplicationReset) parsingFrame).setTelegramTypeAndSubTelegram(b);
                setState(DecodeState.CHECKSUM);
                break;
            case IDENT_NUMBER:
                stack.push(b);
                if (stack.isFull()) {
                    if (parsingFrame instanceof SelectionOfSlaves) {
                        getSelectionOfSlaves().setBcdId(stack.popInteger(4));
                    } else {
                        getUserDataResponse().setIdentNumber(stack.popBcdInteger(8));
                    }
                    stack.init(2);
                    setState(DecodeState.MANUFACTURER);
                }
                break;
            case MANUFACTURER:
                stack.push(b);
                if (stack.isFull()) {
                    if (parsingFrame instanceof SelectionOfSlaves) {
                        getSelectionOfSlaves().setBcdMan(stack.popShort());
                    } else {
                        getUserDataResponse().setManufacturer(stack.popMan());
                    }
                    stack.clear();
                    setState(DecodeState.VERSION);
                }
                break;
            case VERSION:
                if (parsingFrame instanceof SelectionOfSlaves) {
                    getSelectionOfSlaves().setBcdVersion((byte)(b & 0xFF));
                } else {
                    getUserDataResponse().setVersion((byte) (b & 0x00FF));
                }
                setState(DecodeState.MEDIUM);
                break;
            case MEDIUM:
                if (parsingFrame instanceof SelectionOfSlaves) {
                    getSelectionOfSlaves().setBcdMedium((byte)(b & 0xFF));
                    setState(DecodeState.CHECKSUM);
                } else {
                    getUserDataResponse().setMedium(MBusMedium.StdMedium.valueOf(b));
                    setState(DecodeState.ACCESS_NUMBER);
                }
                break;
            case ACCESS_NUMBER:
                getUserDataResponse().setAccessNumber((short) (b & 0x00FF));
                setState(DecodeState.STATUS);
                break;
            case STATUS:
                getUserDataResponse().setStatus(new UserDataResponse.StatusCode[0]);
                switch (b & 0x03) {
                    case 0x00:
                        getUserDataResponse().addStatus(UserDataResponse.StatusCode.APPLICATION_NO_ERROR);
                        break;
                    case 0x01:
                        getUserDataResponse().addStatus(UserDataResponse.StatusCode.APPLICATION_BUSY);
                        break;
                    case 0x02:
                        getUserDataResponse().addStatus(UserDataResponse.StatusCode.APPLICATION_ANY_ERROR);
                        break;
                    case 0x03:
                        getUserDataResponse().addStatus(UserDataResponse.StatusCode.APPLICATION_RESERVED);
                        break;
                }
                if ((b & 0x04) == 0x04) {
                    getUserDataResponse().addStatus(UserDataResponse.StatusCode.POWER_LOW);
                }
                if ((b & 0x08) == 0x08) {
                    getUserDataResponse().addStatus(UserDataResponse.StatusCode.PERMANENT_ERROR);
                }
                if ((b & 0x10) == 0x10) {
                    getUserDataResponse().addStatus(UserDataResponse.StatusCode.TEMPORARY_ERROR);
                }
                if ((b & 0x20) == 0x20) {
                    getUserDataResponse().addStatus(UserDataResponse.StatusCode.MAN_SPEC_0X20);
                }
                if ((b & 0x40) == 0x40) {
                    getUserDataResponse().addStatus(UserDataResponse.StatusCode.MAN_SPEC_0X40);
                }
                if ((b & 0x80) == 0x80) {
                    getUserDataResponse().addStatus(UserDataResponse.StatusCode.MAN_SPEC_0X80);
                }
                stack.init(2);
                setState(DecodeState.SIGNATURE);
                break;
            case SIGNATURE:
                stack.push(b);
                if (stack.isFull()) {
                    getUserDataResponse().setSignature(stack.popShort());
                    stack.clear();
                    if (expectedLengt == dataPos) {
                        setState(DecodeState.CHECKSUM);
                    } else {
                        setState(DecodeState.VARIABLE_DATA_BLOCK);
                    }
                }
                break;
            case VARIABLE_DATA_BLOCK:
                if (vdbd.getState().equals(VariableDataBlockDecoder.DecodeState.WAIT_FOR_INIT)) {
                    vdbd.init(getLongFrame());
                }
                switch (vdbd.addByte(b, expectedLengt - dataPos)) {
                    case ERROR:
                        vdbd.setState(VariableDataBlockDecoder.DecodeState.WAIT_FOR_INIT);
                        break;
                    case RESULT_AVAIL:
                        getLongFrame().addDataBlock(vdbd.getDataBlock());
                        vdbd.setState(VariableDataBlockDecoder.DecodeState.WAIT_FOR_INIT);
                        if (expectedLengt - dataPos == 0) {
                            setState(DecodeState.CHECKSUM);
                        }
                        break;
                }
                break;
            case CHECKSUM:
                checksum -= b;
                if (checksum == b) {
                    setState(DecodeState.END_SIGN);
                    break;
                } else {
                    setState(DecodeState.ERROR);
                    throw new DecodeException(String.format("Checksum mismatch expected: 0x%02x but found: 0x%02x", checksum, b));
                }

            case END_SIGN:
                //TODO
                if (b == 0x16) {
                    setState(DecodeState.EXPECT_START);
                    return parsingFrame;
                } else {
                    setState(DecodeState.ERROR);
                    throw new DecodeException(String.format("Excpected Endsign (0x16) but found: 0x%02x", b));
                }
            default:
                return null;
        }
        return null;
    }

    private int bcd2Int(byte[] data) {
        int result = 0;
        for (int i = data.length - 1; i >= 0; i--) {
            result *= 10;
            result += ((data[i] >> 4) & 0x0F);
            result *= 10;
            result += (data[i] & 0x0F);
        }
        return result;
    }

    private void decodeCiSendUserData(int b) {
        switch (b) {
            case 0x50:
                parsingFrame = new ApplicationReset((SendUserData) parsingFrame);
                setState(DecodeState.APPLICATION_RESET_SUBCODE);
                break;
            case 0x51:
                setState(DecodeState.VARIABLE_DATA_BLOCK);
                break;
            case 0x52:
                parsingFrame = new SelectionOfSlaves((SendUserData) parsingFrame);
                stack.init(4);
                setState(DecodeState.IDENT_NUMBER);
                break;
            case 0x54:
                parsingFrame = new SynchronizeAction((SendUserData) parsingFrame);
                setState(DecodeState.CHECKSUM);
                break;
            case 0xB8:
                parsingFrame = new SetBaudrate((SendUserData) parsingFrame, 300);
                setState(DecodeState.CHECKSUM);
                break;
            case 0xB9:
                parsingFrame = new SetBaudrate((SendUserData) parsingFrame, 600);
                setState(DecodeState.CHECKSUM);
                break;
            case 0xBA:
                parsingFrame = new SetBaudrate((SendUserData) parsingFrame, 1200);
                setState(DecodeState.CHECKSUM);
                break;
            case 0xBB:
                parsingFrame = new SetBaudrate((SendUserData) parsingFrame, 2400);
                setState(DecodeState.CHECKSUM);
                break;
            case 0xBC:
                parsingFrame = new SetBaudrate((SendUserData) parsingFrame, 4800);
                setState(DecodeState.CHECKSUM);
                break;
            case 0xBD:
                parsingFrame = new SetBaudrate((SendUserData) parsingFrame, 9600);
                setState(DecodeState.CHECKSUM);
                break;
            case 0xBE:
                parsingFrame = new SetBaudrate((SendUserData) parsingFrame, 19200);
                setState(DecodeState.CHECKSUM);
                break;
            case 0xBF:
                parsingFrame = new SetBaudrate((SendUserData) parsingFrame, 38400);
                setState(DecodeState.CHECKSUM);
                break;
            case 0xA0:
            case 0xA1:
            case 0xA2:
            case 0xA3:
            case 0xA4:
            case 0xA5:
            case 0xA6:
            case 0xA7:
            case 0xA8:
            case 0xA9:
            case 0xAA:
            case 0xAB:
            case 0xAC:
            case 0xAD:
            case 0xAE:
            case 0xAF:
                parsingFrame = new SendUserDataManSpec((SendUserData) parsingFrame, b);
                setState(DecodeState.CHECKSUM);
                break;
            default:
                setState(DecodeState.ERROR);
                throw new NotSupportedException(String.format("CI field of SND_UD: 0x%02x | %s", b, parsingFrame.getClass().getName()));
        }
    }

    private void decodeCiUserDataResponse(int b) {
        switch (b) {
            case 0x70:
                parsingFrame = new GeneralApplicationError((UserDataResponse) parsingFrame);
                setState(DecodeState.GENERAL_APPLICATION_ERRORCODE);
                break;
            case 0x72:
                stack.init(4);
                setState(DecodeState.IDENT_NUMBER);
                break;
            default:
                setState(DecodeState.ERROR);
                throw new NotSupportedException(String.format("CI field of UD_RESP: 0x%02x | %s", b, parsingFrame.getClass().getName()));
        }
    }

    public Frame getFrame() {
        return parsingFrame;
    }

    private LongFrame getLongFrame() {
        return (LongFrame) parsingFrame;
    }

    private SelectionOfSlaves getSelectionOfSlaves() {
        return (SelectionOfSlaves) parsingFrame;
    }

    public DecodeState getState() {
        return state;
    }

    private UserDataResponse getUserDataResponse() {
        return (UserDataResponse) parsingFrame;
    }

    private void setState(DecodeState state) {
        DecodeState oldState = this.state;
        this.state = state;
        if (log.isDebugEnabled()) {
            log.debug(String.format("DecodeState change from: %20s => %s", oldState, state));
        }
    }
}
