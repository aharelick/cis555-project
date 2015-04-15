package edu.upenn.cis455.xpathengine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
/**
 * this XPath Engine parses XPaths, evaluates them on a given DOM Document object, and checks if the XPaths have valid grammar
 * @author Kelsey Duncombe-Smith
 *
 */
public class XPathEngineImpl implements XPathEngine {
	ArrayList<String> allXPaths = new ArrayList<String>();
	ArrayList<Boolean> isValidXPathArray = new ArrayList <Boolean>();
	InputStream currentXPathIn;
	//ParsedXPath currentParsingXPath;
	String currentParsingXPath="";
	int lookaheadByte;
	
	ArrayList<Node> currentPossibleDOMMatches = new ArrayList<Node>();
	
  public XPathEngineImpl() {
    // Do NOT add arguments to the constructor!!
  }
  /**
   * adds the array of strings to the private ArrayList of XPaths. It clears the private ArrayList and the isValid results
   * so that each time you call setXPaths it replaces the preexisting xpaths
   */
  public void setXPaths(String[] s) {
    /*Store the XPath expressions that are given to this method */
	  allXPaths.clear();
	  isValidXPathArray.clear();
	  for(String xpath : s)
	  	allXPaths.add(xpath.trim());
  }
/**
 * checks if the XPath at the given index is valid. If the xpaths haven't been parsed yet, parse them with a null document
 * so it won't evaluate on a DOM
 */
  public boolean isValid(int i) {
    /* Check which of the XPath expressions is valid */
	  if(isValidXPathArray.isEmpty())
		  parseXPaths(null);
    return isValidXPathArray.get(i);
  }
	/**
	 * evaluates all XPaths against the given document
	 * @returns boolean [] or null if setXPaths has not been called and there are no XPaths to evaluate
	 */
  public boolean[] evaluate(Document d) { 
    /* if allXPaths is empty then setXPaths was not called*/
	if(allXPaths.isEmpty())
		return null;
    boolean [] evaluatePaths = parseXPaths(d);
    return evaluatePaths;
  }
  /**
   * sets the index of the associated xpath in the isValidXPathArray to be true or false to mean valid or invalid
   * @param isValid
   * @param currentPath
   */
  private void setValid(boolean isValid, String currentPath)
  {
	  isValidXPathArray.add(allXPaths.indexOf(currentPath), isValid);
  }
 
  /**
   * parses all XPaths and sets if they are Valid or not. If a Document is given, it evaluates each XPath on the Document
   * If the Document is null, then the XPaths are just parsed, and not compared to a Document.
   * @param d
   * @return a boolean array of the result of matched or not matched xpaths
   */
  private boolean[] parseXPaths(Document d)
  {
	  
	  boolean [] XPathMatches = new boolean [allXPaths.size()];
	  for(String currentPath : allXPaths)
	  {
		  if(d!=null)
			  currentPossibleDOMMatches.add(d);
		  currentParsingXPath = currentPath;
		  currentXPathIn = new ByteArrayInputStream(currentPath.getBytes());
		  try {
			lookaheadByte = currentXPathIn.read();
			//System.out.println("start parseXpaths: "+(char) lookaheadByte);
			//System.out.println("\nstart parse XPath: "+currentPath);
			if(lookaheadByte!=-1)
			{
				parse();
				//System.out.println("in evaluate, DOMMatches is empty? "+currentPossibleDOMMatches.isEmpty());
				if(!currentPossibleDOMMatches.isEmpty())
					XPathMatches[allXPaths.indexOf(currentPath)] = true;
				else
					XPathMatches[allXPaths.indexOf(currentPath)] = false;
			}
		  } catch (IOException e) {
			e.printStackTrace();
		  } catch (ParseException e) {
			  e.printStackTrace();
			setValid(false, currentPath);
			System.out.println("set "+currentPath+" invalid");
			currentPossibleDOMMatches.clear();
			XPathMatches[allXPaths.indexOf(currentPath)] = false;
		  }
		  
	  }
	  return XPathMatches;
	     
  }
  /**
   * recursive function to parse axis step pairs of an XPath
   * @throws IOException
   * @throws ParseException
   */
  private void parse() throws IOException, ParseException
  {
	  //System.out.println("in parse");
	  //parse the axis then call parseStep, then recursively parse the next axis step pair
	  if(lookaheadByte=='/')
	  {
		  lookaheadByte=currentXPathIn.read();
		  if(lookaheadByte =='/'||lookaheadByte == '*')
			  throw new ParseException("Invalid XPath grammar, "+lookaheadByte+ "found", 0);
		  parseStep();
		  parse();
	  }
	  else if(lookaheadByte == -1)
	  {
		  setValid(true, currentParsingXPath);
		  System.out.println("set "+currentParsingXPath+" valid");
		  return;
	  }
	  else
		  throw new ParseException("Unexpected symbol"+lookaheadByte+ " found in parse method", 0);
  }
  /**
   * parse the step component of the XPath which is made up of nodename([ test ])
 * @throws ParseException 
 * @throws IOException 
   */
  private void parseStep() throws ParseException, IOException
  {
	  //System.out.println("in parse step");
	  //System.out.println("current lookaheadByte when starting parseStep = "+(char)lookaheadByte);
	  //if there is not a condition on this node or after all conditions have been parsed,
	  //add all of the possibleDOMMatches existing nodes' children nodes to the list of possibles
	  updatePossibleMatchesWithChildNodes();
	  String nodename="";
	  nodename = getNodeName();
	  nodename=nodename.trim();
	  //System.out.println("nodename = "+nodename);
	  
	  //compare current nodename of XPath to all current possible DOM Node matches
	  //this will remove all possible matches that don't have the correct nodename
	  compareXPathNodeAndDOM(nodename);
	  
	  if(lookaheadByte == -1)
	  {
		  //System.out.println("end of stream reached");
		  //if -1 then end of stream reached
		  return;
	  }//if there is a condition on this node
	  else if(lookaheadByte == '[')
	  {
		  lookaheadByte=currentXPathIn.read();
		  ArrayList<Node> copyOfCurrentMatches = new ArrayList<Node>();
		  for(Node item: currentPossibleDOMMatches) copyOfCurrentMatches.add(item);
		  if(!currentPossibleDOMMatches.isEmpty())
			  copyOfCurrentMatches.addAll(currentPossibleDOMMatches);
		  ArrayList<Node> possibleMatchesAfterCondition = parseCondition(copyOfCurrentMatches);
		  //System.out.println("is possibleMatchesAfterCondition empty? "+possibleMatchesAfterCondition.isEmpty());
		  for(Node node : possibleMatchesAfterCondition) //System.out.println("possibleMatchesAfterCondition"+node.getNodeName());
		  
		  removeNodesNotMatchingCondition(possibleMatchesAfterCondition);
		  //System.out.println("is currentPossibleDOMMatches after condition empty? "+currentPossibleDOMMatches.isEmpty());
		  //System.out.println("current lookaheadByte: "+(char) lookaheadByte);
		  removeAllWhiteSpace();
		  //above parsed the first condition, now this will parse all of the rest of the conditions for this step
		  while(lookaheadByte=='[')
		  {
			  //System.out.println("haven't reached a slash, current lookahaeadByte = "+(char) lookaheadByte);
	
			  copyOfCurrentMatches.clear();
			  for(Node item: currentPossibleDOMMatches) copyOfCurrentMatches.add(item);
			  //System.out.println("found a new open bracket");
			  lookaheadByte=currentXPathIn.read();
			  possibleMatchesAfterCondition = parseCondition(copyOfCurrentMatches);
			  //System.out.println("is possibleMatchesAfterCondition  in while loop empty? "+possibleMatchesAfterCondition.isEmpty());
			  removeNodesNotMatchingCondition(possibleMatchesAfterCondition);
			  //System.out.println("is currentPossibleDOMMatches after condition in while loop empty? "+currentPossibleDOMMatches.isEmpty());
			  for(Node item: currentPossibleDOMMatches) //System.out.println("after condition fails removed: "+item.getNodeName());
			  removeAllWhiteSpace();
		  }
		  if(lookaheadByte==-1)
		  {
			  //System.out.println("in parseStep, finished reading in");
		  }
		  else if(lookaheadByte=='/'){}
		  else
			  throw new ParseException("Unexpected symbol"+lookaheadByte+ " found in parseStep", 0);
	  }
	  
  }
 
  /**
   * removes all white space from before the next important characters
   * @throws IOException
   */
  private void removeAllWhiteSpace() throws IOException
  {
	  while(lookaheadByte==' ')
	  {
		  lookaheadByte=currentXPathIn.read();
	  }
	  //System.out.println("removeAllWhiteSpace complete, current lookaheadByte = "+lookaheadByte);
  }
  /**
   * read in until the bracket or slash and that represents the nodename
   * @return
   * @throws IOException
 * @throws ParseException 
   */
  private String getNodeName() throws IOException, ParseException
  {
	  //System.out.println("in getNodeName");
	  String nodename = "";
	  while(lookaheadByte!='['&&lookaheadByte!='/'&&lookaheadByte!=-1)
	  {
		  nodename+=Character.toString((char)lookaheadByte);
		  lookaheadByte=currentXPathIn.read();
	  }
	  //System.out.println(nodename);
	  //regular regex with non escaped characters: ^(?!(xml|XML))^\s*([a-zA-Z_])+([a-zA-Z0-9\-\._])*\s*$
	  if(!nodename.matches("^(?!(xml|XML))^\\s*([a-zA-Z_])+([a-zA-Z0-9\\-\\._])*\\s*$"))
	  {
		  //System.out.println("regex doesn't match");
		  throw new ParseException("invalid characters in nodename", 0);
	  }
	  return nodename;
  }
  /**
   * read in until the open or close bracket or quote
   * the string read in represents the condition name inside nodename[test]
   * where test is made up of conditionName and Comparison String
   * e.g. 'text() = ', or 'contains(text(), ', or '@attname = '
   * if the stream ends without hitting one of those characters then throw an exception
   * @return conditionName
   * @throws IOException
 * @throws ParseException 
   */
  private String getConditionName() throws IOException, ParseException
  {
	  //System.out.println("in getConditionName");
	  String conditionName = "";
	  while(lookaheadByte!=']'&&lookaheadByte!='"'&&lookaheadByte!='['&&lookaheadByte!='/'&&lookaheadByte!=-1)
	  {
		  conditionName+=Character.toString((char)lookaheadByte);
		  lookaheadByte=currentXPathIn.read();
	  }
	  //if the condition name does not match the regex for a nodename or the regext for text()=, contains(text(),, or @attname=
	  if(!conditionName.matches("^(?!(xml|XML))^\\s*([a-zA-Z_])+([a-zA-Z0-9\\-\\._])*\\s*$")&&!conditionName.matches("^(\\s*contains\\s*\\(\\s*text\\s*\\(\\s*\\)\\s*,)\\s*$")&&!conditionName.matches("^(\\s*text\\s*\\(\\s*\\)\\s*=)\\s*$")&&!conditionName.matches("^\\s*@\\s*([a-zA-Z\\_\\:]+[\\-a-zA-Z0-9\\_\\:\\.]*)\\s*=\\s*$"))
	  {
		  //System.out.println("regex doesn't match for:"+conditionName);
		  throw new ParseException("invalid characters in conditionName", 0);
	  }
	  //System.out.print(conditionName);
	  return conditionName;
  }
  /**
   * read in until you hit a bracket or the end of the string
   * this would be whatever is after the end quote and before the bracket in nodename[test]
   * it should just be a close parenthesis if test was contains() or nothing if it is anything else
   * @return afterComparisonString
   * @throws IOException
 * @throws ParseException 
   */
  private String parseAfterComparisonString() throws IOException, ParseException
  {
	  //System.out.println("in parseAfterComparisonString");
	  String afterComparisonString = "";
	  while(lookaheadByte!=']'&&lookaheadByte!=-1)
	  {
		  afterComparisonString+=Character.toString((char)lookaheadByte);
		  lookaheadByte=currentXPathIn.read();
	  }
	  if(lookaheadByte == -1)
	  {
		  //if -1 then end of stream reached, but should not have been so throw exception
		  throw new ParseException("Unexpected symbol"+lookaheadByte+ " found in parseAfterComparisonString", 0);
	  }
	  return afterComparisonString;
  }
  /**
   * recursive function to parse the condition that follows a nodename, e.g. nodename[condition]. This
   * is equivalent to the test token mentioned in the writeup.
   *
 * @throws ParseException 
 * @throws IOException 
   */
  private ArrayList<Node> parseCondition(ArrayList<Node> possibleConditionMatches) throws ParseException, IOException
  {
	  //System.out.println("in parse condition");
	  String conditionName= "";
	  String afterComparisonString = "";
	  String quoteText = "";
	  
	  conditionName = getConditionName();
	  conditionName = conditionName.trim();
	  //System.out.println("condition name = "+conditionName);
	  if(lookaheadByte==']')
	  {
		  possibleConditionMatches = updateConditionMatchesWithMatchingChildNodes(conditionName, possibleConditionMatches);
		  lookaheadByte=currentXPathIn.read();
		  return possibleConditionMatches;
	  }
	  else if(lookaheadByte=='/')
	  {
		  possibleConditionMatches = updateConditionMatchesWithMatchingChildNodes(conditionName, possibleConditionMatches);
		  lookaheadByte=currentXPathIn.read();
		  possibleConditionMatches = parseCondition(possibleConditionMatches);
		  if(lookaheadByte==-1)
			  return possibleConditionMatches;
	  }
	  else if(lookaheadByte=='"')
	  {
		  lookaheadByte=currentXPathIn.read();
		  quoteText = parseComparisonString();
		  //regex without extra escaped characters: ^(\s*contains\s*\(\s*text\s*\(\s*\)\s*,)\s*$
		  if(conditionName.matches("^(\\s*contains\\s*\\(\\s*text\\s*\\(\\s*\\)\\s*,)\\s*$"))
		  {
			  possibleConditionMatches = updateConditionMatchesWithContainsText(quoteText, possibleConditionMatches);
			  for(Node node : possibleConditionMatches) {/*System.out.println("in parse condition: "+node.getNodeName());*/}
		  }
		  else if(conditionName.matches("^(\\s*text\\s*\\(\\s*\\)\\s*=)\\s*$"))
			  possibleConditionMatches = updateConditionMatchesWithExactText(quoteText, possibleConditionMatches);
		  else if(conditionName.matches("^\\s*@\\s*([a-zA-Z\\_\\:]+[\\-a-zA-Z0-9\\_\\:\\.]*)\\s*=\\s*$"))
		  {
			  //remove the @ symbol in front
			  conditionName = conditionName.substring(1);
			  conditionName = conditionName.substring(0, conditionName.indexOf('='));
			  conditionName.trim();
			  updateConditionMatchesWithAttribute(conditionName, quoteText, possibleConditionMatches);
		  }
		  afterComparisonString = parseAfterComparisonString();
		  afterComparisonString = afterComparisonString.trim();
		  //System.out.println("after comparison string: "+afterComparisonString);
		  
		  //lookaheadByte=currentXPathIn.read();
		  
		  /*if there were characters after the end of the end quote then it must have been 
		  * a contains() condition or an error
		  */
		  if(!afterComparisonString.equals(""))
		  {
			  if(conditionName.startsWith("contains("))
			  {
				  if(!afterComparisonString.equals(")"))
				  {
					  throw new ParseException("Unexpected symbol"+afterComparisonString+ " found in parse comparison string, not a ) but expected one", 0);
				  }		  
			  }
			  else
			  {
				  throw new ParseException("Unexpected symbol"+afterComparisonString+ " found in parse comparison string, found symbol but did not expect one", 0);
			  }
		  }
	  }
	  else if(lookaheadByte=='[')
	  {
		  lookaheadByte=currentXPathIn.read();
		  possibleConditionMatches = updateConditionMatchesWithMatchingChildNodes(conditionName, possibleConditionMatches);
		  ArrayList<Node>resultantPossibleConditionMatches = parseCondition(possibleConditionMatches);
		  //System.out.println("is possibleConditionMatches in parse condition empty? "+possibleConditionMatches.isEmpty());
		  for(Node node : possibleConditionMatches)//System.out.println("possibleConditionMatches in parse condition [ = "+node.getNodeName());
		  //don't think i need this one, lookaheadByte=currentXPathIn.read();
		  removeAllWhiteSpace();
		  
		  while(lookaheadByte=='['||lookaheadByte=='/')
		  {
			  if(lookaheadByte == '/')
			  {
				  //if all of the sibling conditions (e.g. [][]) at this level result in no matches, then return the empty matches all the way up 
				  if(resultantPossibleConditionMatches.isEmpty())
				  {
					  //lookaheadByte=currentXPathIn.read();
					  //return parseCondition(resultantPossibleConditionMatches);
					  return resultantPossibleConditionMatches;
				  }
				  else
				  {
					  lookaheadByte=currentXPathIn.read();
					  return parseCondition(possibleConditionMatches);
				  }
			  }
			  else
			  {
				  possibleConditionMatches = removeNodesNotMatchingIntermediateCondition(possibleConditionMatches, resultantPossibleConditionMatches);
				  //System.out.println("is currentPossibleDOMMatches after condition empty? "+currentPossibleDOMMatches.isEmpty());
				  //System.out.println("current lookaheadByte: "+(char) lookaheadByte);
				  
				  //System.out.println("found a new open bracket");
				  lookaheadByte=currentXPathIn.read();
				  resultantPossibleConditionMatches = parseCondition(possibleConditionMatches);
				  removeAllWhiteSpace();
				
			  }
			  
		  }
		  possibleConditionMatches=resultantPossibleConditionMatches;
		  }
	  
	  if(lookaheadByte != ']')
	  {
		  //if -1 then end of stream reached, but should not have been so throw exception
		  throw new ParseException("Unexpected symbol"+lookaheadByte+ " found in parseCondition end of Stream but shouldn't be?", 0);
	  }
	  //read past the ] of the condition
	  lookaheadByte=currentXPathIn.read();
	  return possibleConditionMatches;
  }
  /**
   * parse the string that would be the comparison for conditions like "text()="..."", 
   * "contains(text(), "...")", and "@attname = "...""
 * @throws IOException 
 * @throws ParseException 
   */
  private String parseComparisonString() throws IOException, ParseException
  {
	  //System.out.println("in parseComparisonString");
	  String comparisonString="";
	  while(lookaheadByte!='"'&&lookaheadByte!=-1)
	  {
		  if(lookaheadByte==92)// \ character
		  {
			  int nextLookaheadByte = currentXPathIn.read();
			  if(nextLookaheadByte =='"')
			  {
				  comparisonString+=Character.toString((char)nextLookaheadByte);
				  lookaheadByte = currentXPathIn.read();
			  }
			  else
			  {
				  comparisonString+=Character.toString((char)lookaheadByte);
				  lookaheadByte = nextLookaheadByte;
			  }
			  
		  }
		  else
		  {
			  comparisonString+=Character.toString((char)lookaheadByte);
			  lookaheadByte=currentXPathIn.read();
		  }
	  }
	  //System.out.print(comparisonString);
	  if(lookaheadByte == -1)
	  {
		  //if -1 then end of stream reached, but should not have been so throw exception
		  throw new ParseException("Unexpected symbol"+lookaheadByte+ " found in parseComparisonString", 0);
	  }
	  lookaheadByte=currentXPathIn.read();
	  return comparisonString;
	  
  }
  
  /**
   * Compares the current nodename with the currentPossibleDOMMatches list of nodes
   *  to check for a match, if there is a match, the node stays in the list, if not, it is removed
   * @param nodename
   * @param e
   */
  private void compareXPathNodeAndDOM(String nodename)
  {
	  ArrayList<Node> newPossibleMatches = new ArrayList<Node>();
	  for(Node currentNode : currentPossibleDOMMatches)
	  {
		  if(currentNode.getNodeName().equals(nodename))
			  newPossibleMatches.add(currentNode);
	  }
	  currentPossibleDOMMatches = newPossibleMatches;
  }
  /**
   * This updates the possibleDOMMatches to be the ChildrenNodes
   * so that all the childNodes can be compared to the next step of the XPath query
   */
  private void updatePossibleMatchesWithChildNodes()
  {
	  ArrayList<Node> newPossibleMatches = new ArrayList<Node>();
	  for(Node currentNode : currentPossibleDOMMatches)
	  {
		  NodeList childNodes = currentNode.getChildNodes();
		  for(int i=0; i<childNodes.getLength(); i++)
		  {
			  if(childNodes.item(i).getNodeType()==Node.ELEMENT_NODE)
			  {
				  newPossibleMatches.add(childNodes.item(i));
				  //System.out.println("child nodes: "+childNodes.item(i).getNodeName());
			  }
		  }
	  }
	  currentPossibleDOMMatches = newPossibleMatches;
  }
  /**
   * This updates the possibleMatches for a condition recursive path to be the valid ChildrenNodes
   * so that all the childNodes can be compared to the next step of the condition or so they can be used
   * to remove the DOMMatches that are no longer valid based on the condition of the step
   */
  private ArrayList<Node> updateConditionMatchesWithMatchingChildNodes(String comparisonNodeName, ArrayList<Node>currentMatches)
  {
	  ArrayList<Node> newPossibleMatches = new ArrayList<Node>();
	  for(Node currentNode : currentMatches)
	  {
		  NodeList childNodes = currentNode.getChildNodes();
		  for(int i=0; i<childNodes.getLength(); i++)
		  {
			  if(childNodes.item(i).getNodeType()==Node.ELEMENT_NODE)
			  {
				  if(childNodes.item(i).getNodeName().equals(comparisonNodeName))
					  newPossibleMatches.add(childNodes.item(i));
			  }
		  }
	  }
	  return newPossibleMatches;
  }
  /**
   * handles parsing the DOM possible matches for the condition/test:contains(text(), "")
   * @param quoteText
   * @param currentMatches
   * @return
   */
  private ArrayList<Node> updateConditionMatchesWithContainsText(String quoteText, ArrayList<Node>currentMatches)
  {
	  ArrayList<Node> newPossibleMatches = new ArrayList<Node>();
	  for(Node currentNode : currentMatches)
	  {
		  NodeList childNodes = currentNode.getChildNodes();
		  for(int i = 0; i<childNodes.getLength();i++)
		  {
			  //if one of the current matches has a child node that is a text node that contains the quote text
			  //then see if the current node has already been added to the newPossibleMatches list and if not, add it
			  if(childNodes.item(i).getNodeType()==Node.TEXT_NODE)
			  {
				  //System.out.println("update Condition matches with Contains text, text node value = "+childNodes.item(i).getNodeValue());
				  String textNodeValue = fixTextNodeEscapedQuotes(childNodes.item(i).getNodeValue());
				  if(textNodeValue.contains(quoteText))
				  {
					  //System.out.println("contains text matched, text matched is: "+childNodes.item(i).getNodeValue());
					  if(!newPossibleMatches.contains(currentNode))
						  newPossibleMatches.add(currentNode);
				  }
					  
			  }
		  }
	  }
	  return newPossibleMatches;
  }
  /**
   * replaces escaped quote characters in xml with a quote character
   * @param text
   * @returns string with &quot; replaced with a quote
   */
  private String fixTextNodeEscapedQuotes(String text)
  {
	  text.replace("&quot;", "\"");
	  return text;
  }
  /**
   * compares the given string to the text node children of all nodes in the arraylist passed in
   * if the string exactly matches the text in the text node, it is added as a possible match and the array is returned
   * @param quoteText
   * @param currentMatches
   * @returns the new list of possible matches
   */
  private ArrayList<Node> updateConditionMatchesWithExactText(String quoteText, ArrayList<Node>currentMatches)
  {
	  ArrayList<Node> newPossibleMatches = new ArrayList<Node>();
	  for(Node currentNode : currentMatches)
	  {
		  NodeList childNodes = currentNode.getChildNodes();
		  for(int i = 0; i<childNodes.getLength();i++)
		  {
			  //if one of the current matches has a child node that is a text node that contains the quote text
			  //then see if the current node has already been added to the newPossibleMatches list and if not, add it
			  if(childNodes.item(i).getNodeType()==Node.TEXT_NODE)
			  {
				  //System.out.println("update Condition matches with exact text, text node value = "+childNodes.item(i).getNodeValue());
				  String textNodeValue = fixTextNodeEscapedQuotes(childNodes.item(i).getNodeValue());
				  if(textNodeValue.equals(quoteText))
				  {
					  //System.out.println("exact text matched, text matched is: "+childNodes.item(i).getNodeValue());
					  if(!newPossibleMatches.contains(currentNode))
						  newPossibleMatches.add(currentNode);
				  }
					  
			  }
		  }
	  }
	  return newPossibleMatches;
  }
  /**
   * This method checks that the Nodes in the arraylist of possible matches have an attribute 
   * with the specified attributeName and attributeText.If they do, they are added to the new 
   * possible matches arraylist and returned.
   * @param attributeName
   * @param attributeText
   * @param currentMatches
   * @return
   */
  private ArrayList<Node>updateConditionMatchesWithAttribute(String attributeName, String attributeText, ArrayList<Node>currentMatches)
  {
	  ArrayList<Node> newPossibleMatches = new ArrayList<Node>();
	  for(Node currentNode : currentMatches)
	  {
		  NamedNodeMap allCurrentAttributes = currentNode.getAttributes();
		  if(allCurrentAttributes!=null)
		  {
			  Node selectedAttribute = allCurrentAttributes.getNamedItem(attributeName);
			  if(selectedAttribute!=null&&selectedAttribute.getNodeValue().equals(attributeText))
				  newPossibleMatches.add(currentNode);
		  }
	  }
	  return newPossibleMatches;
  }
  /**
   * This removes Nodes from the first arraylist that are not ancestors or equal to nodes in the second list.
   * It returns the updated arraylist.
   * @param possibleMatchesBeforeCondition
   * @param possibleMatchesAfterCondition
   * @return
   */
  private ArrayList<Node> removeNodesNotMatchingIntermediateCondition(ArrayList<Node> possibleMatchesBeforeCondition, ArrayList<Node> possibleMatchesAfterCondition)
  {
	  ArrayList<Node> newListOfMatches = new ArrayList<Node>();
	  for(Node currentDescendantNode : possibleMatchesAfterCondition)
	  {
		  //System.out.println("current condition matched nodes in removeNodesNotMatchingCondition: "+currentDescendantNode.getNodeName());
		  for(Node currentOriginalNode : possibleMatchesBeforeCondition)
		  {
			  //System.out.println("current original node in condition matched nodes in removeNodesNotMatchingCondition: "+currentOriginalNode.getNodeName());
			  //if the node after matching is the same as it was before the condition, e.g. its not a child of the original,
			  //then add the original to the new list again.
			  if(currentOriginalNode.isEqualNode(currentDescendantNode)&&!newListOfMatches.contains(currentOriginalNode))
				  newListOfMatches.add(currentOriginalNode);
				
			  //System.out.println("compare document position result = "+currentDescendantNode.compareDocumentPosition(currentOriginalNode));
			  //if the original node contains the descendant node then this is a node that should be kept as a possible match
			  if((currentDescendantNode.compareDocumentPosition(currentOriginalNode)== (Node.DOCUMENT_POSITION_CONTAINS+Node.DOCUMENT_POSITION_PRECEDING)))
			  {
				  //System.out.println("document position contains");
				  if(!newListOfMatches.contains(currentOriginalNode))
					  newListOfMatches.add(currentOriginalNode);
			  }
			  else if(currentDescendantNode.compareDocumentPosition(currentOriginalNode)==Node.DOCUMENT_POSITION_CONTAINED_BY)
			  {
				  //System.out.println("document position contained by");
			  }
		  }
	  }
	  return newListOfMatches;
	  
  }
  /**
   * This method removes nodes from the global list if they are not ancestor nodes of the nodes in the passed in ArrayList, 
   * meaning the node did not match the condition and is no longer a possible match.
   * @param possibleMatchesAfterCondition
   */
  private void removeNodesNotMatchingCondition(ArrayList<Node> possibleMatchesAfterCondition)
  {
	  ArrayList<Node> newListOfMatches = new ArrayList<Node>();
	  for(Node currentDescendantNode : possibleMatchesAfterCondition)
	  {
		  //System.out.println("current condition matched nodes in removeNodesNotMatchingCondition: "+currentDescendantNode.getNodeName());
		  for(Node currentOriginalNode : currentPossibleDOMMatches)
		  {
			  //System.out.println("current original node in condition matched nodes in removeNodesNotMatchingCondition: "+currentOriginalNode.getNodeName());
			  //if the node after matching is the same as it was before the condition, e.g. its not a child of the original,
			  //then add the original to the new list again.
			  if(currentOriginalNode.isEqualNode(currentDescendantNode)&&!newListOfMatches.contains(currentOriginalNode))
				  newListOfMatches.add(currentOriginalNode);
				
			  //System.out.println("compare document position result = "+currentDescendantNode.compareDocumentPosition(currentOriginalNode));
			  //if the original node contains the descendant node then this is a node that should be kept as a possible match
			  if((currentDescendantNode.compareDocumentPosition(currentOriginalNode)== (Node.DOCUMENT_POSITION_CONTAINS+Node.DOCUMENT_POSITION_PRECEDING)))
			  {
				  //System.out.println("document position contains");
				  if(!newListOfMatches.contains(currentOriginalNode))
					  newListOfMatches.add(currentOriginalNode);
			  }
			  else if(currentDescendantNode.compareDocumentPosition(currentOriginalNode)==Node.DOCUMENT_POSITION_CONTAINED_BY)
			  {
				  //System.out.println("document position contained by");
			  }
		  }
	  }
	  currentPossibleDOMMatches = newListOfMatches;
  }
        
}

