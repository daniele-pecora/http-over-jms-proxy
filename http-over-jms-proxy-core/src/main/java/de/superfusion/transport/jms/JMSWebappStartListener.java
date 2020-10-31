package de.superfusion.transport.jms;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class JMSWebappStartListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        printInfo(sce, false);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        printInfo(sce, true);
    }

    private void printInfo(ServletContextEvent sce, boolean destroyed) {
        ServletContext sc = sce.getServletContext();
        System.out.println("**** **** **** **** **** **** **** **** ****");
        System.out.println("Context destroyed " + sc.getContextPath() + " :: " + sc.getServletContextName());
        System.out.println("Server " + sc.getServerInfo() + " ServerName:" + sc.getVirtualServerName());
        System.out.println("**** **** **** **** **** **** **** **** ****");
    }
}
