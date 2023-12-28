package com.Home.Tim.SendSkinPrices;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@PropertySource("classpath:application.properties")
@SpringBootApplication
@Component

public class SendSkinPricesApplication {


    private static final Logger logger = LogManager.getLogger("Logger");

    //Default configuration
    public static int threads = 1;
    public static int mod = 0;
    public static boolean useVPN = false;
    public static String IP;

    public static int restartTimer = 300;

    static String logpath;
    static int workerid;

    //Used for the main loop
    static int i=0;


    public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException {

        // MainMapLookupapLookup.setMainArguments(args);


        if (args.length > 0) {


            useVPN = Boolean.parseBoolean(args[0]);
            threads = Integer.parseInt(args[1]);
            mod = Integer.parseInt(args[2]);

        } else {

            logger.info("No args found using default ones");
            logger.debug("useVPN: " + useVPN);
            logger.debug("threads: " + threads);
            logger.debug("modulo: " + mod);


        }
       //  String logpath = "E:\\Mehr Programmierstuff\\IntelliJProjekte\\SendSkinPrices\\src\\main\\resources\\SendSkinPrices-" + mod + ".log";
        String logpath = "/var/log/SendSkinPrices-" + mod + ".log";
        logger.info("LogfilePath: " + logpath);


        File logfile = new File(logpath);
        if (logfile.length() == 0) {
            logfile.createNewFile();

        }


        org.apache.logging.log4j.ThreadContext.put("logFileName", logpath);


        ConfigurableApplicationContext ctx = SpringApplication.run(SendSkinPricesApplication.class, args);
        Environment env= ctx.getEnvironment();


        final String HOSTNAME = getHostname();
        logger.debug("Hostname: "+HOSTNAME);

        logger.debug("LogPath: " + logpath);
        logger.debug("Mod: " + workerid);
        logger.debug("Threads: " + threads);




        //Print args
        for (int i = 0; i < args.length; i++) {
            logger.debug("Arg[" + i + "]: " + args[i]);
        }

        RestTemplate restTemplate = new RestTemplate();
        String IPwoVPN = restTemplate.getForObject("https://ipinfo.io/ip", String.class);
        logger.info("The global IPv4 Without VPN Address is: " + restTemplate.getForObject("https://ipinfo.io/ip", String.class));

        if (useVPN) {
            logger.debug("Using VPN!");
            Thread t1 = new Thread(new Runnable() {
                public void run() {
                    runScript("sh /root/startup.sh");

                }
            });
            t1.start();

        } else {
            logger.debug("Using Default IP!");
        }
        //Delay for the VPN to connect
        Thread.sleep(10000);

        IP = restTemplate.getForObject("https://ipinfo.io/ip", String.class);
        logger.info("The global IPv4 Address is: " + restTemplate.getForObject("https://ipinfo.io/ip", String.class));


        String Port = env.getProperty("local.server.port");
        logger.info("TomcatPort: "+Port );

        /*
         Start async Thread that counts down from 300 to 0
         TImer is refreshed when a run was successful
         if the program is not running for 5 minutes and is not waiting for a 429
         Then a command is sent to restart the program entirely
         */
        Thread t2 = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);

                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }


                    if (restartTimer-- <= 0 || (useVPN && (IPwoVPN.equals(IP)))) {
                        logger.error("Sending restart Signal...");
                        //send restart signal
                        HashMap<String,String> restartParams = new HashMap<>();
                        try {


                            String hostname= getHostname();
                            String offset=String.valueOf(i);


                            logger.debug("Restarting Host with :"+restartParams);

                            restTemplate.postForObject("http://hauptserver.ddns.net/restartHost?DockerID="+hostname+"&offset="+offset+"&serverPort="+Port,restartParams,String.class);

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }


            }
        });
        t2.start();


        // Never give up, never what?
        while (true) {


            String allSkinsString = restTemplate.getForObject("http://hauptserver.ddns.net/GetAllSkinNames", String.class);
            List<String> allHashnames = Arrays.asList(allSkinsString.split("\\s*,\\s*"));
            logger.info("Listsize: " + allHashnames.size());

            try {

                for (i = 0; i < allHashnames.size(); i++) {
                    if (i % threads == mod) {

                        String hashname = allHashnames.get(i);
                        logger.debug("[" + i + "/" + allHashnames.size() + "]" + "Current HashName: " + hashname);
                        String uri = "https://steamcommunity.com/market/priceoverview/?appid=730&currency=3&market_hash_name=" + hashname;// normalisiert;

                        URI u = new URI(uri);
                        HashMap<String, Object> result;
                        try {
                            result = restTemplate.getForObject(u, HashMap.class);
                        } catch (Exception e) {


                            logger.error(e.getMessage());
                            logger.error(e.getStackTrace().toString());
                            logger.debug("Generated URI: " + uri);


                            if (e.getMessage().contains("502 Bad Gateway")) {
                                //Reconnects to the VPN until it has the same ip as before

                                String currentip = restTemplate.getForObject("https://ipinfo.io/ip", String.class);
                                logger.error("Current IP is: " + currentip + " Should be: " + IP);

                                while (!currentip.equals(IP)) {

                                    logger.error("Trying to reconnect to VPN");
                                    Thread t1 = new Thread(new Runnable() {
                                        public void run() {
                                            runScript("sh /root/startup.sh");

                                        }
                                    });
                                    t1.start();
                                    //Delay for the VPN to connect
                                    Thread.sleep(60000);
                                    logger.error("Current IP is: " + currentip + " Should be: " + IP);


                                }
                                logger.error("Reconnected to VPN, continueing as usual...");

                            } else if (e.getMessage().contains("429 Too Many Requests")) {

                                logger.error("You sent too many requests to the Steammarket");
                                logger.error("Waiting for 61 Minutes...");

                                //waiting and pausing the restart Thread
                                t2.wait();
                                Thread.sleep(3636000);
                                t2.notify();

                            }

                            boolean goOn = false;
                            int waitTime = 10000; // Wait for 10 Seconds

                            // Try again with increasing Watitime until it works
                            while (!goOn) {
                                t2.wait();
                                try {

                                    logger.error("Trying again");

                                    ResponseEntity<String> response = restTemplate.getForEntity(u, String.class);
                                    if (response.getStatusCode().is2xxSuccessful()){
                                        goOn = true;
                                        t2.notify();
                                    }


                                } catch (Exception err) {

                                    logger.error(err.getMessage());
                                    logger.error("Failed");
                                    logger.error("Current URL: " + uri);
                                    waitTime *= 2;
                                    logger.error("Increased WaitTime to: " + waitTime);
                                    logger.error("Waiting...");
                                    Thread.sleep(waitTime);

                                }


                            }
                            result = restTemplate.getForObject(u, HashMap.class);


                        }

                        if (result.containsKey("lowest_price")) {

                            // logger.debug("Determining lowest price");
                            String price = (String) result.get("lowest_price");
                            price = price.replaceAll(" ", "");
                            price = price.replaceAll(",", ".");
                            price = price.replaceAll("€", "");
                            price = price.replaceAll("-", "0");
                            Double pricedouble = Double.parseDouble(price);

                            HashMap<String, Object> sendMap = new HashMap<>();

                            //Fix Later
                            //..or dont haha
                            String url = "http://hauptserver.ddns.net/updateSkin?SkinHash=" + hashname + "&Steamprice=" + pricedouble;
                            // logger.debug("Sending skindata to Server: " + url);
                            Map<String, Object> resultMap = restTemplate.postForObject(url, null, Map.class);
                            logger.debug("Inserted Skin: " + resultMap);

                        } else {
                            //  logger.debug("Item has no Price... skipping!");
                        }

                        Thread.sleep(6000);
                    }
                    //reset restartTimer
                    restartTimer =300;
                }
            } catch (Error error) {

                logger.error(error.getMessage());
                logger.error("Restarting prematurely...");
            }
        }


    }


    /**
     * Run the given command
     *
     * @param command
     */
    public static void runScript(String command) {
        String sCommandString = command;
        CommandLine oCmdLine = CommandLine.parse(sCommandString);
        DefaultExecutor oDefaultExecutor = new DefaultExecutor();
        oDefaultExecutor.setExitValue(0);
        int iExitValue = 0;
        try {
            iExitValue = oDefaultExecutor.execute(oCmdLine);
            System.out.println("Command Executed and finished");

        } catch (ExecuteException e) {
            System.err.println("Execution failed.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("permission denied.");
            e.printStackTrace();
        }


    }

    /**
     * Helpermethod to return the Systems Hostname
     * @return
     * @throws IOException
     */
    public static String getHostname() throws IOException {

      return System.getenv("ContainerName");
    }







}
