package edu.upenn.cis455.xpathengine;

import org.w3c.dom.Document;

interface XPathEngine {

  // Sets the XPath expression(s) that are to be evaluated. 
  void setXPaths(String[] expressions);

  // Returns true if the i.th XPath expression given to the last setXPaths() call
  // was valid, and false otherwise. If setXPaths() has not yet been called, the
  // return value is undefined. 
  boolean isValid(int i);

  // Takes a DOM root node as its argument, which contains the representation of the 
  // HTML or XML document. Returns an array of the same length as the 'expressions'
  // argument to setXPaths(), with the i.th element set to true if the document 
  // matches the i.th XPath expression, and false otherwise. If setXPaths() has not
  // yet been called, the return value is undefined.
  boolean[] evaluate(Document d);
  
}
