package com.simplesi.restserver.licenca.dao.impl;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.simplesi.restserver.licenca.dao.LicencaDAO;
import com.simplesi.restserver.licenca.model.Licenca;
import java.lang.reflect.Type;
import java.util.List;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Rafael Lima
 */
public class LicencaDAOImpl implements LicencaDAO {

    private WebTarget webTarget;
    private static final MediaType JSON_TYPE = MediaType.APPLICATION_JSON_TYPE;

    public LicencaDAOImpl(String baseUrl) {
        if (baseUrl != null) {
            this.webTarget = ClientBuilder.newClient()
                    .target(baseUrl)
                    .register(JacksonJsonProvider.class);
        }
    }

    @Override
    public Licenca getLicencas(String slug) {
        if (getWebTarget() != null) {
            Invocation.Builder builder = getWebTarget()
                    .path(slug)
                    .request(JSON_TYPE)
                    .accept("application/json", "text/plain");

            Response response = builder.get();
            if (response.getStatus() == 200 && response.hasEntity()) {
                return (Licenca) response.readEntity(getGenericTypeList());
            }
        }
        return null;
    }

    @Override
    public Licenca getLicencas(String slug, String produto) {
        if (getWebTarget() != null) {
            Invocation.Builder builder = getWebTarget()
                    .path(slug).path(produto)
                    .request(JSON_TYPE)
                    .accept("application/json", "text/plain");

            Response response = builder.get();
            if (response.getStatus() == 200 && response.hasEntity()) {
                return (Licenca) response.readEntity(getGenericTypeList());
            }
        }
        return null;
    }

    public WebTarget getWebTarget() {
        return this.webTarget;
    }

    public Type getEntityType() {
        return new TypeToken<Licenca>() {
        }.getType();
    }

    public Type getEntityTypeList() {
        Type type = com.google.gson.reflect.TypeToken.getParameterized(List.class, getEntityType()).getType();
        return type;
    }

    public GenericType getGenericType() {
        return new GenericType(getEntityType());
    }

    public GenericType getGenericTypeList() {
        return new GenericType(getEntityTypeList());
    }

    //<editor-fold defaultstate="collapsed" desc="Gson">
    private Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        return builder.create();
    }

    public String getJsonFromEntity(Licenca entity) {
        return getGson().toJson(entity, getEntityType());
    }

    public Licenca getEntityFromJson(String json) {
        return (Licenca) getGson().fromJson(json, getEntityType());
    }

    public List<Licenca> getListFromJson(String json) {
        return getGson().fromJson(json, getEntityTypeList());
    }

    public String getJsonFromList(List<Licenca> list) {
        return getGson().toJson(list, getEntityTypeList());
    }
    //</editor-fold>

}
