package com.thedeanda.ajaxproxy.model.config;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ProxyConfigRequest implements ProxyConfig {
	private String host;
	private int port;
	private String path;
	private boolean enableCache;
	/** cache duration in seconds */
	private int cacheDuration = 500;
	private String hostHeader;
	private List<HttpHeader> headers = new ArrayList<>();

}
