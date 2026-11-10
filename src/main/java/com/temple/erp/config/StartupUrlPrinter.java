package com.temple.erp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupUrlPrinter {
    private static final Logger log = LoggerFactory.getLogger(StartupUrlPrinter.class);
    private final Environment env;

    public StartupUrlPrinter(Environment env) { this.env = env; }

    @EventListener(ApplicationReadyEvent.class)
    public void printUrls() {
        String port = env.getProperty("local.server.port", env.getProperty("server.port", "5000"));
        String base = "http://localhost:" + port;
        log.info("\n----------------------------------------------------------\n"
                + "\tTemple ERP is running! Access URLs:\n"
                + "\tHome:            " + base + "/\n"
                + "\tAdmin Dashboard: " + base + "/admin/dashboard  (Ctrl+Click to open)\n"
                + "\tAdmin Login:     " + base + "/admin/login\n"
                + "\tUser Login:      " + base + "/login\n"
                + "\tRegister:        " + base + "/register\n"
                + "\tNews:            " + base + "/news\n"
                + "\tEvents:          " + base + "/events\n"
                + "\tFestivals:       " + base + "/festivals\n"
                + "\tAPI Health:      " + base + "/api/health\n"
                + "----------------------------------------------------------");
    }
}
