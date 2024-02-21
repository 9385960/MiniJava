package miniJava;

import java.util.List;
import java.util.ArrayList;

//  First of all, errors are simple strings,
//  perhaps it may be worthwhile to augment this reporter
//  with requiring line numbers.
public class ErrorReporter {
	private List<String> _errorQueue;
	
	//Create the ArrayList to hold the error messages
	public ErrorReporter() {
		this._errorQueue = new ArrayList<String>();
	}
	//Check if the error messages list is empty
	public boolean hasErrors() {
		return !(_errorQueue.isEmpty());
	}
	//Prints all errors on a new line
	public void outputErrors() {
		for (String string : _errorQueue) {
			System.out.println(string);
		}
	}
	//Adds an error to the error queue
	public void reportError(String ...error) {
		StringBuilder sb = new StringBuilder();
		
		for(String s : error)
			sb.append(s);
		
		_errorQueue.add(sb.toString());
	}
}
