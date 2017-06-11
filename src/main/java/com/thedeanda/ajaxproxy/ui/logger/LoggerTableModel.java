package com.thedeanda.ajaxproxy.ui.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.swing.table.AbstractTableModel;

import org.japura.gui.model.ListCheckModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thedeanda.ajaxproxy.filter.handler.logger.LoggerMessage;
import com.thedeanda.ajaxproxy.filter.handler.logger.LoggerMessageListener;
import com.thedeanda.javajson.JsonArray;

public class LoggerTableModel extends AbstractTableModel implements LoggerMessageListener {
	private static final long serialVersionUID = 4961880986671181480L;
	private static final Logger log = LoggerFactory.getLogger(LoggerTableModel.class);

	private List<LoggerMessage> allItems = new ArrayList<>();
	private List<LoggerMessage> items = new ArrayList<>();

	private Pattern filterPattern;

	private List<String> filterTags;

	private Set<String> tags = new TreeSet<>();
	private ListCheckModel tagModel;

	@Override
	public int getRowCount() {
		return items.size();
	}

	@Override
	public int getColumnCount() {
		return 5;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (items.size() > rowIndex) {
			LoggerMessage message = items.get(rowIndex);
			return getValue(message, columnIndex);
		}
		return null;
	}

	private Object getValue(LoggerMessage message, int col) {
		switch (col) {
		case 0:
			return message.getUid();
		case 1:
			return message.getIndex();
		case 2:
			return message.getTs();
		case 3:
			return message.getTime();
		case 4:
			return message.getTag();
		case 5:
			return getMessage(message);
		default:
			return null;
		}
	}

	private String getMessage(LoggerMessage msg) {
		String output = "";

		JsonArray arr = msg.getMessage();
		// TODO: json library needs isEmpty on array
		if (arr != null && arr.size() > 0) {
			if (arr.size() == 1) {
				// TODO: first item may not be a string, json library needs a
				// get jsonvalue method
				output = arr.getString(0);
			} else {
				output = arr.toString();
			}
		}

		return output;
	}

	@Override
	public void messageReceived(LoggerMessage message) {
		allItems.add(message);
		message = filter(message);
		if (message != null) {
			// TODO: swing worker/thread ?
			int row = items.size();
			items.add(message);

			if (!tags.contains(message.getTag())) {
				updateTags();
			}

			fireTableRowsInserted(row, row);
		}
	}

	public LoggerMessage getMessage(int index) {
		if (items.size() > index && index >= 0) {
			return items.get(index);
		}
		return null;
	}

	public void clear() {	
		int size = items.size();
		items.clear();
		allItems.clear();
		fireTableRowsDeleted(0, size);
		updateTags();
	}

	public void setFilter(Pattern filter, List<String> tags) {
		log.warn("is event dispatch thread: {}", javax.swing.SwingUtilities.isEventDispatchThread());
		this.filterPattern = filter;
		this.filterTags = tags;
		filterReset();
	}

	private void filterReset() {
		items.clear();

		for (LoggerMessage message : allItems) {
			message = filter(message);
			if (message != null) {
				items.add(message);
			}
		}

		updateTags();
		fireTableDataChanged();
	}

	private void updateTags() {
		Set<String> newTags = new TreeSet<>();

		for (LoggerMessage message : allItems) {
			newTags.add(message.getTag());
		}

		if (!newTags.containsAll(tags) || !tags.containsAll(newTags)) {
			for (String tag : newTags) {
				if (!tags.contains(tag)) {
					tags.add(tag);
					tagModel.addElement(tag);
				}
			}
			for (String tag : tags) {
				if (!newTags.contains(tag)) {
					tagModel.removeElement(tag);
				}
			}
			tags.clear();
			tags.addAll(newTags);
		}
	}

	private LoggerMessage filter(LoggerMessage message) {
		if (message != null && filterTags != null && !filterTags.isEmpty()) {
			log.info("tags found, filter by tags");
			if (!filterTags.contains(message.getTag())) {
				log.info("tag doesn't match, filter {}", message);
				message = null;
			}
		}
		if (message != null && filterPattern != null) {
			String ms = message.getMessage().toString();
			if (!filterPattern.matcher(ms).matches()) {
				log.info("regex doesn't match, filter {}", message);
				message = null;
			}
		}

		return message;
	}

	public void setTagModel(ListCheckModel tagModel) {
		this.tagModel = tagModel;
		updateTags();
	}
}
