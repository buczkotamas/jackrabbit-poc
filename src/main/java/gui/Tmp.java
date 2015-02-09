/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import com.naviextras.zippy.components.io.api.to.ContentTO;
import com.naviextras.zippy.components.io.api.to.SnapshotTO;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 *
 * @author tbuczko
 */
public class Tmp {
    //    private Node getCatalogeFileNode(FileRO file) throws RepositoryException {
//        QueryManager queryManager = session.getWorkspace().getQueryManager();
//        ContentSegment contentSegment = getLatestContentSegment(file);
//        String expression;
//        if (file.getChecksum() != null) {
//            expression = "SELECT * FROM [nt:base] WHERE ISDESCENDANTNODE([/snapshots]) AND [md5Checksum]='" + file.getChecksum().toLowerCase() + "'";
//        } else if (contentSegment != null) {
//            expression = "SELECT * FROM [nt:base] WHERE ISDESCENDANTNODE([/snapshots]) AND "
//                    + "[buildTimestamp]=CAST(" + contentSegment.getContentTimestamp().getTime() + " AS LONG) AND "
//                    + "[contentId]=CAST(" + contentSegment.getContentId() + " AS LONG) AND "
//                    + "[size]=CAST(" + file.getSize() + " AS LONG)";
//        } else {
//            return null;
//        }
//        logger.log(Level.INFO, "Query: {0}", expression);
//        Query query = queryManager.createQuery(expression, Query.JCR_SQL2);
//        QueryResult result = query.execute();
//        NodeIterator ni = result.getNodes();
//        if (ni.hasNext()) {
//            logger.log(Level.INFO, "File found in cataloge.");
//            return ni.nextNode();
//        }
//        logger.log(Level.INFO, "File not found in cataloge.");
//        return null;
//    }
    
//      public String addSnapshot(SnapshotTO snapshot, List<ContentTO> contents) throws RepositoryException {
//        Node node = getNode("/snapshots/" + snapshot.getSnapshotCode());
//        node.setProperty("buildTimestamp", snapshot.getBuildTimestamp());
//        node.setProperty("contentRelease", snapshot.getContentRelease());
//        node.setProperty("contentTypeLongDescription", snapshot.getContentTypeDescription().getLongDescription());
//        node.setProperty("contentTypeShortDescription", snapshot.getContentTypeDescription().getShortDescription());
//        node.setProperty("contentTypeTitle", snapshot.getContentTypeDescription().getTitle());
//        node.setProperty("contentTypeMime", snapshot.getContentTypeMime());
//        node.setProperty("pictureCmsKey", snapshot.getPictureCmsKey());
//        for (ContentTO content : contents) {
//            Node cntNode = getNode("/snapshots/" + snapshot.getSnapshotCode() + content.getFilePath() + "/" + content.getFileName());
//            cntNode.setProperty("additionalInfo", content.getAdditionalInfo());
//            cntNode.setProperty("buildTimestamp", content.getBuildTimestamp());
//            cntNode.setProperty("contentId", content.getContentIds().get(0));
//            cntNode.setProperty("contentRelease", content.getContentRelease());
//            cntNode.setProperty("contentTypeCode", content.getContentTypeCode());
//            cntNode.setProperty("contentTypeLocalized", content.getContentTypeLocalized());
//            cntNode.setProperty("contentTypeMime", content.getContentTypeMime());
//            cntNode.setProperty("country", content.getCountry());
//            cntNode.setProperty("downloadLocation", content.getDownloadLocation());
//            cntNode.setProperty("fileName", content.getFileName());
//            cntNode.setProperty("filePath", content.getFilePath());
//            cntNode.setProperty("igoContentRelease", content.getIgoContentRelease());
//            cntNode.setProperty("installPath", content.getInstallPath());
//            cntNode.setProperty("instanceCode", content.getInstanceCode());
//            cntNode.setProperty("latestMandatoryVersion", content.getLatestMandatoryVersion());
//            cntNode.setProperty("md5Checksum", content.getMd5Checksum());
//            cntNode.setProperty("packageCode", content.getPackageCode());
//            cntNode.setProperty("packageName", content.getPackageName());
//            cntNode.setProperty("partitionCount", content.getPartitionCount());
//            cntNode.setProperty("platfrom", content.getPlatfrom());
//            cntNode.setProperty("poiAmount", content.getPoiAmount());
//            cntNode.setProperty("releaseReasonTitle", content.getReleaseReasonTitle());
//            cntNode.setProperty("roadLength", content.getRoadLength());
//            cntNode.setProperty("roadLengthRate", content.getRoadLengthRate());
//            cntNode.setProperty("size", content.getSize());
//            cntNode.setProperty("supplierCode", content.getSupplierCode());
//            cntNode.setProperty("tbScript", content.getTbScript());
//            cntNode.setProperty("version", content.getVersion());
//            cntNode.setProperty("versionString", content.getVersionString());
//            cntNode.setProperty("houseNumber", content.isHouseNumber());
//            cntNode.setProperty("laneInfo", content.isLaneInfo());
//            cntNode.setProperty("postalCode", content.isPostalCode());
//        }
//        session.save();
//        return node.getIdentifier();
//    }
//    
    
//     public void addChannelContentToCataloge(String server, String channel, List<ContentWO> contents, Session session) throws RepositoryException {
//        for (ContentWO content : contents) {
//            Node cntNode = getNode("/cataloge/" + server + "/" + channel + content.getFilePath() + "/" + content.getFileName());
//            cntNode.setProperty("additionalInfo", content.getAdditionalInfo());
//            cntNode.setProperty("buildTimestamp", content.getBuildTimestamp());
//            cntNode.setProperty("contentId", content.getContentIds().get(0));
//            cntNode.setProperty("contentRelease", content.getContentRelease());
//            cntNode.setProperty("contentTypeCode", content.getContentTypeCode());
//            cntNode.setProperty("contentTypeLocalized", content.getContentTypeLocalized());
//            cntNode.setProperty("contentTypeMime", content.getContentTypeMime());
//            cntNode.setProperty("country", content.getCountry());
//            cntNode.setProperty("downloadLocation", content.getDownloadLocation());
//            cntNode.setProperty("fileName", content.getFileName());
//            cntNode.setProperty("filePath", content.getFilePath());
//            cntNode.setProperty("igoContentRelease", content.getIgoContentRelease());
//            cntNode.setProperty("installPath", content.getInstallPath());
//            cntNode.setProperty("instanceCode", content.getInstanceCode());
//            cntNode.setProperty("latestMandatoryVersion", content.getLatestMandatoryVersion());
//            cntNode.setProperty("md5Checksum", content.getMd5Checksum());
//            cntNode.setProperty("packageCode", content.getPackageCode());
//            cntNode.setProperty("packageName", content.getPackageName());
//            cntNode.setProperty("partitionCount", content.getPartitionCount());
//            cntNode.setProperty("platform", content.getPlatform());
//            cntNode.setProperty("poiAmount", content.getPoiAmount());
//            cntNode.setProperty("releaseReasonTitle", content.getReleaseReasonTitle());
//            cntNode.setProperty("roadLength", content.getRoadLength());
//            cntNode.setProperty("roadLengthRate", content.getRoadLengthRate());
//            cntNode.setProperty("size", content.getSize());
//            cntNode.setProperty("supplierCode", content.getSupplierCode());
//            cntNode.setProperty("tbScript", content.getTbScript());
//            cntNode.setProperty("version", content.getVersion());
//            cntNode.setProperty("versionString", content.getVersionString());
//            cntNode.setProperty("houseNumber", content.isHouseNumber());
//            cntNode.setProperty("laneInfo", content.isLaneInfo());
//            cntNode.setProperty("postalCode", content.isPostalCode());
//        }
//        session.save();
//
//    }
}
