package com.projak.aadharMasking.Operations;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.security.auth.Subject;

import org.apache.log4j.Logger;

import com.filenet.api.core.Connection;
import com.filenet.api.core.Factory;
import com.filenet.api.util.Id;
import com.projak.aadharMasking.Config.ConfigReader;
import com.projak.aadharMasking.Reports.ReportObject;

public class FileOperations {
	
	static Logger log = Logger.getLogger(FileOperations.class.getName());
	
	ReportObject report = ReportObject.getInstance();
	
	public Connection getConn() {
    	String uri = "https://abc.com/wsi/FNCEWS40MTOM/";
		String username = "admin";
		String password = "admin@123";
		Connection conn = Factory.Connection.getConnection(uri);
		Subject subject = com.filenet.api.util.UserContext.createSubject(getConn(), username, password, null);
		com.filenet.api.util.UserContext.get().pushSubject(subject);
		return conn;
	}
	
	public String downloadDocument(InputStream inputStream, String extension, Id Id) throws IOException {
		
		String filePath = ConfigReader.getResourceBundle().getProperty("Report_Path") + report.getParentFolderName()
							+ report.getSubFolderName() + "/TempFiles/" + Id.toString();
		
		log.info("File Path of "+extension+" file is "+filePath);
		FileOutputStream fos = new FileOutputStream(filePath + extension);
		int c;
		while ((c = inputStream.read()) != -1) 
		{
			fos.write(c);
			fos.flush();
		}
		log.info("before infputstream closing");
		inputStream.close();
		log.info("after infputstream closing");
		fos.close();
		return filePath;
		
	}
	
}
