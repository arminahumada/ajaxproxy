package com.thedeanda.ajaxproxy.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class ProxyPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private ProxyTableModel proxyModel;
	private JTable proxyTable;

	public ProxyPanel(ProxyTableModel proxyModel) {
		setLayout(new BorderLayout());
		proxyTable = new JTable(proxyModel);
		proxyTable.setColumnModel(new ProxyColumnModel());
		JScrollPane scroll = new JScrollPane(proxyTable);

		add(BorderLayout.CENTER, scroll);

	}
}
