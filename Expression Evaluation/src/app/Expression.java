package app;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import structures.Stack;

public class Expression {

	public static String delims = " \t*+-/()[]";
			
    /**
     * Populates the vars list with simple variables, and arrays lists with arrays
     * in the expression. For every variable (simple or array), a SINGLE instance is created 
     * and stored, even if it appears more than once in the expression.
     * At this time, values for all variables and all array items are set to
     * zero - they will be loaded from a file in the loadVariableValues method.
     * 
     * @param expr The expression
     * @param vars The variables array list - already created by the caller
     * @param arrays The arrays array list - already created by the caller
     */
    public static void 
    makeVariableLists(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) 
    { //used regex
    	Pattern varPattern = Pattern.compile("[a-zA-Z]+\\b(?!\\[)");
        Pattern arrPattern = Pattern.compile("[a-zA-Z]+\\b(?=\\[)");

        Matcher m = varPattern.matcher(expr);
        while (m.find()) {
            boolean add = true;
            String name = m.group();
            for (Variable var : vars) {
                if (name.equals(var.name)) {
                    add = false;
                    break;
                }
            }
            if (add) {
                vars.add(new Variable(m.group()));
            }
        }

        m = arrPattern.matcher(expr);
        while (m.find()) {
            boolean add = true;
            String name = m.group();
            for (Array arr : arrays) {
                if (name.equals(arr.name)) {
                    add = false;
                    break;
                }
            }
            if (add) {
                arrays.add(new Array(m.group()));
            }
        }
    }
    
    /**
     * Loads values for variables and arrays in the expression
     * 
     * @param sc Scanner for values input
     * @throws IOException If there is a problem with the input 
     * @param vars The variables array list, previously populated by makeVariableLists
     * @param arrays The arrays array list - previously populated by makeVariableLists
     */
    public static void 
    loadVariableValues(Scanner sc, ArrayList<Variable> vars, ArrayList<Array> arrays) 
    throws IOException {
        while (sc.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(sc.nextLine().trim());
            int numTokens = st.countTokens();
            String tok = st.nextToken();
            Variable var = new Variable(tok);
            Array arr = new Array(tok);
            int vari = vars.indexOf(var);
            int arri = arrays.indexOf(arr);
            if (vari == -1 && arri == -1) {
            	continue;
            }
            int num = Integer.parseInt(st.nextToken());
            if (numTokens == 2) { // scalar symbol
                vars.get(vari).value = num;
            } else { // array symbol
            	arr = arrays.get(arri);
            	arr.values = new int[num];
                // following are (index,val) pairs
                while (st.hasMoreTokens()) {
                    tok = st.nextToken();
                    StringTokenizer stt = new StringTokenizer(tok," (,)");
                    int index = Integer.parseInt(stt.nextToken());
                    int val = Integer.parseInt(stt.nextToken());
                    arr.values[index] = val;              
                }
            }
        }
    }
    
    /**
     * Evaluates the expression.
     * 
     * @param vars The variables array list, with values for all variables in the expression
     * @param arrays The arrays array list, with values for all array items
     * @return Result of evaluation
     */
    public static float 
    evaluate(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
    	
    	expr = expr.replaceAll(" ","");
    	expr = expr.replaceAll("\t","");
    	
    	try {
    		return Float.parseFloat(expr);
    	} catch(NumberFormatException e) {}
    	
    	for(int i=0;i<vars.size();i++) {
    		while(expr.contains(vars.get(i).name)) {   			
    			int index = expr.indexOf(vars.get(i).name);
    			expr = expr.substring(0, index) + vars.get(i).value + expr.substring(index + vars.get(i).name.length());
    		}
    		
    	}
    	
    	Stack<Integer> openParr = new Stack<Integer>();
    	Stack<Integer> openBrac = new Stack<Integer>();
    	Stack<Integer> closedBrac = new Stack<Integer>();
    	
    	for(int i=0;i<expr.length();i++) {
    		if(expr.charAt(i)=='(') {
    			openParr.push(i);
    		}
    	}
    	
    	while (!openParr.isEmpty()) {
    		for (int i = 0; i < expr.length(); i++) {
    			if (expr.charAt(i) == ')' && i > openParr.peek()) {
    				float result = evaluate(expr.substring(openParr.peek() + 1, i), vars, arrays);
    				
    				expr = expr.substring(0, openParr.pop()) + result + expr.substring(i + 1);
    				break;
    			}
    		}
    	}
    	
    	String newExpr = expr;
    	for (int i = 0; i < expr.length(); i++) {
    		if (Character.isLetter(expr.charAt(i))) {
    		for (int iVars = 0; iVars < vars.size(); iVars++) {
    			if (i + vars.get(iVars).name.length() <= expr.length()) {
    				if (expr.substring(i, i + vars.get(iVars).name.length()).equals(vars.get(iVars).name)) {
        				float res = vars.get(iVars).value;
        				
        				newExpr = newExpr.substring(0, i) + res + expr.substring(i + vars.get(iVars).name.length());
        				i += vars.get(iVars).name.length();
        				break;
        			}
    			}
    		} 
    		}
    	}
    	expr = newExpr;
    	
    	for (int i = 0; i < expr.length(); i++) {
    		if (expr.charAt(i) == '[')
    			openBrac.push(i);
    	}
    	for (int i = 0; i < expr.length(); i++) {
    		if (expr.charAt(i) == ']')
    			closedBrac.push(i);
    	}   	
    	while (!openBrac.isEmpty()) {
    		for (int i = 0; i < newExpr.length(); i++) {
    			if (newExpr.charAt(i) == '[')
    				break;
    			else if (i == newExpr.length() - 1)
    				openBrac.pop();
    		}
    		A: for (int i = 0; i < newExpr.length(); i++) {
    			if (newExpr.charAt(i) == ']' && i > openBrac.peek()) {
    				int open = openBrac.pop();
    				float res = evaluate(newExpr.substring(open + 1, i), vars, arrays);
    				for (int j = 0; j < arrays.size(); j++) {
    		    		if (open - arrays.get(j).name.length() >= 0) {
    		    			if (newExpr.substring(open - arrays.get(j).name.length(), open).equals(arrays.get(j).name)) {
    		       				newExpr = newExpr.substring(0, open - arrays.get(j).name.length()) + arrays.get(j).values[(int)res] + newExpr.substring(i + 1);
    		       				if (!closedBrac.isEmpty())
    		       					closedBrac.pop();
    		    				break A;
   		        			}
   		    			}
    				}
    		   	}
    		}
    	}
    	expr = newExpr;
    	String orig = expr;
    	
    	for (int i = 1; i < expr.length(); i++) {
    		if (expr.charAt(i) == '*' || expr.charAt(i) == '/') {
    			int left = 0, right = 0;
    			float temp1 = 0, temp2 = 0, result;
    			
    			for (int leftI = i - 1; leftI >= 0; leftI--) {
    				try {
    					temp1 = Float.parseFloat(expr.substring(leftI, i));
    					if (expr.charAt(leftI) == '+')
    						break;
    					left = leftI;
    				} catch (NumberFormatException e) {
    					break;
    				}
    			}
    			
    			for (int rightI = i + 2; rightI <= expr.length(); rightI++) {
    				if (!expr.substring(i + 1, rightI).equals("-")) {
    					try {
    						temp2 = Float.parseFloat(expr.substring(i + 1, rightI));
    						right = rightI;
    					} catch (NumberFormatException e) {
    						break;
    					}
    				}
    			}
    			
    			
    			switch(expr.charAt(i)) {
    			case '*':
    				result = temp1*temp2;
    				break;
    			default:
    				result = temp1/temp2;
    				break;
    			}
    				
    			
    			orig = expr.substring(0, left) + result + expr.substring(right);
    			
    			try {
    				float res = Float.parseFloat(orig);
    				return res;
    			} catch (NumberFormatException e) {
    				return evaluate(orig, vars, arrays);
    			}
    		}
    	}
    		
    	for (int i = 1; i < expr.length(); i++) {
    		if (expr.charAt(i) == '+' || expr.charAt(i) == '-') {
    			int left = 0, right = 0;
    			float temp1 = 0, temp2 = 0, result;
    			
    			for (int iLeft = i - 1; iLeft >= 0; iLeft--) {
    				try {
    					temp1 = Float.parseFloat(expr.substring(iLeft, i));
    					left = iLeft;
    				} catch (NumberFormatException e) {
    					break;
    				}
    			}
    		
    			for (int iRight = i + 2; iRight <= expr.length(); iRight++) {
    				if (!expr.substring(i + 1, iRight).equals("-")) {
    					try {
    						temp2 = Float.parseFloat(expr.substring(i + 1, iRight));
    						right = iRight;
    					} catch (NumberFormatException e) {
    						break;
    					}
    				}
    			}
    			
    			switch(expr.charAt(i)) {
    			case '+':
    				result = temp1 + temp2;
    				break;
    				
    			default:
    				result = temp1 - temp2;
    				break;
    			
    			
    			}
    			
    			orig = expr.substring(0, left) + result + expr.substring(right);
    			
    			try {
    				float res = Float.parseFloat(orig);
    				return res;
    			} catch (NumberFormatException e) {
    				return evaluate(orig, vars, arrays);
    			}
    		}
    	}
    	
    	return evaluate(orig, vars, arrays);
    }
    }

