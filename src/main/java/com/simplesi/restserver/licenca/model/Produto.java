/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.simplesi.restserver.licenca.model;

import java.io.Serializable;
import java.util.Locale;

/**
 * @author Rafael Lima
 */
public class Produto implements Serializable {

    private String descricao;
    private String sigla;

    //<editor-fold defaultstate="collapsed" desc="Getter and Setter">
    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao.toUpperCase(new Locale("pt", "BR"));
    }

    public String getSigla() {
        return sigla;
    }

    public void setSigla(String sigla) {
        this.sigla = sigla.toUpperCase();
    }

    public String getSiglaDescricao() {
        return sigla + " - " + descricao;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="toString()">
    @Override
    public String toString() {
        return descricao;
    }
    //</editor-fold>
}
