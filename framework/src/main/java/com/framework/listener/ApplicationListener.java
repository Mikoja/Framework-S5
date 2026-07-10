package com.framework.listener;

import java.util.HashMap;

import com.framework.util.Mapping;
import com.framework.util.UtilMethode;
import com.framework.util.Utilitaire;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class ApplicationListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        try {

            ServletContext context = sce.getServletContext();

            String pack = context.getInitParameter("controller");

            HashMap<UtilMethode, Mapping> urlMapping = new HashMap<>();

            Utilitaire.getUrlAndMethod(pack, urlMapping);

            context.setAttribute("viewPrefix",
                    context.getInitParameter("viewPrefix"));

            context.setAttribute("viewSuffix",
                    context.getInitParameter("viewSuffix"));

            context.setAttribute("urlMapping", urlMapping);

            System.out.println("Framework initialise");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

}
