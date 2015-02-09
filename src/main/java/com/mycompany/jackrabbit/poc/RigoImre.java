package com.mycompany.jackrabbit.poc;

import com.naviextras.zippy.apis.services.market.ContentSegment;
import com.naviextras.zippy.apis.services.market.FileRO;
import com.naviextras.zippy.apis.services.market.FingerprintRO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.jackrabbit.commons.JcrUtils;

import java.util.logging.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 *
 * @author tbuczko
 */
public class RigoImre {

    Repository repository;
    Session session;
    private static final Logger logger = Logger.getLogger(RigoImre.class.getName());

    public RigoImre(String jcrServerAddress, String user, String password) throws RepositoryException {
        repository = JcrUtils.getRepository(jcrServerAddress);
        session = repository.login(new SimpleCredentials(user, password.toCharArray()));
    }

    public Session getSession() {
        return session;
    }

    public Node getNode(String path) throws RepositoryException {
        Node node = JcrUtils.getNodeIfExists(path, session);
        if (node == null) {
            node = JcrUtils.getOrCreateByPath(path, null, session);
            node.addMixin("mix:referenceable"); //uuid
        }
        return node;
    }

    public String backupFingerprint(FingerprintRO fingerprint, String name, String regServer, long deviceCode) throws RepositoryException {
        name = name == null ? fingerprint.getChecksum() : name;
        String fpPath = "/backup/" + regServer + "/" + Long.toString(deviceCode) + "/fingerprint/" + name;
        Node node = getNode(fpPath);
        node.setProperty("checksum", fingerprint.getChecksum());
        node.setProperty("xml", Utils.toXMLString(fingerprint));
        for (FileRO file : fingerprint.getFiles()) {
            Node fNode = getNode(fpPath + "/" + file.getPath() + "/" + file.getName());
            ContentSegment contentSegment = Utils.getLatestContentSegment(file);
            fNode.setProperty("md5Checksum", file.getChecksum());
            fNode.setProperty("first64Checksum", file.getFirst64Checksum());
            fNode.setProperty("size", file.getSize());
            if (file.getInstanceCode() != null) {
                fNode.setProperty("instanceCode", file.getInstanceCode());
            }
            fNode.setProperty("createTimestamp", file.getCreateTimestamp().getTime());
            fNode.setProperty("modifyTimestamp", file.getModifyTimestamp().getTime());
            if (contentSegment != null) {
                fNode.setProperty("contentId", contentSegment.getContentId());
                fNode.setProperty("buildTimestamp", contentSegment.getContentTimestamp().getTime());
            }
            if (!file.isDirectory()) {
                boolean missing = true;
                for (Node fsNode : getFilestoreNodes(file)) {
                    for (Property property : JcrUtils.getProperties(fsNode)) {
                        if (property.getType() == PropertyType.BINARY && property.getName().equals("jcr:data")) {
//                        fNode.setProperty("reference", fsNode);
                            fNode.setProperty("ref", fsNode.getPath() + "/" + fsNode.getName());
                            fNode.setProperty("complete", true);
                            missing = false;
                            break;
                        }
                        if (property.getType() == PropertyType.STRING && property.getName().equals("downloadLocation")) {
//                        fNode.setProperty("reference", fsNode);
                            fNode.setProperty("ref", fsNode.getPath() + "/" + fsNode.getName());
                            fNode.setProperty("complete", true);
                            missing = false;
                            break;
                        }
                    }
                }
                if (missing) {
                    fNode.setProperty("complete", false);
                    Node fsNode = getNode("/backup/" + regServer + "/" + Long.toString(deviceCode) + "/filestore/" + file.getPath() + "/" + file.getName());
                    fsNode.setProperty("complete", false);
                    fsNode.setProperty("md5Checksum", file.getChecksum());
                    fsNode.setProperty("first64Checksum", file.getFirst64Checksum());
                    fsNode.setProperty("size", file.getSize());
                    int chunkCount = (int) file.getSize() / (1024 * 1024);
                    for (int i = 0; i < chunkCount; i++) {
                        Node chunkNode = fsNode.addNode("chunk");
                        chunkNode.setProperty("index", i);
                        chunkNode.setProperty("complete", false);
                    }
//                fNode.setProperty("reference", fsNode);
                    fNode.setProperty("ref", fsNode.getPath() + "/" + fsNode.getName());
                }
            }
        }
        session.save();
        return node.getIdentifier();
    }

    public List<Node> getFilestoreNodes(FileRO file) throws RepositoryException {
        String expression;
        if (file.getChecksum() != null) {
            expression = "SELECT * FROM [nt:base] WHERE ISDESCENDANTNODE([/filestore]) AND [md5Checksum]='" + file.getChecksum().toLowerCase() + "'";
        } else if (file.getFirst64Checksum() != null && file.getSize() != 0) {
            expression = "SELECT * FROM [nt:base] WHERE ISDESCENDANTNODE([/filestore]) AND "
                    + "[first64Checksum]='" + file.getFirst64Checksum().toLowerCase() + "' AND "
                    + "[size]=CAST(" + file.getSize() + " AS LONG)";
        } else {
            return Collections.EMPTY_LIST;
        }
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(expression, Query.JCR_SQL2);
        QueryResult result = query.execute();
        List<Node> nodes = new ArrayList<Node>();
        for (Node node : JcrUtils.getNodes(result)) {
            nodes.add(node);
        }
        return nodes;
    }

}
