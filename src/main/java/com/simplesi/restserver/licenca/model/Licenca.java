/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.simplesi.restserver.licenca.model;

import java.io.Serializable;
import java.util.List;

/**
 * @author Rafael
 */
public class Licenca implements Serializable {

    private EmpresaLicenciada empresa;
    private List<ProdutoLicenciado> licencas; 

    public EmpresaLicenciada getEmpresa() {
        return empresa;
    }

    public void setEmpresa(EmpresaLicenciada empresa) {
        this.empresa = empresa;
    }

    public List<ProdutoLicenciado> getLicencas() {
        return licencas;
    }

    public void setLicencas(List<ProdutoLicenciado> licencas) {
        this.licencas = licencas;
    }
}
