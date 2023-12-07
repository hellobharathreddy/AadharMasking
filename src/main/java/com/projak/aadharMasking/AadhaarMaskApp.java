package com.projak.aadharMasking;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.time.Year;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
//import java.util.logging.Logger;

import javax.security.auth.Subject;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.apache.log4j.*;

import com.filenet.api.collection.ContentElementList;
import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.ClassNames;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.constants.ReservationType;
import com.filenet.api.core.Connection;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Domain;
import com.filenet.api.core.EngineObject;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Document;
import com.filenet.api.property.Properties;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.Id;
import com.filenet.api.util.UserContext;
import com.projak.aadharMasking.Config.ConfigReader;
import com.projak.aadharMasking.Reports.ReportGenerator;


public class AadhaarMaskApp {
	
	//private static final String CLASS_NAME = AadhaarMaskApp.class.getName();
	//private static Logger logger = Logger.getLogger( CLASS_NAME );
	
	static Logger log = Logger.getLogger(AadhaarMaskApp.class.getName());
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_RED = "\u001B[31m";
	
	public static void main(String[] args) {
		int currentYear = Year.now().getValue();
		
		log.info(ANSI_GREEN
				+ "\r\n"
				+ ".______   .______        ______          __       ___       __  ___     __  .__   __.  _______   ______   .___________. _______   ______  __    __  \r\n"
				+ "|   _  \\  |   _  \\      /  __  \\        |  |     /   \\     |  |/  /    |  | |  \\ |  | |   ____| /  __  \\  |           ||   ____| /      ||  |  |  | \r\n"
				+ "|  |_)  | |  |_)  |    |  |  |  |       |  |    /  ^  \\    |  '  /     |  | |   \\|  | |  |__   |  |  |  | `---|  |----`|  |__   |  ,----'|  |__|  | \r\n"
				+ "|   ___/  |      /     |  |  |  | .--.  |  |   /  /_\\  \\   |    <      |  | |  . `  | |   __|  |  |  |  |     |  |     |   __|  |  |     |   __   | \r\n"
				+ "|  |      |  |\\  \\----.|  `--'  | |  `--'  |  /  _____  \\  |  .  \\     |  | |  |\\   | |  |     |  `--'  |     |  |     |  |____ |  `----.|  |  |  | \r\n"
				+ "| _|      | _| `._____| \\______/   \\______/  /__/     \\__\\ |__|\\__\\    |__| |__| \\__| |__|      \\______/      |__|     |_______| \\______||__|  |__| \r\n"
				+ "                                                                                                                                                    \r\n"
				+ ANSI_RESET
				+ ANSI_RED
				+ " <<< Building the Future of automation world   ||   AI-Assisted Aadhaar Masking Solution - FileNet API v1.0 >>>\r\n"
				+ ANSI_RESET
				+ANSI_GREEN
				+ "     Copyright © " + currentYear + ". Karnataka Bank. All rights reserved. \r\n"
				+ ANSI_RESET);
		
		log.info("::::::::::::::::::::      APPLICATION STARTED     ::::::::::::::::::::::::");
		long startTime = System.currentTimeMillis();
	    
		// Make connection.
	    Connection conn = Factory.Connection.getConnection(ConfigReader.getResourceBundle().getProperty("FileNet_URI"));
	    Subject subject = UserContext.createSubject(conn, ConfigReader.getResourceBundle().getProperty("Username"), 
	    		ConfigReader.getResourceBundle().getProperty("Password"), null);
	    UserContext.get().pushSubject(subject);

	    try
	    {
	       // Get default domain.
	       Domain domain = Factory.Domain.fetchInstance(conn, null, null);
	       log.info("DOMAIN NAME : " + domain.get_Name());
	       ObjectStore objectStore = Factory.ObjectStore.fetchInstance(domain,ConfigReader.getResourceBundle().getProperty("Repository"), null);
	       log.info("OBJECTSTORE DISPLAY NAME : " + ConfigReader.getResourceBundle().getProperty("Repository"));
	       log.info("::: OBJECTSTORE CONNECTED SUCCESSFULLY :::");
	       
	     //  AadhaarMaskApp ap = new AadhaarMaskApp();
		 //  ap.run(domain, objectStore);
	       
	       MaskingService maskService = new MaskingService();
	       maskService.run(domain, objectStore);
		   
	    }catch(Exception e) {
	    	log.error("EXCEPTION IN MAIN METHOD ==> ",e);
	    	e.printStackTrace();
	    }
	    finally
	    {
	    	log.info("::: RELEASING THE CONNECTION :::");
	        UserContext.get().popSubject();
	        long endTime = System.currentTimeMillis();
	        long processTimeMillis = endTime - startTime;
	        long processTimeSeconds = processTimeMillis / 1000;
	        long minutes = processTimeSeconds / 60;
	        long hours = minutes / 60;
	        long remainingSeconds = processTimeSeconds % 60;
	        log.info("TOTAL APPLICATION PROCESS TIME::: " + hours + " hours, " + minutes % 60 + " minutes, " + remainingSeconds + " seconds.");
	        log.info("::::::::::::::::::::      APPLICATION STOPPED     ::::::::::::::::::::::::");
	    }
	}

}
