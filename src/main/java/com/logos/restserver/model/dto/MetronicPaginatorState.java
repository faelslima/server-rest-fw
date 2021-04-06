package com.logos.restserver.model.dto;

import java.io.Serializable;
import java.util.List;
import org.json.JSONObject;

/**
 * @author rafael
 */
public class MetronicPaginatorState  implements Serializable{
    private Integer page;
    private Integer pageSize;
    private Integer total;
    private List<Integer> pageSizes;

    public MetronicPaginatorState() {
    }
    
    public MetronicPaginatorState(JSONObject json) {
        if(json != null && !json.isEmpty()) {
            if(json.has("page")) {
                this.page = json.getInt("page");
            }
            if(json.has("pageSize")) {
                this.page = json.getInt("pageSize");
            }
            if(json.has("total")) {
                this.page = json.getInt("total");
            }
        }
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<Integer> getPageSizes() {
        return pageSizes;
    }

    public void setPageSizes(List<Integer> pageSizes) {
        this.pageSizes = pageSizes;
    }
    
    
}
