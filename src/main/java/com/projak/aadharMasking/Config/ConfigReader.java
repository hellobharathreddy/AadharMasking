package com.projak.aadharMasking.Config;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
//import java.util.logging.Logger;
import org.apache.log4j.*;

public class ConfigReader {
	
	//private static final String CLASS_NAME = ConfigReader.class.getName();
	//private static Logger logger = Logger.getLogger( CLASS_NAME );
	
	static Logger log = Logger.getLogger(ConfigReader.class.getName());
	
	public static Properties getResourceBundle() 
	  {
		
		//String resourceName = "application.properties"; // could also be a constant
		//ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Properties props = new Properties();
		//try(InputStream resourceStream = loader.getResourceAsStream(resourceName)) {
		  
		//try(InputStream resourceStream = new FileInputStream("C:\\Users\\Admin\\Documents\\Aadhaar Config\\application.properties")){
		try(InputStream resourceStream = new FileInputStream("/data/projakinfotech/kbank_adharprj/AadhaarFolder/AadhaarConfig/application.properties")){
			props.load(resourceStream);
		    
		    //ClassLoader classLoader = ClassLoader.getSystemClassLoader();
			
			/*URL url = classLoader.getResource("application.properties");
	        if (url == null) {
	            url = classLoader.getResource("/application.properties");
	        }
	        InputStream in = url.openStream();
			props.load(in);*/
			
			/*Class cr = ConfigReader.class;
			cr.getResource("Application.properties");
			props.load(classLoader.getResourceAsStream("application.properties"));*/
			return props;
		} catch (IOException e) {
			log.error("Property File Not Found", e);
		}
		return props;
	  }
	
}
