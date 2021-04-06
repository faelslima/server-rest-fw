package com.logos.restserver.model.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.logos.restserver.model.lazytype.HibernateProxyTypeAdapter;
import com.logos.restserver.rest.AbstractREST;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

/**
 * @author rafael
 */
public class MetronicTableModel<T> implements Serializable {

    private List<T> items;
    private Long total;

    public MetronicTableModel() {
        this.items = new ArrayList();
        this.total = 0L;
    }

    public MetronicTableModel(List<T> items, Long total) {
        this.items = items;
        this.total = total;
        if (total == null && items != null && !items.isEmpty()) {
            this.total = Long.valueOf(String.valueOf(items.size()));
        }
    }
    
    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
    
     //<editor-fold defaultstate="collapsed" desc="Gson">
    public static Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithModifiers(Modifier.STATIC, Modifier.ABSTRACT);
        builder.registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY);
        builder.registerTypeHierarchyAdapter(byte[].class, new ByteArrayToBase64TypeAdapter());
        builder.registerTypeAdapter(java.util.Calendar.class, new CalendarAdapter());
        builder.registerTypeAdapter(java.util.GregorianCalendar.class, new GregorianCalendarAdapter());
        builder.registerTypeAdapter(java.util.Date.class, new DateAdapter());
        builder.registerTypeAdapter(java.sql.Date.class, new SqlDateAdapter());
        builder.registerTypeAdapter(java.sql.Time.class, new SqlTimeAdapter());
        builder.registerTypeAdapter(java.sql.Timestamp.class, new SqlTimestampAdapter());
        return builder.create();
    }

    public static String getJsonFromEntity(MetronicTableModel entity) {
        return getGson().toJson(entity, getEntityType());
    }

    public static MetronicTableModel getEntityFromJson(String json) {
        return (MetronicTableModel) getGson().fromJson(json, getEntityType());
    }

    public static List<MetronicTableModel> getListFromJson(String json) {
        return getGson().fromJson(json, getEntityTypeList());
    }

    public static String getStringFromList(List<MetronicTableModel> list) {
        return getGson().toJson(list, getEntityTypeList());
    }

    public static Type getEntityType() {
        return MetronicTableModel.class;
    }

    public static Type getEntityTypeList() {
        Type type = com.google.gson.reflect.TypeToken.get(List.class).getType();
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
    private static class CalendarAdapter implements JsonSerializer<java.util.Calendar>, JsonDeserializer<java.util.Calendar> {

        @Override
        public JsonElement serialize(java.util.Calendar t, Type type, JsonSerializationContext jsc) {
            return new JsonPrimitive(dateToJson(t));
        }

        @Override
        public java.util.Calendar deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            if (json.isJsonObject()) {
                JSONObject jobj = new JSONObject(json.toString());
                Integer dia = jobj.optInt("dayOfMonth", 0);
                Integer mes = jobj.optInt("month", 0);
                Integer ano = jobj.optInt("year", 0);
                Integer hora = jobj.optInt("hourOfDay", 0);
                Integer min = jobj.optInt("minute", 0);
                Integer seg = jobj.optInt("second", 0);

                java.util.Calendar calendar = java.util.GregorianCalendar.getInstance();
                calendar.set(ano, mes, dia, hora, min, seg);
                return calendar;
            }

            return jsonToDate(json.getAsJsonPrimitive().getAsString());
        }

        /**
         * Converte uma Date em um valor ISO 8601.
         *
         * @param calendar Date a ser convertido em String.
         * @return String com a data. Ex: "2013-01-17T00:00:00.124-02:00"
         */
        private String dateToJson(java.util.Calendar calendar) {
            if (calendar == null) {
                return null;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            return sdf.format(calendar.getTime());
        }

        /**
         * Coverte uma data ISO 8601 em DateTime.
         *
         * @param strDt
         * @return Date Ex: "2013-01-17T00:00:00.124-02:00"
         */
        private java.util.Calendar jsonToDate(String strDt) {
            if (strDt == null) {
                return null;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            if (strDt.endsWith("AM") || strDt.endsWith("PM")) {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss a");
                if (strDt.length() == 11) {
                    sdf = new SimpleDateFormat("hh:mm:ss a");
                }
            }
            try {
                java.util.Calendar calendar = java.util.GregorianCalendar.getInstance();
                calendar.setTime(sdf.parse(strDt));
                return calendar;

            } catch (ParseException ex) {
                Logger.getLogger(AbstractREST.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

    }

    /**
     * Serializer de Date.
     */
    private static class GregorianCalendarAdapter implements JsonSerializer<java.util.GregorianCalendar>, JsonDeserializer<java.util.Calendar> {

        @Override
        public JsonElement serialize(java.util.GregorianCalendar t, Type type, JsonSerializationContext jsc) {
            return new JsonPrimitive(dateToJson(t));
        }

        @Override
        public java.util.Calendar deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            if (json.isJsonObject()) {
                JSONObject jobj = new JSONObject(json.toString());
                Integer dia = jobj.optInt("dayOfMonth", 0);
                Integer mes = jobj.optInt("month", 0);
                Integer ano = jobj.optInt("year", 0);
                Integer hora = jobj.optInt("hourOfDay", 0);
                Integer min = jobj.optInt("minute", 0);
                Integer seg = jobj.optInt("second", 0);

                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.set(ano, mes, dia, hora, min, seg);
                return (java.util.GregorianCalendar) calendar;
            }

            return jsonToDate(json.getAsJsonPrimitive().getAsString());
        }

        /**
         * Converte uma Date em um valor ISO 8601.
         *
         * @param calendar Date a ser convertido em String.
         * @return String com a data. Ex: "2013-01-17T00:00:00.124-02:00"
         */
        private String dateToJson(java.util.GregorianCalendar calendar) {
            if (calendar == null) {
                return null;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            return sdf.format(calendar.getTime());
        }

        /**
         * Coverte uma data ISO 8601 em DateTime.
         *
         * @param strDt
         * @return Date Ex: "2013-01-17T00:00:00.124-02:00"
         */
        private java.util.Calendar jsonToDate(String strDt) {
            if (strDt == null) {
                return null;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            if (strDt.endsWith("AM") || strDt.endsWith("PM")) {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss a");
                if (strDt.length() == 11) {
                    sdf = new SimpleDateFormat("hh:mm:ss a");
                }
            }
            try {
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.setTime(sdf.parse(strDt));
                return calendar;

            } catch (ParseException ex) {
                Logger.getLogger(AbstractREST.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

    }

    /**
     * Serializer de Date.
     */
    private static class DateAdapter implements JsonSerializer<java.util.Date>, JsonDeserializer<java.util.Date> {

        @Override
        public JsonElement serialize(java.util.Date t, Type type, JsonSerializationContext jsc) {
            return new JsonPrimitive(dateToJson(t));
        }

        @Override
        public java.util.Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return jsonToDate(json.getAsJsonPrimitive().getAsString());
        }

        /**
         * Converte uma Date em um valor ISO 8601.
         *
         * @param data Date a ser convertido em String.
         * @return String com a data. Ex: "2013-01-17T00:00:00.124-02:00"
         */
        private String dateToJson(java.util.Date data) {
            if (data == null) {
                return null;
            }
//            // O toString do Joda DateTime por padrão retorna um String de data em ISO 8601.
//            DateTime dt = new DateTime(data, DateTimeZone.forID("-03:00"));
//            return dt.toString().replace("-03:00", "Z");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            if (data instanceof java.sql.Timestamp) {
                java.util.Date uDate = new Date(((java.sql.Timestamp) data).getTime());
                return sdf.format(uDate);
            }
            return sdf.format(data);
        }

        /**
         * Coverte uma data ISO 8601 em DateTime.
         *
         * @param strDt
         * @return Date Ex: "2013-01-17T00:00:00.124-02:00"
         */
        private java.util.Date jsonToDate(String strDt) {
            if (strDt == null) {
                return null;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            if (strDt.endsWith("AM") || strDt.endsWith("PM")) {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss a");
                if (strDt.length() == 11) {
                    sdf = new SimpleDateFormat("hh:mm:ss a");
                }
            }
            try {
                Date date = sdf.parse(strDt);
                return date;

            } catch (ParseException ex) {
                Logger.getLogger(AbstractREST.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

    }

    /**
     * Serializer de SqlDate.
     */
    private static class SqlDateAdapter implements JsonSerializer<java.sql.Date>, JsonDeserializer<java.sql.Date> {

        @Override
        public JsonElement serialize(java.sql.Date t, Type type, JsonSerializationContext jsc) {
            return new JsonPrimitive(dateToJson(t));
        }

        @Override
        public java.sql.Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return jsonToDate(json.getAsJsonPrimitive().getAsString());
        }

        /**
         * Converte uma Date em um valor ISO 8601.
         *
         * @param data Date a ser convertido em String.
         * @return String com a data. Ex: "2013-01-17T00:00:00.124-02:00"
         */
        private String dateToJson(java.sql.Date data) {
            if (data == null) {
                return null;
            }
            // O toString do Joda DateTime por padrão retorna um String de data em ISO 8601.
//            DateTime dt = new DateTime(new java.util.Date(data.getTime()), DateTimeZone.forID("-03:00"));
//            return dt.toString().replace("-03:00", "Z");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            return sdf.format(data);
        }

        /**
         * Coverte uma data ISO 8601 em DateTime.
         *
         * @param strDt
         * @return Date Ex: "2013-01-17T00:00:00.124-02:00"
         */
        private java.sql.Date jsonToDate(String strDt) {
            if (strDt == null) {
                return null;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            if (strDt.endsWith("AM") || strDt.endsWith("PM")) {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss a");
                if (strDt.length() == 11) {
                    sdf = new SimpleDateFormat("hh:mm:ss a");
                }
            }
            try {
                Date date = sdf.parse(strDt);
                return new java.sql.Date(date.getTime());

            } catch (ParseException ex) {
                Logger.getLogger(AbstractREST.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }

    /**
     * Serializer de SqlTime.
     */
    private static class SqlTimeAdapter implements JsonSerializer<java.sql.Time>, JsonDeserializer<java.sql.Time> {

        @Override
        public JsonElement serialize(java.sql.Time t, Type type, JsonSerializationContext jsc) {
            return new JsonPrimitive(dateToJson(t));
        }

        @Override
        public java.sql.Time deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return jsonToDate(json.getAsJsonPrimitive().getAsString());
        }

        /**
         * Converte uma Date em um valor ISO 8601.
         *
         * @param data Date a ser convertido em String.
         * @return String com a data. Ex: "2013-01-17T00:00:00.124-02:00"
         */
        private String dateToJson(java.sql.Time data) {
            if (data == null) {
                return null;
            }
            // O toString do Joda DateTime por padrão retorna um String de data em ISO 8601.
//            DateTime dt = new DateTime(new java.util.Date(data.getTime()), DateTimeZone.forID("-03:00"));
//            return dt.toString().replace("-03:00", "Z");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            return sdf.format(new java.util.Date(data.getTime()));
        }

        /**
         * Coverte uma data ISO 8601 em DateTime.
         *
         * @param strDt
         * @return Date Ex: "2013-01-17T00:00:00.124-02:00"
         */
        private java.sql.Time jsonToDate(String strDt) {
            if (strDt == null) {
                return null;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            if (strDt.endsWith("AM") || strDt.endsWith("PM")) {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss a");
                if (strDt.length() == 11) {
                    sdf = new SimpleDateFormat("hh:mm:ss a");
                }
            }
            try {
                Date date = sdf.parse(strDt);
                return new java.sql.Time(date.getTime());

            } catch (ParseException ex) {
                Logger.getLogger(AbstractREST.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }

    /**
     * Serializer de SqlTime.
     */
    private static class SqlTimestampAdapter implements JsonSerializer<java.sql.Timestamp>, JsonDeserializer<java.sql.Timestamp> {

        @Override
        public JsonElement serialize(java.sql.Timestamp t, Type type, JsonSerializationContext jsc) {
            return new JsonPrimitive(dateToJson(t));
        }

        @Override
        public java.sql.Timestamp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return jsonToDate(json.getAsJsonPrimitive().getAsString());
        }

        /**
         * Converte uma Date em um valor ISO 8601.
         *
         * @param data Date a ser convertido em String.
         * @return String com a data. Ex: "2013-01-17T00:00:00.124-02:00"
         */
        private String dateToJson(java.sql.Timestamp data) {
            if (data == null) {
                return null;
            }
            // O toString do Joda DateTime por padrão retorna um String de data em ISO 8601.
//            DateTime dt = new DateTime(new java.util.Date(data.getTime()), DateTimeZone.forID("-03:00"));
//            return dt.toString().replace("-03:00", "Z");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            java.util.Date uDate = new java.util.Date(data.getTime());
            return sdf.format(uDate);
        }

        /**
         * Coverte uma data ISO 8601 em DateTime.
         *
         * @param strDt
         * @return Date Ex: "2013-01-17T00:00:00.124-02:00"
         */
        private java.sql.Timestamp jsonToDate(String strDt) {
            if (strDt == null) {
                return null;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            if (strDt.endsWith("AM") || strDt.endsWith("PM")) {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss a");
                if (strDt.length() == 11) {
                    sdf = new SimpleDateFormat("hh:mm:ss a");
                }
            }
            try {
                Date date = sdf.parse(strDt);
                return new java.sql.Timestamp(date.getTime());

            } catch (ParseException ex) {
                Logger.getLogger(AbstractREST.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }
//</editor-fold>

}
