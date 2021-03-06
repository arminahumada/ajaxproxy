package com.thedeanda.ajaxproxy.ui.viewer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class ImageViewer extends JPanel {
	private static final long serialVersionUID = 1L;
	private BufferedImage image;

	public ImageViewer(BufferedImage image) {
		this.image = image;
		setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
		setMinimumSize(new Dimension(image.getWidth(), image.getHeight()));
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		int x = 0;
		int y = 0;
		g2.drawImage(image, x, y, this);
	}
}
