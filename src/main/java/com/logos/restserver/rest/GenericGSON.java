package com.logos.restserver.rest;

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
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

/**
 * @author rafael
 */
public class GenericGSON<T> implements Serializable {
    //<editor-fold defaultstate="collapsed" desc="Gson">

    private Gson getGson() {
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
    private class CalendarAdapter implements JsonSerializer<java.util.Calendar>, JsonDeserializer<java.util.Calendar> {

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
                Logger.getLogger(GenericGSON.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

    }

    /**
     * Serializer de Date.
     */
    private class GregorianCalendarAdapter implements JsonSerializer<java.util.GregorianCalendar>, JsonDeserializer<java.util.Calendar> {

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
                Logger.getLogger(GenericGSON.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

    }

    /**
     * Serializer de Date.
     */
    public class DateAdapter implements JsonSerializer<java.util.Date>, JsonDeserializer<java.util.Date> {

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
            if (!strDt.contains("-")) {
                if (StringUtils.isNumeric(strDt)) {
                    if (strDt.length() == 4) {
                        sdf = new SimpleDateFormat("HHmm");
                    }
                    if (strDt.length() == 7) {
                        strDt = "0" + strDt;
                    }
                    if (strDt.length() == 8) {
                        if (strDt.charAt(4) == '2') {
                            sdf = new SimpleDateFormat("ddMMyyyy");
                        } else {
                            sdf = new SimpleDateFormat("yyyyMMdd");
                        }
                    }
                    if (strDt.length() > 4 && strDt.length() < 8) {
                        return null;
                    }

                } else {
                    if (strDt.contains("/") && strDt.length() == 10) {
                        String[] quebra = strDt.split("/");
                        if (quebra.length == 3) {
                            String mascara = quebra[0].length() == 2 ? "dd/MM/yyyy" : "yyyy/MM/dd";
                            sdf = new SimpleDateFormat(mascara);
                        }
                    }
                    if (strDt.contains(":")) {
                        if (strDt.length() == 5) {
                            sdf = new SimpleDateFormat("HH:mm");
                        }

                        if (strDt.length() == 8) {
                            sdf = new SimpleDateFormat("HH:mm:ss");
                        }
                    }
                }
            }

            if (strDt.endsWith("AM") || strDt.endsWith("PM")) {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss a");
                if (strDt.length() == 11) {
                    sdf = new SimpleDateFormat("hh:mm:ss a");
                }
            }
            try {
                return sdf.parse(strDt);

            } catch (ParseException ex) {
                Logger.getLogger(GenericGSON.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

    }

    /**
     * Serializer de SqlDate.
     */
    private class SqlDateAdapter implements JsonSerializer<java.sql.Date>, JsonDeserializer<java.sql.Date> {

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
                Logger.getLogger(GenericGSON.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }

    /**
     * Serializer de SqlTime.
     */
    private class SqlTimeAdapter implements JsonSerializer<java.sql.Time>, JsonDeserializer<java.sql.Time> {

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
                Logger.getLogger(GenericGSON.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }

    /**
     * Serializer de SqlTime.
     */
    private class SqlTimestampAdapter implements JsonSerializer<java.sql.Timestamp>, JsonDeserializer<java.sql.Timestamp> {

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
                Logger.getLogger(GenericGSON.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }
//</editor-fold>
}
