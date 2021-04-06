/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.logos.restserver.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.logos.restserver.model.dto.MetronicFilter;
import com.logos.restserver.model.dto.MetronicPaginatorState;
import com.logos.restserver.model.dto.MetronicSorteState;
import com.logos.restserver.model.dto.MetronicTableModel;
import com.logos.restserver.model.dto.MetronicTableState;
import com.logos.restserver.model.lazytype.LazyTypeObjectMapperEnum;
import com.xpert.core.crud.AbstractBusinessObject;
import com.xpert.core.exception.BusinessException;
import com.xpert.persistence.dao.BaseDAO;
import com.xpert.persistence.exception.DeleteException;
import com.xpert.persistence.query.LikeType;
import com.xpert.persistence.query.QueryParameter;
import com.xpert.persistence.query.RestrictionType;
import com.xpert.persistence.query.Restrictions;
import com.xpert.persistence.utils.EntityUtils;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Rafael Lima
 * @param <T>
 */
public abstract class AbstractREST<T> extends GenericGSON<T> {

    public final String JSON_UTF8 = "application/json;charset=utf-8";

    protected abstract AbstractBusinessObject<T> getBO();

    protected BaseDAO getDAO() {
        return getBO().getDAO();
    }

    @POST
    @Path("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response save(String json, @HeaderParam("Authorization") String tk) throws BusinessException {
        T entity = getEntityFromJson(json);
        if (entity != null) {
            getBO().save(entity);
            return Response.ok(getJsonFromEntity(entity), JSON_UTF8).build();
        }
        return Response.noContent().build();
    }

    @PUT
    @Path("/edit")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response edit(String json, @Context UriInfo uriInfo, @HeaderParam("Authorization") String tk) throws BusinessException, IOException {
        T entity = getEntityFromJson(json);
        if (entity != null) {
            getBO().save(entity);
            return find((String) EntityUtils.getId(entity), uriInfo);
        }
        return Response.noContent().build();
    }

    @DELETE
    @Path("delete/{id}")
    public Response delete(@PathParam("id") String id, @HeaderParam("Authorization") String tk) throws DeleteException {
        T entity = (T) findEntity(id);
        if (entity != null) {
            getBO().remove(entity);
            return Response.ok("Ok", MediaType.TEXT_PLAIN).build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/list")
    public Response list(@Context UriInfo uriInfo) throws IOException {

        LazyTypeObjectMapperEnum lazyType = getLazyType(uriInfo.getQueryParameters());
        List<T> list = loadLazyObjectFromList(findList(uriInfo.getQueryParameters()), lazyType);
        if (list != null && !list.isEmpty()) {
            return Response
                    .ok()
                    .entity(getStringFromList(list))
                    .type(JSON_UTF8)
                    .build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/get")
    public Response get(@Context UriInfo uriInfo) throws IOException {

        LazyTypeObjectMapperEnum lazyType = getLazyType(uriInfo.getQueryParameters());

        List<T> list = loadLazyObjectFromList(findList(uriInfo.getQueryParameters()), lazyType);
        if (list != null && !list.isEmpty()) {
            if (list.size() == 1) {
                return Response
                        .ok(getJsonFromEntity(list.get(0)))
                        .type(JSON_UTF8)
                        .build();
            } else {
                return Response
                        .ok()
                        .entity(getStringFromList(list))
                        .type(JSON_UTF8)
                        .build();
            }
        }
        return Response.noContent().build();
    }

    @GET
    @Path("find/{id}")
    public Response find(@PathParam("id") String id, @Context UriInfo uriInfo) throws IOException {
        if (id != null) {
            LazyTypeObjectMapperEnum lazyType = getLazyType(uriInfo.getQueryParameters());

            T entity = loadLazyObjectMapping(findEntity(id), lazyType);
            if (entity != null) {
                return Response
                        .ok(getJsonFromEntity(entity))
                        .type(JSON_UTF8)
                        .build();
            }
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/datamodel")
    public Response listToDataModel(@Context UriInfo uriInfo) throws IOException {
        return list(uriInfo);
    }

    @POST
    @Path("/metronic/list")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response dataTableMetronic(MetronicTableState tableState, @Context UriInfo uriInfo) {

        MetronicTableModel<T> table = new MetronicTableModel<T>();
        if (tableState != null) {
            List<T> list = loadLazyObjectFromList(findList(tableState, uriInfo.getQueryParameters()));

            if (list != null && !list.isEmpty()) {
                Restrictions rest = getRestrictionsFromTableState(tableState.getFilter());
                table = new MetronicTableModel<T>(list, getDAO().count(rest));
            }
        }

        return Response
                .ok(MetronicTableModel.getJsonFromEntity(table))
                .type(JSON_UTF8)
                .build();
    }

    protected T findEntity(String id) {
        if (id != null) {
            Class typeId = EntityUtils.getIdType(getDAO().getEntityClass());
            if ((typeId == Long.class || typeId == Integer.class) && isNumeric(id)) {
                return (T) getDAO().find(Long.valueOf(id));
            }
            return (T) getDAO().find(id);
        }
        return null;
    }

    protected List<T> findList(UriInfo uriInfo) {
        return findList(uriInfo.getQueryParameters());
    }

    protected List<T> findList(MetronicTableState tableState, MultivaluedMap queryParams) {
        if (tableState != null) {
            MetronicPaginatorState paginator = tableState.getPaginator();
            MetronicSorteState sort = tableState.getSorting();

            String orderByFromQueryParams = getOrderByFromQueryParams(queryParams);
            String orderBy = orderByFromQueryParams == null ? "id" : orderByFromQueryParams;
            Integer limit = null;
            Integer offset = null;

            if (sort != null && !sort.getColumn().equalsIgnoreCase("id")) {
                orderBy = sort.getColumn() + " " + sort.getDirection();
            }

            if (paginator != null) {
                Integer page = paginator.getPage();
                limit = paginator.getPageSize();
                if (page > 1) {
                    offset = (page - 1) * paginator.getPageSize();
                }
            }
            Restrictions rest = getRestrictionsFromTableState(tableState.getFilter());
            return getDAO().list(rest, orderBy, offset, limit);
        }
        return null;
    }

    protected Restrictions getRestrictionsFromTableState(List<MetronicFilter> filter) {
        Restrictions rest = new Restrictions();
        if (filter != null && !filter.isEmpty()) {
            for (MetronicFilter item : filter) {
                String column = item.getColumn();
                String value = item.getValue();
                String restrcType = item.getRestrictionType();
                RestrictionType restrictionType = RestrictionType.EQUALS;

                if (restrcType != null && !restrcType.trim().isEmpty()) {
                    restrictionType = RestrictionType.getByAcronym(item.getRestrictionType());
                }

                if (restrictionType.equals(RestrictionType.LIKE)) {
                    rest.like("comum.sem_acento(" + column + ")", removeAcentuacao(value), LikeType.BOTH);
                } else {
                    Object columnValue = castValue(column, value);
                    rest.add(column, restrictionType, columnValue);
                }
            }
        }
        return rest;
    }

    protected List<T> findList(MultivaluedMap queryParams) {
        Restrictions rest = null;
        if (queryParams.containsKey("$filter") || queryParams.containsKey("$queryString")) {
            rest = getRestrictionsFromQueryParams(queryParams);
        }
        String orderBy = getOrderByFromQueryParams(queryParams);
        Integer limit = null;
        Integer offset = null;

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

    protected Restrictions getRestrictionsFromQueryParams(UriInfo uriInfo) {
        return getRestrictionsFromQueryParams(uriInfo.getQueryParameters());
    }

    protected String getOrderByFromQueryParams(UriInfo uriInfo) {
        return getOrderByFromQueryParams(uriInfo.getQueryParameters());
    }

    protected String getOrderByFromQueryParams(MultivaluedMap queryParams) {
        String orderBy = "id";
        if (queryParams.containsKey("$orderby") || queryParams.containsKey("$orderBy")) {
            Object params = queryParams.getOrDefault("$orderby", null) == null ? queryParams.get("$orderBy") : queryParams.get("$orderby");
            if (params instanceof List) {
                orderBy = (String) ((List<String>) params).get(0);
            }
            if (params instanceof String) {
                orderBy = (String) params;
            }
        }
        return orderBy;
    }

    protected Restrictions getRestrictionsFromQueryParams(MultivaluedMap queryParams) {
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
                String[] split = filter.split(" and ");
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
                            rest.addQueryString("UPPER(comum.sem_acento(" + field + ") NOT LIKE UPPER(:var)", new QueryParameter("var", "%" + removeAcentuacao(value.toString()).toUpperCase() + "%"));
                        }
                    }
                    if (query.contains(" like ")) {
                        String[] quebra = query.split(" like ", 2);
                        if (!quebra[0].trim().contains(" ")) {
                            String field = quebra[0].trim();
                            Object value = castValue(field, quebra[1].trim());
                            rest.like("comum.sem_acento(" + field + ")", removeAcentuacao(value.toString()), LikeType.BOTH);
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

    protected LazyTypeObjectMapperEnum getLazyType(MultivaluedMap queryParams) {
        LazyTypeObjectMapperEnum lazyType = LazyTypeObjectMapperEnum.ONLYID;
        if (queryParams != null && queryParams.containsKey("$lazyloadingtype")) {
            Object lazyTypeObj = queryParams.get("$lazyloadingtype");
            if (lazyTypeObj instanceof List && ((List) lazyTypeObj).get(0).toString().equalsIgnoreCase("eager")) {
                lazyType = LazyTypeObjectMapperEnum.EAGER;
            }
            if (lazyTypeObj instanceof String && lazyType.toString().equalsIgnoreCase("eagle")) {
                lazyType = LazyTypeObjectMapperEnum.EAGER;
            }
        }
        return lazyType;
    }

    private Object castValue(String field, String value) {
        if (field != null && value != null) {
            Class fieldType = getFieldType(field);
            if (fieldType == null) {
                return null;
            }
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
        Class entityClass = getDAO().getEntityClass();
        try {
            Field field = entityClass.getDeclaredField(fieldName);
            if (field.getName().equals(fieldName)) {
                return field.getType();
            }

        } catch (NoSuchFieldException | SecurityException ex) {
            try {

                Class fieldType = null;

                //CASO A entityClass SEJA UMA CLASSE COM HERANÇA, FAZER O TESTE
                Class superClass = entityClass.getSuperclass();
                if (!(superClass instanceof Object)) {
                    fieldType = getFieldType(superClass, fieldName);
                }

                if (fieldType == null) {
                    //Caso o field nao seja localizado então fazer o teste se é um objeto
                    String object[] = fieldName.split("[.]");
                    if (object.length > 1) {
                        Class newEntityClass = entityClass.getDeclaredField(object[0]).getType();
                        String concat = "";

                        for (int x = 1; x < object.length; x++) {
                            if (x == 1) {
                                concat = object[x];
                            } else {
                                concat += ("." + object[x]);
                            }
                        }
                        fieldType = getFieldType(newEntityClass, concat);
                    }
                }
                return fieldType;

            } catch (NoSuchFieldException | SecurityException ex1) {
                Logger.getLogger(AbstractREST.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        return null;
    }

    private Class getFieldType(Class entityClass, String fieldName) {
        try {
            Field field = entityClass.getDeclaredField(fieldName);
            if (field.getName().equals(fieldName)) {
                return field.getType();
            }
        } catch (NoSuchFieldException | SecurityException ex) {
            try {
                Class fieldType = null;

                //CASO A entityClass SEJA UMA CLASSE COM HERANÇA, FAZER O TESTE
                Class superClass = entityClass.getSuperclass();
                if (!(superClass instanceof Object)) {
                    fieldType = getFieldType(superClass, fieldName);
                }

                if (fieldType == null) {
                    //Caso o field nao seja localizado então fazer o teste se é um objeto
                    String object[] = fieldName.split("[.]");
                    if (object.length > 1) {
                        Class newEntityClass = entityClass.getDeclaredField(object[0]).getType();
                        String concat = "";

                        for (int x = 1; x < object.length; x++) {
                            if (x == 1) {
                                concat = object[x];
                            } else {
                                concat += ("." + object[x]);
                            }
                        }
                        fieldType = getFieldType(newEntityClass, concat);
                    }
                }
                return fieldType;

            } catch (NoSuchFieldException | SecurityException ex1) {
                Logger.getLogger(AbstractREST.class.getName()).log(Level.SEVERE, null, ex1);
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

    private String getTranslateLike(String field) {
        String query = "SELECT translate(" + field + ",'ÀÁÂÃÄÅĀĂĄÈÉÊËĒĔĖĘĚÌÍÎÏĨĪĮİÒÓÔÕÖØŌŎŐÙÚÛÜŨŪŬŮŰŲàáâãäåāăąèéêëēĕėęěìíîïĩīĭįòóôõöøōŏőùúûüũūŭůųÇçÑñÝýÿĆćĈĉĊċČčĎďĐđĜĝĞğĠġĢģĤĥĦħ',";
        query += "'AAAAAAAAAEEEEEEEEEIIIIIIIIOOOOOOOOOUUUUUUUUUUaaaaaaaaaeeeeeeeeeiiiiiiiiooooooooouuuuuuuuuCcNnYyyCcCcCcCcDdDdGgGgGgGgHhHh')";
        return query;
    }

    protected String removeAcentuacao(String string) {
        if (string != null && !string.trim().isEmpty()) {
            string = string.replaceAll("[ÁÀÂÃÄ]", "A");
            string = string.replaceAll("[ÉÈÊË]", "E");
            string = string.replaceAll("[ÍÌÎÏ]", "I");
            string = string.replaceAll("[ÓÒÔÕÖ]", "O");
            string = string.replaceAll("[ÚÙÛÜ]", "U");
            string = string.replaceAll("[Ç]", "C");

            string = string.replaceAll("[áàâãä]", "a");
            string = string.replaceAll("[éèêë]", "e");
            string = string.replaceAll("[íìîï]", "i");
            string = string.replaceAll("[óòôõö]", "o");
            string = string.replaceAll("[úùûü]", "u");
            string = string.replaceAll("[ç]", "c");
        }
        return string;
    }

    public T toObject(String json) throws JsonProcessingException, IOException {
        return getEntityFromJson(json);
    }

    public String toJson(T entity, LazyTypeObjectMapperEnum enumLazy) throws JsonProcessingException {
        String json = getJsonFromEntity(entity);
        return json;
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

    public List<T> loadLazyObjectFromList(List<T> records, LazyTypeObjectMapperEnum lazyType) throws IOException {
        List<T> newList = null;
        if (records != null && !records.isEmpty()) {
            if (lazyType == null) {
                lazyType = LazyTypeObjectMapperEnum.ONLYID;
            }

            newList = new LinkedList();
            for (T field : records) {
                newList.add(loadLazyObjectMapping(field, lazyType));
            }
        }
        return newList;
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

}
