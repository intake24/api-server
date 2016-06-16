package net.scran24.common.restygwt;

import org.fusesource.restygwt.client.Json;
import org.fusesource.restygwt.rebind.JsonEncoderDecoderClassCreator;
import org.fusesource.restygwt.rebind.JsonEncoderDecoderInstanceLocator;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.json.client.JSONArray;

public class OptionCodecGenerator extends JsonEncoderDecoderClassCreator {

    private JClassType typeArg;
    
    private static final String JSON_ARRAY_CLASS = JSONArray.class.getName();

    public OptionCodecGenerator(TreeLogger logger, GeneratorContext context, JClassType source)
            throws UnableToCompleteException {
        super(logger, context, source);
    }

    private JClassType getTypeArg() throws UnableToCompleteException {
        JParameterizedType parameterizedType = source.isParameterized();
        if (parameterizedType == null || parameterizedType.getTypeArgs() == null || parameterizedType.getTypeArgs().length != 1) {
            getLogger().log(ERROR, "Optional types must have exactly one type parameter");
            throw new UnableToCompleteException();
        }
        return parameterizedType.getTypeArgs()[0];
    }

    @Override
    public void generate() throws UnableToCompleteException {
        locator = new JsonEncoderDecoderInstanceLocator(context, getLogger());
        generateSingleton(shortName);
        typeArg = getTypeArg();
        generateEncodeMethod();
        generateDecodeMethod();
    }

    private void generateEncodeMethod() throws UnableToCompleteException {
        p("public " + JSON_VALUE_CLASS + " encode(" + source.getParameterizedQualifiedSourceName() + " value) {").i(1);
        	p(JSON_ARRAY_CLASS + " array = new " + JSON_ARRAY_CLASS + "();");
            p("if (!value.isEmpty())").i(1);
            	p("array.set(0, " + locator.encodeExpression(typeArg, "value.getOrDie()", Json.Style.DEFAULT) + ");");            
            p("return array;");
        p("}");
        p();
    }

    private void generateDecodeMethod() throws UnableToCompleteException {
        p("public " + source.getName() + " decode(" + JSON_VALUE_CLASS + " value) {").i(1);
        	p(JSON_ARRAY_CLASS + " array = value.isArray();");
            p("if (array == null)").i(1);
                p("throw new DecodingException(\"Expected array, got something else\");").i(-1);
            p("else if (array.size() == 0)");
            	p("return Option.none();");
            p("else if (array.size() == 1)");
            	p("return Option.some(" + locator.decodeExpression(typeArg, "array.get(0)", Json.Style.DEFAULT) + ");");
            p("else throw new DecodingException(\"Expected array of zero or one elements, got more\");").i(-1);
        p("}");
        p();
    }
}