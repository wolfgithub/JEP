/*****************************************************************************

JEP - Java Expression Parser
    JEP is a Java package for parsing and evaluating mathematical
	expressions. It currently supports user defined variables,
	constant, and functions. A number of common mathematical
	functions and constants are included.

Author: Nathan Funk
Copyright (C) 2000 Nathan Funk

    JEP is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    JEP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with JEP; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

*****************************************************************************/

package org.nfunk.jep;

import java.io.*;
import java.util.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;
import org.nfunk.jep.type.*;

/**
 * The JEP class is the main interface with which the user should
 * interact. It contains all neccessary methods to parse and evaluate
 * expressions.
 * <p>
 * The most important methods are parseExpression(String), for parsing the
 * mathematical expression, and getValue() for obtaining the value of the
 * expression.
 * <p>
 * Visit <a href="http://jep.sourceforge.net/">http://jep.sourceforge.net/</a>
 * for the newest version of JEP, and complete documentation.
 *
 * @author Nathan Funk
 */
public class JEP {

	/** Debug flag for extra command line output */
	private boolean debug;
	
	/** Traverse option */
	private boolean traverse;
	
	/** Allow undeclared variables option */
	protected boolean allowUndeclared;
	
	/** Implicit multiplication option */
	protected boolean implicitMul;
	
	/** Symbol Table */
	protected SymbolTable symTab;

	/** Function Table */
	protected FunctionTable funTab;
	
	/** Error List */
	protected Vector errorList;

	/** The parser object */
	private Parser parser;
	
	/** Node at the top of the parse tree */
	private Node topNode;


	/**
	 * Constructor; initializes all member variables.
	 */
	public JEP() {
		debug = false;
		topNode = null;
		traverse = false;
		allowUndeclared = false;
		implicitMul = false;
		initSymTab();
		initFunTab();
		errorList = new Vector();

		parser = new Parser(new StringReader(""));

		//Ensure errors are reported for the initial expression
		//e.g. No expression entered
		parseExpression("");
	}

	/**
	 * Creates a new SymbolTable object as symTab.
	 */
	public void initSymTab() {
		//Init SymbolTable
		symTab = new SymbolTable();
	}

	/**
	 * Creates a new FunctionTable object as funTab.
	 */
	public void initFunTab() {
		//Init FunctionTable
		funTab = new FunctionTable();
	}

	/**
	 * Adds the standard functions to the parser. If this function is not called
	 * before parsing an expression, functions such as sin() or cos() would
	 * produce an "Unrecognized function..." error.
	 * In most cases, this method should be called immediately after the JEP
	 * object is created.
	 */
	public void addStandardFunctions() {
		//add functions to Function Table
		funTab.put("sin", new Sine());
		funTab.put("cos", new Cosine());
		funTab.put("tan", new Tangent());
		funTab.put("asin", new ArcSine());
		funTab.put("acos", new ArcCosine());
		funTab.put("atan", new ArcTangent());

		funTab.put("sinh", new SineH());
		funTab.put("cosh", new CosineH());
		funTab.put("tanh", new TanH());
		funTab.put("asinh", new ArcSineH());
		funTab.put("acosh", new ArcCosineH());
		funTab.put("atanh", new ArcTanH());

		funTab.put("log", new Logarithm());
		funTab.put("ln", new NaturalLogarithm());

		funTab.put("sqrt",new SquareRoot());
		funTab.put("angle", new Angle());
		funTab.put("abs", new Abs());
		funTab.put("mod", new Modulus());
		funTab.put("sum", new Sum());

		funTab.put("rand", new org.nfunk.jep.function.Random());
	}

	/**
	 * Adds the constants pi and e to the parser. As addStandardFunctions(), this
	 * method should be called immediatly after the JEP object is created.
	 */
	public void addStandardConstants() {
		//add constants to Symbol Table
		symTab.put("pi", new Double(Math.PI));
		symTab.put("e", new Double(Math.E));
	}
	
	/**
	 * Call this function if you want to parse expressions which involve
	 * complex numbers. This method specifies "i" as the imaginary unit
	 * (0,1). Two functions re() and im() are also added for extracting the
	 * real or imaginary components of a complex number respectively.
	 */
	public void addComplex() {
		//add constants to Symbol Table
		symTab.put("i", new Complex(0,1));
		funTab.put("re", new Real());
		funTab.put("im", new Imaginary());
	}

	/**
	 * Adds a new function to the parser. This must be done before parsing
	 * an expression so the parser is aware that the new function may be
	 * contained in the expression.
	 * @param functionName The name of the function
	 * @param function The function object that is used for evaluating the
	 * function
	 */
	public void addFunction(String functionName,
							PostfixMathCommandI function) {
		funTab.put(functionName, function);
	}

	/**
	 * Adds a new variable to the parser, or updates the value of an
	 * existing variable. This must be done before parsing
	 * an expression so the parser is aware that the new variable may be
	 * contained in the expression.
	 * @param name Name of the variable to be added
	 * @param value Initial value or new value for the variable
	 * @return Double object of the variable
	 */
	public Double addVariable(String name, double value) {
		Double object = new Double(value);
		symTab.put(name, object);
		return object;
	}

	/**
	 * Adds a new complex variable to the parser, or updates the value of an
	 * existing variable. This must be done before parsing
	 * an expression so the parser is aware that the new variable may be
	 * contained in the expression.
	 * @param name Name of the variable to be added
	 * @param re Initial real value or new real value for the variable
	 * @param re Initial imaginary value or new imaginary value for the variable
	 * @return Complex object of the variable
	 */
	public Complex addComplexVariable(String name, double re, double im) {
		Complex object = new Complex(re,im);
		symTab.put(name, object);
		return object;
	}
		
	/**
	 * Adds a new variable to the parser as an object, or updates the value of an
	 * existing variable. This must be done before parsing
	 * an expression so the parser is aware that the new variable may be
	 * contained in the expression.
	 * @param name Name of the variable to be added
	 * @param object Initial value or new value for the variable
	 */
	public void addVariableAsObject(String name, Object object) {
		symTab.put(name, object);
	}

	/**
	 * Sets the value of the traverse option. setTraverse is useful for
	 * debugging purposes. When traverse is set to true, the parse-tree
	 * will be dumped to the standard ouput device.
	 * <p>
	 * The default value is false.
	 * @param value The boolean traversal option.
	 */
	public void setTraverse(boolean value) {
		traverse = value;
	}

	/**
	 * Sets the value of the implicit multiplication option.
	 * If this option is set to true before parsing, implicit multiplication
	 * will be allowed. That means that an expression such as
	 * <pre>"1 2"</pre> is valid and is interpreted as <pre>"1*2"</pre>.
	 * <p>
	 * The default value is false.
	 * @param value The boolean implicit multiplication option.
	 */
	public void setImplicitMul(boolean value) {
		implicitMul = value;
	}
	
	/**
	 * Sets the value for the undeclared variables option. If this option
	 * is set to true, expressions containing variables that were not
	 * previously added to JEP will not produce an "Unrecognized Symbol"
	 * error. The new variables will automatically be added while parsing,
	 * and initialized to 0.
	 * <p>
	 * If this option is set to false, variables that were not previously
	 * added to JEP will produce an error while parsing.
	 * <p>
	 * The default value is false.
	 * @param value The boolean option for allowing undeclared variables.
	 */
	public void setAllowUndeclared(boolean value) {
		allowUndeclared = value;
	}

	/**
	 * Parses the expression. If there are errors in the expression,
	 * they are added to the <code>errorList</code> member.
	 *
	 * @param expression_in The input expression string
	 */
	public void parseExpression(String expression_in) {
		Reader reader = new StringReader(expression_in);
		
		try {
			// try parsing
			errorList.clear();
			topNode = parser.parseStream(reader, this);
		} catch (Throwable e) {
			// an exception was thrown, so there is no parse tree
			topNode = null;
			
			// check the type of error
			if (e instanceof ParseException) {
				// the ParseException object contains additional error
				// information
				errorList.add(((ParseException)e).getErrorInfo());
			} else {
				// if the exception was not a ParseException, it was most
				// likely a syntax error
				if (debug) {
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				errorList.add("Syntax error");
			}
		}
		
				
		// If traversing is enabled, print a dump of the tree to
		// standard output
		if (traverse && !hasError()) {
			ParserVisitor v = new ParserDumpVisitor();
			topNode.jjtAccept(v, null);
		}
	}


	/**
	 * Evaluates and returns the value of the expression. If the value is
	 * complex, the real component of the complex number is returned. To
	 * get the complex value, use getComplexValue().
	 * @return The calculated value of the expression. If the value is
	 * complex, the real component is returned. If an error occurs during
	 * evaluation, 0 is returned.
	 */
	public double getValue() {
		Object value = getValueAsObject();
		
		if (value==null) {
			return 0;
		} else if (value instanceof Double) {
			return ((Double)value).doubleValue();
		} else if (value instanceof Complex) {
			return ((Complex)value).re();
		} else {
			return 0;
		}
	}


	/**
	 * Evaluates and returns the value of the expression as a complex number.
	 * @return The calculated value of the expression as a complex number if
	 * no errors occur. Returns null otherwise.
	 */
	public Complex getComplexValue() {
		Object value = getValueAsObject();
		
		if (value == null) {
			return null;
		} else if (value instanceof Double) {
			return new Complex(((Double)value).doubleValue(), 0);
		} else if (value instanceof Complex) {
			return (Complex)value;
		} else {
			return null;
		}
	}



	/**
	 * Evaluates and returns the value of the expression as an object.
	 * The EvaluatorVisitor member ev is used to do the evaluation procedure.
	 * This method is useful when the type of the value is unknown, or
	 * not important.
	 * @return The calculated value of the expression if no errors occur.
	 * Returns null otherwise.
	 */
	public Object getValueAsObject() {
		Object result;
		
		if (topNode != null) {
			//
			errorList.clear();

			// evaluate the expression
			try {
				result =
					EvaluatorVisitor.getInstance().getValue(topNode,errorList);
			} catch (Exception e) {
				if (debug) System.out.println(e);
				return null;
			}
			
			return result;
		} else {
			return null;
		}
	}

	/**
	 * @return Returns <code>true</code> if an error occured during the most
	 * recent action (parsing or evaluation).
	 */
	public boolean hasError() {
		return !errorList.isEmpty();
	}

	/**
	 * Reports information on the errors that occured during the most recent
	 * action.
	 * @return A string containing information on the errors, each separated
	 * by a newline character; null if no error has occured
	 */
	public String getErrorInfo() {
		if (hasError()) {
			String str = "";
			
			// iterate through all errors and add them to the return string
			for (int i=0; i<errorList.size(); i++) {
				str += errorList.elementAt(i) + "\n";
			}
			
			return str;
		} else {
			return null;
		}
	}

	/**
	 * Returns the top node of the expression tree.
	 * @return The top node of the expression tree
	 */
	public Node getTopNode() {
		return topNode;
	}

	/**
	 * Returns the symbol table
	 * @return The symbol table
	 */
	public SymbolTable getSymbolTable() {
		return symTab;
	}

//------------------------------------------------------------------------
// Old code


/*
	/**
	* Returns the position (vertical) at which the last error occured.
	/
	public int getErrorColumn() {
		if (hasError && parseException != null)
			return parseException.getColumn();
		else
			return 0;
	}

	/**
	* Returns the line in which the last error occured.
	/
	public int getErrorLine() {
		if (hasError && parseException != null)
			return parseException.getLine();
		else
			return 0;
	}
*/

	/**
	* Evaluates and returns the value of the expression as an object.
	* This method is useful when the type of the value is unknown, or
	* not important.
	* @return The calculated value of the expression if no errors occur.
	* Returns null otherwise.
	*/
/*	public Object getValueAsObject() {
		//ensure the stack is empty
		stack.clear();
		
		if (!hasError && topNode != null)
		{
			try {
				// evaluate the expression
				topNode.evaluate(stack);
				return stack.pop();
			}
			catch (Exception e)
			{
				hasError = true;
				if (e instanceof ParseException) parseException = (ParseException)e;
				return null;
			}
		}
		
		// something went wrong
		return null;
	}
*/

}

