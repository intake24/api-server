/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

import javax.swing.JLabel;

public class SimpleFlowLayout implements LayoutManager {
	@SuppressWarnings("serial")
	public static class LineBreak extends JLabel {
		public int gap;
		public LineBreak(int gap) {
			super();
			setVisible(false);
			this.gap = gap;
		}
		
		public LineBreak() {
			this(0);
		}
	}
	
	//public final static LineBreak BR = new LineBreak(); 
	
	private int hgap;
	private int vgap;
	private int maxWidth;
	private boolean applyLayout;

	public SimpleFlowLayout(int maxWidth) {
		this(5, 5, maxWidth);
	}

	public SimpleFlowLayout(int hgap, int vgap, int maxWidth) {
		this.hgap = hgap;
		this.vgap = vgap;
		this.maxWidth = maxWidth;
	}
	
	public int getHgap() {
		return hgap;
	}

	public void setHgap(int hgap) {
		this.hgap = hgap;
	}

	public int getVgap() {
		return vgap;
	}

	public void setVgap(int vgap) {
		this.vgap = vgap;
	}

	public void addLayoutComponent(String name, Component comp) {
	}

	public void removeLayoutComponent(Component comp) {
	}

	public Dimension preferredLayoutSize(Container target) {
		synchronized (target.getTreeLock()) {
			applyLayout = false;
			return doLayout(target);
		}
	}

	public Dimension minimumLayoutSize(Container target) {
		return new Dimension(0,0);
	}
	
	public void layoutContainer(Container target) {
		applyLayout = true;
		doLayout(target);
	}

	private Dimension doLayout(Container target) {
		synchronized (target.getTreeLock()) {
			Insets insets = target.getInsets();
			int availableWidth = maxWidth - (insets.left + insets.right + hgap*2);
			int nmembers = target.getComponentCount();

			int x = insets.left, y = insets.top;
			int rowh = 0;
			
			for (int i = 0 ; i < nmembers ; i++) {
				Component m = target.getComponent(i);
				
				if (m instanceof LineBreak) {
					x = insets.left;
					y += vgap + rowh + ((LineBreak)m).gap;
					rowh = 0;
					continue;
				}
				
				if (m.isVisible()) {
					Dimension d = m.getPreferredSize();

					if (applyLayout)
						m.setSize(d.width, d.height);
					
					if (x > insets.left)
						x += hgap;
					
					if (x + d.width >= availableWidth) {
						x = insets.left;
						y += vgap + rowh;
						rowh = d.height;
					} else
						rowh = Math.max(rowh, d.height);

					if (applyLayout)
						m.setLocation(x, y);
					
					x+=d.width;
				}
			}
			

			return new Dimension (maxWidth, y + rowh + insets.bottom/*+ (rowh!=0?vgap:0)*/);
		}
	}
}
