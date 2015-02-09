package com.mycompany.jackrabbit.poc;

import gui.JCRForm;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author tbuczko
 */
public class LocalFileParser {

    private final RigoImre rigoImre;
    private final String repositoryName;
    private final String downloadLocationPrefix;
    private final boolean storeBinary;
    private final File root;
    private static final Logger logger = Logger.getLogger(JCRForm.class.getName());

    public LocalFileParser(String repositoryName, String downloadLocationPrefix, File root, RigoImre rigoImre, boolean storeBinary) {
        this.rigoImre = rigoImre;
        this.storeBinary = storeBinary;
        this.repositoryName = repositoryName;
        this.downloadLocationPrefix = downloadLocationPrefix.endsWith("/") ? downloadLocationPrefix : downloadLocationPrefix + "/";
        this.root = root;
    }

    public void parse() throws RepositoryException {
        try {
            parse(root);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RepositoryException(ex);
        } catch (NoSuchAlgorithmException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RepositoryException(ex);
        }
    }

    private String getJcrPath(File file) {
        if (file == root) {
            return repositoryName + "/" + file.getName();
        }
        String jcrPath = repositoryName + "/" + root.getName() + file.getPath().replace(root.getPath(), "");
        if (jcrPath.endsWith(".md5")) {
            return jcrPath.substring(0, jcrPath.lastIndexOf(".")).concat(".fbl");
        } else if (jcrPath.endsWith(".md5sum")) {
            return jcrPath.substring(0, jcrPath.lastIndexOf("."));
        } else if (jcrPath.endsWith(".tgz.desc") || jcrPath.endsWith(".zip.desc")) {
            return jcrPath.substring(0, jcrPath.lastIndexOf("."));
        } else {
            return jcrPath;
        }
    }

    private String getDownloadLocation(File file) {
        return downloadLocationPrefix + root.getName() + file.getPath().replace(root.getPath(), "");
    }

    private void parse(File file) throws RepositoryException, IOException, FileNotFoundException, NoSuchAlgorithmException {
        logger.log(Level.INFO, "Parse local file: {0}", new Object[]{file.getPath()});
        if (file.isDirectory()) {
            rigoImre.getNode(getJcrPath(file)).setProperty("directory", true);
//            JcrUtils.getOrCreateByPath(getJcrPath(file), "nt:folder", rigoImre.getSession());
            for (File file2 : file.listFiles()) {
                parse(file2);
            }
        } else {
            if (file.getName().endsWith(".md5") || file.getName().endsWith(".md5sum")) {
                parseChecksumFile(file);
            } else if (file.getName().endsWith(".tgz.desc") || file.getName().endsWith(".zip.desc")) {
                parseDescriptorFile(file);
            } else {
                parseFile(file);
            }
        }
    }

    private void parseFile(File file) throws RepositoryException, FileNotFoundException, NoSuchAlgorithmException, IOException {
        Node node = rigoImre.getNode(getJcrPath(file));
        if (storeBinary) {
            Binary binary = rigoImre.getSession().getValueFactory().createBinary(new FileInputStream(file));
            node.setProperty("jcr:data", binary);
        }
        node.setProperty("first64Checksum", Utils.getFirst64KBChecksum(file));
        node.setProperty("size", file.length());
        if (downloadLocationPrefix != null) {
            node.setProperty("downloadLocation", getDownloadLocation(file));
        }
    }

    private void parseDescriptorFile(File file) throws IOException, RepositoryException {
        String jcrPath = getJcrPath(file);
        Node node = rigoImre.getNode(jcrPath);
        node.setProperty("archive", true);
        String json = new Scanner(file).useDelimiter("\\Z").next();
        JSONObject obj = new JSONObject(json);
        JSONArray instanceParts = obj.getJSONArray("instanceParts");
        for (int i = 0; i < instanceParts.length(); i++) {
            String filename = instanceParts.getJSONObject(i).getString("filename");
            String filepath = instanceParts.getJSONObject(i).getString("filepath");
            String first64Md5Checksum = instanceParts.getJSONObject(i).getString("first64Md5Checksum");
            String md5Checksum = instanceParts.getJSONObject(i).getString("md5Checksum");
            long size = instanceParts.getJSONObject(i).getLong("size");
            //
            Node childNode = rigoImre.getNode(jcrPath + filepath + "/" + filename);
            childNode.setProperty("first64Checksum", first64Md5Checksum);
            childNode.setProperty("md5Checksum", md5Checksum);
            childNode.setProperty("size", size);
            childNode.setProperty("extractFrom", jcrPath);
        }
    }

    private void parseChecksumFile(File file) throws RepositoryException, FileNotFoundException, IOException {
        InputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] buffer = new byte[32];
            if (fis.read(buffer) == 32) {
                String jcrPath = getJcrPath(file);
                rigoImre.getNode(jcrPath).setProperty("md5Checksum", new String(buffer));
            } else {
                throw new RepositoryException("Invalid checksum file!");
            }
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

}
