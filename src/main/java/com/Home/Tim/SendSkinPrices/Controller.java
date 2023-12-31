package com.Home.Tim.SendSkinPrices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;


@RestController
public class Controller {

    private static final Logger logger = LogManager.getLogger("Logger");

    /**
     * Set the index of the main loop from outside
     * @param index
     * @return
     */
    @PostMapping("/setIndex")
    public int updateSkin(@RequestParam("index") int index) {

        logger.debug("/setIndex" + " " + index);
        logger.debug("setting index i to: "+index);
        SendSkinPricesApplication.i = index;


        return SendSkinPricesApplication.i;

    }

    @GetMapping("/")
    public String StartSeite() throws IOException {

        return SendSkinPricesApplication.getHostname();

    }





}
