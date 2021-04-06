/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.logos.restserver.model.dto;

/**
 *
 * @author rafael
 */
public class MetronicFilter {

    private String column;
    private String value;
    private String restrictionType;

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getRestrictionType() {
        return restrictionType;
    }

    public void setRestrictionType(String restriction) {
        this.restrictionType = restriction;
    }

}
