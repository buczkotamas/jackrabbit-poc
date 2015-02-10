package com.mycompany.jackrabbit.poc;

import com.naviextras.zippy.apis.services.market.ContentSegment;
import com.naviextras.zippy.apis.services.market.FileRO;
import com.naviextras.zippy.apis.services.market.FingerprintRO;
import gui.JCRForm;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
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
public class FingerprintParser {

    private static final Logger logger = Logger.getLogger(JCRForm.class.getName());
    private final String repositoryName;
    private final boolean usePrivateFilestore;
    private final Session session;
    private final ParseResult parseResult = new ParseResult();
    private final String filestoreName;
    private Node fsRootNode;
    private Node pfsRootNode;
    private Node repNode;
    private final FingerprintRO fingerprint;
    private final int CHUNK_SIZE = 1024 * 1024;

    public static class ParseResult {
        public int parsedFile = 0;
        public int newFiles = 0;
    }

    public FingerprintParser(String repositoryName, String filestoreName, FingerprintRO fingerprint, boolean usePrivateFilestore, Session session) {
        this.session = session;
        this.filestoreName = filestoreName;
        this.fingerprint = fingerprint;
        this.repositoryName = repositoryName;
        this.usePrivateFilestore = usePrivateFilestore;
    }

    public ParseResult parse() throws RepositoryException {
        fsRootNode = JcrUtils.getOrCreateByPath(filestoreName, null, session);
        // fsRootNode = JcrUtils.getOrCreateByPath(filestoreName, JcrConstants.NT_FOLDER, session);
        repNode = JcrUtils.getOrCreateByPath(repositoryName, null, session);
        // repositoryNode = JcrUtils.getOrCreateByPath(repositoryName, JcrConstants.NT_FOLDER, session);
        repNode.setProperty("checksum", fingerprint.getChecksum());
        if (usePrivateFilestore) {
            pfsRootNode = JcrUtils.getOrCreateByPath(repNode.getParent(), "filestore", false, null, null, false);
        } else {
            pfsRootNode = fsRootNode;
        }
        for (FileRO file : fingerprint.getFiles()) {
            parseResult.parsedFile++;
            Node fNode = JcrUtils.getOrCreateByPath(repNode, file.getPath() + "/" + file.getName(), false, null, null, false);
            if (!file.isDirectory()) {
                Node fsNode = getFilestoreNode(fsRootNode, file, false);
                if (fsNode == null) {
                    fsNode = getFilestoreNode(pfsRootNode, file, true);
                }
                fNode.setProperty(JcrConstants.JCR_CONTENT, fsNode);
            }
        }
        return parseResult;
    }

    private Node getFilestoreNode(Node fsRootNode, FileRO file, boolean createIfNotExists) throws RepositoryException {

        String fsPath = fsRootNode.getPath();
        String md5Checksum = file.getChecksum() == null ? null : file.getChecksum().toLowerCase();
        String first64Checksum = file.getFirst64Checksum() == null ? null : file.getFirst64Checksum().toLowerCase();
        String size = Long.toString(file.getSize());
        ContentSegment contentSegment = Utils.getLatestContentSegment(file);
        String contentId = null;
        String buildTimestamp = null;
        if (contentSegment != null) {
            contentId = Long.toString(contentSegment.getContentId());
            buildTimestamp = Long.toString(contentSegment.getContentTimestamp().getTime());
        }

        String expression;
        if (first64Checksum != null && md5Checksum != null && contentSegment != null) {
            expression = "SELECT * FROM [nt:base] WHERE ISDESCENDANTNODE([" + fsPath + "]) AND "
                    + "(([md5Checksum]='" + md5Checksum + "' OR [first64Checksum]='" + first64Checksum + "') OR "
                    + "([contentId]=CAST(" + contentId + " AS LONG) AND [buildTimestamp]=CAST(" + buildTimestamp + " AS LONG)))"
                    + " AND [size]=CAST(" + size + " AS LONG)";
        } else if (first64Checksum != null && contentSegment != null) {
            expression = "SELECT * FROM [nt:base] WHERE ISDESCENDANTNODE([" + fsPath + "]) AND "
                    + "([first64Checksum]='" + first64Checksum + "' OR "
                    + "([contentId]=CAST(" + contentId + " AS LONG) AND [buildTimestamp]=CAST(" + buildTimestamp + " AS LONG)))"
                    + " AND [size]=CAST(" + size + " AS LONG)";
        } else if (first64Checksum != null && md5Checksum != null) {
            expression = "SELECT * FROM [nt:base] WHERE ISDESCENDANTNODE([" + fsPath + "]) AND ([md5Checksum]='" + md5Checksum + "'"
                    + " OR [first64Checksum]='" + first64Checksum + "') AND [size]=CAST(" + size + " AS LONG))";
        } else if (md5Checksum != null) {
            expression = "SELECT * FROM [nt:base] WHERE ISDESCENDANTNODE([" + fsPath + "]) AND "
                    + "[md5Checksum]='" + md5Checksum + "' AND [size]=CAST(" + size + " AS LONG)";
        } else if (first64Checksum != null) {
            expression = "SELECT * FROM [nt:base] WHERE ISDESCENDANTNODE([" + fsPath + "]) AND "
                    + "[first64Checksum]='" + first64Checksum + "' AND [size]=CAST(" + size + " AS LONG)";
        } else {
            throw new RuntimeException("File can not be identified! " + file.getPath() + "/" + file.getName());
        }

        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Search: {0}", expression);
        }

        QueryResult result = session.getWorkspace().getQueryManager().createQuery(expression, Query.JCR_SQL2).execute();
        List<Node> nodes = new ArrayList<Node>();
        for (Node node : JcrUtils.getNodes(result)) {
            nodes.add(node);
        }

        if (nodes.size() == 1) {
            // TODO: Add new properties
            return nodes.get(0);
        } else if (nodes.isEmpty()) {
            if (createIfNotExists) {
                parseResult.newFiles++;
                String fsNodeName = UUID.randomUUID().toString();
                Node fsNode = JcrUtils.getNodeIfExists(fsRootNode, fsNodeName);
                while (fsNode != null) {
                    fsNodeName = UUID.randomUUID().toString();
                    fsNode = JcrUtils.getNodeIfExists(fsRootNode, fsNodeName);
                }
                fsNode = fsRootNode.addNode(fsNodeName);
//            fsNode = fsRootNode.addNode(fsNodeName, JcrConstants.JCR_CONTENT);   
                if (md5Checksum != null) {
                    fsNode.setProperty("md5Checksum", md5Checksum);
                }
                if (first64Checksum != null) {
                    fsNode.setProperty("first64Checksum", first64Checksum);
                }
                fsNode.setProperty("size", file.getSize());
                String mime = URLConnection.guessContentTypeFromName(file.getName());
                if (mime != null) {
                    fsNode.setProperty(JcrConstants.JCR_MIMETYPE, mime);
                }
                if (contentSegment != null) {
                    fsNode.setProperty("contentId", contentSegment.getContentId());
                    fsNode.setProperty("buildTimestamp", contentSegment.getContentTimestamp().getTime());
                }
                if (file.getCreateTimestamp() != null) {
                    fsNode.setProperty("createTimestamp", file.getCreateTimestamp().getTime());
                }
                if (file.getModifyTimestamp() != null) {
                    fsNode.setProperty("createTimestamp", file.getModifyTimestamp().getTime());
                }
                fsNode.setProperty("complete", false);
                if (file.getSize() > CHUNK_SIZE) {
                    fsNode.setProperty("chunked", true);
                    fsNode.setProperty("complete", false);
                    int chunkCount = (int) (file.getSize() / CHUNK_SIZE);
                    for (int i = 0; i < chunkCount; i++) {
                        Node chunkNode = fsNode.addNode("chunk-" + i);
                        chunkNode.setProperty("offset", CHUNK_SIZE * i);
                        chunkNode.setProperty("complete", false);
                    }
                }
                fsNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
                session.save();
                return fsNode;
            }
            return null;
        } else {
            throw new AssertionError("More then one instance found in filestore[" + fsPath + "]!");
        }
    }
}
