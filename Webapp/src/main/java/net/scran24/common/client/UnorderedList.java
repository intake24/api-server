/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.UListElement;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

public class UnorderedList<T extends Widget> extends ComplexPanel
{
		public final LinkedHashMap<T, UnorderedListItem<T>> items = new LinkedHashMap<T, UnorderedListItem<T>>();
	
    public UnorderedList()
    {
        setElement(Document.get().createULElement());
    }
 
    public void setId(String id)
    {
        // Set an attribute common to all tags
        getElement().setId(id);
    }
 
    public void setDir(String dir)
    {
        // Set an attribute specific to this tag
        ((UListElement) getElement().cast()).setDir(dir);
    }
 
    @SuppressWarnings("deprecation")
		public void addItem(T item)
    {
        // ComplexPanel requires the two-arg add() method
        UnorderedListItem<T> li = new UnorderedListItem<T>(item);
				
        super.add(li, getElement());
        items.put(item, li);
    }
    
    public void removeItem(T item) {
    	super.remove(items.get(item));
    	items.remove(item);
    }
    
    public List<T> getItems() {
    	return new ArrayList<T>(items.keySet());
    }
    
    @Override
    public void clear() {
    	super.clear();
    	items.clear();
    }
}