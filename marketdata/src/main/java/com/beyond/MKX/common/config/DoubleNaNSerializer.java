package com.beyond.MKX.common.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Double NaN/Infinity를 null로 직렬화하는 커스텀 Serializer
 * 
 * 프론트엔드에서 NaN 처리 시 발생하는 NullPointerException 방지
 */
public class DoubleNaNSerializer extends JsonSerializer<Double> {
    
    @Override
    public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.isNaN() || value.isInfinite()) {
            gen.writeNull();
        } else {
            gen.writeNumber(value);
        }
    }
}
