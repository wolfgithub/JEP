/* @author rich
 * Created on 22-Mar-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djepExamples;

import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.lsmp.djep.xjep.XJep;
import org.lsmp.djep.sjep.PolynomialCreator;

/**
 * @author Rich Morris
 * Created on 22-Mar-2005
 */
public class SJepConsole extends XJepConsole
{
	PolynomialCreator pc = null;
	
	public static void main(String[] args)
	{
		Console c = new SJepConsole();
		c.run(args);
	}

	public String getPrompt()
	{
		return "SJep > ";
	}

	public void initialise()
	{
		super.initialise();
		pc = new PolynomialCreator((XJep) j);
	}

	public void printIntroText()
	{
		println("SJep: advanced simplification/expansion");
	}

	public void processEquation(Node node) throws ParseException
	{
		XJep xj = (XJep) j;

		Node simp = pc.simplify(node);
		print("Simplified:\t"); 
		println(xj.toString(simp));

		Node expand = pc.expand(node);
		print("Expanded:\t"); 
		println(xj.toString(expand));

		Object val = xj.evaluate(simp);
		String s = xj.getPrintVisitor().formatValue(val);
		println("Value:\t\t"+s);
	}

}
