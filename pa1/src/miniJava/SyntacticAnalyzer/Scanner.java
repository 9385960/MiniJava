package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import miniJava.ErrorReporter;

public class Scanner {
	private InputStream _in;
	private ErrorReporter _errors;
	private StringBuilder _currentText;
	private char _currentChar;
	private LinkedList<Token> _tokenization = new LinkedList<Token>();
	private boolean _endOfDocument = false;
	
	public Scanner( InputStream in, ErrorReporter errors ) {
		this._in = in;
		this._errors = errors;
		this._currentText = new StringBuilder();
		
		nextChar();
		_tokenization.add(scan());
	}

	private void tokenize()
	{
		while(!_endOfDocument){
			_tokenization.add(scan());
		}
	}
	
	private Token scan() {
		// TODO: This function should check the current char to determine what the token could be.
		
		// TODO: Consider what happens if the current char is whitespace
		if(true)
		{

		}
		// TODO: Consider what happens if there is a comment (// or /* */)
		
		// TODO: What happens if there are no more tokens?
		
		// TODO: Determine what the token is. For example, if it is a number
		//  keep calling takeIt() until _currentChar is not a number. Then
		//  create the token via makeToken(TokenType.IntegerLiteral) and return it.
		return null;
	}
	
	private void takeIt() {
		_currentText.append(_currentChar);
		nextChar();
	}
	
	private void skipIt() {
		nextChar();
	}
	
	private void nextChar() {
		try {
			int c = _in.read();
			_currentChar = (char)c;
			
			// TODO: What happens if c == -1?
			if(c == -1)
			{
				_endOfDocument = true;
			}
			// TODO: What happens if c is not a regular ASCII character?
			
		} catch( IOException e ) {
			// TODO: Report an error here
		}
	}
	
	private Token makeToken( TokenType toktype ) {
		// TODO: return a new Token with the appropriate type and text
		//  contained in 
		Token toReturn = new Token(toktype,_currentText.toString());
		return toReturn;
	}

	public Token lookAhead(int i)
	{
		if(_tokenization.size() < i-1){
			while(_tokenization.size() < i-1)
			{
				_tokenization.add(scan());
			}
		}
		return _tokenization.get(i);
	}

	public Token getCurrentToken()
	{
		return _tokenization.get(0);
	}

	public void acceptToken()
	{
		_tokenization.poll();
		if(_tokenization.size() == 0)
		{
			_tokenization.add(scan());
		}
	}
}
