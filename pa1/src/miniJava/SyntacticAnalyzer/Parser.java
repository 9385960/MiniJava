package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

public class Parser {
	private Scanner _scanner;
	private ErrorReporter _errors;
	private Token _currentToken;
	
	public Parser( Scanner scanner, ErrorReporter errors ) {
		this._scanner = scanner;
		this._errors = errors;
		this._currentToken = this._scanner.getCurrentToken();
	}
	
	class SyntaxError extends Error {
		private static final long serialVersionUID = -6461942006097999362L;
	}
	
	public void parse() {
		try {
			// The first thing we need to parse is the Program symbol
			parseProgram();
		} catch( SyntaxError e ) { }
	}
	
	// Program ::= (ClassDeclaration)* eot
	private void parseProgram() throws SyntaxError {
		// TODO: Keep parsing class declarations until eot
		while(_currentToken.getTokenType() != TokenType.EOT)
		{
			parseClassDeclaration();
		}
	}

	// ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
	private void parseClassDeclaration() throws SyntaxError
	{
		accept(TokenType.CLASS);
		accept(TokenType.ID);
		accept(TokenType.LBRACE);
		while(_currentToken.getTokenType() != TokenType.RBRACE)
		{
			parseVisibility();
			parseAccess();
			if(lookAhead(2).getTokenType() == TokenType.SEMICOLON)
			{
				parseFieldDec();
			}else
			{
				parseMethodDec();
			}
		}
		accept(TokenType.RBRACE);
	}
	
	// FieldDeclaration ::= Visiblity Access Type id;
	private void parseFieldDec()
	{
		parseType();
		accept(TokenType.ID);
		accept(TokenType.SEMICOLON);
	}

	//MethodDeclaration ::= Visibility Access (Type|void) id ( ParameterList? ) {Statement*}
	private void parseMethodDec()
	{
		if(_currentToken.getTokenType() == TokenType.VOID)
		{
			accept(TokenType.VOID);
		}else{
			parseType();
		}
		accept(TokenType.ID);
		accept(TokenType.LPAREN);
		if(_currentToken.getTokenType() == TokenType.RPAREN)
		{
			accept(TokenType.RPAREN);
		}else{
			parseParameterList();
			accept(TokenType.RPAREN);
		}
		accept(TokenType.LBRACE);
		while(_currentToken.getTokenType() != TokenType.RBRACE)
		{
			parseStatement();
		}
		accept(TokenType.RBRACE);
	}

	//Visibility ::= (public|private)?
	private void parseVisibility()
	{
		if(_currentToken.getTokenType() == TokenType.PUBLIC)
		{
			accept(TokenType.PUBLIC);
		}else if(_currentToken.getTokenType() == TokenType.PRIVATE){
			accept(TokenType.PRIVATE);
		}
	}
	
	//Access ::= static?
	private void parseAccess()
	{
		if(_currentToken.getTokenType() == TokenType.STATIC)
		{
			accept(TokenType.STATIC);
		}
	}

	//Type ::= int | boolean | id | (int | id) []
	private void parseType()
	{
		if(_currentToken.getTokenType() == TokenType.INT)
		{
			accept(TokenType.INT);
			if(_currentToken.getTokenType() == TokenType.LBRACKET)
			{
				accept(TokenType.LBRACKET);
				accept(TokenType.RBRACKET);
			}
		}else if(_currentToken.getTokenType() == TokenType.BOOLEAN)
		{
			accept(TokenType.BOOLEAN);
		}else if(_currentToken.getTokenType() == TokenType.ID)
		{
			accept(TokenType.ID);
			if(_currentToken.getTokenType() == TokenType.LBRACKET)
			{
				accept(TokenType.LBRACKET);
				accept(TokenType.RBRACKET);
			}
		}
	}

	//ParameterList ::= Type id (, Type id)*
	private void parseParameterList()
	{
		parseType();
		accept(TokenType.ID);
		while(_currentToken.getTokenType() == TokenType.COMMA)
		{
			accept(TokenType.COMMA);
			parseType();
			accept(TokenType.ID);
		}
	}
	//ArgumentList ::= Expression (, Expression)*
	private void parseArgumentList()
	{
		parseExpression();
		while(_currentToken.getTokenType() == TokenType.COMMA)
		{
			accept(TokenType.COMMA);
			parseExpression();
		}
	}

	//Reference ::= id|this|Reference.id
	private void parseReference()
	{
		if(_currentToken.getTokenType() == TokenType.THIS)
		{
			accept(TokenType.THIS);
		}else
		{
			accept(TokenType.ID);
		}
		while(_currentToken.getTokenType() == TokenType.PERIOD)
		{
			accept(TokenType.PERIOD);
			accept(TokenType.ID);
		}
	}

	private void parseStatement()
	{
		if(_currentToken.getTokenType() == TokenType.WHILE)
		{
			accept(TokenType.LPAREN);
			parseExpression();
			accept(TokenType.RPAREN);
			parseStatement();
		}else if(_currentToken.getTokenType() == TokenType.IF)
		{
			accept(TokenType.IF);
			accept(TokenType.LPAREN);
			parseExpression();
			accept(TokenType.RPAREN);
			parseStatement();
			if(_currentToken.getTokenType() == TokenType.ELSE)
			{
				parseStatement();
			}
		}else if(_currentToken.getTokenType() == TokenType.RETURN)
		{
			accept(TokenType.RETURN);
			if(_currentToken.getTokenType() == TokenType.SEMICOLON)
			{
				accept(TokenType.SEMICOLON);
			}else{
				parseExpression();
				accept(TokenType.SEMICOLON);
			}
		}else if(_currentToken.getTokenType() == TokenType.LBRACE)
		{
			while(_currentToken.getTokenType() != TokenType.RBRACE)
			{
				parseStatement();
			}
			accept(TokenType.RBRACE);
		}else{
			if(_currentToken.getTokenType() == TokenType.BOOLEAN || _currentToken.getTokenType() == TokenType.INT)
			{
				parseTypeStatement();
			}else if(_currentToken.getTokenType() == TokenType.ID)
			{
				TokenType ahead = _scanner.lookAhead(1).getTokenType();
				if(ahead == TokenType.ID)
				{
					parseTypeStatement();
				}else if(ahead == TokenType.LBRACKET)
				{
					TokenType ahead2 = _scanner.lookAhead(2).getTokenType();
					if(ahead2 == TokenType.RBRACKET)
					{
						parseTypeStatement();
					}
				}else{
					parseReferenceStatement();
				}
			}else{
				parseReferenceStatement();
			}
		}
	}

	private void parseTypeStatement()
	{
		parseType();
		accept(TokenType.ID);
		accept(TokenType.EQUALS);
		parseExpression();
		accept(TokenType.SEMICOLON);
	}

	private void parseReferenceStatement()
	{
		parseReference();
		if(_currentToken.getTokenType() == TokenType.EQUALS)
		{
			accept(TokenType.EQUALS);
			parseExpression();
			accept(TokenType.SEMICOLON);
		}else if(_currentToken.getTokenType() == TokenType.LBRACKET)
		{
			accept(TokenType.LBRACKET);
			parseExpression();
			accept(TokenType.RBRACKET);
			accept(TokenType.EQUALS);
			parseExpression();
			accept(TokenType.SEMICOLON);
		}else{
			accept(TokenType.LPAREN);
			if(_currentToken.getTokenType() != TokenType.RPAREN)
			{
				parseArgumentList();
			}
			accept(TokenType.RPAREN);
			accept(TokenType.SEMICOLON);
		}
	}

	private void parseExpression()
	{
		if(_currentToken.getTokenType() == TokenType.NEW)
		{
			accept(TokenType.NEW);
			if(_currentToken.getTokenType() == TokenType.ID)
			{
				accept(TokenType.ID);
				if(_currentToken.getTokenType() == TokenType.LPAREN)
				{
					accept(TokenType.LPAREN);
					accept(TokenType.RPAREN);
				}else{
					accept(TokenType.LBRACKET);
					parseExpression();
					accept(TokenType.RBRACKET);
				}
			}else {
				accept(TokenType.INT);
				accept(TokenType.LBRACKET);
				parseExpression();
				accept(TokenType.RBRACKET);
			}
		}else if(_currentToken.getTokenType() == TokenType.NUM){
			accept(TokenType.NUM);
		}else if(_currentToken.getTokenType() == TokenType.TRUE)
		{
			accept(TokenType.TRUE);
		}else if(_currentToken.getTokenType() == TokenType.FALSE)
		{
			accept(TokenType.FALSE);
		}else if(_currentToken.getTokenType() == TokenType.OPERATOR && isUnop(_currentToken))
		{
			accept(TokenType.OPERATOR);
			parseExpression();
		}else if(_currentToken.getTokenType() == TokenType.LPAREN)
		{
			accept(TokenType.LPAREN);
			parseExpression();
			accept(TokenType.RPAREN);
		}else {
			parseReference();
			if(_currentToken.getTokenType() == TokenType.LBRACKET)
			{
				accept(TokenType.LBRACKET);
				parseExpression();
				accept(TokenType.RBRACKET);
			}else if(_currentToken.getTokenType() == TokenType.LPAREN)
			{
				accept(TokenType.RPAREN);
				if(_currentToken.getTokenType() != TokenType.RPAREN)
				{
					parseArgumentList();
				}
				accept(TokenType.RPAREN);
			}
		}

		while(isBinop(_currentToken))
		{
			accept(TokenType.OPERATOR);
			parseExpression();
		}
	}

	// This method will accept the token and retrieve the next token.
	//  Can be useful if you want to error check and accept all-in-one.
	private void accept(TokenType expectedType) throws SyntaxError {
		if( _currentToken.getTokenType() == expectedType ) {
			_scanner.acceptToken();
			_currentToken = _scanner.getCurrentToken();
			return;
		}
		
		// TODO: Report an error here.
		//  "Expected token X, but got Y"

		_errors.reportError("Expected " + expectedType.name() + ", but got " + _currentToken.getTokenType().name() + " Instead" );
		throw new SyntaxError();
	}

	private Token lookAhead(int i) {
		return _scanner.lookAhead(i);
	}

	private boolean isBinop(Token t)
	{
		if(t.getTokenType() == TokenType.OPERATOR)
		{
			if(t.getTokenText().equals("!"))
			{
				return false;
			}else{
				return true;
			}
		}
		return false;
	}

	private boolean isUnop(Token t)
	{
		if(t.getTokenType() == TokenType.OPERATOR)
		{
			if(t.getTokenText().equals("!") || t.getTokenText().equals("-"))
			{
				return true;
			}else{
				return false;
			}
		}
		return false;
	}
}
