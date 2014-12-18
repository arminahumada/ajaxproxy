package com.thedeanda.ajaxproxy.http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClient {
	private static final Logger log = LoggerFactory.getLogger(HttpClient.class);
	HttpRequestExecutor httpexecutor;
	HttpProcessor httpproc;

	public enum RequestMethod {
		GET(false), POST(true), PUT(true), DELETE(false), HEAD(false);

		private boolean acceptsPayload;

		private RequestMethod(boolean accepts) {
			this.acceptsPayload = accepts;
		}

		public boolean isAcceptsPayload() {
			return acceptsPayload;
		}
	}

	public HttpClient() {
		httpproc = HttpProcessorBuilder.create().add(new RequestContent())
				.add(new RequestTargetHost()).add(new RequestConnControl())
				.add(new RequestUserAgent("AjaxProxy/1.1"))
				.add(new RequestExpectContinue(true)).build();

		httpexecutor = new HttpRequestExecutor();

	}

	private void fireNewRequest(UUID uuid, String url, String method,
			RequestListener... listeners) {
		if (listeners != null) {
			for (RequestListener listener : listeners) {
				listener.newRequest(uuid, url, method);
			}
		}
	}

	private void fireError(UUID id, String message, Exception e,
			RequestListener... listeners) {
		if (listeners != null) {
			for (RequestListener listener : listeners) {
				listener.error(id, message, e);
			}
		}
	}

	private void fireStartRequest(UUID id, URL url, Header[] requestHeaders,
			byte[] data, RequestListener... listeners) {
		if (listeners != null) {
			for (RequestListener listener : listeners) {
				listener.startRequest(id, url, requestHeaders, data);
			}
		}
	}

	private void fireRequestComplete(UUID id, int status, String reason,
			long duation, Header[] responseHeaders, byte[] data,
			RequestListener... listeners) {
		if (listeners != null) {
			for (RequestListener listener : listeners) {
				listener.requestComplete(id, status, reason, duation,
						responseHeaders, data);
			}
		}
	}

	public void makeRequest(RequestMethod method, String url, String headers,
			byte[] input, RequestListener... listener) {

		UUID uuid = UUID.randomUUID();
		URL urlobj = null;
		fireNewRequest(uuid, url, method.name(), listener);
		try {
			urlobj = new URL(url);
		} catch (MalformedURLException e) {
			fireError(uuid, e.getMessage(), e, listener);
		}

		Map<String, String> hds = new HashMap<>();
		if (!StringUtils.isBlank(headers)) {
			String[] lines = StringUtils.split(headers, "\n");
			for (String line : lines) {
				String[] parts = StringUtils.split(line, ":", 2);
				hds.put(parts[0], parts[1]);
			}
		}
		Header[] requestHeaders = null;
		if (hds.size() > 0) {
			requestHeaders = new Header[hds.size()];
			int i = 0;
			for (String key : hds.keySet()) {
				Header h = new BasicHeader(key, hds.get(key));
				requestHeaders[i++] = h;
			}
		}

		makeRequest(method, urlobj, requestHeaders, input, listener);
	}

	public void makeRequest(RequestMethod method, URL url, Header[] headers,
			byte[] input, RequestListener... listener) {

		UUID uuid = UUID.randomUUID();
		fireNewRequest(uuid, url.toString(), method.toString(), listener);
		makeRequest_internal(uuid, method, url, headers, input, listener);
	}

	public void makeRequest_internal(UUID uuid, RequestMethod method, URL url,
			Header[] headers, byte[] input, RequestListener... listener) {

		fireStartRequest(uuid, url, headers, input, listener);

		try {
			makeRequestInternal(method, uuid, url, headers, input, listener);
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			log.warn(e.getMessage(), e);
		}

	}

	private HttpHost getHost(URL url) {
		int port = url.getPort();
		if (port < 0)
			port = url.getDefaultPort();
		if (port < 0) {
			if ("https".equals(url.getProtocol()))
				port = 443;
			else
				port = 80;
		}
		HttpHost host = new HttpHost(url.getHost(), port);
		return host;
	}

	private void makeRequestInternal(RequestMethod method, UUID id, URL url,
			Header[] requestHeaders, byte[] data, RequestListener... listener)
			throws NoSuchAlgorithmException, KeyManagementException {
		HttpCoreContext coreContext = HttpCoreContext.create();
		HttpHost host = getHost(url);
		coreContext.setTargetHost(host);

		DefaultBHttpClientConnection conn = new DefaultBHttpClientConnection(
				8 * 1024);
		ConnectionReuseStrategy connStrategy = DefaultConnectionReuseStrategy.INSTANCE;
		try {
			String target = url.getPath();
			if (url.getQuery() != null) {
				target += "?" + url.getQuery();
			}
			if (!conn.isOpen()) {
				if (host.getPort() != 443) {
					Socket socket1 = new Socket(host.getHostName(),
							host.getPort());
					conn.bind(socket1);
				} else {

					// SSLContext sslcontext = SSLContext.getDefault();
					SSLContext sslcontext = SSLContext.getInstance("TLS");
					sslcontext.init(new KeyManager[0],
							new TrustManager[] { new DefaultTrustManager() },
							new SecureRandom());

					SocketFactory sf = sslcontext.getSocketFactory();
					SSLSocket socket = (SSLSocket) sf.createSocket(
							host.getHostName(), host.getPort());
					conn.bind(socket);
				}
			}
			HttpRequestBase request = null;
			HttpEntity requestBody;
			switch (method) {
			case GET:
				request = new HttpGet(target);
				break;
			case POST:
				requestBody = new ByteArrayEntity(data,
						ContentType.APPLICATION_OCTET_STREAM);
				HttpPost post = new HttpPost(target);
				post.setEntity(requestBody);
				request = post;
				break;
			case PUT:
				requestBody = new ByteArrayEntity(data,
						ContentType.APPLICATION_OCTET_STREAM);
				HttpPut put = new HttpPut(target);
				put.setEntity(requestBody);
				request = put;
				break;
			case DELETE:
				HttpDelete del = new HttpDelete(target);
				request = del;
				break;
			case HEAD:
				HttpHead head = new HttpHead(target);
				request = head;
				break;
			}
			request.setHeaders(requestHeaders);
			log.info(">> Request URI: " + request.getRequestLine().getUri());

			long start = System.currentTimeMillis();
			httpexecutor.preProcess(request, httpproc, coreContext);
			HttpResponse response = httpexecutor.execute(request, conn,
					coreContext);
			httpexecutor.postProcess(response, httpproc, coreContext);

			long end = System.currentTimeMillis();
			StatusLine status = response.getStatusLine();
			log.info("<< Response: " + response.getStatusLine());

			byte[] bytes = null;
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				bytes = EntityUtils.toByteArray(response.getEntity());
			}
			fireRequestComplete(id, status.getStatusCode(),
					status.getReasonPhrase(), (end - start),
					response.getAllHeaders(), bytes, listener);

			log.info("==============");
			if (!connStrategy.keepAlive(response, coreContext)) {
				conn.close();
			} else {
				log.info("Connection kept alive...");
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			fireError(id, e.getMessage(), e, listener);
		} finally {
			try {
				conn.close();
			} catch (IOException e) {
				log.warn(e.getMessage(), e);
			}
		}
	}

	private static class DefaultTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1)
				throws CertificateException {
			// TODO Auto-generated method stub

		}

		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1)
				throws CertificateException {
			// TODO Auto-generated method stub

		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			// TODO Auto-generated method stub
			return null;
		}

	}
}
