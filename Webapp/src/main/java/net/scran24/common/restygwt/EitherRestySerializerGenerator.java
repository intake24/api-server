package net.scran24.common.restygwt;

import org.fusesource.restygwt.rebind.JsonEncoderDecoderClassCreator;
import org.fusesource.restygwt.rebind.RestyJsonSerializerGenerator;
import org.workcraft.gwt.shared.client.Either;

import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

public class EitherRestySerializerGenerator implements RestyJsonSerializerGenerator {

    @Override
    public Class<? extends JsonEncoderDecoderClassCreator> getGeneratorClass() {
        return EitherCodecGenerator.class;
    }

    @Override
    public JType getType(TypeOracle typeOracle) {
        return typeOracle.findType(Either.class.getName());
    }

}