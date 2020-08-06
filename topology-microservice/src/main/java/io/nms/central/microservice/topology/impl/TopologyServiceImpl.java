package io.nms.central.microservice.topology.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.nms.central.microservice.common.functional.Functional;
import io.nms.central.microservice.common.service.JdbcRepositoryWrapper;
import io.nms.central.microservice.topology.TopologyService;
import io.nms.central.microservice.topology.model.Edge;
import io.nms.central.microservice.topology.model.Face;
import io.nms.central.microservice.topology.model.ModelObjectMapper;
import io.nms.central.microservice.topology.model.Node;
import io.nms.central.microservice.topology.model.PrefixAnn;
import io.nms.central.microservice.topology.model.Route;
import io.nms.central.microservice.topology.model.Vctp;
import io.nms.central.microservice.topology.model.Vlink;
import io.nms.central.microservice.topology.model.VlinkConn;
import io.nms.central.microservice.topology.model.Vltp;
import io.nms.central.microservice.topology.model.Vnode;
import io.nms.central.microservice.topology.model.Vsubnet;
import io.nms.central.microservice.topology.model.Vtrail;
import io.nms.central.microservice.topology.model.Vxc;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.CompositeFutureImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.SQLConnection;

/**
 *
 */
public class TopologyServiceImpl extends JdbcRepositoryWrapper implements TopologyService {

	private static final Logger logger = LoggerFactory.getLogger(TopologyServiceImpl.class);
	private Routing routing;

	public TopologyServiceImpl(Vertx vertx, JsonObject config) {
		super(vertx, config);
		routing = new Routing();
	}

	@Override
	public TopologyService initializePersistence(Handler<AsyncResult<List<Integer>>> resultHandler) {
		List<String> statements = new ArrayList<String>();
		statements.add(ApiSql.CREATE_TABLE_VSUBNET);
		statements.add(ApiSql.CREATE_TABLE_VNODE);
		statements.add(ApiSql.CREATE_TABLE_VLTP);
		statements.add(ApiSql.CREATE_TABLE_VLINK);
		statements.add(ApiSql.CREATE_TABLE_VCTP);
		statements.add(ApiSql.CREATE_TABLE_VLINKCONN);
		statements.add(ApiSql.CREATE_TABLE_VTRAIL);
		statements.add(ApiSql.CREATE_TABLE_VXC);
		statements.add(ApiSql.CREATE_TABLE_PREFIX_ANN);
		statements.add(ApiSql.CREATE_TABLE_FACE);
		statements.add(ApiSql.CREATE_TABLE_ROUTE);
		client.getConnection(connHandler(resultHandler, connection -> {
			connection.batch(statements, r -> {
				resultHandler.handle(r);
				connection.close();
			});
		}));
		return this;
	}


	/********** Vsubnet **********/
	@Override
	public TopologyService addVsubnet(Vsubnet vsubnet, Handler<AsyncResult<Integer>> resultHandler) {
		// logger.debug("addSubnet: "+vsubnet.toString());
		JsonArray params = new JsonArray()
				.add(vsubnet.getName())
				.add(vsubnet.getLabel())
				.add(vsubnet.getDescription())
				.add(new JsonObject(vsubnet.getInfo()).encode())
				.add(vsubnet.getStatus());
		insertAndGetId(params, ApiSql.INSERT_VSUBNET, resultHandler);
		return this;
	}
	@Override
	public TopologyService getVsubnet(String vsubnetId, Handler<AsyncResult<Vsubnet>> resultHandler) {
		this.retrieveOne(vsubnetId, ApiSql.FETCH_VSUBNET_BY_ID)
		.map(option -> option.map(json -> {
			Vsubnet vsubnet = new Vsubnet(json);
			vsubnet.setInfo(new JsonObject(json.getString("info")).getMap());
			return vsubnet;
		}).orElse(null))
			.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVsubnets(Handler<AsyncResult<List<Vsubnet>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VSUBNETS)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vsubnet vsubnet = new Vsubnet(row);
					vsubnet.setInfo(new JsonObject(row.getString("info")).getMap());
					return vsubnet;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVsubnet(String vsubnetId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(vsubnetId, ApiSql.DELETE_VSUBNET, resultHandler);
		return this;
	}
	@Override
	public TopologyService updateVsubnet(String id, Vsubnet vsubnet, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vsubnet.getLabel())
				.add(vsubnet.getDescription())
				.add(new JsonObject(vsubnet.getInfo()).encode())
				.add(vsubnet.getStatus())
				.add(id);
		executeNoResult(params, ApiSql.UPDATE_VSUBNET, resultHandler);
		return this;
	}


	/********** Vnode **********/
	@Override
	public TopologyService addVnode(Vnode vnode, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vnode.getName())
				.add(vnode.getLabel())
				.add(vnode.getDescription())
				.add(new JsonObject(vnode.getInfo()).encode())
				.add(vnode.getStatus())
				.add(vnode.getPosx())
				.add(vnode.getPosy())
				.add(vnode.getLocation())
				.add(vnode.getType())	
				.add(vnode.getVsubnetId());
		insertAndGetId(params, ApiSql.INSERT_VNODE, resultHandler);
		return this;
	}
	@Override
	public TopologyService getVnode(String vnodeId, Handler<AsyncResult<Vnode>> resultHandler) {
		this.retrieveOneNested(vnodeId, ApiSql.FETCH_VNODE_BY_ID)
		.map(option -> option.map(json -> {
			return ModelObjectMapper.toVnodeFromJsonRows(json);
		}).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVnodes(Handler<AsyncResult<List<Vnode>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VNODES)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vnode vnode = new Vnode(row);
					vnode.setInfo(new JsonObject(row.getString("info")).getMap());
					return vnode;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVnodesByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vnode>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.retrieveMany(params, ApiSql.FETCH_VNODES_BY_VSUBNET)
		.map(rawList -> rawList.stream()
				.map(Vnode::new)
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVnode(String vnodeId, Handler<AsyncResult<Void>> resultHandler) {
		Promise<Void> vltpsDeleted = Promise.promise();
		
		JsonArray params = new JsonArray().add(vnodeId);
		retrieveMany(params, InternalSql.FETCH_LTPS_BY_NODE).onComplete(ar -> {
			if (ar.succeeded()) {
				List<JsonObject> ltps = ar.result();
				List<Future> futures = new ArrayList<>();
				for (JsonObject ltp : ltps) {
					Promise<Void> p = Promise.promise();
					futures.add(p.future());				
					deleteVltp(String.valueOf(ltp.getInteger("id")), p);
				}
				CompositeFuture.all(futures).map((Void) null).onComplete(vltpsDeleted);
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
		
		vltpsDeleted.future().onComplete(res -> {
			if (res.succeeded()) {
				removeOne(vnodeId, ApiSql.DELETE_VNODE, resultHandler);
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}
	@Override
	public TopologyService updateVnode(String id, Vnode vnode, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vnode.getLabel())
				.add(vnode.getDescription())
				.add(new JsonObject(vnode.getInfo()).encode())
				.add(vnode.getStatus())
				.add(vnode.getPosx())
				.add(vnode.getPosy())
				.add(vnode.getLocation())
				.add(vnode.getType())
				.add(id);
		executeNoResult(params, ApiSql.UPDATE_VNODE, resultHandler);
		return this;
	}


	/********** Vltp **********/
	@Override 
	public TopologyService addVltp(Vltp vltp, Handler<AsyncResult<Integer>> resultHandler) {
		if (! isValidMACAddress((String)vltp.getInfo().get("port"))) {
			resultHandler.handle(Future.failedFuture("Port must be a MAC address"));
			return this;
		}
		getVnode(String.valueOf(vltp.getVnodeId()), ar -> {
			if (ar.succeeded()) {
				Vnode vnode = ar.result();
				if (vnode.getId() > 0) {
					JsonArray params = new JsonArray()
							.add(vltp.getName())
							.add(vltp.getLabel())
							.add(vltp.getDescription())
							.add(new JsonObject(vltp.getInfo()).encode())
							.add(vnode.getStatus())
							.add(vltp.isBusy())
							.add(vltp.getVnodeId());
					insertAndGetId(params, ApiSql.INSERT_VLTP, resultHandler);
				} else {
					resultHandler.handle(Future.failedFuture(new IllegalStateException("Node not found")));
				}
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
		return this;
	}
	@Override
	public TopologyService getVltp(String vltpId, Handler<AsyncResult<Vltp>> resultHandler) {
		this.retrieveOneNested(vltpId, ApiSql.FETCH_VLTP_BY_ID)
		.map(option -> option.map(json -> {
			return ModelObjectMapper.toVltpFromJsonRows(json);
		}).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVltps(Handler<AsyncResult<List<Vltp>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VLTPS)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vltp vltp = new Vltp(row);
					vltp.setInfo(new JsonObject(row.getString("info")).getMap());
					return vltp;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVltpsByVnode(String vnodeId, Handler<AsyncResult<List<Vltp>>> resultHandler) {
		JsonArray params = new JsonArray().add(vnodeId);
		this.retrieveMany(params, ApiSql.FETCH_VLTPS_BY_VNODE)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vltp vltp = new Vltp(row);
					vltp.setInfo(new JsonObject(row.getString("info")).getMap());
					return vltp;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVltp(String vltpId, Handler<AsyncResult<Void>> resultHandler) {
		Promise<Void> linkDeleted = Promise.promise();
		
		JsonArray params = new JsonArray().add(vltpId);
		retrieveOne(params, InternalSql.FETCH_LINK_BY_LTP)
			.map(option -> option.orElse(null))
			.onComplete(ar -> {
				if (ar.succeeded()) {
					if (ar.result() != null) {
						JsonObject link = ar.result();
						deleteVlink(String.valueOf(link.getInteger("id")), linkDeleted);
					} else {
						linkDeleted.complete();
						// resultHandler.handle(Future.succeededFuture());
					}
				} else {
					resultHandler.handle(Future.failedFuture(ar.cause()));
				}
			});
		
		linkDeleted.future().onComplete(res -> {
			if (res.succeeded()) {
				removeOne(vltpId, ApiSql.DELETE_VLTP, resultHandler);
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}
	@Override
	public TopologyService updateVltp(String id, Vltp vltp, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vltp.getLabel())
				.add(vltp.getDescription())
				.add(new JsonObject(vltp.getInfo()).encode())
				.add(vltp.getStatus())
				.add(vltp.isBusy())				
				.add(id);
		executeNoResult(params, ApiSql.UPDATE_VLTP, resultHandler);
		return this;
	}


	/********** Vctp **********/	
	@Override
	public TopologyService addVctp(Vctp vctp, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vctp.getName())
				.add(vctp.getLabel())
				.add(vctp.getDescription())
				.add(new JsonObject(vctp.getInfo()).encode())
				.add(vctp.getVltpId());
		insertAndGetId(params, ApiSql.INSERT_VCTP, resultHandler);
		return this;
	}
	@Override
	public TopologyService getVctp(String vctpId, Handler<AsyncResult<Vctp>> resultHandler) {
		this.retrieveOne(vctpId, ApiSql.FETCH_VCTP_BY_ID)
		.map(option -> option.map(json -> {
			Vctp vctp = new Vctp(json);
			vctp.setInfo(new JsonObject(json.getString("info")).getMap());
			return vctp;
		}).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVctps(Handler<AsyncResult<List<Vctp>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VCTPS)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vctp vctp = new Vctp(row);
					vctp.setInfo(new JsonObject(row.getString("info")).getMap());
					return vctp;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVctpsByVltp(String vltpId, Handler<AsyncResult<List<Vctp>>> resultHandler) {
		JsonArray params = new JsonArray().add(vltpId);
		this.retrieveMany(params, ApiSql.FETCH_VCTPS_BY_VLTP)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vctp vctp = new Vctp(row);
					vctp.setInfo(new JsonObject(row.getString("info")).getMap());
					return vctp;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVctpsByVnode(String vnodeId, Handler<AsyncResult<List<Vctp>>> resultHandler) {
		JsonArray params = new JsonArray().add(vnodeId);
		this.retrieveMany(params, ApiSql.FETCH_VCTPS_BY_VNODE)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vctp vctp = new Vctp(row);
					vctp.setInfo(new JsonObject(row.getString("info")).getMap());
					return vctp;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVctp(String vctpId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(vctpId, ApiSql.DELETE_VCTP, resultHandler);
		return this;
	}
	@Override
	public TopologyService updateVctp(String id, Vctp vctp, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vctp.getLabel())
				.add(vctp.getDescription())
				.add(new JsonObject(vctp.getInfo()).encode())
				.add(id);
		executeNoResult(params, ApiSql.UPDATE_VCTP, resultHandler);
		return this;
	}


	/********** Vlink **********/
	@Override 
	public TopologyService addVlink(Vlink vlink, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vlink.getName())
				.add(vlink.getLabel())
				.add(vlink.getDescription())
				.add(new JsonObject(vlink.getInfo()).encode())
				.add(vlink.getStatus())
				.add(vlink.getType())
				.add(vlink.getSrcVltpId())
				.add(vlink.getDestVltpId());
		JsonArray updSrcLtp = new JsonArray().add(true).add(vlink.getSrcVltpId());
		JsonArray updDestLtp = new JsonArray().add(true).add(vlink.getDestVltpId());

		Future<SQLConnection> f =  txnBegin();
		Future<Integer> fId = f.compose(r -> txnExecute(f.result(), ApiSql.INSERT_VLINK, params));
		fId.compose(r -> txnExecuteNoResult(f.result(), InternalSql.UPDATE_LTP_BUSY, updSrcLtp))
		.compose(r -> txnExecuteNoResult(f.result(), InternalSql.UPDATE_LTP_BUSY, updDestLtp))
		.compose(r -> txnEnd(f.result()))
		.map(fId.result())
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVlink(String vlinkId, Handler<AsyncResult<Vlink>> resultHandler) {
		this.retrieveOneNested(vlinkId, ApiSql.FETCH_VLINK_BY_ID)
		.map(option -> option.map(json -> {
			return ModelObjectMapper.toVlinkFromJsonRows(json);
		}).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVlinks(Handler<AsyncResult<List<Vlink>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VLINKS)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vlink vlink = new Vlink(row);
					vlink.setInfo(new JsonObject(row.getString("info")).getMap());
					return vlink;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVlinksByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vlink>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.retrieveMany(params, ApiSql.FETCH_VLINKS_BY_VSUBNET)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vlink vlink = new Vlink(row);
					vlink.setInfo(new JsonObject(row.getString("info")).getMap());
					return vlink;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVlink(String vlinkId, Handler<AsyncResult<Void>> resultHandler) {
		Promise<Void> vlcsDeleted = Promise.promise();
		
		JsonArray params = new JsonArray().add(vlinkId);
		retrieveMany(params, InternalSql.FETCH_LCS_BY_LINK).onComplete(ar -> {
			if (ar.succeeded()) {
				List<JsonObject> lcs = ar.result();
				List<Future> futures = new ArrayList<>();
				for (JsonObject lc : lcs) {
					Promise<Void> p = Promise.promise();
					futures.add(p.future());				
					deleteVlinkConn(String.valueOf(lc.getInteger("id")), p);
				}
				CompositeFuture.all(futures).map((Void) null).onComplete(vlcsDeleted);
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
		
		vlcsDeleted.future().onComplete(res -> {
			if (res.succeeded()) {
				retrieveOne(vlinkId, ApiSql.FETCH_VLINK_BY_ID)
					.map(option -> option.map(Vlink::new).orElse(null))
					.onComplete(ar -> {
						if (ar.result() != null) {
							JsonArray delLink = new JsonArray().add(vlinkId);
							JsonArray updSrcLtp = new JsonArray().add(false).add(ar.result().getSrcVltpId());
							JsonArray updDestLtp = new JsonArray().add(false).add(ar.result().getDestVltpId());
							Future<SQLConnection> f =  txnBegin();
							f.compose(r -> txnExecuteNoResult(f.result(), ApiSql.DELETE_VLINK, delLink))
							// TODO: use one SQL statement
								.compose(r -> txnExecuteNoResult(f.result(), InternalSql.UPDATE_LTP_BUSY, updSrcLtp))
								.compose(r -> txnExecuteNoResult(f.result(), InternalSql.UPDATE_LTP_BUSY, updDestLtp))
								.compose(r -> txnEnd(f.result()))
								.onComplete(resultHandler);
						} else {
							resultHandler.handle(Future.failedFuture("Vlink not found"));
						}
					});
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});		
		return this;
	}
	@Override
	public TopologyService updateVlink(String id, Vlink vlink, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vlink.getLabel())
				.add(vlink.getDescription())
				.add(new JsonObject(vlink.getInfo()).encode())
				.add(vlink.getStatus())
				.add(vlink.getType())
				.add(id);
		executeNoResult(params, ApiSql.UPDATE_VLINK, resultHandler);
		return this;
	}


	/********** VlinkConn **********/
	@Override
	public TopologyService addVlinkConn(VlinkConn vlinkConn, Handler<AsyncResult<Integer>> resultHandler) {
		generateCtps(vlinkConn).onComplete(ar -> {
			if (ar.succeeded()) {
				List<Vctp> vctps = ar.result();
				vlinkConn.setSrcVctpId(vctps.get(0).getId());
				vlinkConn.setDestVctpId(vctps.get(1).getId());

				JsonArray vlc = new JsonArray()
						.add(vlinkConn.getName())
						.add(vlinkConn.getLabel())
						.add(vlinkConn.getDescription())
						.add(new JsonObject(vlinkConn.getInfo()).encode())
						.add(vlinkConn.getStatus())
						.add(vlinkConn.getSrcVctpId())
						.add(vlinkConn.getDestVctpId())
						.add(vlinkConn.getVlinkId());
				insertAndGetId(vlc, ApiSql.INSERT_VLINKCONN, vlcId -> {
					if (vlcId.succeeded()) {
						generateFaces(vlcId.result())
						.map(vlcId.result())
						.onComplete(resultHandler);
					} else {
						resultHandler.handle(Future.failedFuture(vlcId.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
		return this;
	}
	@Override
	public TopologyService getVlinkConn(String vlinkConnId, Handler<AsyncResult<VlinkConn>> resultHandler) {
		this.retrieveOne(vlinkConnId, ApiSql.FETCH_VLINKCONN_BY_ID)
		.map(option -> option.map(json -> {
			VlinkConn vlinkConn = new VlinkConn(json);
			vlinkConn.setInfo(new JsonObject(json.getString("info")).getMap());
			return vlinkConn;
		}).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVlinkConns(Handler<AsyncResult<List<VlinkConn>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VLINKCONNS)
		.map(rawList -> rawList.stream()
				.map(row -> {
					VlinkConn vlinkConn = new VlinkConn(row);
					vlinkConn.setInfo(new JsonObject(row.getString("info")).getMap());
					return vlinkConn;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVlinkConnsByVlink(String vlinkId, Handler<AsyncResult<List<VlinkConn>>> resultHandler) {
		JsonArray params = new JsonArray().add(vlinkId);
		this.retrieveMany(params, ApiSql.FETCH_VLINKCONNS_BY_VLINK)
		.map(rawList -> rawList.stream()
				.map(row -> {
					VlinkConn vlinkConn = new VlinkConn(row);
					vlinkConn.setInfo(new JsonObject(row.getString("info")).getMap());
					return vlinkConn;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVlinkConnsByVsubnet(String vsubnetId, Handler<AsyncResult<List<VlinkConn>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.retrieveMany(params, ApiSql.FETCH_VLINKCONNS_BY_VSUBNET)
		.map(rawList -> rawList.stream()
				.map(row -> {
					VlinkConn vlinkConn = new VlinkConn(row);
					vlinkConn.setInfo(new JsonObject(row.getString("info")).getMap());
					return vlinkConn;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVlinkConn(String vlinkConnId, Handler<AsyncResult<Void>> resultHandler) {
		retrieveOne(vlinkConnId, ApiSql.FETCH_VLINKCONN_BY_ID)
		.map(option -> option.map(VlinkConn::new).orElse(null))
		.onComplete(ar -> {
			if (ar.result() != null) {
				JsonArray delLc = new JsonArray().add(vlinkConnId);
				JsonArray delSrcVctp = new JsonArray().add(ar.result().getSrcVctpId());
				JsonArray delDestVctp = new JsonArray().add(ar.result().getDestVctpId());
				Future<SQLConnection> f = txnBegin();
				f.compose(r -> txnExecuteNoResult(f.result(), ApiSql.DELETE_VLINKCONN, delLc))
				.compose(r -> txnExecuteNoResult(f.result(), ApiSql.DELETE_VCTP, delSrcVctp))
				.compose(r -> txnExecuteNoResult(f.result(), ApiSql.DELETE_VCTP, delDestVctp))
				.compose(r -> txnEnd(f.result()))
				.onComplete(resultHandler);	
			} else {
				resultHandler.handle(Future.failedFuture("VlinkConn not found"));
			}
		});
		return this;
	}
	@Override
	public TopologyService updateVlinkConn(String id, VlinkConn vlinkConn, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vlinkConn.getLabel())
				.add(vlinkConn.getDescription())
				.add(new JsonObject(vlinkConn.getInfo()).encode())
				.add(vlinkConn.getStatus())
				.add(id);
		executeNoResult(params, ApiSql.UPDATE_VLINKCONN, resultHandler);
		return this;
	}


	/********** Vtrail **********/
	@Override
	public TopologyService addVtrail(Vtrail vtrail, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vtrail.getName())
				.add(vtrail.getLabel())
				.add(vtrail.getDescription())
				.add(new JsonObject(vtrail.getInfo()).encode())
				.add(vtrail.getStatus())
				.add(vtrail.getSrcVctpId())
				.add(vtrail.getDestVctpId());
		insertAndGetId(params, ApiSql.INSERT_VTRAIL, resultHandler);
		return this;
	}
	@Override
	public TopologyService getVtrail(String vtrailId, Handler<AsyncResult<Vtrail>> resultHandler) {
		this.retrieveOneNested(vtrailId, ApiSql.FETCH_VTRAIL_BY_ID)
		.map(option -> option.map(json -> {
			return ModelObjectMapper.toVtrailFromJsonRows(json);
		}).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVtrails(Handler<AsyncResult<List<Vtrail>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VTRAILS)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vtrail vtrail = new Vtrail(row);
					vtrail.setInfo(new JsonObject(row.getString("info")).getMap());
					return vtrail;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVtrailsByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vtrail>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.retrieveMany(params, ApiSql.FETCH_VTRAILS_BY_VSUBNET)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vtrail vtrail = new Vtrail(row);
					vtrail.setInfo(new JsonObject(row.getString("info")).getMap());
					return vtrail;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVtrail(String vtrailId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(vtrailId, ApiSql.DELETE_VTRAIL, resultHandler);
		return this;
	}
	@Override
	public TopologyService updateVtrail(String id, Vtrail vtrail, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vtrail.getLabel())
				.add(vtrail.getDescription())
				.add(new JsonObject(vtrail.getInfo()).encode())
				.add(vtrail.getStatus())
				.add(id);
		executeNoResult(params, ApiSql.UPDATE_VTRAIL, resultHandler);
		return this;
	}


	/********** Vxc **********/
	@Override
	public TopologyService addVxc(Vxc vxc, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vxc.getName())
				.add(vxc.getLabel())
				.add(vxc.getDescription())
				.add(new JsonObject(vxc.getInfo()).encode())
				.add(vxc.getStatus())
				.add(vxc.getType())
				.add(vxc.getVnodeId())
				.add(vxc.getVtrailId())
				.add(vxc.getSrcVctpId())
				.add(vxc.getDestVctpId());				
		if(vxc.getDropVctpId() == 0) {
			insertAndGetId(params, ApiSql.INSERT_VXC_1, resultHandler);
		} else {
			params.add(vxc.getDropVctpId());
			insertAndGetId(params, ApiSql.INSERT_VXC, resultHandler);
		}
		return this;
	}
	@Override
	public TopologyService getVxc(String vxcId, Handler<AsyncResult<Vxc>> resultHandler) {
		this.retrieveOne(vxcId, ApiSql.FETCH_VXC_BY_ID)
		.map(option -> option.map(json -> {
			Vxc vxc = new Vxc(json);
			vxc.setInfo(new JsonObject(json.getString("info")).getMap());
			return vxc;
		}).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVxcs(Handler<AsyncResult<List<Vxc>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VXCS)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vxc vxc = new Vxc(row);
					vxc.setInfo(new JsonObject(row.getString("info")).getMap());
					return vxc;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVxcsByVtrail(String vtrailId, Handler<AsyncResult<List<Vxc>>> resultHandler) {
		JsonArray params = new JsonArray().add(vtrailId);
		this.retrieveMany(params, ApiSql.FETCH_VXC_BY_VTRAIL)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vxc vxc = new Vxc(row);
					vxc.setInfo(new JsonObject(row.getString("info")).getMap());
					return vxc;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVxcsByVnode(String vnodeId, Handler<AsyncResult<List<Vxc>>> resultHandler) {
		JsonArray params = new JsonArray().add(vnodeId);
		this.retrieveMany(params, ApiSql.FETCH_VXC_BY_VNODE)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vxc vxc = new Vxc(row);
					vxc.setInfo(new JsonObject(row.getString("info")).getMap());
					return vxc;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVxc(String vxcId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(vxcId, ApiSql.DELETE_VXC, resultHandler);
		return this;
	}
	@Override
	public TopologyService updateVxc(String id, Vxc vxc, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vxc.getLabel())
				.add(vxc.getDescription())
				.add(new JsonObject(vxc.getInfo()).encode())
				.add(vxc.getStatus())
				.add(vxc.getType())
				.add(id);
		executeNoResult(params, ApiSql.UPDATE_VXC, resultHandler);
		return this;
	}


	/********** PrefixAnn **********/ 
	@Override
	public TopologyService addPrefixAnn(PrefixAnn prefixAnn, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(prefixAnn.getName())
				.add(prefixAnn.getOriginId())
				.add(prefixAnn.getAvailable());
		insertAndGetId(params, ApiSql.INSERT_PA, paId -> {
			if (paId.succeeded()) {
				generateRoutesToPrefix(prefixAnn.getName(), ar -> {
					if (ar.succeeded()) {
						resultHandler.handle(Future.succeededFuture(paId.result()));
					} else {
						resultHandler.handle(Future.failedFuture(ar.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(paId.cause()));
			}
		});
		return this;
	}
	@Override
	public TopologyService getPrefixAnn(String prefixAnnId, Handler<AsyncResult<PrefixAnn>> resultHandler) {
		this.retrieveOne(prefixAnnId, ApiSql.FETCH_PA_BY_ID)
		.map(option -> option.map(json -> {
			PrefixAnn pa = new PrefixAnn(json);
			return pa;
		}).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllPrefixAnns(Handler<AsyncResult<List<PrefixAnn>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_PAS)
		.map(rawList -> rawList.stream()
				.map(PrefixAnn::new)
				.collect(Collectors.toList()))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getPrefixAnnsByVsubnet(String vsubnetId, Handler<AsyncResult<List<PrefixAnn>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.retrieveMany(params, ApiSql.FETCH_PAS_BY_VSUBNET)
		.map(rawList -> rawList.stream()
				.map(PrefixAnn::new)
				.collect(Collectors.toList()))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getPrefixAnnsByVnode(String nodeId, Handler<AsyncResult<List<PrefixAnn>>> resultHandler) {
		JsonArray params = new JsonArray().add(nodeId);
		this.retrieveMany(params, ApiSql.FETCH_PAS_BY_NODE)
		.map(rawList -> rawList.stream()
				.map(PrefixAnn::new)
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deletePrefixAnn(String prefixAnnId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(prefixAnnId, ApiSql.DELETE_PREFIX_ANN, resultHandler);
		return this;
	}


	/********** Route **********/
	@Override
	public TopologyService addRoute(Route route, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(route.getPaId())
				.add(route.getNodeId())
				.add(route.getNextHopId())
				.add(route.getFaceId())
				.add(route.getCost())				
				.add(route.getOrigin());				
		insertAndGetId(params, ApiSql.INSERT_ROUTE, resultHandler);
		return this;
	}
	@Override
	public TopologyService getRoute(String routeId, Handler<AsyncResult<Route>> resultHandler) {
		this.retrieveOne(routeId, ApiSql.FETCH_ROUTE_BY_ID)
		.map(option -> option.map(Route::new).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllRoutes(Handler<AsyncResult<List<Route>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_ROUTES)
		.map(rawList -> rawList.stream()
				.map(Route::new)
				.collect(Collectors.toList()))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getRoutesByVsubnet(String vsubnetId, Handler<AsyncResult<List<Route>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.retrieveMany(params, ApiSql.FETCH_ROUTES_BY_VSUBNET)
		.map(rawList -> rawList.stream()
				.map(Route::new)
				.collect(Collectors.toList()))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getRoutesByNode(String nodeId, Handler<AsyncResult<List<Route>>> resultHandler) {
		JsonArray params = new JsonArray().add(nodeId);
		this.retrieveMany(params, ApiSql.FETCH_ROUTES_BY_NODE)
		.map(rawList -> rawList.stream()
				.map(Route::new)
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteRoute(String routeId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(routeId, ApiSql.DELETE_ROUTE, resultHandler);
		return this;
	}


	/********** Face **********/
	@Override
	public TopologyService addFace(Face face, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(face.getLabel())
				.add(face.getLocal())
				.add(face.getRemote())
				.add(face.getScheme())
				.add(face.getVctpId())
				.add(face.getVlinkConnId());
		insertAndGetId(params, ApiSql.INSERT_FACE, resultHandler);
		return this;
	}
	@Override
	public TopologyService getFace(String faceId, Handler<AsyncResult<Face>> resultHandler) {
		this.retrieveOne(faceId, ApiSql.FETCH_FACE_BY_ID)
		.map(option -> option.map(Face::new).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllFaces(Handler<AsyncResult<List<Face>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_FACES)
		.map(rawList -> rawList.stream().map(Face::new)
				.collect(Collectors.toList()))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getFacesByVsubnet(String vsubnetId, Handler<AsyncResult<List<Face>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.retrieveMany(params, ApiSql.FETCH_FACES_BY_VSUBNET)
		.map(rawList -> rawList.stream()
				.map(Face::new)
				.collect(Collectors.toList()))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getFacesByNode(String nodeId, Handler<AsyncResult<List<Face>>> resultHandler) {
		JsonArray params = new JsonArray().add(nodeId);
		this.retrieveMany(params, ApiSql.FETCH_FACES_BY_NODE)
		.map(rawList -> rawList.stream().map(Face::new)
				.collect(Collectors.toList()))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteFace(String faceId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(faceId, ApiSql.DELETE_FACE, resultHandler);
		return this;
	}


	/* ---------------- BG ------------------ */
	public Future<List<Vctp>> generateCtps(VlinkConn vlc) {
		Promise<List<Vctp>> promise = Promise.promise();
		Future<List<Vctp>> vctps = retrieveOne(vlc.getVlinkId(), InternalSql.FETCH_CTPGEN_INFO)
				.map(option -> option.orElseGet(JsonObject::new))
				.compose(info -> routing.computeCtps(vlc.getName(), info));

		vctps.compose(f -> insertVctps(f))
		.onComplete(promise);
		return promise.future();
	}

	public Future<List<Face>> generateFaces(int linkConnId) {
		Promise<List<Face>> promise = Promise.promise();
		Future<List<Face>> faces = retrieveOne(linkConnId, InternalSql.FETCH_FACEGEN_INFO)
				.map(option -> option.orElseGet(JsonObject::new))
				.compose(info -> routing.computeFaces(info));
		// TODO: test if any update
		faces.compose(f -> upsertFaces(f))
		.onSuccess(r -> {
			generateAllRoutes(ar -> {
				if (ar.succeeded()) {
					promise.complete(faces.result());
				} else {
					promise.fail(ar.cause());
				}
			});
		})
		.onFailure(e -> {
			promise.fail(e.getCause());
		});
		return promise.future();
	}
	@Override
	public TopologyService generateRoutesToPrefix(String name, Handler<AsyncResult<List<Route>>> resultHandler) {
		Future<List<Node>> nodes = this.retrieveAll(InternalSql.FETCH_ROUTEGEN_NODES)
				.map(rawList -> rawList.stream()
						.map(Node::new)
						.collect(Collectors.toList()));
		Future<List<Edge>> edges =this.retrieveAll(InternalSql.FETCH_ROUTEGEN_LCS)
				.map(rawList -> rawList.stream()
						.map(Edge::new)
						.collect(Collectors.toList()));
		JsonArray params = new JsonArray().add(name);
		Future<List<PrefixAnn>> pas = this.retrieveMany(params, InternalSql.FETCH_ROUTEGEN_PAS_BY_NAME)
				.map(rawList -> rawList.stream()
						.map(PrefixAnn::new)
						.collect(Collectors.toList()));

		Future<List<Route>> routes = CompositeFuture
				.all(Arrays.asList(nodes, edges, pas))
				.compose(r -> routing.computeRoutes(nodes.result(), edges.result(), pas.result()));
		routes.compose(r -> upsertRoutes(r, false))
			.map(routes.result())
			.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService generateAllRoutes(Handler<AsyncResult<List<Route>>> resultHandler) {
		Future<List<Node>> nodes = this.retrieveAll(InternalSql.FETCH_ROUTEGEN_NODES)
				.map(rawList -> rawList.stream()
						.map(Node::new)
						.collect(Collectors.toList()));
		Future<List<Edge>> edges =this.retrieveAll(InternalSql.FETCH_ROUTEGEN_LCS)
				.map(rawList -> rawList.stream()
						.map(Edge::new)
						.collect(Collectors.toList()));
		Future<List<PrefixAnn>> pas = this.retrieveAll(InternalSql.FETCH_ROUTEGEN_ALL_PAS)
				.map(rawList -> rawList.stream()
						.map(PrefixAnn::new)
						.collect(Collectors.toList()));

		Future<List<Route>> routes = CompositeFuture
				.all(Arrays.asList(nodes, edges, pas))
				.compose(r -> routing.computeRoutes(nodes.result(), edges.result(), pas.result()));
		routes.compose(r -> upsertRoutes(r, true))
			.map(routes.result())
			.onComplete(resultHandler);

		return this;
	}

	private Future<List<Integer>> upsertRoutes(List<Route> routes, boolean clean) {
		Promise<List<Integer>> promise = Promise.promise();

		Promise<Void> pClean = Promise.promise();
		if (clean) {			
			removeAll(InternalSql.DELETE_ALL_ROUTES, pClean);
		} else {
			pClean.complete();
		}

		pClean.future().onComplete(ar -> {
			if (ar.succeeded()) {
				List<Future<Integer>> fts = new ArrayList<>();
				for (Route r : routes) {
					Promise<Integer> p = Promise.promise();
					fts.add(p.future());
					JsonArray params = new JsonArray()
						.add(r.getPaId())
						.add(r.getNodeId())
						.add(r.getNextHopId())
						.add(r.getFaceId())
						.add(r.getCost())				
						.add(r.getOrigin());
					upsert(params, InternalSql.UPDATE_ROUTE, p.future());
				}
				Functional.allOfFutures(fts).onComplete(promise);
			} else {
				promise.fail(ar.cause());
			}
		});
		return promise.future();
	}

	private Future<List<Integer>> upsertFaces(List<Face> faces) {
		Promise<List<Integer>> promise = Promise.promise();
		List<Future<Integer>> fts = new ArrayList<>();
		for (Face face : faces) {
			Promise<Integer> p = Promise.promise();
			fts.add(p.future());
			JsonArray params = new JsonArray()
					.add(face.getLabel())
					.add(face.getStatus())
					.add(face.getLocal())
					.add(face.getRemote())
					.add(face.getScheme())
					.add(face.getVctpId())
					.add(face.getVlinkConnId());
			upsert(params, InternalSql.UPDATE_FACE, p.future());
		}
		Functional.allOfFutures(fts).onComplete(promise);
		return promise.future();
	}

	private Future<List<Vctp>> insertVctps(List<Vctp> vctps) {
		// TODO: TXN
		Promise<List<Vctp>> promise = Promise.promise();

		List<Future<Void>> fts = new ArrayList<>();
		for (Vctp vctp : vctps) {
			Promise<Void> p = Promise.promise();
			fts.add(p.future());
			JsonArray params = new JsonArray()
					.add(vctp.getName())
					.add(vctp.getLabel())
					.add(vctp.getDescription())
					.add(new JsonObject().encode())
					.add(vctp.getVltpId());
			insertAndGetId(params, ApiSql.INSERT_VCTP, id -> {
				if (id.succeeded()) {
					vctp.setId(id.result());
					p.complete();
				} else {
					p.fail(id.cause());
				}
			});
		}
		CompositeFutureImpl.all(fts.toArray(new Future[fts.size()]))
			.map(vctps)
			.onComplete(promise);
		return promise.future();
	}

	@Override
	public TopologyService updateNodeStatus(int id, String status, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray().add(status).add(id);
		update(params, InternalSql.UPDATE_NODE_STATUS, u -> {
			if (u.succeeded()) {
				params.remove(0);
				retrieveMany(params, InternalSql.FETCH_LTPS_BY_NODE).onComplete(ar -> {
					if (ar.succeeded()) {
						// Update LTPs status
						List<JsonObject> ltps = ar.result();
						List<Future> futures = new ArrayList<>();
						for (JsonObject ltp : ltps) {
							Promise<Void> p = Promise.promise();
							futures.add(p.future());				
							updateLtpStatus(ltp.getInteger("id"), status).onComplete(p);
						}
						// Update PAs availability
						// TODO: get nodeId if not provided
						Promise<Void> p = Promise.promise();
						futures.add(p.future());			
						JsonArray updPAs = new JsonArray()
								.add((status.equals("UP")))
								.add(id);
						executeNoResult(updPAs, InternalSql.UPDATE_PA_STATUS_BY_NODE, p);

						CompositeFuture.all(futures).map((Void) null).onComplete(resultHandler);
					} else {
						resultHandler.handle(Future.failedFuture(ar.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(u.cause()));
			}
		});
		return this;
	}

	public Future<Void> updateLtpStatus(int id, String status) {
		Promise<Void> promise = Promise.promise();
		JsonArray params = new JsonArray().add(status).add(id);
		update(params, InternalSql.UPDATE_LTP_STATUS, u -> {
			if (u.succeeded()) {
				params.remove(0);
				retrieveOne(params, InternalSql.FETCH_LINK_BY_LTP)
				.map(option -> option.orElse(null))
				.onComplete(ar -> {
					if (ar.succeeded()) {
						if (ar.result() != null) {
							JsonObject link = ar.result();
							updateLinkStatus(link.getInteger("id"), status).onComplete(promise);
						} else {
							promise.complete();
						}
					} else {
						promise.fail("Failed to fetch Link");
					}
				});
			} else {
				promise.fail(u.cause());
			}
		});
		return promise.future();
	}

	public Future<Void> updateLinkStatus(int id, String status) {
		Promise<Void> promise = Promise.promise();

		Promise<Boolean> linkStatus = Promise.promise();
		if (status.equals("UP")) {
			JsonArray params = new JsonArray().add(id);
			retrieveMany(params, InternalSql.FETCH_LINK_UP).onComplete(ar -> {
				if (ar.succeeded()) {
					linkStatus.complete((ar.result().size() == 2));
				} else {
					linkStatus.fail("Failed to fetch LTPs by link");
				}
			});
		} else {
			linkStatus.complete(true);
		}

		linkStatus.future().onSuccess(ar -> {
			if (ar) {
				JsonArray params = new JsonArray().add(status).add(id);
				update(params, InternalSql.UPDATE_LINK_STATUS, u -> {
					if (u.succeeded()) {
						params.remove(0);
						retrieveMany(params, InternalSql.FETCH_LCS_BY_LINK).onComplete(res -> {
							if (res.succeeded()) {
								List<JsonObject> lcs = res.result();
								List<Future> futures = new ArrayList<>();
								for (JsonObject lc : lcs) {
									Promise<Void> p = Promise.promise();
									futures.add(p.future());				
									updateLcStatus(lc.getInteger("id"), status).onComplete(p);
								}
								CompositeFuture.all(futures).map((Void) null).onComplete(promise);
							} else {
								promise.fail("Failed to fetch LinkConns");
							}
						});
					} else {
						promise.fail(u.cause());
					}
				});
			} else {
				promise.complete();
			}
		});	
		linkStatus.future().onFailure(e -> promise.fail(e));
		return promise.future();
	}

	public Future<Void> updateLcStatus(int id, String status) {
		Promise<Void> promise = Promise.promise();
		JsonArray params = new JsonArray().add(status).add(id);
		update(params, InternalSql.UPDATE_LC_STATUS, u -> {
			if (u.succeeded()) {
				params.remove(0);
				retrieveMany(params, InternalSql.FETCH_FACES_BY_LC).onComplete(ar -> {
					if (ar.succeeded()) {
						List<JsonObject> faces = ar.result();
						JsonArray updFace1 = new JsonArray().add(status).add(faces.get(0).getInteger("id"));
						JsonArray updFace2 = new JsonArray().add(status).add(faces.get(1).getInteger("id"));
						Future<SQLConnection> f = txnBegin();
						f.compose(r -> txnExecuteNoResult(f.result(), InternalSql.UPDATE_FACE_STATUS, updFace1))
							.compose(r -> txnExecuteNoResult(f.result(), InternalSql.UPDATE_FACE_STATUS, updFace2))
							.compose(r -> txnEnd(f.result()))
							.onComplete(promise);
					} else {
						promise.fail("Failed to fetch Faces");
					}
				});
			} else {
				promise.fail(u.cause());
			}
		});
		return promise.future();
	}
	
	private boolean isValidMACAddress(String str) {
		if (str == null) {
			return false;
		}
		String regex = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(str);
		return m.matches();
	}
}





