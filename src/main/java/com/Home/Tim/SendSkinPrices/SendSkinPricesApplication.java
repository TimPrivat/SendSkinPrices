package com.Home.Tim.SendSkinPrices;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@SpringBootApplication
public class SendSkinPricesApplication {
    public static int threads = 1;
    public static int mod = 0;
    public static boolean useVPN = false;

    public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException {
        SpringApplication.run(SendSkinPricesApplication.class, args);






        for (int i = 0; i < args.length; i++) {
            System.out.println("Arg[" + i + "]: " + args[i]);
        }

        useVPN = Boolean.parseBoolean(args[0]);
        threads = Integer.parseInt(args[1]);
        mod = Integer.parseInt(args[2]);


        //test
        if (useVPN) {
            System.out.println("Using VPN!");
            Thread t1 = new Thread(new Runnable() {
                public void run() {
                    runScript("sh /root/startup.sh");

                }
            });
            t1.start();

        } else {
            System.out.println("Using Default IP!");
        }

        Thread.sleep(10000);
        RestTemplate restTemplate = new RestTemplate();
        System.out.println("The global IPv4 Address is: " + restTemplate.getForObject("https://ipinfo.io/ip", String.class));


        while (true) {


            String allSkinsString = restTemplate.getForObject("http://hauptserver.ddns.net/GetAllSkinNames", String.class);
        //    System.out.println(allSkinsString);
            List<String> allHashnames = Arrays.asList(allSkinsString.split("\\s*,\\s*"));
            System.out.println("Listsize: " + allHashnames.size());


            for (int i = 0; i < allHashnames.size(); i++) {
                if (i % threads == mod) {

                    String hashname = allHashnames.get(i);
                     System.out.println("["+i+"/"+allHashnames.size()+"]"+"Current HashName: " + hashname);
                    //String normalisiert = normalisieren(hashname);
                    String uri = "https://steamcommunity.com/market/priceoverview/?appid=730&currency=3&market_hash_name=" + hashname;// normalisiert;


                    URI u = new URI(uri);


                    HashMap<String, Object> result;
                    try {
                        result = restTemplate.getForObject(u, HashMap.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Thread.sleep(61000);
                        result = restTemplate.getForObject(u, HashMap.class);

                    }

                    if (result.containsKey("lowest_price")) {

                        String price = (String) result.get("lowest_price");
                        price = price.replaceAll(" ", "");
                        price = price.replaceAll(",", ".");
                        price = price.replaceAll("€", "");
                        price = price.replaceAll("-", "0");
                        Double pricedouble = Double.parseDouble(price);

                        HashMap<String,Object> sendMap = new HashMap<>();

                        //Fix Later
                        String url = "http://hauptserver.ddns.net/updateSkin?SkinHash="+hashname+"&Steamprice="+pricedouble;
                        restTemplate.postForObject(url,null,String.class);
                    }

                    Thread.sleep(6000);
                }
            }

        }


    }


    /**
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



}
