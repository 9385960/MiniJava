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
	private boolean _insideComment = false;
	private Map<String,TokenType> reservedWords = new HashMap<>();
	private int rowNum = 1;
	private int columnNum = 1;
	
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
			if(_insideComment)
			{
				_errors.reportError("No End of Comment");
			}
			return makeToken(TokenType.EOT,new SourcePosition(rowNum,columnNum));
		}
		while(Character.isWhitespace(_currentChar)||Character.isISOControl(_currentChar))
		{
			skipIt();
		}

		if(Character.isDigit(_currentChar))
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			makeNum();
			Token toReturn = makeToken(TokenType.NUM,position);
			return toReturn;
		}else if(_currentChar == '/')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			if(_currentChar == '/')
			{
			
				while(_currentChar != '\n' && _currentChar != '\r' && !_endOfDocument)
				{
					skipIt();
				}
				_currentText.delete(0, _currentText.length());
				return scan();
			}else if(_currentChar == '*')
			{
				_insideComment = true;
				skipIt();
				boolean foundStar = false;
				boolean foundSlash = false;
				while((!foundStar || !foundSlash) && !_endOfDocument)
				{
					if(_currentChar == '/' && foundStar)
					{
						foundSlash = true;
						_insideComment = false;
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
				return makeToken(TokenType.OPERATOR,position);
			}
		}else if(Character.isLetter(_currentChar))
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			makeID();
			return checkString(position);
		}else if(_currentChar == '.')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			return makeToken(TokenType.PERIOD,position);
		}else if(_currentChar == '(')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			return makeToken(TokenType.LPAREN,position);
		}else if(_currentChar == ')')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			return makeToken(TokenType.RPAREN,position);
		}else if(_currentChar == '{')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			return makeToken(TokenType.LBRACE,position);
		}else if(_currentChar == '}')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			return makeToken(TokenType.RBRACE,position);
		}else if(_currentChar == '[')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			return makeToken(TokenType.LBRACKET,position);
		}else if(_currentChar == ']')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			return makeToken(TokenType.RBRACKET,position);
		}else if(_currentChar == ';')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			return makeToken(TokenType.SEMICOLON,position);
		}else if(_currentChar == ',')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			return makeToken(TokenType.COMMA,position);
		}else if(_currentChar == '=')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			if(_currentChar == '=')
			{
				takeIt();
				return makeToken(TokenType.OPERATOR,position);
			}
			return makeToken(TokenType.EQUALS,position);
		}else if(_currentChar == '>')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			if(_currentChar == '=')
			{
				takeIt();
			}
			return makeToken(TokenType.OPERATOR,position);
		}
		else if(_currentChar == '<')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			if(_currentChar == '=')
			{
				takeIt();
			}
			return makeToken(TokenType.OPERATOR,position);
		}
		else if(_currentChar == '!')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			if(_currentChar == '=')
			{
				takeIt();
			}
			return makeToken(TokenType.OPERATOR,position);
		}else if(_currentChar == '&')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			if(_currentChar == '&')
			{
				takeIt();
			}else{
				_errors.reportError("single & encountered 2 are required at "+position.toString());
			}
			return makeToken(TokenType.OPERATOR,position);
		}else if(_currentChar == '|')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			if(_currentChar == '|')
			{
				takeIt();
			}else{
				_errors.reportError("single | encountered 2 are required at "+position.toString());
			}
			return makeToken(TokenType.OPERATOR,position);
		}else if(_currentChar == '+')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			return makeToken(TokenType.OPERATOR,position);
		}else if(_currentChar == '*')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			return makeToken(TokenType.OPERATOR,position);
		}else if(_currentChar == '-')
		{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			takeIt();
			return makeToken(TokenType.OPERATOR,position);
		}
		else{
			SourcePosition position = new SourcePosition(rowNum,columnNum);
			if(_endOfDocument)
			{
				skipIt();
				return makeToken(TokenType.EOT,position);
			}
			_errors.reportError("Unknown letter encountered at "+position.toString());
		}
		return null;
	}

	private Token checkString(SourcePosition position)
	{
		if(reservedWords.containsKey(_currentText.toString()))
		{
			return makeToken(reservedWords.get(_currentText.toString()),position);
		}else{
			return makeToken(TokenType.ID,position);
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
		if(_currentChar == '\n')
		{
			rowNum += 1;
			columnNum = 1;
		}else{
			columnNum += 1;
		}
		_currentText.append(_currentChar);
		nextChar();
	}
	
	private void skipIt() {
		if(_currentChar == '\n')
		{
			rowNum += 1;
			columnNum = 1;
		}else{
			columnNum += 1;
		}
		nextChar();
	}
	
	private void nextChar() {
		try {
			int c = _in.read();
			_currentChar = (char)c;
			
			//What happens if c == -1?
			if(c == -1)
			{
				_endOfDocument = true;
			}
			//What happens if c is not a regular ASCII character?
			if((int)c > 127)
			{
				//make error
				throw new IOException();
			}
		} catch( IOException e ) {
			//Report an error here
			_errors.reportError("Character could not be read");
		}
	}
	
	private Token makeToken( TokenType toktype , SourcePosition position) {
		Token toReturn = new Token(toktype,_currentText.toString(), position);
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
		reservedWords.put("null",TokenType.NULL);
	}
}
