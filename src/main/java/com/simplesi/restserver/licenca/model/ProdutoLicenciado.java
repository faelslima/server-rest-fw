package com.simplesi.restserver.licenca.model;

import java.io.Serializable;
import java.util.List;

/**
 * @author Rafael Lima
 */
public class ProdutoLicenciado implements Serializable {

    private Produto produto;
    private List<String> exercicios;

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public List<String> getExercicios() {
        return exercicios;
    }

    public void setExercicios(List<String> exercicios) {
        this.exercicios = exercicios;
    }
}
