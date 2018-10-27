package com.simplesi.restserver.licenca.dao;

import com.simplesi.restserver.licenca.model.Licenca;
import java.io.Serializable;

/**
 * @author Rafael Lima
 */
public interface LicencaDAO extends Serializable {

    public Licenca getLicencas(String slug);

    public Licenca getLicencas(String slug, String produto);
}
