/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.imagecompiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;

public class ContainerDef {
	public final int choice_id;
	public final String photo_id;
	public final String desc;
	public final List<VolumeSample> volumeSamples;
	
	public ContainerDef(int choice_id, String photo_id, String desc, List<VolumeSample> volumeSamples) {
		this.choice_id = choice_id;
		this.photo_id = photo_id;
		this.desc = desc;
		this.volumeSamples = volumeSamples;
	}
	
	public static Map<String, ContainerDef> parseContainerDef (File file) throws FileNotFoundException, IOException {
		CSVReader csvReader = new CSVReader(new FileReader(file));
		List<String[]> rows = csvReader.readAll();
		csvReader.close();
		
		Map<String, ContainerDef> result = new HashMap<String, ContainerDef>();
		
		Iterator<String[]> it = rows.iterator();
		
		it.next();
		
		//		GLASSES	Photo ID	DESCRIPTION	HEIGHT OF GLASS (cm)	FULL VOLUME (g)	1cm	2cm	3cm	4cm	5cm	6cm	7cm	8cm	9cm	10cm	11cm	12cm	13cm	14cm	15cm
		//       0       1           2          3                     4 
		
		while(it.hasNext()) {
			String[] row = it.next();
			List<VolumeSample> volumeSamples = new ArrayList<VolumeSample>();
			
			double height = Double.parseDouble(row[3]);
						
			for (int i = 5; i <row.length; i++) {
				if (row[i].isEmpty()) break;
				volumeSamples.add(new VolumeSample( (i-4) / height, Double.parseDouble(row[i])));
			}
			
			volumeSamples.add(new VolumeSample(1.0, Double.parseDouble(row[4])));
			
			result.put(row[1], new ContainerDef(Integer.parseInt(row[0]), row[1], row[2], volumeSamples));						
		}
		
		return result;
	}
}
