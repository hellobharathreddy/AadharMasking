package com.projak.aadharMasking.Operations;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import com.projak.aadharMasking.AadhaarMaskApp;
import com.projak.aadharMasking.Config.ConfigReader;
import com.projak.aadharMasking.Reports.ReportObject;

public class FolderCreation {
	
	static Logger log = Logger.getLogger(FolderCreation.class.getName());
	
	public void createFolderStructure(String fromDate, String toDate) throws IOException {
		
		ReportObject report = ReportObject.getInstance();
		
		final Date folderDate = new Date();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE");

		simpleDateFormat = new SimpleDateFormat("YYYY");
	    final String year = simpleDateFormat.format(folderDate).toUpperCase();
		
		simpleDateFormat = new SimpleDateFormat("MMM");
	    final String month = simpleDateFormat.format(folderDate).toUpperCase();
	    
	    final String parentFolderName = year + " " + month;
	    final String subFolderName = "/" + fromDate + "_" + toDate;
	    final String reportFolderName = "/Report";
	    final String tempFolderName = "/TempFiles";
	    
	    log.info("Parent Folder Name is-"+parentFolderName+" SubFolderName is-"+subFolderName);
	    
	    final File folderPath = new File(ConfigReader.getResourceBundle().getProperty("Report_Path") + parentFolderName);
		final File subFolderPath = new File(ConfigReader.getResourceBundle().getProperty("Report_Path")
				+ parentFolderName + subFolderName);
		final File reportFolderPath = new File(ConfigReader.getResourceBundle().getProperty("Report_Path")
				+ parentFolderName + subFolderName + reportFolderName);
		final File tempFolderPath = new File(ConfigReader.getResourceBundle().getProperty("Report_Path")
				+ parentFolderName + subFolderName + tempFolderName);
		
		if(!folderPath.exists()) {
			log.info("Parent Folder doesn't exist");
			folderPath.mkdirs();
			log.info("Parent Folder Created");
		}else {
			log.info("Parent Folder Exists");
		}
		
		if(!subFolderPath.exists()) {
			subFolderPath.mkdir();
			log.info("Sub Folder Created");
		}else{
			log.info("Sub Folder Exists");
		}
		
		if(!reportFolderPath.exists()) {
			reportFolderPath.mkdir();
			log.info("Report Folder Created");
		}else {
			log.info("Report Folder Exists");
		}
		
		if(!tempFolderPath.exists()) {
			tempFolderPath.mkdir();
			log.info("Temporary Files Folder Created");
		}else {
			log.info("Temp Folder Exists");
		}
		
		report.setParentFolderName(parentFolderName);
		report.setSubFolderName(subFolderName);
		log.info("Parent and Sub Folder Name are "+report.getParentFolderName() + report.getSubFolderName());
	    
	}
	
}
