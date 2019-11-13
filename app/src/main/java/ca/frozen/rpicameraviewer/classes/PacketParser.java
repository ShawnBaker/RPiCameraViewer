package ca.frozen.rpicameraviewer.classes;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PacketParser {
    public final static char DEFAULT_SoM = '$';
    public final static char DEFAULT_EoM = '&';
    private char SoM;
    private char EoM;
    private List<PacketDescriptor> descriptors;
    private String idBuffer;
    private String valueBuffer;
    private String crcBuffer;
    private int crcSum;
    private int index;
    private Status status;
    private Error error;
    private PacketDescriptor currentDescriptor = null;

    public PacketParser(final char som_value, final char eom_value, @NonNull PacketDescriptor... descriptors_values) {
        this(som_value, eom_value);

        addDescriptors(descriptors_values);
    }

    public PacketParser(final char som_value, final char eom_value) {
        SoM = som_value;
        EoM = eom_value;

        status = Status.idle;
        error = Error.none;

        descriptors = new ArrayList<>();
    }

    public PacketParser() {
        this(DEFAULT_SoM, DEFAULT_EoM);
    }

    public List<PacketDescriptor> getDescriptors() {
        return descriptors;
    }

    @NonNull
    public Result parse(final char token) {
        switch (status) {
            case idle:
                if (token == SoM) {
                    status = Status.id;
                    error = Error.none;
                    currentDescriptor = null;
                    valueBuffer = "";
                    idBuffer = "";
                    crcBuffer = "";
                    crcSum = 0;
                    index = 0;
                }
                break;

            case id: {
                currentDescriptor = getDescriptor(token);

                if (currentDescriptor != null) {
                    idBuffer += token;

                    if (currentDescriptor.getHasIndex())
                        status = Status.index;
                    else
                        status = Status.value;
                } else {
                    status = Status.idle;
                    error = Error.unknown_id_error;
                }
            }
            break;

            case index: {
                int _index = Character.getNumericValue(token);

                if (currentDescriptor.indexIsValid(_index)) {
                    index = _index;
                    status = Status.value;
                } else {
                    status = Status.idle;
                    error = Error.format_error;
                }
            }
            break;

            case value:
                if ((((token >= '0') && (token <= '9')) || ((token >= 'A') && (token <= 'F')))) {
                    valueBuffer += token;
                    crcSum += (int) token;
                } else {
                    status = Status.idle;
                    error = Error.format_error;
                }

                if (valueBuffer.length() == currentDescriptor.getBufferLength())
                    status = Status.crc;
                break;

            case crc:
                if ((((token >= '0') && (token <= '9')) || ((token >= 'A') && (token <= 'F'))))
                    crcBuffer += token;
                else {
                    status = Status.idle;
                    error = Error.format_error;
                }

                if (crcBuffer.length() == 2) {
                    crcSum &= 0xFF;
                    status = Status.eom;

                    if (Integer.valueOf(crcBuffer, 16) != crcSum) {
                        status = Status.idle;
                        error = Error.crc_error;
                    }
                }
                break;

            case eom:
                status = Status.idle;

                if (token == EoM){
                    return Result.completed;
                }
                else
                    error = Error.format_error;
                break;

            default:
                break;
        }

        if (error != Error.none)
            return Result.error;

        return Result.parsing;
    }

    public int getValue() {
        int _value = Integer.valueOf(valueBuffer, 16);

        if (_value > 32767)
            _value -= 65536;

        return _value;
    }

    public int getUnsigned() {
        int _value = Integer.valueOf(valueBuffer, 16);
        return _value;
    }

    public float getFloat() {
        final Long intValue = Long.parseLong(valueBuffer, 16);
        final Float floatValue = Float.intBitsToFloat(intValue.intValue());

        return floatValue;
    }

    @NonNull
    public String getId() {
        return idBuffer;
    }

    public int getIndex() {
        return index;
    }

    @NonNull
    public Error getError() {
        return error;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    public PacketParser addDescriptor(@NonNull PacketDescriptor descriptor) {
        descriptors.add(descriptor);

        return this;
    }

    public PacketParser addDescriptors(@NonNull PacketDescriptor... descriptors_values) {
        if (descriptors_values == null)
            return this;

        descriptors.addAll(Arrays.asList(descriptors_values));

        return this;
    }

    @Nullable
    private PacketDescriptor getDescriptor(final char id) {
        for (PacketDescriptor desc : descriptors) {
            if (desc.isValid(id))
                return desc;
        }

        return null;
    }

    public enum Status {
        idle,
        id,
        index,
        crc,
        value,
        eom
    }

    public enum Result {
        parsing,
        error,
        completed
    }

    public enum Error {
        none,
        format_error,
        crc_error,
        unknown_id_error
    }

    public static class PacketDescriptor<N> {
        private N typeValue;

        private String messageId;
        private int bufferLength;
        private boolean hasIndex;
        private int minIndex;
        private int maxIndex;

        public PacketDescriptor(final char messageId_value, final int bufferLength_value, final boolean hasIndex_value, final int minIndex_value, final int maxIndex_value) {
            messageId = "";
            messageId += messageId_value;
            bufferLength = bufferLength_value;
            hasIndex = hasIndex_value;
            minIndex = minIndex_value;
            maxIndex = maxIndex_value;
        }

        public PacketDescriptor(final char messageId_value, final int bufferLength_value) {
            this(messageId_value, bufferLength_value, false, 0, 0);
        }

        public int getBufferLength() {
            return bufferLength;
        }

        public boolean isValid(final char id) {
            return (messageId.charAt(0) == id);
        }

        public boolean isValid(@NonNull final String id) {
            return id.equals(messageId);
        }

        public boolean getHasIndex() {
            return hasIndex;
        }

        public boolean indexIsValid(final int index) {
            return ((index <= maxIndex) && (index >= minIndex));
        }
    }
}
