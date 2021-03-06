package io.nms.central.microservice.topology;

import java.util.List;

import io.nms.central.microservice.notification.model.Status.StatusEnum;
import io.nms.central.microservice.topology.model.PrefixAnn;
import io.nms.central.microservice.topology.model.Route;
import io.nms.central.microservice.topology.model.Vconnection;
import io.nms.central.microservice.topology.model.Vctp;
import io.nms.central.microservice.topology.model.Vctp.ConnTypeEnum;
import io.nms.central.microservice.topology.model.Vlink;
import io.nms.central.microservice.topology.model.VlinkConn;
import io.nms.central.microservice.topology.model.Vltp;
import io.nms.central.microservice.topology.model.Vnode;
import io.nms.central.microservice.topology.model.Vsubnet;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A service interface managing topology.
 * <p>
 * This service is an event bus service (aka. service proxy)
 * </p>
 */
@VertxGen
@ProxyGen
public interface TopologyService {

	/**
	 * The name of the event bus service.
	 */
	String SERVICE_NAME = "topology-eb-service";

	/**
	 * The address on which the service is published.
	 */
	String SERVICE_ADDRESS = "service.topology";
	
	
	String UI_ADDRESS = "nms.to.ui";
	
	String CONFIG_ADDRESS = "topology.to.config";

	String EVENT_ADDRESS = "topology.event";
	
	
	@Fluent	
	TopologyService initializePersistence(Handler<AsyncResult<Void>> resultHandler);

	
	/* Vsubnet */
	@Fluent	
	TopologyService addVsubnet(Vsubnet vsubnet, Handler<AsyncResult<Integer>> resultHandler);
	
	@Fluent	
	TopologyService getVsubnet(String vsubnetId, Handler<AsyncResult<Vsubnet>> resultHandler);
	
	@Fluent
	TopologyService getAllVsubnets(Handler<AsyncResult<List<Vsubnet>>> resultHandler);
	
	@Fluent	
	TopologyService deleteVsubnet(String vsubnetId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent 
	TopologyService updateVsubnet(String id, Vsubnet vsubnet, Handler<AsyncResult<Void>> resultHandler);
	
	
	/* Vnode */
	@Fluent	
	TopologyService addVnode(Vnode vnode, Handler<AsyncResult<Integer>> resultHandler);
	
	@Fluent	
	TopologyService getVnode(String vnodeId, Handler<AsyncResult<Vnode>> resultHandler);
	
	@Fluent	
	TopologyService getAllVnodes(Handler<AsyncResult<List<Vnode>>> resultHandler);
	
	@Fluent	
	TopologyService getVnodesByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vnode>>> resultHandler);
	
	@Fluent	
	TopologyService deleteVnode(String vnodeId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent 
	TopologyService updateVnode(String id, Vnode vnode, Handler<AsyncResult<Void>> resultHandler);
	
	
	/* Vltp */
	@Fluent	
	TopologyService addVltp(Vltp vltp, Handler<AsyncResult<Integer>> resultHandler);
	
	@Fluent	
	TopologyService getVltp(String vltpId, Handler<AsyncResult<Vltp>> resultHandler);
	
	@Fluent	
	TopologyService getAllVltps(Handler<AsyncResult<List<Vltp>>> resultHandler);
	
	@Fluent	
	TopologyService getVltpsByVnode(String vnodeId, Handler<AsyncResult<List<Vltp>>> resultHandler);
	
	@Fluent	
	TopologyService deleteVltp(String vltpId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent 
	TopologyService updateVltp(String id, Vltp vltp, Handler<AsyncResult<Void>> resultHandler);
	

	/* Vctp */
	@Fluent	
	TopologyService addVctp(Vctp vctp, Handler<AsyncResult<Integer>> resultHandler);
	
	@Fluent	
	TopologyService getVctp(String vctpId, Handler<AsyncResult<Vctp>> resultHandler);
	
	@Fluent	
	TopologyService getAllVctps(Handler<AsyncResult<List<Vctp>>> resultHandler);
	
	@Fluent	
	TopologyService getVctpsByType(String type, Handler<AsyncResult<List<Vctp>>> resultHandler);

	@Fluent	
	TopologyService getVctpsByVltp(String vltpId, Handler<AsyncResult<List<Vctp>>> resultHandler);

	@Fluent	
	TopologyService getVctpsByVctp(String vctpId, Handler<AsyncResult<List<Vctp>>> resultHandler);
	
	@Fluent	
	TopologyService getVctpsByVnode(String vnodeId, Handler<AsyncResult<List<Vctp>>> resultHandler);
	
	@Fluent	
	TopologyService deleteVctp(String vctpId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent 
	TopologyService updateVctp(String id, Vctp vctp, Handler<AsyncResult<Void>> resultHandler);
	
	
	/* Vlink */
	@Fluent	
	TopologyService addVlink(Vlink vlink, Handler<AsyncResult<Integer>> resultHandler);
	
	@Fluent	
	TopologyService getVlink(String vlinkId, Handler<AsyncResult<Vlink>> resultHandler);
	
	@Fluent	
	TopologyService getAllVlinks(Handler<AsyncResult<List<Vlink>>> resultHandler);
	
	@Fluent	
	TopologyService getVlinksByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vlink>>> resultHandler);
	
	@Fluent	
	TopologyService deleteVlink(String vlinkId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent 
	TopologyService updateVlink(String id, Vlink vlink, Handler<AsyncResult<Void>> resultHandler);
	

	/* VlinkConn */
	@Fluent	
	TopologyService addVlinkConn(VlinkConn vlinkConn, Handler<AsyncResult<Integer>> resultHandler);
	
	@Fluent	
	TopologyService getVlinkConn(String vlinkConnId, Handler<AsyncResult<VlinkConn>> resultHandler);
	
	@Fluent	
	TopologyService getAllVlinkConns(Handler<AsyncResult<List<VlinkConn>>> resultHandler);
	
	@Fluent	
	TopologyService getVlinkConnsByVlink(String vlinkId, Handler<AsyncResult<List<VlinkConn>>> resultHandler);
	
	@Fluent	
	TopologyService getVlinkConnsByVsubnet(String vsubnetId, Handler<AsyncResult<List<VlinkConn>>> resultHandler);
	
	@Fluent	
	TopologyService deleteVlinkConn(String vlinkConnId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent 
	TopologyService updateVlinkConn(String id, VlinkConn vlinkConn, Handler<AsyncResult<Void>> resultHandler);


	/* Vconnection */
	@Fluent	
	TopologyService addVconnection(Vconnection vconnection, Handler<AsyncResult<Integer>> resultHandler);
	
	@Fluent	
	TopologyService getVconnection(String vconnectionId, Handler<AsyncResult<Vconnection>> resultHandler);
	
	@Fluent	
	TopologyService getAllVconnections(Handler<AsyncResult<List<Vconnection>>> resultHandler);

	@Fluent	
	TopologyService getVconnectionsByType(String type, Handler<AsyncResult<List<Vconnection>>> resultHandler);
		
	@Fluent	
	TopologyService getVconnectionsByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vconnection>>> resultHandler);
	
	@Fluent	
	TopologyService deleteVconnection(String vconnectionId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent 
	TopologyService updateVconnection(String id, Vconnection vconnection, Handler<AsyncResult<Void>> resultHandler);

	
	/* PrefixAnn */
	@Fluent	
	TopologyService addPrefixAnn(PrefixAnn pa, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent	
	TopologyService getPrefixAnn(String paId, Handler<AsyncResult<PrefixAnn>> resultHandler);
	
	@Fluent
	TopologyService getAllPrefixAnns(Handler<AsyncResult<List<PrefixAnn>>> resultHandler);
	
	@Fluent	
	TopologyService getPrefixAnnsByVsubnet(String vsubnetId, Handler<AsyncResult<List<PrefixAnn>>> resultHandler);
	
	@Fluent
	TopologyService getPrefixAnnsByVnode(String nodeId, Handler<AsyncResult<List<PrefixAnn>>> resultHandler);
	
	@Fluent	
	TopologyService deletePrefixAnn(String paId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent	
	TopologyService deletePrefixAnnByName(int originId, String name, Handler<AsyncResult<Void>> resultHandler);
	
	
	/* Route */
	@Fluent	
	TopologyService addRoute(Route route, Handler<AsyncResult<Integer>> resultHandler);
	
	@Fluent	
	TopologyService getRoute(String routeId, Handler<AsyncResult<Route>> resultHandler);
	
	@Fluent
	TopologyService getAllRoutes(Handler<AsyncResult<List<Route>>> resultHandler);
	
	@Fluent	
	TopologyService getRoutesByVsubnet(String vsubnetId, Handler<AsyncResult<List<Route>>> resultHandler);
	
	@Fluent
	TopologyService getRoutesByNode(String nodeId, Handler<AsyncResult<List<Route>>> resultHandler);
	
	@Fluent	
	TopologyService deleteRoute(String routeId, Handler<AsyncResult<Void>> resultHandler);
	

	/* Status management */
	@Fluent
	TopologyService updateNodeStatus(int id, StatusEnum status, Handler<AsyncResult<Void>> resultHandler);

	@Fluent
	TopologyService updateLtpStatus(int id, StatusEnum status, String op, Handler<AsyncResult<Void>> resultHandler);

	@Fluent
	TopologyService updateCtpStatus(int id, ConnTypeEnum type, StatusEnum status, String op, Handler<AsyncResult<Void>> resultHandler);

	@Fluent
	TopologyService updateLinkStatus(int id, StatusEnum status, String op, Handler<AsyncResult<Void>> resultHandler);

	@Fluent
	TopologyService updateLcStatus(int id, StatusEnum status, String op, Handler<AsyncResult<Void>> resultHandler);

	@Fluent
	TopologyService updateConnectionStatus(int id, StatusEnum status, String op, Handler<AsyncResult<Void>> resultHandler);
}