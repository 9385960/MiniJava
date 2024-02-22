package miniJava;

import java.io.FileInputStream;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

public class Compiler {
	// Main function, the file to compile will be an argument.
	public static void main(String[] args) {
		// TODO: Instantiate the ErrorReporter object
		ErrorReporter error = new ErrorReporter();
		// TODO: Check to make sure a file path is given in args
		AST tree = null;
		if(args.length == 0)
		{
			error.reportError("File path not given");
		}else{
			// TODO: Create the inputStream using new FileInputStream
			try{
				FileInputStream stream = new FileInputStream(args[0]);
				Scanner scan = new Scanner(stream,error);
				Parser p = new Parser(scan, error);
				tree = p.parse();

			}catch(Exception e){
				error.reportError("File Not Found");
			}
			// TODO: Instantiate the scanner with the input stream and error object
			
			// TODO: Instantiate the parser with the scanner and error object
			
			// TODO: Call the parser's parse function
			
			// TODO: Check if any errors exist, if so, println("Error")
			//  then output the errors
			
			// TODO: If there are no errors, println("Success")
		}
		if(error.hasErrors())
		{
			System.out.println("Error");
			error.outputErrors();
		}else{
			ASTDisplay disp = new ASTDisplay();
			disp.showTree(tree);
		}
	}
}
