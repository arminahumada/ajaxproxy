package com.thedeanda.ajaxproxy.ui.proxy;

import javax.swing.table.AbstractTableModel;

import com.thedeanda.javajson.JsonArray;
import com.thedeanda.javajson.JsonObject;
import com.thedeanda.javajson.JsonValue;

public class ProxyTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	private JsonArray data;
	private final static String DOMAIN = "domain";
	private final static String PORT = "port";
	private final static String PATH = "path";
	private final static String PREFIX = "prefix";
	private final static String[] COLS = { DOMAIN, PORT, PATH, PREFIX };

	public ProxyTableModel(JsonArray data) {
		this.data = data;
	}

	public ProxyTableModel() {
		this.data = new JsonArray();
		data.add(new JsonObject());
		data.add(new JsonObject());
		fireTableDataChanged();
	}

	public void clear() {
		data = new JsonArray();
		fireTableDataChanged();
		normalizeData();
	}

	@Override
	public Class<?> getColumnClass(int col) {
		return String.class;
	}

	public JsonArray getConfig() {
		normalizeData();
		JsonArray arr = new JsonArray();
		for (JsonValue v : data) {
			arr.add(v.getJsonObject());
		}
		arr.remove(arr.size() - 1);
		return arr;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public int getColumnCount() {
		return COLS.length;
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		if (data.size() < row || row < 0)
			return null;
		JsonObject json = data.getJsonObject(row);
		if (json != null)
			return json.getString(COLS[col]);
		else
			return null;
	}

	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if (value == null)
			value = "";
		data.getJsonObject(rowIndex).put(COLS[columnIndex], value.toString());
		normalizeData();
	}

	public void setValue(int row, String domain, String port, String path,
			String prefix) {
		JsonObject json = data.getJsonObject(row);
		if (json != null) {
			json.put(DOMAIN, domain);
			json.put(PORT, port);
			json.put(PATH, path);
			json.put(PREFIX, prefix);
			fireTableRowsUpdated(row, row);
			normalizeData();
		}
	}

	private void normalizeData() {
		boolean changed = false;
		for (int j = 0; j < data.size(); j++) {
			JsonObject rowObj = data.getJsonObject(j);
			boolean keep = false;
			for (int i = 0; i < COLS.length; i++) {
				String v = rowObj.getString(COLS[i]);
				if (v != null && !v.equals("")) {
					keep = true;
					break;
				}
			}
			if (!keep) {
				data.remove(j);
				j--;
				changed = true;
			}
		}
		data.add(new JsonObject());
		fireTableDataChanged();
	}

	public void setConfig(JsonArray data) {
		if (data == null)
			data = new JsonArray();
		this.data = data;
		this.fireTableDataChanged();
		this.normalizeData();
	}
}
