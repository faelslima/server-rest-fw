package com.logos.restserver.model.dto;

import java.io.Serializable;
import org.json.JSONObject;

/**
 *
 * @author rafael
 */
public class MetronicSorteState implements Serializable {

    private String column;
    private String direction;

    public MetronicSorteState() {
        this.column = "id";
        this.direction = "asc";
    }

    public MetronicSorteState(JSONObject json) {
        if (json != null && !json.isEmpty()) {
            if (json.has("column")) {
                this.column = json.getString("column");
            }
            if (json.has("direction")) {
                this.direction = json.getString("direction");
            }
        }
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

}
