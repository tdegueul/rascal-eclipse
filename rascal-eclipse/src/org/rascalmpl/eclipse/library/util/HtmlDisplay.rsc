/*******************************************************************************
 * Copyright (c) 2009-2011 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bert Lisser    - Bert.Lisser@cwi.nl
 *******************************************************************************/
module util::HtmlDisplay

str responseString = "";


@javaClass{org.rascalmpl.eclipse.library.util.HtmlDisplay}
@reflect{Uses URI Resolver Registry}
private java void _htmlDisplay(loc location, str htmlInput); 


@javaClass{org.rascalmpl.eclipse.library.util.HtmlDisplay}
@reflect{Uses URI Resolver Registry}
public java void htmlDisplay(loc location); 

public void htmlDisplay(loc location, str htmlInput){
    responseString = htmlInput;
    _htmlDisplay(location, htmlInput);
}

public str getResponseString() {
  return responseString;
  }


 
