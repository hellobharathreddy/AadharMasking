package com.projak.aadharMasking.Reports;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class ReportObject {
	static Logger log = Logger.getLogger(ReportObject.class.getName());
	
	private static ReportObject reportObject = new ReportObject();
	
	private List<String[]> reportData = new ArrayList<String[]>();
	private String parentFolderName;
	private String subFolderName;
	
	private ReportObject() {}
	
	public static ReportObject getInstance() {
		return reportObject;
	}
	
	public void setParentFolderName(String parentFolderName) {
	    this.parentFolderName = parentFolderName;
	}
	
	public String getParentFolderName() {
	    return parentFolderName;
	}
	
	public void setSubFolderName(String subFolderName) {
	    this.subFolderName = subFolderName;
	}
	
	public String getSubFolderName() {
	    return subFolderName;
	}
	
	public void addReportData(String[] newData) {
	    this.reportData.add(newData);
	}
	
	public List<String[]> getReportData() {
	    return reportData;
	}
}
