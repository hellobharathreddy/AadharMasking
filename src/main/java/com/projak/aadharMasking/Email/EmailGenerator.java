package com.projak.aadharMasking.Email;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
//import java.util.logging.Logger;

import org.apache.log4j.*;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.projak.aadharMasking.Config.ConfigReader;


public class EmailGenerator {
	
	//private static final String CLASS_NAME = EmailGenerator.class.getName();
	//private static Logger logger = Logger.getLogger( CLASS_NAME );
	
	static Logger log = Logger.getLogger(EmailGenerator.class.getName());
	
    public Boolean sendEmail(String localCsvFilePath) {
    	
    	String[] to = ConfigReader.getResourceBundle().getProperty("Email_To").split(",");
        String from = ConfigReader.getResourceBundle().getProperty("Email_From");
        
        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", ConfigReader.getResourceBundle().getProperty("Email_Host"));
        properties.put("mail.smtp.port", ConfigReader.getResourceBundle().getProperty("Email_Port"));
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");
        
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
        	protected PasswordAuthentication getPasswordAuthentication() {
        		return new PasswordAuthentication(ConfigReader.getResourceBundle().getProperty("Email_Username"), 
        				ConfigReader.getResourceBundle().getProperty("Email_Password"));
        	}
        });
        
        log.info("Email session created");
        
        try {
            	
        	MimeMessage message = new MimeMessage(session);
        	message.setFrom(new InternetAddress(from));
        	for (int i=0; i<to.length; i++){
        		message.addRecipient(Message.RecipientType.TO, new InternetAddress(to[i]));
            }
        	message.setSubject("Reports for Aadhaar Masking");
        	Multipart multipart = new MimeMultipart();
        	MimeBodyPart attachmentPart = new MimeBodyPart();
        	MimeBodyPart textPart = new MimeBodyPart();

            try {
            	File file =new File(localCsvFilePath);
                attachmentPart.attachFile(file);
                textPart.setText("Please verify the attachment for report");
                multipart.addBodyPart(textPart);
                multipart.addBodyPart(attachmentPart);

            } catch (IOException e) {
            	log.error("IO Exception in Email Generation",e);
                e.printStackTrace();
            }
            
            message.setContent(multipart);
            log.info("Sending email.....");
            Transport.send(message);
            log.info("Email sent successfully....");
            return true;
            
        } catch (MessagingException mex) {
        	log.error("Exception during Email",mex);
            mex.printStackTrace();
            return false;
        }
    }
	
}