package gui;

import com.naviextras.zippy.apis.services.market.FingerprintRO;

import java.io.File;
import java.io.StringWriter;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author tbuczko
 */
public class TestUtil {

    private static final Logger logger = Logger.getLogger(TestUtil.class.getName());
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;

    static {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Wrapper.class);

            unmarshaller = JAXBContext.newInstance(Wrapper.class).createUnmarshaller();
            marshaller = jaxbContext.createMarshaller();

            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        } catch (JAXBException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    public static FingerprintRO getFingerprint(File file) {
        try {
            return ((Wrapper) unmarshaller.unmarshal(file)).getFingerprint();
        } catch (JAXBException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    public static String toString(FingerprintRO fingerprint) {
        if (fingerprint == null) {
            return "null";
        } else {
            StringWriter sw = new StringWriter();

            try {
                marshaller.marshal(new Wrapper(fingerprint), sw);
            } catch (JAXBException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }

            return sw.toString();
        }
    }
}
