package com.projak.aadharMasking;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.json.JSONObject;
import com.filenet.api.collection.ContentElementList;
import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.constants.ReservationType;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Document;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.property.Properties;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.Id;
import com.projak.aadharMasking.Config.ConfigReader;
import com.projak.aadharMasking.Operations.FileOperations;
import com.projak.aadharMasking.Operations.FolderCreation;
import com.projak.aadharMasking.Reports.ReportGenerator;
import com.projak.aadharMasking.Reports.ReportObject;

public class MaskingService {
	
	static Logger log = Logger.getLogger(MaskingService.class.getName());
	
	void run(Domain domain, ObjectStore objectStore) throws IOException, Exception {
		
		log.info("Inside Run");
		Document pwc = null;
		Boolean cancelCheckOutFlag = false;
		Boolean isDocumentPresent = false;
		Id Id;
		FileOperations operations = new FileOperations();
		ReportObject report = ReportObject.getInstance();
		
		SearchScope searchScope = new SearchScope(objectStore);
		
		final int imageSize = Integer.parseInt(ConfigReader.getResourceBundle().getProperty("Image_Max_Size"));
		
		final String fromDate = ConfigReader.getResourceBundle().getProperty("SQL_From_Date");
		final String toDate = ConfigReader.getResourceBundle().getProperty("SQL_To_Date");
		
		final String mySQLString = ConfigReader.getResourceBundle().getProperty("SQL_Query") + 
				" AND DateCreated >=" + fromDate + " AND DateCreated <=" + toDate;
		
        final SearchSQL sqlObject = new SearchSQL();
        sqlObject.setQueryString(mySQLString);
		final RepositoryRowSet rowSet = searchScope.fetchRows(sqlObject, null, null, new Boolean(true));
		final Iterator iter = rowSet.iterator();
		log.info("***results from query " + mySQLString);
		int count = 1;
		String originalFilename = null;
		
		FolderCreation folderCreation = new FolderCreation();
		folderCreation.createFolderStructure(fromDate, toDate);
		
		while (iter.hasNext()) {
			long startTime = System.currentTimeMillis();
			log.info("  ");
			log.info("  ");
			cancelCheckOutFlag = false;
			isDocumentPresent = true;
		   	RepositoryRow repositoryRow = (RepositoryRow) iter.next();
		   	Id=repositoryRow.getProperties().get("Id").getIdValue();
		   	log.info("ID OF THE CURRENT DOCUMENT::: " + Id.toString());
		   	
		   	try {
		   		
		   		com.filenet.api.core.Document document = Factory.Document.fetchInstance(objectStore, new Id(Id.toString()), null);
			   	
			   	originalFilename = document.get_Name();
			   	String filename = originalFilename.replace(".", "");
			   	log.info("FILE NAME::: " + filename);
		        String mimetype = document.get_MimeType();
			   	Double contentSize = document.get_ContentSize();
			   	log.info("CONTENT SIZE OF DOCUMENT::: " + contentSize);
			   	log.info("DOCUMENT MIMETYPE "+mimetype);
		        
			   	InputStream inputStream = document.accessContentStream(0);
			   	if(inputStream!=null) {
			   		
			   		if(contentSize<imageSize &&  (mimetype.equalsIgnoreCase("image/jpg") || mimetype.equalsIgnoreCase("image/jpeg"))) {
			   			log.info("::: Content is JPG image :::");
			   			byte[] content = IOUtils.toByteArray(inputStream);
					   	
					   	String encoded = Base64.getEncoder().encodeToString(content);
				        
				        Masking am = new Masking();
			    		JSONObject jsonMaskingObject = am.maskDocument(encoded, filename+".jpg");
			    		String apiResponse = jsonMaskingObject.getString("apiResponse");
			    		
			    		if(jsonMaskingObject.getString("apiResponse")!=null) {
			    			
			    			String processCount = jsonMaskingObject.getString("processCount");
		        			
		        			JSONObject jsonObject = new JSONObject(apiResponse);
		        			boolean keyExists = jsonObject.has("manual_masking");
		        			
		        			if(keyExists) {
		        				log.info("REPROCESS FLAG::: " + jsonObject.getBoolean("reprocess"));
			        			log.info("MANUAL MASKING VALUE::: " + jsonObject.getBoolean("manual_masking"));
			        			
			        				if(jsonObject.getBoolean("manual_masking")!=true) {
				        				
				        				byte[] decoded = Base64.getDecoder().decode(jsonObject.getString("mskd_img").getBytes("UTF-8"));
				            			document.checkout(ReservationType.OBJECT_STORE_DEFAULT, null, null, null);
				            			document.save(RefreshMode.REFRESH);
				            			log.info("CHECKOUT DONE FOR ==> "+Id);
				            			cancelCheckOutFlag=true;
				            			
				            			pwc = (Document) document.get_Reservation();
				            			log.info("::: Reservation Document Retrieved :::");
				            			
				            			InputStream newInputStream = new ByteArrayInputStream(decoded);
				            			ContentElementList contentElements = Factory.ContentElement.createList();
				            			ContentTransfer contentTransfer = Factory.ContentTransfer.createInstance();
				            			contentTransfer.setCaptureSource(newInputStream);
				            			contentTransfer.set_RetrievalName(originalFilename);
				            			contentTransfer.set_ContentType(mimetype);
				            			contentElements.add(contentTransfer);
				            			pwc.set_ContentElements(contentElements);
				            			pwc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
				            			log.info("::: Content Set for Reservation Document :::");
				            			Date date = new Date();
				            			
				            			Properties prop = pwc.getProperties();
				                        prop.putValue("MaskFlag",true);
				                        prop.putValue("MaskDate",date);
				                        prop.putValue("MaskStatus", "2");
				                        prop.putValue("MaskID", Id.toString());
				            			
				            			
				            			pwc.save(RefreshMode.NO_REFRESH);
				            			Id newId = pwc.get_Id();
				            			log.info("UPDATED ID OF THE DOCUMENT ==> "+ newId.toString());
				            			cancelCheckOutFlag=false;
				            			log.info("CHECKIN DONE FOR::: "+ newId);
				            			
				            			report.addReportData(new String[] { originalFilename, newId.toString(), Id.toString(), "True", "2", date.toString(), "Successful", processCount});
				            			insertIntoDatabase(originalFilename, newId.toString(), Id.toString(), "True", "2", date, "Successful", processCount);
				            		
				        			}else {
				        				log.info("::: Document Was Not Masked Properly :::");
				        				
				        				Properties prop = document.getProperties();
				        				prop.putValue("MaskFlag",false);
				        				prop.putValue("MaskStatus", "0");
				        				document.save(RefreshMode.NO_REFRESH);
				        				
				        				report.addReportData(new String[] { originalFilename, "", Id.toString(), "False", "0", "", "Not Successful", processCount});
				        				insertIntoDatabase(originalFilename, "", Id.toString(), "False", "0", null, "Not Successful", processCount);
				        			}
		        			} else {
		        				log.error("manual_masking is not found in the apiResponse. " + "keyExists is::: " + keyExists);
		        			}
			    		}
			   			
			        }else if(contentSize > imageSize && (mimetype.equalsIgnoreCase("image/jpg") || mimetype.equalsIgnoreCase("image/jpeg"))) {
			        
			        	log.info("Can't process JPG document as size is larger than " + imageSize + " bytes");
			        	report.addReportData(new String[] { originalFilename, "", Id.toString(), "", "", "", "Image larger than than " + imageSize + " bytes", ""});
			        	insertIntoDatabase(originalFilename, "", Id.toString(), "", "", null, "Image larger than than " + imageSize + " bytes", "");
			        
			        }else if(mimetype.equalsIgnoreCase("application/pdf")) {
			        	log.info("::: Content is PDF File :::");
			        	
			        	Boolean createPDFDocument = false;
			        	Boolean partialSuccess = false;
			        	String extension = getExtension(mimetype);
			        	String filePath = operations.downloadDocument(inputStream, extension, Id);
			        	log.info("Received FilePath from operations for PDF::: "+ filePath);
			        	String[] imageFilePaths=null;
			        	try {
			        		PDDocument pdfDocument = PDDocument.load(new File(filePath + extension));
			        		log.info("::: PDF Loaded :::");
				            PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);
				            imageFilePaths = new String[pdfDocument.getNumberOfPages()];
				            log.info("NO. OF PAGES IN PDF::: " + pdfDocument.getNumberOfPages());
				            for (int page = 0; page < pdfDocument.getNumberOfPages(); page++) {
				                BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300);
				                log.info("PDF RENDERED FOR PAGE::: " + page);
				                String imagePath = filePath + (page + 1) + ".jpg";
				                File outputJpegFile = new File(imagePath);
				                ImageIO.write(image, "jpg", outputJpegFile);
				                imageFilePaths[page]=imagePath;
				            }
				            pdfDocument.close();
			        	}catch(Exception e) {
			        		log.error("::: Exception while converting PDF to JPG files :::");
			        		log.error(e.getMessage());
			        	}
			        	log.info("IMAGE FILE PATHS::: " + imageFilePaths.toString());
			            if(imageFilePaths!=null) {
			            	for(int page = 0; page < imageFilePaths.length; page++) {
			            		byte[] fileContent = FileUtils.readFileToByteArray(new File(imageFilePaths[page]));
			    		        if(fileContent.length < imageSize) {
			    		        	String encodedString = Base64.getEncoder().encodeToString(fileContent);

				    		        Masking am = new Masking();
						    		JSONObject jsonMaskingObject = am.maskDocument(encodedString, filename+"_"+(page+1)+".jpg");
						    		String apiResponse = jsonMaskingObject.getString("apiResponse");
						    		
						    		if(jsonMaskingObject.getString("apiResponse")!=null) {
						    			
						    			String processCount = jsonMaskingObject.getString("processCount");
					        			JSONObject jsonObject = new JSONObject(apiResponse);
					        			boolean keyExists = jsonObject.has("manual_masking");
					        			
					        			if(keyExists) {
					        				log.info("REPROCESS FLAG::: " + jsonObject.getBoolean("reprocess"));
						        			log.info("MANUAL MASKING VALUE::: " + jsonObject.getBoolean("manual_masking"));
						        			
						        				if(jsonObject.getBoolean("manual_masking")!=true) {
								    				
								    				byte[] decoded = Base64.getDecoder().decode(jsonObject.getString("mskd_img").getBytes("UTF-8"));
								    				
								    				try (FileOutputStream fos = new FileOutputStream(filePath + (page + 1) + "_msk_" + ".jpg")) {
								    	                fos.write(decoded);
								    	            }
								    				
								    				imageFilePaths[page]=filePath + (page + 1) + "_msk_" + ".jpg";
								    				createPDFDocument=true;
								    			}else {
								    				log.info(page + 1 + " Document under PDF is not masked properly");
								    				partialSuccess=true;
								    			}
					        			} else {
					        				log.error("manual_masking is not found in the apiResponse. " + "keyExists is::: " + keyExists);
					        				
					        			}
						    		}
			    		        }else {
			    		        	log.info(page + 1 + " Document under PDF is larger than " +imageSize+" Bytes");
			    		        	partialSuccess=true;
						        }
				            	
				            }
			            }else {
			            	log.info("::: No data in imagefilepath to convert to PDF :::");
			            }
			            
			            if(createPDFDocument) {
			            	
			            	PDDocument combinedPDFDoc = new PDDocument();

			            	for (String imageFileName : imageFilePaths) {
			                    BufferedImage image = ImageIO.read(new File(imageFileName));
			                    PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
			                    combinedPDFDoc.addPage(page);

			                    PDPageContentStream contentStream = new PDPageContentStream(combinedPDFDoc, page);
			                    contentStream.drawImage(PDImageXObject.createFromByteArray(combinedPDFDoc, imageToByteArray(image), imageFileName), 0, 0, image.getWidth(), image.getHeight());
			                    contentStream.close();
			                }
			            	String combinedPDFDocPath = filePath + "_combined_" + extension;
			            	combinedPDFDoc.save(combinedPDFDocPath);
			            	combinedPDFDoc.close();
			            	
			            	
			            	document.checkout(ReservationType.OBJECT_STORE_DEFAULT, null, null, null);
	            			document.save(RefreshMode.REFRESH);
	            			log.info("CHECKOUT DONE FOR ==> " + Id);
	            			cancelCheckOutFlag=true;
	            			
	            			pwc = (Document) document.get_Reservation();
	            			log.info("::: Reservation Document Retrieved :::");
	            			
	            			InputStream newInputStream = new FileInputStream(combinedPDFDocPath);
	            			ContentElementList contentElements = Factory.ContentElement.createList();
	            			ContentTransfer contentTransfer = Factory.ContentTransfer.createInstance();
	            			contentTransfer.setCaptureSource(newInputStream);
	            			contentTransfer.set_RetrievalName(originalFilename);
	            			contentTransfer.set_ContentType(mimetype);
	            			contentElements.add(contentTransfer);
	            			pwc.set_ContentElements(contentElements);
	            			pwc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
	            			log.info("::: Content Set for Reservation Document :::");
	            			Date date = new Date();
	            			
	            			Properties prop = pwc.getProperties();
	                        prop.putValue("MaskFlag",true);
	                        prop.putValue("MaskDate",date);
	                        if(partialSuccess) {
	                        	prop.putValue("MaskStatus", "1");
		                    }else {
	                        	prop.putValue("MaskStatus", "2");
		                    }
	                        prop.putValue("MaskID", Id.toString());
	            			
	            			pwc.save(RefreshMode.NO_REFRESH);
	            			Id newId = pwc.get_Id();
	            			log.info("UPDATED ID OF THE DOCUMENT ==> " + newId.toString());
	            			cancelCheckOutFlag=false;
	            			log.info("CHECKIN DONE FOR ==> " + newId);
	            			
	            			if(partialSuccess) {
	            				report.addReportData(new String[] { originalFilename, newId.toString(), Id.toString(), "True", "1", date.toString(), "Partially Successful", "2"});
	            				insertIntoDatabase(originalFilename, newId.toString(), Id.toString(), "True", "1", date, "Partially Successful", "2");
				            }else {
		                    	report.addReportData(new String[] { originalFilename, newId.toString(), Id.toString(), "True", "2", date.toString(), "Successful", ""});
		                    	insertIntoDatabase(originalFilename, newId.toString(), Id.toString(), "True", "2", date, "Successful", "");
				            }
	            		
			            }else {
			            	
			            	log.info("::: Document Was Not Masked Properly :::");
	        				
	        				Properties prop = document.getProperties();
	        				prop.putValue("MaskFlag",false);
	        				prop.putValue("MaskStatus", "0");
	        				document.save(RefreshMode.NO_REFRESH);
	        				
	        				report.addReportData(new String[] { originalFilename, "", Id.toString(), "False", "0", "", "Not Successful", "2"});
	        				insertIntoDatabase(originalFilename, "", Id.toString(), "False", "0", null, "Not Successful", "2");
			            	
			            }
			            
			        }else if(mimetype.equalsIgnoreCase("image/png") || mimetype.equalsIgnoreCase("image/tiff") || mimetype.equalsIgnoreCase("image/tif") || mimetype.equalsIgnoreCase("image/bmp")) {
			        	log.info("Content is::: " + mimetype);
			        	
			        	boolean convertErrorFlag = false;
			        	// Download Document to local
			        	String extension = getExtension(mimetype);
			        	String filePath = operations.downloadDocument(inputStream, extension, Id);
	    			    log.info("Filepath received from operations for image::: " + filePath);
	    			    
			        	try {
			        		// Convert local file to JPG
		    			    File imageFile = new File(filePath + extension);
		    			    BufferedImage buffImage = ImageIO.read(imageFile);
		    			    File output = new File(filePath + ".jpg");
		    			    BufferedImage result = new BufferedImage(
		    			    		buffImage.getWidth(),
		    			    		buffImage.getHeight(),
		    			            BufferedImage.TYPE_INT_RGB);
		    			    result.createGraphics().drawImage(buffImage, 0, 0, Color.WHITE, null);
		    			    ImageIO.write(buffImage, "jpg", output);
			        	}catch(Exception e) {
			        		convertErrorFlag = true;
			        		log.error("Exception while converting "+extension+" file to jpg",e);
			        		log.error(e.getMessage());
			        	}
	    			    
	    		        if(!convertErrorFlag) {
	    		        	// Get Base64 String of new image file
		    		        byte[] fileContent = FileUtils.readFileToByteArray(new File(filePath+".jpg"));
		    		        if(fileContent.length < imageSize) {
		    		        	String encodedString = Base64.getEncoder().encodeToString(fileContent);

			    		        Masking am = new Masking();
					    		JSONObject jsonMaskingObject = am.maskDocument(encodedString, filename + ".jpg");
					    		String apiResponse = jsonMaskingObject.getString("apiResponse");
					    		
					    		if(jsonMaskingObject.getString("apiResponse")!=null) {
					    			
					    			String processCount = jsonMaskingObject.getString("processCount");
				        			JSONObject jsonObject = new JSONObject(apiResponse);
				        			boolean keyExists = jsonObject.has("manual_masking");
				        			
				        			if(keyExists) {
				        				log.info("REPROCESS FLAG::: " + jsonObject.getBoolean("reprocess"));
					        			log.info("MANUAL MASKING VALUE::: " + jsonObject.getBoolean("manual_masking"));
					        			
					        				if(jsonObject.getBoolean("manual_masking")!=true) {
							    				
							    				byte[] decoded = Base64.getDecoder().decode(jsonObject.getString("mskd_img").getBytes("UTF-8"));
							    				
							    				document.checkout(ReservationType.OBJECT_STORE_DEFAULT, null, null, null);
						            			document.save(RefreshMode.REFRESH);
						            			log.info("CHECKOUT DONE FOR ==> "+Id);
						            			cancelCheckOutFlag=true;
						            			
						            			pwc = (Document) document.get_Reservation();
						            			log.info("::: Reservation Document Retrieved :::");
						            			
						            			InputStream newInputStream = new ByteArrayInputStream(decoded);
						            			ContentElementList contentElements = Factory.ContentElement.createList();
						            			ContentTransfer contentTransfer = Factory.ContentTransfer.createInstance();
						            			contentTransfer.setCaptureSource(newInputStream);
						            			contentTransfer.set_RetrievalName(originalFilename);
						            			contentTransfer.set_ContentType("image/jpg");
						            			contentElements.add(contentTransfer);
						            			pwc.set_ContentElements(contentElements);
						            			pwc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
						            			log.info("::: Content Set for Reservation Document :::");
						            			Date date = new Date();
						            			
						            			Properties prop = pwc.getProperties();
						                        prop.putValue("MaskFlag",true);
						                        prop.putValue("MaskDate",date);
						                        prop.putValue("MaskStatus", "2");
						                        prop.putValue("MaskID", Id.toString());
						            			
						            			pwc.save(RefreshMode.NO_REFRESH);
						            			Id newId=pwc.get_Id();
						            			log.info("UPDATED ID OF THE DOCUMENT ==> " + newId.toString());
						            			cancelCheckOutFlag=false;
						            			log.info("CHECKIN DONE FOR ==> " + newId);
						            			
						            			report.addReportData(new String[] { originalFilename, newId.toString() ,Id.toString(), "True", "2", date.toString(), "Successful", processCount});
						            			insertIntoDatabase(originalFilename, newId.toString(), Id.toString(), "True", "2", date, "Successful", processCount);
							    			
							    			} else {
						        				log.info("::: Document Was Not Masked Properly :::");
						        				
						        				Properties prop = document.getProperties();
						        				prop.putValue("MaskFlag",false);
						        				prop.putValue("MaskStatus", "0");
						        				document.save(RefreshMode.NO_REFRESH);
						        				
						        				report.addReportData(new String[] { originalFilename, "", Id.toString(), "False", "0", "", "Not Successful", processCount});
						        				insertIntoDatabase(originalFilename, "", Id.toString(), "False", "0", null, "Not Successful", processCount);
						        			}
				        			} else {
				        				log.error("manual_masking is not found in the apiResponse. " + "keyExists is::: " + keyExists);
				        			}
					    		}
		    		        }else {
		    		        	log.info("Can't process "+ extension +" document as size is larger than "+imageSize+" bytes");
					        	report.addReportData(new String[] { originalFilename, "", Id.toString(), "", "", "", "Image larger than "+imageSize+" bytes", ""});
					        	insertIntoDatabase(originalFilename, "", Id.toString(), "", "", null, "Image larger than "+imageSize+" bytes", "");
		    		        }
	    		        }
	    		        
	    		    }else {
	    		    	log.info("::: Document File Format Not Supported :::");
	    		    	report.addReportData(new String[] { originalFilename, "", Id.toString(), "", "", "", "Document File Format not supported", ""});
	    		    	insertIntoDatabase(originalFilename, "", Id.toString(), "", "", null, "Document File Format not supported", "");
	    		    }
			   	
			   	}else {
			   		log.info("::: No Stream :::");
			   		report.addReportData(new String[] { originalFilename, "", Id.toString(), "", "", "", "No Document Found", ""});
			   		insertIntoDatabase(originalFilename, "", Id.toString(), "", "", null, "No Document Found", "");
			   	}
		   		log.info("NO. OF DOCUMENTS PROCESSED UNTIL NOW :::::    " + count);
		   	}catch(ConnectException ce) {
		   		log.error("Unable to Connect to Masking API ", ce);
		   		log.error(ce.getMessage());
		   		log.info("::::::::::::::::::::      Stopping Application     ::::::::::::::::::::::::");
		   		System.exit(0);
		   	}catch(IOException e) {
		   		e.printStackTrace();
        		log.error("IO Exception While Checking Out Document", e);
        		log.error(e.getMessage());
        		report.addReportData(new String[] { originalFilename, "", Id.toString(), "", "", "", "No Response From Masking API", ""});
        		insertIntoDatabase(originalFilename, "", Id.toString(), "", "", null, "No Response From Masking API", "");
        	}catch(Exception e) {
		   		e.printStackTrace();
        		log.error("Exception While Checking Out Doucment", e);
        		log.error(e.getMessage());
        		if(cancelCheckOutFlag) {
        			pwc.cancelCheckout();
        			pwc.save(RefreshMode.NO_REFRESH);
        		}
		   	}
		   	count++;
		   	long endTime = System.currentTimeMillis();
	        long processTimeMillis = endTime - startTime;
	        long processTimeSeconds = processTimeMillis / 1000;
	        long minutes = processTimeSeconds / 60;
		   	log.info("PROCESS TIME FOR DOCUMENT ==>" + originalFilename + " :: " + processTimeMillis + " MILLISECONDS, " + processTimeSeconds + " SECONDS, " + minutes % 60 + " MINUTES.");
		}
		
		if(!report.getReportData().isEmpty()) {
			ReportGenerator rp = new ReportGenerator();
		    Boolean reportFlag = rp.generateCSV(report.getReportData());
		    String tempFolderPath = ConfigReader.getResourceBundle().getProperty("Report_Path")
					+ report.getParentFolderName() + report.getSubFolderName() + "/TempFiles";
		 //   log.info("Deleting Temporary Folder");
		    File deleteTempFolder = new File(tempFolderPath);
		  //  deleteTempFolder.delete();
		}
		
		if(!isDocumentPresent) {
			log.info("::: No Documents Present for the Query :::");
		}
	}

	private static byte[] imageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

	private String getExtension(String mimetype) {
		if(mimetype.equalsIgnoreCase("image/tiff") || mimetype.equalsIgnoreCase("image/tif")) {
			return ".tif";
		}else if(mimetype.equalsIgnoreCase("image/bmp")) {
			return ".bmp";
		}else if(mimetype.equalsIgnoreCase("application/pdf")) {
			return ".pdf";
		}else if(mimetype.equalsIgnoreCase("image/jpg") || mimetype.equalsIgnoreCase("image/jpeg")) {
			return ".jpg";
		}else {
			return ".png";
		}
	}
	
	private void insertIntoDatabase(String documentTitle, String newId, String oldId, String maskFlag, String maskCode, Date maskDate, String maskStatus, String processCount) {
		String jdbcUrl = ConfigReader.getResourceBundle().getProperty("DB_URL");
	    String username = ConfigReader.getResourceBundle().getProperty("DB_USERNAME");
	    String password = ConfigReader.getResourceBundle().getProperty("DB_PASSWORD");

	    Connection connection = null;

	    try {
	    	log.info("::: Connecting to Reports Database :::");
	        connection = DriverManager.getConnection(jdbcUrl, username, password);
	        log.info("Connected Successfully!");
	        String sql = ConfigReader.getResourceBundle().getProperty("REPORTS_INSERT_QUERY");
	        
	        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
	            preparedStatement.setString(1, documentTitle);
	            preparedStatement.setString(2, newId);
	            preparedStatement.setString(3, oldId);
	            preparedStatement.setString(4, maskFlag);
	            preparedStatement.setString(5, maskCode);
	            
	            if (maskDate != null) {
	                java.sql.Date sqlDate = new java.sql.Date(maskDate.getTime());
	                preparedStatement.setDate(6, sqlDate);
	            } else {
	                preparedStatement.setNull(6, Types.DATE);
	            }
	            
	            preparedStatement.setString(7, maskStatus);
	            preparedStatement.setString(8, processCount);

	            preparedStatement.executeUpdate();
	            log.info("::: Data inserted successfully into reports table :::");
	        }
	    } catch (Exception e) {
	        log.error("Exception in insertIntoDatabase ==> " + e.getMessage());
	        e.getMessage();
	        e.printStackTrace();
	    } finally {
	        if (connection != null) {
	            try {
	                connection.close();
	                log.info("Database connection closed!");
	            } catch (SQLException e) {
	                log.error("Error closing database connection", e);
	                e.getMessage();
	                e.printStackTrace();
	            }
	        }
	    }
	}	
}
