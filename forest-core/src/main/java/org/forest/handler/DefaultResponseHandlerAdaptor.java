package org.forest.handler;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.forest.converter.ForestConverter;
import org.forest.exceptions.ForestHandlerException;
import org.forest.utils.ForestDataType;
import org.forest.reflection.ForestMethod;
import org.forest.http.ForestRequest;
import org.forest.http.ForestResponse;
import org.forest.exceptions.ForestRuntimeException;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by Administrator on 2016/5/25.
 */
public class DefaultResponseHandlerAdaptor extends ResponseHandler {


    public DefaultResponseHandlerAdaptor(ForestMethod method) {
        super(method);
    }

    private static Class typeToClass(Type genType) {
        if (genType instanceof ParameterizedType) {
            return (Class) ((ParameterizedType) genType).getRawType();
        }
        return (Class) genType;
    }


    public Object getResult(ForestRequest request, ForestResponse response, Type resultType) {
        Class clazz = typeToClass(resultType);

        HttpEntity entity = response.getHttpResponse().getEntity();
        if (entity != null) {
            try {
                if (void.class.isAssignableFrom(clazz)) {
                    return null;
                }
                if (ForestResponse.class.isAssignableFrom(clazz)) {
                    return response;
                }
                if (boolean.class.isAssignableFrom(clazz) || Boolean.class.isAssignableFrom(clazz)) {
                    return response.isSuccess();
                }
                if (clazz.isArray()) {
                    if (byte[].class.isAssignableFrom(clazz)) {
                        return EntityUtils.toByteArray(entity);
                    }
                }
                String responseText = response.getContent();
                response.setContent(responseText);
                if (CharSequence.class.isAssignableFrom(clazz)) {
                    return responseText;
                }
                if (InputStream.class.isAssignableFrom(clazz)) {
                    return entity.getContent();
                }

                ForestDataType dataType = request.getDataType();
                ForestConverter converter = method.getDataConverter(dataType);
                return converter.convertToJavaObject(responseText, resultType);

            } catch (IOException e) {
                throw new ForestHandlerException(e, request, response);
            }
        }
        else if (ForestResponse.class.isAssignableFrom(clazz)) {
            return response;
        }
        return null;
    }
}
