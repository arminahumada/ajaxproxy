package com.thedeanda.ajaxproxy.http;

import java.net.URL;
import java.util.UUID;

import org.apache.http.Header;

public interface RequestListener {
	public void newRequest(UUID id, URL url, Header[] requestHeaders,
			byte[] data);

	public void requestComplete(UUID id, int status, Header[] responseHeaders,
			byte[] data);
}