/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.jackrabbit.poc;

import gui.JCRForm;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

/**
 *
 * @author tbuczko
 */
public class FTPParser {

    FTPClient client;
    RigoImre rigoImre;
    String jcrServerNodePath;
    private static Logger logger = Logger.getLogger(JCRForm.class.getName());

    public FTPParser(String host, int port, String user, String pass, String root, RigoImre rigoImre) {
        this.rigoImre = rigoImre;
        jcrServerNodePath = "/downloadservers/" + host;
        try {
            client = new FTPClient();
            client.setControlEncoding("UTF-8");
            client.connect(host, port);
            client.login(user, pass);
            parse(root);
            rigoImre.getSession().save();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    private void parse(String workingDirectory) throws RepositoryException, IOException, Exception {
        Node node = rigoImre.getNode(jcrServerNodePath + workingDirectory);
        String encoded = new String(workingDirectory.getBytes("UTF-8"), "ISO-8859-1");
        FTPFile[] files = client.listFiles(encoded);
        logger.log(Level.INFO, "Parse {0} item in directory: {1}", new Object[]{files.length, workingDirectory});
        for (FTPFile file : files) {
            if (file.isDirectory()) {
                parse(workingDirectory + "/" + file.getName());
            } else {
                String remote = workingDirectory + "/" + file.getName();
                if (remote.endsWith(".md5") || remote.endsWith(".md5sum")) {
                    parseChecksumFile(remote);
                } else if (remote.endsWith(".tgz.desc") || remote.endsWith(".zip.desc")) {
                    parseDescriptorFile(remote);
                } else {
                    parseFile(remote, file.getSize());
                }
            }
        }
    }

    private void parseFile(String remote, long size) throws IOException, RepositoryException, NoSuchAlgorithmException {
        InputStream is = getInputStream(remote);
        Node node = rigoImre.getNode(jcrServerNodePath + remote);
        node.setProperty("first64Checksum", Utils.getFirst64KBChecksum(is));
        node.setProperty("size", size);
        is.close();
        if (!client.completePendingCommand()) {
            logger.log(Level.SEVERE, "completePendingCommand Reply: {0}", client.getReplyString());
        }
    }

    private void parseDescriptorFile(String remote) throws IOException {
        InputStream is = getInputStream(remote);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String str;
            while ((str = reader.readLine()) != null) {
                if (str.startsWith("  {\"filename\":")) {
//                        str = 
                }
            }
        } finally {
            try {
                is.close();
            } catch (Throwable ignore) {
            }
        }
    }

    private void parseChecksumFile(String remote) throws IOException, RepositoryException {
        logger.log(Level.INFO, "Read checksum file: {0}", new Object[]{remote});
        InputStream is = getInputStream(remote);
        byte[] buffer = new byte[32];
        int loop = 0;
        int read = is.read(buffer);
        logger.log(Level.INFO, "Bytes read from .md5 file: {0}", read);
        while (read != 32 && loop++ < 3) {
            is.close();
            if (!client.completePendingCommand()) {
                logger.log(Level.SEVERE, "completePendingCommand Reply: {0}", client.getReplyString());
            }
            is = getInputStream(remote);
            read = is.read(buffer);
            logger.log(Level.INFO, "Bytes read from .md5 file: {0}", read);
        }
        if (read != 32) {
            throw new RuntimeException("Invalid checksum file!");
        }
        //
        String nodeName = jcrServerNodePath + remote;
        if (nodeName.endsWith(".md5")) {
            nodeName = nodeName.substring(0, nodeName.lastIndexOf(".")).concat(".fbl");
        } else {
            nodeName = nodeName.substring(0, nodeName.lastIndexOf("."));
        }
        rigoImre.getNode(nodeName).setProperty("md5Checksum", new String(buffer));
    }

    private InputStream getInputStream(String remote) {
        InputStream is = null;
        try {
            String encoded = new String(remote.getBytes("UTF-8"), "ISO-8859-1");
            is = client.retrieveFileStream(encoded);
            logger.log(Level.INFO, "Retrieve File Stream({0}) Reply: {1}", new Object[]{remote, client.getReplyString()});
            int loop = 0;
            while (is == null && loop++ < 3) {
                is = client.retrieveFileStream(remote);
                logger.log(Level.INFO, "Retry [{0}] Retrieve File Stream({1}) Reply: {2}", new Object[]{loop, remote, client.getReplyString()});
            }
            if (is == null) {
                throw new RuntimeException(client.getReplyString());
            }
            return is;
        } catch (Exception ex) {
            ex.printStackTrace();
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ex1) {
                    ex1.printStackTrace();
                    throw new RuntimeException(ex1);
                }
            }
            throw new RuntimeException(ex);
        }
    }

}
