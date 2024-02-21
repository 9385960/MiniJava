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
	private boolean _endOfToken = false;
	private Map<String,TokenType> reservedWords = new HashMap<>();
	
	public Scanner( InputStream in, ErrorReporter errors ) {
		this._in = in;
		this._errors = errors;
		this._currentText = new StringBuilder();
		
		initReservedWords();
		nextChar();
		_tokenization.add(scan());
	}

	private Token scan() {
		if(_endOfToken)
		{
			return null;
		}
		if(_endOfDocument)
		{
			_endOfToken = true;
			return makeToken(TokenType.EOT);
		}
		while(Character.isWhitespace(_currentChar)||Character.isISOControl(_currentChar))
		{
			skipIt();
		}

		if(Character.isDigit(_currentChar))
		{
			makeNum();
			Token toReturn = makeToken(TokenType.NUM);
			return toReturn;
		}else if(_currentChar == '/')
		{
			takeIt();
			if(_currentChar == '/')
			{
				while(_currentChar != '\n' && _currentChar != '\r')
				{
					skipIt();
				}
				_currentText.delete(0, _currentText.length());
				return scan();
			}else if(_currentChar == '*')
			{
				skipIt();
				boolean foundStar = false;
				boolean foundSlash = false;
				while(!foundStar || !foundSlash)
				{
					if(_currentChar == '/' && foundStar)
					{
						foundSlash = true;
					}
					if(_currentChar == '*')
					{
						foundStar = true;
					}else{
						if(foundStar && !foundSlash)
						{
							foundStar = false;
						}
					}
					skipIt();
				}
				_currentText.delete(0, _currentText.length());
				return scan();
			}else {
				return makeToken(TokenType.OPERATOR);
			}
		}else if(Character.isLetter(_currentChar))
		{
			makeID();
			return checkString();
		}else if(_currentChar == '.')
		{
			takeIt();
			return makeToken(TokenType.PERIOD);
		}else if(_currentChar == '(')
		{
			takeIt();
			return makeToken(TokenType.LPAREN);
		}else if(_currentChar == ')')
		{
			takeIt();
			return makeToken(TokenType.RPAREN);
		}else if(_currentChar == '{')
		{
			takeIt();
			return makeToken(TokenType.LBRACE);
		}else if(_currentChar == '}')
		{
			takeIt();
			return makeToken(TokenType.RBRACE);
		}else if(_currentChar == '[')
		{
			takeIt();
			return makeToken(TokenType.LBRACKET);
		}else if(_currentChar == ']')
		{
			takeIt();
			return makeToken(TokenType.RBRACKET);
		}else if(_currentChar == ';')
		{
			takeIt();
			return makeToken(TokenType.SEMICOLON);
		}else if(_currentChar == ',')
		{
			takeIt();
			return makeToken(TokenType.COMMA);
		}else if(_currentChar == '=')
		{
			takeIt();
			if(_currentChar == '=')
			{
				takeIt();
				return makeToken(TokenType.OPERATOR);
			}
			return makeToken(TokenType.EQUALS);
		}else if(_currentChar == '>')
		{
			takeIt();
			if(_currentChar == '=')
			{
				takeIt();
			}
			return makeToken(TokenType.OPERATOR);
		}
		else if(_currentChar == '<')
		{
			takeIt();
			if(_currentChar == '=')
			{
				takeIt();
			}
			return makeToken(TokenType.OPERATOR);
		}
		else if(_currentChar == '!')
		{
			takeIt();
			if(_currentChar == '=')
			{
				takeIt();
			}
			return makeToken(TokenType.OPERATOR);
		}else if(_currentChar == '&')
		{
			takeIt();
			if(_currentChar == '&')
			{
				takeIt();
			}else{
				_errors.reportError("single & encountered 2 are required");
			}
			return makeToken(TokenType.OPERATOR);
		}else if(_currentChar == '|')
		{
			takeIt();
			if(_currentChar == '|')
			{
				takeIt();
			}else{
				_errors.reportError("single & encountered 2 are required");
			}
			return makeToken(TokenType.OPERATOR);
		}else if(_currentChar == '+')
		{
			takeIt();
			return makeToken(TokenType.OPERATOR);
		}else if(_currentChar == '*')
		{
			takeIt();
			return makeToken(TokenType.OPERATOR);
		}else if(_currentChar == '-')
		{
			takeIt();
			return makeToken(TokenType.OPERATOR);
		}
		else{
			if(_endOfDocument)
			{
				skipIt();
				return makeToken(TokenType.EOT);
			}
			_errors.reportError("Unknown letter encountered");
		}
		return null;
	}

	private Token checkString()
	{
		if(reservedWords.containsKey(_currentText.toString()))
		{
			return makeToken(reservedWords.get(_currentText.toString()));
		}else{
			return makeToken(TokenType.ID);
		}
	}

	private void makeID()
	{
		while(Character.isLetterOrDigit(_currentChar) || _currentChar == '_')
		{
			takeIt();
		}
	}

	private void makeNum()
	{
		while(Character.isDigit(_currentChar))
		{
			takeIt();
		}
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
			if((int) c < 0 || (int)c > 127)
			{
				//TODO: make error
				//throw new IOException();
			}
		} catch( IOException e ) {
			// TODO: Report an error here
			_errors.reportError("Character could not be read");
		}
	}
	
	private Token makeToken( TokenType toktype ) {
		Token toReturn = new Token(toktype,_currentText.toString(), new SourcePosition());
		_currentText.delete(0, _currentText.length());
		return toReturn;
	}

	public Token lookAhead(int i)
	{
		if(_tokenization.size() < i+1){
			while(_tokenization.size() < i+1 && !_endOfToken)
			{
				_tokenization.add(scan());
			}
			return _tokenization.getLast();
		}
		return _tokenization.get(i);
	}

	public Token getCurrentToken()
	{
		return _tokenization.getFirst();
	}

	public void acceptToken()
	{
		_tokenization.poll();
		if(_tokenization.size() == 0)
		{
			_tokenization.add(scan());
		}
	}

	private void initReservedWords()
	{
		reservedWords.put("class", TokenType.CLASS);
		reservedWords.put("while", TokenType.WHILE);
		reservedWords.put("public", TokenType.PUBLIC);
		reservedWords.put("private", TokenType.PRIVATE);
		reservedWords.put("static", TokenType.STATIC);
		reservedWords.put("this", TokenType.THIS);
		reservedWords.put("return", TokenType.RETURN);
		reservedWords.put("if", TokenType.IF);
		reservedWords.put("else", TokenType.ELSE);
		reservedWords.put("true", TokenType.TRUE);
		reservedWords.put("false", TokenType.FALSE);
		reservedWords.put("new", TokenType.NEW);
		reservedWords.put("void",TokenType.VOID);
		reservedWords.put("boolean", TokenType.BOOLEAN);
		reservedWords.put("int",TokenType.INT);
	}
}
