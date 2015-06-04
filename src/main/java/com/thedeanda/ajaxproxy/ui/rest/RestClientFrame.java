package com.thedeanda.ajaxproxy.ui.rest;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.swing.JFrame;

import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thedeanda.ajaxproxy.LoadedResource;
import com.thedeanda.ajaxproxy.http.RequestListener;
import com.thedeanda.ajaxproxy.ui.busy.BusyNotification;

public class RestClientFrame extends JFrame implements RequestListener {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory
			.getLogger(RestClientFrame.class);
	private RestClientPanel panel;
	private BusyNotification busy;

	private static final Set<String> BLACKLIST_HEADERS = new HashSet<String>();
	static {
		BLACKLIST_HEADERS.add("Host");
		BLACKLIST_HEADERS.add("Content-Length");
	}

	public RestClientFrame() {
		panel = new RestClientPanel();
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, panel);
		panel.setDefaultButton();
		panel.setListener(this);
		// panel.setResource(resource);
		setTitle("Ajax Proxy - Rest Client");
		setPreferredSize(new Dimension(1000, 700));
		setMinimumSize(new Dimension(600, 380));

		busy = new BusyNotification();
		setGlassPane(busy);

		URL imgUrl = ClassLoader.getSystemClassLoader().getResource("icon.png");
		Image image = Toolkit.getDefaultToolkit().getImage(imgUrl);
		this.setIconImage(image);

		pack();
	}

	public void fromResource(LoadedResource resource) {
		String url = resource.getUrl();
		panel.setUrl(url);

		Header[] hdrs = resource.getRequestHeaders();
		if (hdrs != null) {
			StringBuilder sb = new StringBuilder();
			for (Header h : hdrs) {
				if (!BLACKLIST_HEADERS.contains(h.getName())) {
					sb.append(h.getName() + ": " + h.getValue() + "\n");
				}
			}
			panel.setHeaders(sb.toString());
		}
		panel.setInput(resource.getInputAsText());
		panel.setMethod(resource.getMethod());
	}

	public static void main(String[] args) {
		RestClientFrame f = new RestClientFrame();
		f.pack();
		f.setDefaultCloseOperation(EXIT_ON_CLOSE);
		f.setVisible(true);
	}

	private void busy() {
		busy.setVisible(true);
	}

	private void notBusy() {
		busy.setVisible(false);
	}

	@Override
	public void newRequest(UUID id, String url, String method) {
		busy();
	}

	@Override
	public void startRequest(UUID id, URL url, Header[] requestHeaders,
			byte[] data) {
	}

	@Override
	public void requestComplete(UUID id, int status, String reason,
			long duation, Header[] responseHeaders, byte[] data) {
		notBusy();
	}

	@Override
	public void error(UUID id, String message, Exception e) {
		notBusy();
	}
}
