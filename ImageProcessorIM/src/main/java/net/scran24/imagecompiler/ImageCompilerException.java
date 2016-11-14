/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.imagecompiler;

@SuppressWarnings("serial")
public class ImageCompilerException extends RuntimeException {
	public ImageCompilerException() {
	}

	public ImageCompilerException(String arg0) {
		super(arg0);
	}

	public ImageCompilerException(Throwable arg0) {
		super(arg0);
	}

	public ImageCompilerException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public ImageCompilerException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}
}
