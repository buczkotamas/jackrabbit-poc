package com.mycompany.jackrabbit.poc;

import com.naviextras.zippy.apis.services.market.ContentSegment;
import com.naviextras.zippy.apis.services.market.FileRO;
import com.naviextras.zippy.apis.services.market.FingerprintRO;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

public class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class.getName());

    public static String getChecksum(File file) throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        InputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            while (true) {
                int numRead = fis.read(buffer);
                if (numRead == -1) {
                    break;
                }
                messageDigest.update(buffer, 0, numRead);
            }
            return getHEXString(messageDigest.digest());
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    public static String getFirst64KBChecksum(File file) throws NoSuchAlgorithmException, IOException {
        return getFirst64KBChecksum(new FileInputStream(file));
    }

    public static String getFirst64KBChecksum(InputStream is) throws NoSuchAlgorithmException, IOException {
        try {
            byte[] buffer = new byte[1024];
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            int loop = 0;
            while (true) {
                int numRead = is.read(buffer);
                if (numRead == -1) {
                    break;
                }
                messageDigest.update(buffer, 0, numRead);
                if (++loop == 64) {
                    break;
                }
            }
            return getHEXString(messageDigest.digest());

        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    public static String getHEXString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static ContentSegment getLatestContentSegment(FileRO file) {
        if ((file.getContentSegments() == null) || file.getContentSegments().isEmpty()) {
            return null;
        }
        long maxContentTimestamp = 0;
        ContentSegment latestSegment = null;
        for (ContentSegment segment : file.getContentSegments()) {
            if (segment.getContentTimestamp().getTime() > maxContentTimestamp) {
                maxContentTimestamp = segment.getContentTimestamp().getTime();
                latestSegment = segment;
            }
        }
        return latestSegment;
    }

    public static String toXMLString(FingerprintRO fingerprint) {
        try {
            StringWriter stringWriter = new StringWriter();
            JAXBContext jaxbContext = JAXBContext.newInstance(FingerprintRO.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            QName qName = new QName("info.source4code.jaxb.model", "car");
            JAXBElement<FingerprintRO> root = new JAXBElement<FingerprintRO>(qName, FingerprintRO.class, fingerprint);
            jaxbMarshaller.marshal(root, stringWriter);
            return stringWriter.toString();
        } catch (JAXBException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    public static FingerprintRO toFingerprintRO(File file) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(FingerprintRO.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
//            unmarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
//            QName qName = new QName("info.source4code.jaxb.model", "car");
//            JAXBElement<FingerprintRO> root = new JAXBElement<FingerprintRO>(qName, FingerprintRO.class, fingerprint);
            return (FingerprintRO) unmarshaller.unmarshal(file);
        } catch (JAXBException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

}
