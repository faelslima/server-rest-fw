/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.simplesi.restserver.rest.filter;

import com.simplesi.restserver.licenca.bo.LicencaBO;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;

/**
 * @author Rafael Lima
 */
public abstract class LicencaFilter implements ContainerRequestFilter {

    @Context
    private HttpServletRequest servletRequest;
    private static final Response.Status UNAUTHORIZED = Response.Status.UNAUTHORIZED;
    private String token;

    @Override
    public void filter(ContainerRequestContext containerRequest) throws IOException {
        String pathInfo = servletRequest.getPathInfo().trim();
        if (!pathInfo.contains("security") && containerRequest.getMethod().equals("POST")) {
            token = containerRequest.getHeaderString("Authorization");
            if (!userLoggedIsSuporte()) {
                LicencaBO licencaBO = new LicencaBO(getBaseUrlLicenca());
                if (!licencaBO.existeLicencaAtiva(getSlug(), getNomeProduto())) {
                    String competenciaAtual = new SimpleDateFormat("yyyyMM").format(new Date());
                    containerRequest.abortWith(Response
                            .status(UNAUTHORIZED)
                            .type(MediaType.TEXT_PLAIN)
                            .entity("Não existe licença para o exercício '" + getCompetencia(competenciaAtual) + "'")
                            .build());
                }
            }
        }
    }

    public String getToken() {
        return this.token;
    }

    private String getSlug() {
        String pathInfo = servletRequest.getPathInfo().trim();
        if (pathInfo.startsWith("/")) {
            pathInfo = pathInfo.replaceFirst("/", "");
        }
        return pathInfo.split("/")[0];
    }

    private String getCompetencia(String competencia) {
        if (competencia != null && competencia.length() == 6) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
            try {
                Date dataCompetencia = sdf.parse(competencia);
                sdf = new SimpleDateFormat("MM/yyyy");
                competencia = sdf.format(dataCompetencia);

            } catch (ParseException ex) {
                Logger.getLogger(StringUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return competencia;
    }

    public abstract String getBaseUrlLicenca();

    public abstract String getNomeProduto();

    public abstract boolean userLoggedIsSuporte();

}
