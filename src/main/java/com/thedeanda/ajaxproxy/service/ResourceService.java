package com.thedeanda.ajaxproxy.service;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.thedeanda.ajaxproxy.cache.LruCache;
import com.thedeanda.ajaxproxy.http.NetworkUtil;
import com.thedeanda.ajaxproxy.http.RequestListener;
import com.thedeanda.ajaxproxy.ui.model.Resource;
import com.thedeanda.ajaxproxy.ui.rest.HistoryItem;

public class ResourceService implements RequestListener {
	private static Logger log = LoggerFactory.getLogger(ResourceService.class);
	private final LruCache<String, StoredResource> cache;
	private JdbcConnectionSource connectionSource;
	private Dao<StoredResource, String> dao;
	private File dbFile;

	public ResourceService(int cacheSize, File dbFile) {
		cache = new LruCache<String, StoredResource>(cacheSize);
		this.dbFile = dbFile;
		try {
			initConnection();
			verifyTable();
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			dao = null;
		}
	}

	private void initConnection() throws SQLException {
		try {
			String databaseUrl = "jdbc:h2:file:" + dbFile.getAbsolutePath() + ";AUTO_SERVER=TRUE";
			connectionSource = new JdbcConnectionSource(databaseUrl);

			dao = DaoManager.createDao(connectionSource, StoredResource.class);

			if (!dao.isTableExists()) {
				TableUtils.createTableIfNotExists(connectionSource, StoredResource.class);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void verifyTable() throws SQLException {
		// check if we can save/load/delete an empty record, if not, recreate
		// table
		String id = UUID.randomUUID().toString();
		try {
			StoredResource r = new StoredResource();
			r.setId(id);
			r.setContentEncoding("test");
			if (!saveImmediately(r)) {
				throw new Exception("failed to save");
			}
			
			dao.queryForId(id);
			dao.deleteById(id);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			log.debug("recreating table");
			
			TableUtils.dropTable(connectionSource, StoredResource.class, true);
			TableUtils.createTableIfNotExists(connectionSource, StoredResource.class);
		}
	}

	public void save(StoredResource resource) {
		log.debug("saving resource: {}", resource);
		// TODO: save to db in the background
		saveImmediately(resource);
		cache.put(resource.getId(), resource);
	}

	private boolean saveImmediately(StoredResource resource) {
		if (dao == null)
			return false;

		try {
			if (StringUtils.isBlank(resource.getId())) {
				dao.create(resource);
			} else {
				dao.update(resource);
			}
		} catch (SQLException e) {
			log.warn(e.getMessage(), e);
			return false;
		}

		return true;
	}

	public StoredResource get(UUID id) {
		return get(id.toString());
	}

	public StoredResource get(String id) {
		StoredResource ret = cache.get(id);
		if (ret == null && dao != null) {
			try {
				ret = dao.queryForId(id);
				decompressIfNeeded(ret);
				cache.put(ret.getId(), ret);
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
			}
		}
		return ret;
	}

	private void decompressIfNeeded(StoredResource resource) {
		byte[] bytes = NetworkUtil.decompress(resource.getContentEncoding(), resource.getOutput());
		resource.setOutputDecompressed(bytes);
	}

	@Override
	public void newRequest(UUID id, String url, String method) {
		log.debug("new request: {} {} {}", id, url, method);
		StoredResource sr = new StoredResource();
		sr.setId(id.toString());
		sr.setUrl(url);
		sr.setMethod(method);
		sr.setStartTime(System.currentTimeMillis());
		save(sr);
	}

	@Override
	public void startRequest(UUID id, URL url, Header[] requestHeaders, byte[] data) {
		log.debug("start request: {} {}", id, url);

		String headers = headersToString(requestHeaders);
		StoredResource sr = get(id.toString());
		if (sr != null) {
			sr.setHeaders(headers.toString());
			sr.setInput(data);
			save(sr);
		}
	}

	@Override
	public void requestComplete(UUID id, int status, String reason, long duration, Header[] responseHeaders,
			byte[] data) {
		log.debug("request complete: {} {}", id, status);
		String headers = headersToString(responseHeaders);
		StoredResource sr = get(id.toString());
		if (sr != null) {
			String encoding = NetworkUtil.getContentEncoding(responseHeaders);
			sr.setContentEncoding(encoding);
			sr.setStatus(status);
			sr.setReason(reason);
			sr.setDuration(duration);
			sr.setResponseHeaders(headers);
			sr.setOutput(data);
			decompressIfNeeded(sr);
			save(sr);
		}
	}

	@Override
	public void error(UUID id, String message, Exception e) {
		log.debug("request error: {} {}", id, message);
		StoredResource sr = get(id.toString());
		if (sr != null) {
			sr.setErrorMessage(message);
			save(sr);
		}
	}

	private String headersToString(Header[] headers) {
		StringBuilder ret = new StringBuilder();
		for (Header h : headers) {
			if (h.getValue() != null) {
				ret.append(h.getName() + ": " + h.getValue() + "\n");
			} else {
				ret.append(h.getName() + ":\n");
			}
		}
		return ret.toString();
	}

}
