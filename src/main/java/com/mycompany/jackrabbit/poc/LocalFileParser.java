package com.mycompany.jackrabbit.poc;

import gui.JCRForm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;

/**
 *
 * @author tbuczko
 */
public class LocalFileParser {

    private static final Logger logger = Logger.getLogger(JCRForm.class.getName());
    private final String downloadLocationPrefix;
    private final String repositoryName;
    private final Session session;
    private final File root;
    private final ParseResult parseResult = new ParseResult();
    private final String filestoreName;
    private Node fsRootNode;
    private Node repositoryNode;
    private boolean parseChecksumFiles = false;
    private boolean parseDescriptorFiles = false;

    public static class ParseResult {

        public int parsedFile = 0;
        public int newFiles = 0;
    }

    public LocalFileParser(String repositoryName, String filestoreName, String downloadLocationPrefix, File root, Session session) {
        this.session = session;
        this.filestoreName = filestoreName;
        this.repositoryName = repositoryName;
        if (downloadLocationPrefix != null) {
            this.downloadLocationPrefix = downloadLocationPrefix.endsWith("/") ? downloadLocationPrefix : (downloadLocationPrefix + "/");
        } else {
            this.downloadLocationPrefix = null;
        }
        this.root = root;
    }

    public ParseResult parse() throws RepositoryException {
        try {
            fsRootNode = JcrUtils.getOrCreateByPath(filestoreName, null, session);
//            fsRootNode = JcrUtils.getOrCreateByPath(filestoreName, JcrConstants.NT_FOLDER, session);
            repositoryNode = JcrUtils.getOrCreateByPath(repositoryName, null, session);
//            repositoryNode = JcrUtils.getOrCreateByPath(repositoryName, JcrConstants.NT_FOLDER, session);
            parse(root);
            return parseResult;

        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RepositoryException(ex);
        } catch (NoSuchAlgorithmException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RepositoryException(ex);
        }
    }

    private String getDownloadLocation(File file) {
        return downloadLocationPrefix + root.getName() + file.getPath().replace(root.getPath(), "");
    }

    private String getRepositoryPath(File file) {
        if (file == root) {
            return repositoryName + "/" + file.getName();
        }
        String jcrPath = repositoryName + "/" + root.getName() + file.getPath().replace(root.getPath(), "").replaceAll("\\\\", "/");
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

    private void parse(File file) throws RepositoryException, IOException, FileNotFoundException, NoSuchAlgorithmException {
        logger.log(Level.INFO, "Parse local file: {0}", new Object[]{file.getPath()});
        if (file.isDirectory()) {
            getNode(getRepositoryPath(file)).setProperty("directory", true);
//            JcrUtils.getOrCreateByPath(getJcrPath(file), "nt:folder", rigoImre.getSession());
            for (File file2 : file.listFiles()) {
                parse(file2);
            }
        } else {
            if (file.getName().endsWith(".md5") || file.getName().endsWith(".md5sum")) {
                if (parseChecksumFiles) {
                    parseChecksumFile(file);
                }
            } else if (file.getName().endsWith(".tgz.desc") || file.getName().endsWith(".zip.desc")) {
                if(parseDescriptorFiles) {
                parseDescriptorFile(file);
                }
            } else {
                parseFile(file);
            }
        }
    }

    private void parseChecksumFile(File file) throws RepositoryException, FileNotFoundException, IOException {
        InputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] buffer = new byte[32];

            if (fis.read(buffer) == 32) {
                Node node = JcrUtils.getOrCreateByPath(getRepositoryPath(file), null, session);
                node.setProperty("md5Checksum", new String(buffer));
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

    private void parseDescriptorFile(File file) throws IOException, RepositoryException, FileNotFoundException, NoSuchAlgorithmException {
        parseResult.parsedFile++;
        Node node = JcrUtils.getOrCreateByPath(getRepositoryPath(file), null, session);
//        Node node = JcrUtils.getOrCreateByPath(getRepositoryPath(file), JcrConstants.NT_LINKEDFILE, session);
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
            Node childNode = JcrUtils.getOrCreateByPath(node, filepath.replaceFirst("/", "") + "/" + filename, false, null, null, false);
            childNode.setProperty("first64Checksum", first64Md5Checksum);
            childNode.setProperty("md5Checksum", md5Checksum);
            childNode.setProperty("size", size);
            childNode.setProperty("extractFrom", node.getPath());
        }
    }

    private void parseFile(File file) throws RepositoryException, FileNotFoundException, NoSuchAlgorithmException, IOException {
        parseResult.parsedFile++;
        Node node = JcrUtils.getOrCreateByPath(getRepositoryPath(file), null, session);
//        Node node = JcrUtils.getOrCreateByPath(getRepositoryPath(file), JcrConstants.NT_LINKEDFILE, session);
        if (downloadLocationPrefix != null) {
            node.setProperty("downloadLocation", getDownloadLocation(file));
        }
        if (fsRootNode != null) {
            node.setProperty(JcrConstants.JCR_CONTENT, getOrCreateFilestoreNode(fsRootNode, file));
        }
    }

    private Node getOrCreateFilestoreNode(Node fsRootNode, File file) throws RepositoryException, IOException, FileNotFoundException, NoSuchAlgorithmException {

        String checksum = Utils.getChecksum(file);
        String first64Checksum = Utils.getFirst64KBChecksum(file);
        String fsPath = fsRootNode.getPath();

        String expression = "SELECT * FROM [nt:base] WHERE ISDESCENDANTNODE([" + fsPath + "]) AND ([md5Checksum]='" + checksum + "'"
                + " OR [first64Checksum]='" + first64Checksum + "') AND [size]=CAST(" + Long.toString(file.length()) + " AS LONG)";
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Search: {0}", expression);
        }

        QueryResult result = session.getWorkspace().getQueryManager().createQuery(expression, Query.JCR_SQL2).execute();
        List<Node> nodes = new ArrayList<Node>();
        for (Node node : JcrUtils.getNodes(result)) {
            nodes.add(node);
        }
        if (nodes.size() == 1) {
            return nodes.get(0);
        } else if (nodes.isEmpty()) {
            parseResult.newFiles++;
            String fsNodeName = UUID.randomUUID().toString();
            Node fsNode = JcrUtils.getNodeIfExists(fsRootNode, fsNodeName);
            while (fsNode != null) {
                fsNodeName = UUID.randomUUID().toString();
                fsNode = JcrUtils.getNodeIfExists(fsRootNode, fsNodeName);
            }
            fsNode = fsRootNode.addNode(fsNodeName);
//            fsNode = fsRootNode.addNode(fsNodeName, JcrConstants.JCR_CONTENT);
            fsNode.setProperty(JcrConstants.JCR_DATA, session.getValueFactory().createBinary(new FileInputStream(file)));
            fsNode.setProperty("md5Checksum", checksum);
            fsNode.setProperty("first64Checksum", first64Checksum);
            fsNode.setProperty("size", file.length());
            fsNode.setProperty("chunked", false);
            String mime = URLConnection.guessContentTypeFromName(file.getName());
            if (mime != null) {
                fsNode.setProperty(JcrConstants.JCR_MIMETYPE, mime);
            }
            fsNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
            fsNode.getSession().save();
            return fsNode;
        } else {
            throw new AssertionError("More then one instance found in filestore[" + fsPath + "]!");
        }
    }

    private Node getNode(String path) throws RepositoryException {
        Node node = JcrUtils.getNodeIfExists(path, session);
        if (node == null) {
            node = JcrUtils.getOrCreateByPath(path, null, session);
            node.addMixin("mix:referenceable");
        }
        return node;
    }
}
