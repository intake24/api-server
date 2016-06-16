package net.scran24.common.restygwt;

import org.fusesource.restygwt.rebind.JsonEncoderDecoderClassCreator;
import org.fusesource.restygwt.rebind.RestyJsonSerializerGenerator;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

public class OptionRestySerializerGenerator implements RestyJsonSerializerGenerator {

    @Override
    public Class<? extends JsonEncoderDecoderClassCreator> getGeneratorClass() {
        return OptionCodecGenerator.class;
    }

    @Override
    public JType getType(TypeOracle typeOracle) {
        return typeOracle.findType(Option.class.getName());
    }

}