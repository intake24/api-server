/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared.lookup;

import com.google.gwt.user.client.rpc.IsSerializable;

public class DrinkScaleDef implements IsSerializable {
	public String baseImage;
	public String overlayImage;
	public int choice_id;
	public int width;
	public int height;
	public int emptyLevel;
	public int fullLevel;
	public double[][] volumeSamples;

	@Deprecated
	public DrinkScaleDef() {
	}
	// choice_id: Int, baseImage: String, overlayImage: String, width: Int, height: Int, emptyLevel: Int, fullLevel: Int
	public DrinkScaleDef(int choice_id, String baseImage, String overlayImage, int width, int height, int emptyLevel, int fullLevel, double[][] volumeSamples) {
		this.choice_id = choice_id;
		this.baseImage = baseImage;
		this.overlayImage = overlayImage;
		this.width = width;
		this.height = height;
		this.emptyLevel = emptyLevel;
		this.fullLevel = fullLevel;
		this.volumeSamples = volumeSamples;
	}
	
	private double interp (double fill, double sf0, double sv0, double sf1, double sv1) {
		double a = (fill - sf0) / (sf1 - sf0);
		return sv0 + (sv1 - sv0) * a;
	}
	
	public double calcVolume(double fillLevel) {
		if (fillLevel < 0.0) return 0.0;
		
		int i;
		
		for (i=0; i<volumeSamples[0].length; i++)
			if (volumeSamples[0][i] >= fillLevel)
				break;
		
		if (i == 0)
			return interp (fillLevel, 0.0, 0.0, volumeSamples[0][0], volumeSamples[1][0]);					
		
		if (i == volumeSamples[0].length)
			return volumeSamples[1][i-1];
		
		return interp (fillLevel, volumeSamples[0][i-1], volumeSamples[1][i-1], volumeSamples[0][i], volumeSamples[1][i]);
	}
}