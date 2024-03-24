package miniJava;

import java.io.FileInputStream;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.ContextualAnalysis.Identification;
import miniJava.ContextualAnalysis.ScopedIdentification;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

public class Compiler {
	// Main function, the file to compile will be an argument.
	public static void main(String[] args) {
		//Instantiate the ErrorReporter object
		ErrorReporter error = new ErrorReporter();
		//Check to make sure a file path is given in args
		AST tree = null;

		ScopedIdentification.init(error);

		if(args.length == 0)
		{
			error.reportError("File path not given");
		}else{
			//Create the inputStream using new FileInputStream
			try{
				FileInputStream stream = new FileInputStream(args[0]);
				Scanner scan = new Scanner(stream,error);
				Parser p = new Parser(scan, error);
				tree = p.parse();
			}catch(Exception e){
				error.reportError("File Not Found");
			}
			//Instantiate the scanner with the input stream and error object
			
			//Instantiate the parser with the scanner and error object
			
			//Call the parser's parse function
			
			//Check if any errors exist, if so, println("Error")
			//  then output the errors
			
			//If there are no errors, println("Success")
		}

		
		Identification id = new Identification();
		id.identify(tree,error);
		if(error.hasErrors())
		{
			System.out.println("Error");
			error.outputErrors();
		}else{
			System.out.println("Success");
			//ASTDisplay disp = new ASTDisplay();
			//disp.showTree(tree);
			
		}
	}
}
