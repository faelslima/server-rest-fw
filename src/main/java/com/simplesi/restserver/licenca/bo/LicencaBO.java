package com.simplesi.restserver.licenca.bo;

import com.simplesi.restserver.licenca.dao.LicencaDAO;
import com.simplesi.restserver.licenca.dao.impl.LicencaDAOImpl;
import com.simplesi.restserver.licenca.model.Licenca;
import com.simplesi.restserver.licenca.model.ProdutoLicenciado;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Rafael Lima
 */
public class LicencaBO implements Serializable {

    private final LicencaDAO licencaDAO;

    public LicencaBO(String baseUrl) {
        licencaDAO = new LicencaDAOImpl(baseUrl);
    }
    
    public Licenca getLicencas(String slug) {
        return licencaDAO.getLicencas(slug);
    }
    
    public Licenca getLicencas(String slug, String produtoOuSigla) {
        return licencaDAO.getLicencas(slug, produtoOuSigla);
    }

    public boolean existeLicencaAtiva(String slug, String nomeProduto) {
        Licenca licenca = getLicencas(slug, nomeProduto);
        if (licenca != null && !licenca.getLicencas().isEmpty()) {
            String competenciaAtual = new SimpleDateFormat("yyyyMM").format(new Date());
            ProdutoLicenciado produtoLicenciado = licenca.getLicencas().get(0);
            return produtoLicenciado.getExercicios().contains(competenciaAtual);
        }
        return false;
    }
}
