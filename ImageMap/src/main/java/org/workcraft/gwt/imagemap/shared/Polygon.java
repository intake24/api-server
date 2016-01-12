/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.imagemap.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Polygon implements IsSerializable{
	public Point[] vertices;
	
	public Polygon() { }
	
	private void verifyLength() {
		if (this.vertices.length < 3) throw new IllegalArgumentException("Too few distinct vertices for polygon definition");
	}
	
	public Polygon(Point[] vertices) {
		this.vertices = preprocess(vertices);
		verifyLength();
	}
	
	public Polygon(double[] coords) {
		if (coords.length < 6) throw new IllegalArgumentException ("Too few coordinates for polygon definition");
		
		Point[] v = new Point[coords.length/2];
		
		for (int i=0; i<coords.length; i+=2) {
			Point p = new Point(coords[i], coords[i+1]);
			v[i/2] = p;
		}
		
		this.vertices = preprocess(v);
		verifyLength();
	}
	
	public static Point[] preprocess (Point[] points) {
		if (points.length < 2) return points;
		
		Point[] buf = new Point[points.length + 1];
		buf[0] = points[0];
		int cnt = 1;
		
		for (int i = 1; i<points.length; i++) {
			if (!points[i-1].equals(points[i]))
				buf[cnt++] = points[i];
		}
		
		if (!points[0].equals(points[points.length-1]))
			buf[cnt++] = points[0];
		
		Point[] result = new Point[cnt];
		
		for (int i=0; i<cnt; i++)
			result[i] = buf[i];
		
		return result;
	}
 
	// Uses ray-casting algorithm
	// Implementation based on: http://introcs.cs.princeton.edu/java/35purple/Polygon.java.html
	// Algorithm given there is faulty though, because it will result in 
	// division by zero if there are strictly horizontal segments.
    public boolean isInside(Point p) {
        int crossings = 0;
        for (int i = 0; i < vertices.length - 1; i++) {
        	double dy = vertices[i+1].y - vertices[i].y;

        	// ignore horizontal segments
        	if (Math.abs(dy) < 1E-6) continue;
        	
        	double dx = vertices[i+1].x - vertices[i].x;
            double slope = dx/dy;
            
            boolean cond1 = (vertices[i].y <= p.y) && (p.y < vertices[i+1].y);
            boolean cond2 = (vertices[i+1].y <= p.y) && (p.y < vertices[i].y);
            boolean cond3 = p.x <  slope * (p.y - vertices[i].y) + vertices[i].x;
            if ((cond1 || cond2) && cond3) crossings++;
        }
        return (crossings % 2 != 0);
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("( ");
    	for (Point p: vertices) {
    		sb.append(p.toString());    	
    	}
    	sb.append(" )");
    	return sb.toString();
    }
}