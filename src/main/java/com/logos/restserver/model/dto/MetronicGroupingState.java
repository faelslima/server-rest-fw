/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.logos.restserver.model.dto;

import java.io.Serializable;
import java.util.List;

/**
 * @author rafael
 */
public class MetronicGroupingState implements Serializable {

    private List<Object> selectedRowIds;
    private List<Object> itemIds;

    public MetronicGroupingState() {
    }

    public List<Object> getSelectedRowIds() {
        return selectedRowIds;
    }

    public void setSelectedRowIds(List<Object> selectedRowIds) {
        this.selectedRowIds = selectedRowIds;
    }

    public List<Object> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<Object> itemIds) {
        this.itemIds = itemIds;
    }

}
