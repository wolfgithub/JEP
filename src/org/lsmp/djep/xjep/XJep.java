/* @author rich
 * Created on 16-Nov-2003
 *
 * This code is covered by a Creative Commons
 * Attribution, Non Commercial, Share Alike license
 * <a href="http://creativecommons.org/licenses/by-nc-sa/1.0">License</a>
 */
package org.lsmp.djep.xjep;

import org.nfunk.jep.*;
import java.util.*;
import java.io.PrintStream;
/**
 * An extended version of JEP adds various routines for working with trees.
 * Has a NodeFactory, and OperatorSet, TreeUtils
 * and Visitors DoNothingVisitor,
 * @author Rich Morris
 * Created on 16-Nov-2003
 */
public class XJep extends JEP implements XJepI {
	/** Creates new nodes */
	public NodeFactory nf = null;
	/** Collects operators **/
	public OperatorSet opSet = null;
	/** A few utility functions. */
	public TreeUtils tu = null;
	public DeepCopyVisitor copier = null;
	public SubstitutionVisitor subv = null;
	public SimplificationVisitor simpv = null;
	public CommandVisitor commandv = null;
	public PrintVisitor pv = null;

	public XJep()
	{
		/** Creates new nodes */
		nf = new NodeFactory();
		/** Collects operators **/
		opSet = new OperatorSet(this.getFunctionTable());
		/** A few utility functions. */
		tu = TreeUtils.getInstance();
		copier = new DeepCopyVisitor();
		subv = new SubstitutionVisitor();
		ev = new EvaluatorVisitor();
		simpv = new SimplificationVisitor();
		commandv = new CommandVisitor();
		pv = new PrintVisitor();
	}

	protected XJep(XJep j)
	{
		super((JEP) j);
		this.commandv=j.commandv;
		this.copier=j.copier;
		this.ev=j.ev;
		this.nf=j.nf;
		this.opSet=j.opSet;
		this.pv=j.pv;
		this.simpv=j.simpv;
		this.subv=j.subv;
		this.tu=j.tu;
	}

	public XJepI newInstance()
	{
		XJep newJep = new XJep(this);
		return newJep;
	}
	public XJepI newInstance(SymbolTable st)
	{
		XJep newJep = new XJep(this);
		newJep.symTab = st;
		return newJep;
	}
	
	public Node deepCopy(Node node) throws ParseException
	{
		return copier.deepCopy(node,this);
	}
	public Object evaluate(Node node) throws Exception
	{
		return ev.getValue(node,new Vector(),this.symTab);
	}
	public Node simplify(Node node) throws ParseException
	{
		return simpv.simplify(node,this);
	}
	public Node preprocess(Node node) throws ParseException
	{
		return commandv.process(node,this);
	}
	public Node substitute(Node orig,String name,Node replacement) throws ParseException
	{
		return subv.substitute(orig,name,replacement,this);
	}
	public Node substitute(Node orig,String names[],Node replacements[]) throws ParseException
	{
		return subv.substitute(orig,names,replacements,this);
	}
	/**
	 * Prints a node on System.out.
	 * See PrintVisitor for other methods
	 */
	public void print(Node node) { pv.print(node); }
	public void print(Node node,PrintStream out) { pv.print(node,out); }
	public void println(Node node) { pv.println(node); }
	public void println(Node node,PrintStream out) { pv.println(node,out); }
	public String toString(Node node) { return pv.toString(node); }
		
	public NodeFactory getNodeFactory() {return nf;}
	public OperatorSet getOperatorSet() {return opSet;}
	public TreeUtils getTreeUtils() { return tu; }
//	public SimplificationVisitor getSimpV() { return simpv; }

}