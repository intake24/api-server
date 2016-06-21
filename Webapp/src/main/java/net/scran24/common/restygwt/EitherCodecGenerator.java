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
import com.google.gwt.json.client.JSONNumber;

public class EitherCodecGenerator extends JsonEncoderDecoderClassCreator {

    private JClassType leftType;
    private JClassType rightType;
    
    private static final String JSON_ARRAY_CLASS = JSONArray.class.getName();
    private static final String JSON_NUMBER_CLASS = JSONNumber.class.getName();

    public EitherCodecGenerator(TreeLogger logger, GeneratorContext context, JClassType source)
            throws UnableToCompleteException {
        super(logger, context, source);
    }


    @Override
    public void generate() throws UnableToCompleteException {
        locator = new JsonEncoderDecoderInstanceLocator(context, getLogger());
        generateSingleton(shortName);
        
        JParameterizedType parameterizedType = source.isParameterized();
        
        if (parameterizedType == null || parameterizedType.getTypeArgs() == null || parameterizedType.getTypeArgs().length != 2) {
            getLogger().log(ERROR, "Either types must have exactly two type parameters");
            throw new UnableToCompleteException();
        }

        leftType = parameterizedType.getTypeArgs()[0];
        rightType = parameterizedType.getTypeArgs()[1];

        generateEncodeMethod();
        generateDecodeMethod();
    }

    private void generateEncodeMethod() throws UnableToCompleteException {
        p("public " + JSON_VALUE_CLASS + " encode(" + source.getParameterizedQualifiedSourceName() + " value) {").i(1);
        	p(JSON_ARRAY_CLASS + " array = new " + JSON_ARRAY_CLASS + "();");        	
        	p("if (value.isLeft()) {");
        	p("array.set(0, new " + JSON_NUMBER_CLASS + "(0));");
        	p("array.set(1, " + locator.encodeExpression(leftType, "value.getLeftOrDie()", Json.Style.DEFAULT) + ");");
        	p("} else {");
        	p("array.set(0, new " + JSON_NUMBER_CLASS + "(1));");
        	p("array.set(1, " + locator.encodeExpression(rightType, "value.getRightOrDie()", Json.Style.DEFAULT) + ");");
        	p("}");
            p("return array;");
        p("}");
        p();
    }

    private void generateDecodeMethod() throws UnableToCompleteException {
        p("public " + source.getName() + " decode(" + JSON_VALUE_CLASS + " value) {").i(1);
        	p(JSON_ARRAY_CLASS + " array = value.isArray();");
            p("if (array == null)").i(1);
                p("throw new DecodingException(\"Expected array, got something else\");").i(1);
            p("else if (array.size() != 2)");
            	p("throw new DecodingException(\"Array size must be 2 for Either\");").i(1);
            p("else {");
            	p(JSON_NUMBER_CLASS + " typeCodeObj = array.get(0).isNumber();");
            	p("if (typeCodeObj == null) {");
            		p("throw new DecodingException(\"First element of an Either array must be a number\");");
            	p("} else {");
            	p("int typeCode = (int)typeCodeObj.doubleValue();");
            		p("if (typeCode == 0) {");
            			p("return new Either.Left<" + leftType.getParameterizedQualifiedSourceName() + ", " + rightType.getParameterizedQualifiedSourceName() + ">("  + locator.decodeExpression(leftType, "array.get(1)", Json.Style.DEFAULT) + ");");
            		p("} else if (typeCode == 1) {");
            			p("return new Either.Right<" + leftType.getParameterizedQualifiedSourceName() + ", " + rightType.getParameterizedQualifiedSourceName() + ">("  + locator.decodeExpression(rightType, "array.get(1)", Json.Style.DEFAULT) + ");");
            		p("} else {");
            			p("throw new DecodingException(\"Type code of an Either array must be 0 or 1\");");
            		p("}");            		
            	p("}");
            p("}");          
        p("}");
        p();
    }
}