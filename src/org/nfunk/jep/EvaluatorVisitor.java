/*****************************************************************************

@header@
@date@
@copyright@
@license@

*****************************************************************************/

package org.nfunk.jep;

import java.util.*;
import org.nfunk.jep.function.PostfixMathCommandI;
import org.nfunk.jep.function.SpecialEvaluationI;

/**
 * This class is used for the evaluation of an expression. It uses the Visitor
 * design pattern to traverse the function tree and evaluate the expression
 * using a stack.
 * <p>
 * Function nodes are evaluated by first evaluating all the children nodes,
 * then applying the function class associated with the node. Variable and
 * constant nodes are evaluated by pushing their value onto the stack.

 * <p>
 * Some changes implemented by rjm. Nov 03.
 * Added hook to SpecialEvaluationI.
 * Clears stack before evaluation.
 * Simplifies error handling by making visit methods throw ParseException.
 * Changed visit(ASTVarNode node) so messages not calculated every time. 
 */
public class EvaluatorVisitor implements ParserVisitor {
	/** Stack used for evaluating the expression */
	protected Stack stack;

	/** The current error list */
	//protected Vector errorList;

	/** The symbol table for variable lookup */
	protected SymbolTable symTab;

	/** Flag for errors during evaluation */
	//protected boolean errorFlag;

	/** Debug flag */
	protected static final boolean debug = false;

	/** TrapNull **/
	protected boolean trapNullValues = true;
	
	/** Constructor. Initialise the stack member */
	public EvaluatorVisitor() {
		//errorList = null;
		symTab = null;
		stack = new Stack();
	}

	/**
	 * Adds an error message to the list of errors
	 */
	/*protected void addToErrorList(String errorStr) {
		if (errorList != null) {
			errorList.addElement(errorStr);
		}
	}*/

	/**
	 * Returns the value of the expression as an object. The expression
	 * tree is specified with its top node. The algorithm uses a stack
	 * for evaluation.
	 * <p>
	 * The <code>errorList_in</code> parameter is used to
	 * add error information that may occur during the evaluation. It is not
	 * required, and may be set to <code>null</code> if no error information is
	 * needed.
	 * <p>
	 * The symTab parameter can be null, if no variables are expected in the
	 * expression. If a variable is found, an error is added to the error list.
	 * <p>
	 * An exception is thrown, if an error occurs during evaluation.
	 * @return The value of the expression as an object.
	 * @throws ParseException if some problem with evaluation.
	 */
	public Object getValue(Node topNode,SymbolTable symTab_in)
		throws ParseException {

		// check if arguments are ok
		if (topNode == null) {
			throw new ParseException("topNode parameter is null");
		}

		// set member vars
		//errorList = errorList_in;
		symTab = symTab_in;
		//errorFlag = false;
		stack.removeAllElements();
		// rjm addition ensure stack is correct before beginning.
		// njf changed from clear() to removeAllElements for 1.1 compatibility

		// evaluate by letting the top node accept the visitor
		topNode.jjtAccept(this,null);
		/*
		} catch (ParseException e) {
			this.addToErrorList("Error: "+e.getMessage());
			return null;
		}
		if(errorFlag) return null;
		*/
		
		// something is wrong if not exactly one item remains on the stack
		// or if the error flag has been set
		if (stack.size() != 1) {
			throw new ParseException("Stack corrupted");
		}

		// return the value of the expression
		return stack.pop();
	}

	/*
	 * The following methods was used to facilitate 
	 * using visitors which implemented a interface
	 * which sub-classed ParserVisitor.
	 *  
	 * If sub-classed to extend to implement a different visitor
	 * this method should be overwritten to ensure the correct 
	 * accept method is called.
	 * This method simply calls the jjtAccept(ParserVisitor this,Object data) of node.
	 *
	 * We no longer need this as we use ParseVisitor everywhere,
	 * but kept for future reference.
	 * 
	private Object nodeAccept(Node node, Object data) throws ParseException
	{
		return node.jjtAccept(this,data);
	}
	*/

	/**
	 * This method should never be called when evaluation a normal
	 * expression.
	 */
	public Object visit(SimpleNode node, Object data) throws ParseException {
		throw new ParseException(
			"No visit method for " + node.getClass().getName());
	}

	/**
	 * This method should never be called when evaluating a normal
	 * expression.
	 */
	public Object visit(ASTStart node, Object data) throws ParseException {
		throw new ParseException("Start node encountered during evaluation");
	}

	/**
	 * Visit a function node. The values of the child nodes
	 * are first pushed onto the stack. Then the function class associated
	 * with the node is used to evaluate the function.
	 * <p>
	 * If a function implements SpecialEvaluationI then the
	 * evaluate method of PFMC is called.
	 */
	public Object visit(ASTFunNode node, Object data) throws ParseException {

		if (node == null)
			return null;
		PostfixMathCommandI pfmc = node.getPFMC();

		// check if the function class is set
		if (pfmc == null)
			throw new ParseException(
				"No function class associated with " + node.getName());

		// Some operators (=) need a special method for evaluation
		// as the pfmc.run method does not have enough information
		// in such cases we call the evaluate method which passes
		// all available info. Note evaluating the children is
		// the responsibility of the evaluate method. 
		if (pfmc instanceof SpecialEvaluationI) {
			return ((SpecialEvaluationI) node.getPFMC()).evaluate(
				node,data,this,stack,this.symTab);
		}

		if (debug == true) {
			System.out.println(
				"Stack size before childrenAccept: " + stack.size());
		}

		// evaluate all children (each leaves their result on the stack)

		data = node.childrenAccept(this, data);

		if (debug == true) {
			System.out.println(
				"Stack size after childrenAccept: " + stack.size());
		}

		if (pfmc.getNumberOfParameters() == -1) {
			// need to tell the class how many parameters it can take off
			// the stack because it accepts a variable number of params
			pfmc.setCurNumberOfParameters(node.jjtGetNumChildren());
		}

		// try to run the function

		pfmc.run(stack);

		if (debug == true) {
			System.out.println("Stack size after run: " + stack.size());
		}

		return data;
	}

	/**
	 * Visit a variable node. The value of the variable is obtained from the
	 * symbol table (symTab) and pushed onto the stack.
	 */
	public Object visit(ASTVarNode node, Object data) throws ParseException {

		// old code
		//		if (symTab == null)
		//			throw new ParseException(message += "the symbol table is null");

		// Optimise (table lookup is costly?)
		//		Object temp = symTab.get(node.getName());

		// new code

		Variable var = node.getVar();
		if (var == null) {
			String message = "Could not evaluate " + node.getName() + ": ";
			throw new ParseException(message + " variable not set");
		}

		Object temp = var.getValue();

		if (trapNullValues && temp == null) {
			String message = "Could not evaluate " + node.getName() + ": ";
			throw new ParseException(message + "the variable was not found in the symbol table");
		}
		// all is fine
		// push the value on the stack
		stack.push(temp);
		return data;
	}

	/**
	 * Visit a constant node. The value of the constant is pushed onto the
	 * stack.
	 */
	public Object visit(ASTConstant node, Object data) {
		stack.push(node.getValue());
		return data;
	}
	
	
	/**
	 * Tests whether null variable values are trapped by evaluator. 
	 * @return true is nulls are trapped
	 */
	public boolean isTrapNullValues()
	{
		return trapNullValues;
	}

	/**
	 * Sets the behaviour when a variable's value is null.
	 * If true an exception will be thrown is a variable value is null.
	 * If false then the value will be passed to other functions, this may cause error
	 * else where.
	 * 
	 * @param b 
	 */
	public void setTrapNullValues(boolean b)
	{
		trapNullValues = b;
	}

}
