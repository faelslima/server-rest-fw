package com.simplesi.restserver.licenca.model;

import java.io.Serializable;

/**
 * @author Rafael Lima
 */
public class EmpresaLicenciada implements Serializable {

    private String id;
    private String slug;
    private String nome;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

}
