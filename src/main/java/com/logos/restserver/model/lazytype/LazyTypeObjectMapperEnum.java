/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.logos.restserver.model.lazytype;

/**
 * @author Rafael
 */
public enum LazyTypeObjectMapperEnum {
    EAGER("eager"), ONLYID("onlyid");
    private final String descricao;

    private LazyTypeObjectMapperEnum(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
