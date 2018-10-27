/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.simplesi.restserver.rest;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.simplesi.restserver.model.lazytype.LazyTypeObjectMapperEnum;
import com.xpert.core.crud.AbstractBusinessObject;
import com.xpert.core.exception.BusinessException;
import com.xpert.persistence.dao.BaseDAO;
import com.xpert.persistence.exception.DeleteException;
import com.xpert.persistence.query.LikeType;
import com.xpert.persistence.query.QueryParameter;
import com.xpert.persistence.query.Restrictions;
import com.xpert.persistence.utils.EntityUtils;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * @author Rafael Lima
 * @param <T>
 */
public abstract class AbstractREST<T> {

    protected abstract AbstractBusinessObject<T> getBO();

    protected BaseDAO getDAO() {
        return getBO().getDAO();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response save(String json) throws BusinessException {
        T entity = getEntityFromJson(json);
        if (entity != null) {
            getBO().save(entity);
            return Response.ok(getJsonFromEntity(entity), MediaType.APPLICATION_JSON).build();
        }
        return Response.noContent().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response edit(String json) throws BusinessException {
        T entity = getEntityFromJson(json);
        if (entity != null) {
            getBO().save(entity);
            return Response
                    .ok(getJsonFromEntity(entity), MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") String id) throws DeleteException {
        T entity = (T) findEntity(id);
        if (entity != null) {
            getBO().remove(entity);
            return Response.status(202).build();
        }
        return Response.noContent().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@Context UriInfo uriInfo) {
        List<T> list = loadLazyObjectFromList(findList(uriInfo.getQueryParameters()));
        if (list != null && !list.isEmpty()) {
            return Response
                    .ok()
                    .entity(getStringFromList(list))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("{id}")
    public Response find(@PathParam("id") String id) {
        if (id != null) {
            T entity = loadLazyObjectMapping(findEntity(id));
            if (entity != null) {
                return Response
                        .ok(getJsonFromEntity(entity))
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
        }
        return Response.noContent().build();
    }

    private T findEntity(String id) {
        if (id != null) {
            Class typeId = EntityUtils.getIdType(getDAO().getEntityClass());
            if ((typeId == Long.class || typeId == Integer.class) && isNumeric(id)) {
                return (T) getDAO().find(Long.valueOf(id));
            }
            return (T) getDAO().find(id);
        }
        return null;
    }

    private List<T> findList(MultivaluedMap queryParams) {
        Restrictions rest = null;
        if (queryParams.containsKey("$filter") || queryParams.containsKey("$queryString")) {
            rest = getRestrictionsFromQueryParams(queryParams);
        }
        String orderBy = null;
        Integer limit = null;
        Integer offset = null;

        if (queryParams.containsKey("$orderby") || queryParams.containsKey("$orderBy")) {
            Object params = queryParams.getOrDefault("$orderby", null) == null ? queryParams.get("$orderBy") : queryParams.get("$orderby");
            if (params instanceof List) {
                orderBy = (String) ((List<String>) params).get(0);
            }
            if (params instanceof String) {
                orderBy = (String) params;
            }
        }

        if (queryParams.containsKey("$limit")) {
            Object params = queryParams.get("$limit");
            if (params instanceof List) {
                limit = Integer.valueOf(((List<String>) params).get(0));
            }
            if (params instanceof String) {
                limit = Integer.valueOf((String) params);
            }
        }

        if (queryParams.containsKey("$offset")) {
            Object params = queryParams.get("$offset");
            if (params instanceof List) {
                offset = Integer.valueOf(((List<String>) params).get(0));
            }
            if (params instanceof String) {
                offset = Integer.valueOf((String) params);
            }
        }

        return getDAO().list(rest, orderBy, offset, limit);
    }

    private Restrictions getRestrictionsFromQueryParams(MultivaluedMap queryParams) {
        Restrictions rest = null;
        String filter = null;
        String queryString = null;
        Object paramsFilter = queryParams.getOrDefault("$filter", null);
        if (paramsFilter != null) {
            if (paramsFilter instanceof List) {
                filter = (String) ((List<String>) paramsFilter).get(0);
            }
            if (paramsFilter instanceof String) {
                filter = (String) paramsFilter;
            }
            if (filter != null) {
                rest = new Restrictions();
                String[] split = filter.split(" and ", 2);
                for (String query : split) {
                    if (query.contains(" equals ")) {
                        String[] quebra = query.split(" equals ", 2);
                        if (!quebra[0].trim().contains(" ")) {
                            String field = quebra[0].trim();
                            Object value = castValue(field, quebra[1].trim());
                            rest.add(field, value);
                        }
                    }
                    if (query.contains(" not_equals ")) {
                        String[] quebra = query.split(" not_equals ", 2);
                        if (!quebra[0].trim().contains(" ")) {
                            String field = quebra[0].trim();
                            Object value = castValue(field, quebra[1].trim());
                            rest.notEquals(field, value);
                        }
                    }
                    if (query.contains(" not_like ")) {
                        String[] quebra = query.split(" not_like ", 2);
                        if (!quebra[0].trim().contains(" ")) {
                            String field = quebra[0].trim();
                            Object value = castValue(field, quebra[1].trim());
                            rest.addQueryString("UPPER(" + field + ") NOT LIKE UPPER(:var)", new QueryParameter("var", "%" + value.toString() + "%"));
                        }
                    }
                    if (query.contains(" like ")) {
                        String[] quebra = query.split(" like ", 2);
                        if (!quebra[0].trim().contains(" ")) {
                            String field = quebra[0].trim();
                            Object value = castValue(field, quebra[1].trim());
                            rest.like(field, value, LikeType.BOTH);
                        }
                    }
                    if (query.contains(" starts_with ")) {
                        String[] quebra = query.split(" starts_with ", 2);
                        if (!quebra[0].trim().contains(" ")) {
                            String field = quebra[0].trim();
                            Object value = castValue(field, quebra[1].trim());
                            rest.addQueryString("UPPER(" + field + ") LIKE UPPER(:var)", new QueryParameter("var", value.toString() + "%"));
                        }
                    }
                    if (query.contains(" ends_with ")) {
                        String[] quebra = query.split(" ends_with ", 2);
                        if (!quebra[0].trim().contains(" ")) {
                            String field = quebra[0].trim();
                            Object value = castValue(field, quebra[1].trim());
                            rest.addQueryString("UPPER(" + field + ") LIKE UPPER(:var)", new QueryParameter("var", "%" + value.toString()));
                        }
                    }
                    if (query.contains(" greater_than ")) {
                        String[] quebra = query.split(" greater_than ", 2);
                        if (!quebra[0].trim().contains(" ")) {
                            String field = quebra[0].trim();
                            Object value = castValue(field, quebra[1].trim());
                            rest.greaterThan(field, value);
                        }
                    }
                    if (query.contains(" greater_equals_than ")) {
                        String[] quebra = query.split(" greater_equals_than ", 2);
                        if (!quebra[0].trim().contains(" ")) {
                            String field = quebra[0].trim();
                            Object value = castValue(field, quebra[1].trim());
                            rest.greaterEqualsThan(field, value);
                        }
                    }
                    if (query.contains(" less_than ")) {
                        String[] quebra = query.split(" less_than ", 2);
                        if (!quebra[0].trim().contains(" ")) {
                            String field = quebra[0].trim();
                            Object value = castValue(field, quebra[1].trim());
                            rest.lessThan(field, value);
                        }
                    }
                    if (query.contains(" less_equals_than ")) {
                        String[] quebra = query.split(" less_equals_than ", 2);
                        if (!quebra[0].trim().contains(" ")) {
                            String field = quebra[0].trim();
                            Object value = castValue(field, quebra[1].trim());
                            rest.lessEqualsThan(field, value);
                        }
                    }
                    if (query.contains(" is_null ")) {
                        String[] quebra = query.split(" is_null ", 2);
                        if (!quebra[0].trim().contains(" ")) {
                            String field = quebra[0].trim();
                            rest.isNull(field);
                        }
                    }
                    if (query.contains(" is_not_null ")) {
                        String[] quebra = query.split(" is_not_null ", 2);
                        if (!quebra[0].trim().contains(" ")) {
                            String field = quebra[0].trim();
                            rest.isNotNull(field);
                        }
                    }
                    if (query.contains(" is_empty ")) {
                        String[] quebra = query.split(" is_empty ", 2);
                        if (!quebra[0].trim().contains(" ")) {
                            String field = quebra[0].trim();
                            rest.isEmpty(field);
                        }
                    }
                    if (query.contains(" is_not_empty ")) {
                        String[] quebra = query.split(" is_not_empty ", 2);
                        if (!quebra[0].trim().contains(" ")) {
                            String field = quebra[0].trim();
                            rest.isNotEmpty(field);
                        }
                    }
                }
            }
        }

        Object paramsQueryString = queryParams.getOrDefault("$queryString", null);
        if (paramsQueryString != null) {
            if (paramsQueryString instanceof List) {
                queryString = (String) ((List<String>) paramsQueryString).get(0);
            }
            if (paramsQueryString instanceof String) {
                queryString = (String) paramsQueryString;
            }
            if (queryString != null) {
                if (rest == null) {
                    rest = new Restrictions();
                }
                rest.addQueryString(queryString);
            }
        }
        return rest;
    }

    private Object castValue(String field, String value) {
        if (field != null && value != null) {
            Class fieldType = getFieldType(field);
            if (fieldType == String.class) {
                return value;
            }
            if (fieldType == Integer.class) {
                return Integer.valueOf(value);
            }
            if (fieldType == Long.class) {
                return Long.valueOf(value);
            }
            if (fieldType == Double.class) {
                return Double.valueOf(value);
            }
            if (fieldType == Float.class) {
                return Float.valueOf(value);
            }
            if (fieldType == BigDecimal.class) {
                return new BigDecimal(value);
            }
            if (fieldType == Date.class) {
                try {
                    return new SimpleDateFormat("yyyy-MM-ddTHH:mm:ss").parse(value);
                } catch (ParseException ex) {
                    Logger.getLogger(AbstractREST.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                //CASO NÃO SEJA NENHUM DOS TIPOS ANTERIORES, ENTÃO VERIFICA SE É UMA ENTIDADE
                //CASO SEJA UMA ENTIDADE ENTÃO É GERADA NOVA INSTANCIA E SETADO ID
                Object obj = fieldType.newInstance();
                Class typeId = EntityUtils.getIdType(fieldType);
                String fieldIdName = EntityUtils.getIdFieldName(fieldType);
                Method methodSetId = fieldType.getMethod("set" + getCamelCase(fieldIdName), typeId);
                methodSetId.invoke(obj, typeId.cast(value));
                return obj;

            } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException ex) {
                Logger.getLogger(AbstractREST.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    private Class getFieldType(String fieldName) {
        for (Field field : getDAO().getEntityClass().getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return field.getType();
            }
        }
        return null;
    }

    private String getCamelCase(String string) {
        if (string != null && !string.isEmpty()) {
            string = String.valueOf(string.charAt(0)).toUpperCase() + string.substring(1, string.length());
        }
        return string;
    }

    public T toObject(String json) throws JsonProcessingException, IOException {
        return getEntityFromJson(json);
    }

    public String toJson(T entity, LazyTypeObjectMapperEnum enumLazy) throws JsonProcessingException {
        Hibernate4Module h4m = new Hibernate4Module();
        if (enumLazy == null || enumLazy.equals(LazyTypeObjectMapperEnum.ONLYID)) {
            h4m.enable(Hibernate4Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS);
        } else {
            h4m.enable(Hibernate4Module.Feature.FORCE_LAZY_LOADING);
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.setBase64Variant(Base64Variants.getDefaultVariant());
        mapper.registerModule(h4m);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        return mapper.writeValueAsString(entity);
    }

    public T loadLazyObjectMapping(T entity) {
        if (entity != null) {
            try {
                return loadLazyObjectMapping(entity, null);
            } catch (IOException ex) {
                Logger.getLogger(AbstractREST.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
        return null;
    }

    public T loadLazyObjectMapping(T entity, LazyTypeObjectMapperEnum enumLazy) throws JsonProcessingException, IOException {
        return toObject(toJson(entity, enumLazy));
    }

    public List<T> loadLazyObjectFromList(List<T> records) {
        if (records != null && !records.isEmpty()) {
            try {
                return loadLazyObjectFromList(records, false);
            } catch (IOException ex) {
                Logger.getLogger(AbstractREST.class.getName()).log(Level.SEVERE, null, ex);
                return new LinkedList();
            }
        }
        return new LinkedList();
    }

    public List<T> loadLazyObjectFromList(List<T> records, boolean lazyTypeEager) throws IOException {
        List<T> newList = null;
        if (records != null && !records.isEmpty()) {
            LazyTypeObjectMapperEnum enumLazy = LazyTypeObjectMapperEnum.ONLYID;
            if (lazyTypeEager) {
                enumLazy = LazyTypeObjectMapperEnum.EAGER;
            }

            newList = new LinkedList();
            for (T field : records) {
                newList.add(loadLazyObjectMapping(field, enumLazy));
            }
        }
        return newList;
    }

    public static boolean isNumeric(String numero) {
        if (numero != null && !numero.isEmpty()) {
            boolean isNumeric = true;
            if (!numero.isEmpty()) {
                for (int x = 0; x < numero.length(); x++) {
                    char ch = numero.charAt(x);
                    if (!(ch >= 48 && ch <= 57)) {
                        isNumeric = false;
                        break;
                    }
                }
            }
            return isNumeric;
        }
        return false;
    }

    //<editor-fold defaultstate="collapsed" desc="Gson">
    private Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeHierarchyAdapter(byte[].class, new ByteArrayToBase64TypeAdapter());
        builder.registerTypeAdapter(Date.class, new DateAdapter());
        return builder.create();
    }

    public String getJsonFromEntity(T entity) {
        return getGson().toJson(entity, getEntityType());
    }

    public T getEntityFromJson(String json) {
        return (T) getGson().fromJson(json, getEntityType());
    }

    public List<T> getListFromJson(String json) {
        return getGson().fromJson(json, getEntityTypeList());
    }

    public String getStringFromList(List<T> list) {
        return getGson().toJson(list, getEntityTypeList());
    }

    public Type getEntityType() {
        Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        return type;
    }

    public Type getEntityTypeList() {
        Type type = com.google.gson.reflect.TypeToken.getParameterized(List.class, getEntityType()).getType();
        return type;
    }

    /**
     * Serializer byteArray.
     */
    public static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

        @Override
        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.encodeBase64String(src));
        }

        @Override
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Base64.decodeBase64(json.getAsString());
        }

    }

    /**
     * Serializer de Date.
     */
    private class DateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

        @Override
        public JsonElement serialize(Date t, Type type, JsonSerializationContext jsc) {
            return new JsonPrimitive(dateToJson(t));
        }

        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return jsonToDate(json.getAsJsonPrimitive().getAsString());
        }

        /**
         * Converte uma Date em um valor ISO 8601.
         *
         * @param data Date a ser convertido em String.
         * @return String com a data. Ex: "2013-01-17T00:00:00.124-02:00"
         */
        private String dateToJson(Date data) {
            if (data == null) {
                return null;
            }
            // O toString do Joda DateTime por padrão retorna um String de data em ISO 8601.
            DateTime dt = new DateTime(data, DateTimeZone.forID("-03:00"));
            return dt.toString().replace("-03:00", "Z");
        }

        /**
         * Coverte uma data ISO 8601 em DateTime.
         *
         * @param strDt
         * @return Date Ex: "2013-01-17T00:00:00.124-02:00"
         */
        private Date jsonToDate(String strDt) {
            if (strDt == null) {
                return null;
            }
            DateTime dt = new DateTime(strDt);
            return dt.toDate();
        }
    }
    //</editor-fold>

}
