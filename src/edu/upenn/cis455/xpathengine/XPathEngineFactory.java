package edu.upenn.cis455.xpathengine;

public class XPathEngineFactory {
	public static XPathEngine getXPathEngine() {
		return new XPathEngineImpl();
	}
}
