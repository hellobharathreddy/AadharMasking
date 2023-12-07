package com.projak.aadharMasking.Reports;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
//import java.util.logging.Logger;

//import org.apache.commons.csv.CSVFormat;
//import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.*;

import com.opencsv.CSVWriter;
import com.projak.aadharMasking.Config.ConfigReader;
import com.projak.aadharMasking.Email.EmailGenerator;

import java.util.List;

public class ReportGenerator {
	
	//private static final String CLASS_NAME = ReportGenerator.class.getName();
	//private static Logger logger = Logger.getLogger( CLASS_NAME );
	
	static Logger log = Logger.getLogger(ReportGenerator.class.getName());
	
	public Boolean generateCSV(List<String[]> data) {
		
		//ReportObject report = new ReportObject();
		ReportObject report = ReportObject.getInstance();
		log.info("Inside report");
		data.add(0,ConfigReader.getResourceBundle().getProperty("Report_Columns").split(","));
		log.info("CSV Report is :::::::: " + data.toString());
		
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
		String folderPath = ConfigReader.getResourceBundle().getProperty("Report_Path") + report.getParentFolderName() + report.getSubFolderName() + "/Report/";
		String csvFilePath = folderPath+"Aadhaar_Masking_Report_"+dateFormat.format(date)+".csv";
		log.info("The CSV FilePath is "+csvFilePath);
		File folder = new File(folderPath);
		File file = new File(csvFilePath);
		 
	    try {
	    	if(!folder.exists()) {
	    		log.info("Folder doesn't exist");
	    		folder.mkdirs();
	    		log.info("Folder Created");
	    	}
	    	FileWriter outputfile = new FileWriter(file); 
	        CSVWriter writer = new CSVWriter(outputfile); 
	        writer.writeAll(data); 
	        writer.close(); 
	    } 
	    catch (IOException e) {
	    	log.error("Exception while writing to CSV file",e);
	    	e.printStackTrace();
	    	return false;
	    }
	    log.info("Report Created");
	    
	    /*EmailGenerator emailGenerator = new EmailGenerator();
	    Boolean emailSuccessful=emailGenerator.sendEmail(csvFilePath);
	    
	    if (emailSuccessful) {
	    	//file.delete();
	    	//log.info("Local CSV file deleted !!");
	    }*/
	    
	    return true;
	} 
	
	public String generateCSV(StringBuffer csvBuffer) {
		 
		 log.info("Inside generateCSV");
		 
		 StringBuffer resultString = new StringBuffer();
	     resultString.append("");
	     String []columnOrder = ConfigReader.getResourceBundle().getProperty("Report_Columns").split(",");
	     for (String column : columnOrder) {
             resultString.append('"' + column + '"' + ",");
         }
         resultString.append("\n");
         resultString.append(csvBuffer.toString());
         return "Success";
	 }
	 
     
}
