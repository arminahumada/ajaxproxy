package com.thedeanda.ajaxproxy.ui.proxy;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import org.apache.commons.lang3.StringUtils;

import com.thedeanda.ajaxproxy.ui.SwingUtils;

public class ProxyEditorPanel extends JPanel {
	private static final long serialVersionUID = 5379224168584631339L;
	private SpringLayout layout;
	private JLabel domainLabel;
	private JTextField hostField;
	private JLabel portLabel;
	private JTextField portField;
	private JLabel pathLabel;
	private JTextField pathField;
	private JLabel newProxyLabel;
	private JComboBox<?> newProxyField;
	private JButton btn;
	private EditorListener listener;

	public ProxyEditorPanel(EditorListener listener) {
		this.listener = listener;
		layout = new SpringLayout();
		setLayout(layout);

		domainLabel = new JLabel("Domain");
		hostField = SwingUtils.newJTextField();
		add(domainLabel);
		add(hostField);

		portLabel = new JLabel("Port");
		portField = SwingUtils.newJTextField();
		add(portLabel);
		add(portField);

		pathLabel = new JLabel("Path");
		pathField = SwingUtils.newJTextField();
		add(pathLabel);
		add(pathField);

		newProxyLabel = new JLabel("New Proxy");
		newProxyField = SwingUtils.newJComboBox(new Boolean[] { Boolean.FALSE,
				Boolean.TRUE });

		add(newProxyLabel);
		add(newProxyField);

		btn = new JButton("Ok");
		btn.setMargin(new Insets(0, 0, 0, 0));
		btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				commitEdit();
			}
		});
		add(btn);

		initLayout();
		setPreferredSize(new Dimension(1000, 50));
	}

	private void initLayout() {
		JLabel[] labels = new JLabel[] { domainLabel, portLabel, pathLabel,
				newProxyLabel, null };
		Component[] fields = new Component[] { hostField, portField, pathField,
				newProxyField, btn };
		int[] cols = new int[] { 250, 70, 250, 150, 60 };

		for (int i = cols.length - 1; i >= 0; i--) {
			JLabel lbl = labels[i];
			Component fld = fields[i];
			if (i == 0) {
				layout.putConstraint(SpringLayout.NORTH, lbl, 0,
						SpringLayout.NORTH, this);

				layout.putConstraint(SpringLayout.NORTH, fld, 10,
						SpringLayout.SOUTH, lbl);

				layout.putConstraint(SpringLayout.WEST, fld, 10,
						SpringLayout.WEST, this);

				layout.putConstraint(SpringLayout.WEST, lbl, 10,
						SpringLayout.WEST, this);
			} else {
				layout.putConstraint(SpringLayout.NORTH, fld, 0,
						SpringLayout.NORTH, fields[0]);

				layout.putConstraint(SpringLayout.WEST, fld, 10,
						SpringLayout.EAST, fields[i - 1]);
				if (lbl != null) {
					layout.putConstraint(SpringLayout.NORTH, lbl, 0,
							SpringLayout.NORTH, labels[0]);
					layout.putConstraint(SpringLayout.WEST, lbl, 0,
							SpringLayout.WEST, fld);
				}
			}
			layout.putConstraint(SpringLayout.EAST, fld, cols[i],
					SpringLayout.WEST, fld);

		}
		layout.putConstraint(SpringLayout.NORTH, btn, 0, SpringLayout.NORTH,
				hostField);

	}

	public void startEdit(String host, int port, String path, boolean newProxy) {
		String sport = "";
		if (port > 0)
			sport = String.valueOf(port);

		this.hostField.setText(host);
		this.portField.setText(sport);
		this.pathField.setText(path);

		if (newProxy) {
			this.newProxyField.setSelectedIndex(1);
		} else {
			this.newProxyField.setSelectedIndex(0);
		}
	}

	private void commitEdit() {
		String host = hostField.getText();
		int port = 0;
		try {
			String sport = portField.getText();
			if (!StringUtils.isBlank(sport)) {
				port = Integer.parseInt(sport);
			}
		} catch (NumberFormatException nfe) {
			port = 0;
		}
		String path = pathField.getText();
		boolean newProxy = newProxyField.getSelectedIndex() == 1;
		listener.commitChanges(host, port, path, newProxy);
	}
}