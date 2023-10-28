package com.Home.Tim.SendSkinPrices;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@SpringBootApplication
public class SendSkinPricesApplication {

	public static void main(String[] args) throws InterruptedException, IOException {
		SpringApplication.run(SendSkinPricesApplication.class, args);

		RestTemplate r = new RestTemplate();

	//test
		Thread t1 = new Thread(new Runnable() {
			public void run()
			{
				runScript("sh /root/startup.sh");
			}});
		t1.start();

		Thread.sleep(60000);
		System.out.println("The global IPv4 Address is: "+ r.getForObject("https://ipinfo.io/ip", String.class));


	}



	/**
	 * retVal = 1: Sucess
	 * retVal = 2: Failure
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
