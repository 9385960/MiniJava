package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
//Class to parse the tokens
public class Parser {
	private Scanner _scanner;
	private ErrorReporter _errors;
	private Token _currentToken;
	//Makes a new parser with the required objects
	public Parser( Scanner scanner, ErrorReporter errors ) {
		this._scanner = scanner;
		this._errors = errors;
		this._currentToken = this._scanner.getCurrentToken();
	}
	//Creates new error class
	class SyntaxError extends Error {
		private static final long serialVersionUID = -6461942006097999362L;
	}
	//Attempts to parse
	public void parse() {
		try {
			// The first thing we need to parse is the Program symbol
			parseProgram();
		} catch( SyntaxError e ) { }
	}
	
	// Program ::= (ClassDeclaration)* eot
	private void parseProgram() throws SyntaxError {
		//Keep parsing class declarations until eot
		while(_currentToken.getTokenType() != TokenType.EOT)
		{
			//We can have any number of class declarations
			parseClassDeclaration();
		}
	}

	// ClassDeclaration ::= class identifier { (Visiblity Access (FieldDeclaration|MethodDeclaration))* }
	private ClassDecl parseClassDeclaration() throws SyntaxError
	{
		String className = _scanner.getCurrentToken().getTokenText();
		FieldDeclList fields = new FieldDeclList();
		MethodDeclList methods = new MethodDeclList();
		SourcePosition position = _scanner.getCurrentToken().getTokenPosition();
		//Need class token
		accept(TokenType.CLASS);
		//Need id token
		accept(TokenType.ID);
		//Need LBrace
		accept(TokenType.LBRACE);
		//Wait until we get an RBrace which signifies the end of our class
		while(_currentToken.getTokenType() != TokenType.RBRACE)
		{
			//Needed to differantiate between field and method declaration
			boolean acceptedVoid = false;
			//Needs To start with a visibility
			parseVisibility();
			//Followed by access
			parseAccess();
			//If the void token is found, then we know we are in a MethodDeclaration
			if(_currentToken.getTokenType() == TokenType.VOID)
			{
				accept(TokenType.VOID);
				acceptedVoid = true;
			}else{
			//If we haven't found a void token, then bothe method and field dec start with types
			//So we need to parse type
				parseType();
			}
			//If we find a semicolon after looking ahead and we haven't accepted the void token,
			//We must be in a Field Dec
			if(lookAhead(1).getTokenType() == TokenType.SEMICOLON && !acceptedVoid)
			{
				parseFieldDec();
			//Otherwise we expect a method dec
			}else
			{
				parseMethodDec();
			}
		}
		//Finish the class with a Right Brace
		accept(TokenType.RBRACE);

		return new ClassDecl(className, fields, methods, position);
	}
	
	// FieldDeclaration ::= Type id;
	private void parseFieldDec()
	{
		//Type has already been parsed above
		//Need to accept an ID toke
		accept(TokenType.ID);
		//Need to accept a SemiColon
		accept(TokenType.SEMICOLON);
	}

	//MethodDeclaration ::= (Type|void) id ( ParameterList? ) {Statement*}
	private void parseMethodDec()
	{
		//Type or void has already been parsed
		//Needs to be followed by an ID
		accept(TokenType.ID);
		//Needs to be followed by an LParen
		accept(TokenType.LPAREN);
		//We can have one parameter list
		//If we have an RParen we have no parameter list, since parameter list can't start with rparen
		if(_currentToken.getTokenType() == TokenType.RPAREN)
		{
			accept(TokenType.RPAREN);
		//We have parameter list and need to parse it
		}else{
			parseParameterList();
			accept(TokenType.RPAREN);
		}
		//We expect a Lbrace for the paramter statements
		accept(TokenType.LBRACE);
		//Wait until we find the Rbrace and then parse as many statements as needed
		while(_currentToken.getTokenType() != TokenType.RBRACE)
		{
			parseStatement();
		}
		//Finish the method dec
		accept(TokenType.RBRACE);
	}

	//Visibility ::= (public|private)?
	private void parseVisibility()
	{
		//Can be public or private, but need not appear
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
		//Could be static
		if(_currentToken.getTokenType() == TokenType.STATIC)
		{
			accept(TokenType.STATIC);
		}
	}

	//Type ::= int | boolean | id | (int | id) []
	private void parseType()
	{
		//Check if the type starts with an int identifier
		if(_currentToken.getTokenType() == TokenType.INT)
		{
			accept(TokenType.INT);
			//Could be an array
			if(_currentToken.getTokenType() == TokenType.LBRACKET)
			{
				accept(TokenType.LBRACKET);
				//If it is an array we expect a RBracket
				accept(TokenType.RBRACKET);
			}
		//Check for boolean
		}else if(_currentToken.getTokenType() == TokenType.BOOLEAN)
		{
			accept(TokenType.BOOLEAN);
		//Check for ID 
		}else if(_currentToken.getTokenType() == TokenType.ID)
		{
			accept(TokenType.ID);
			//Could be an array
			if(_currentToken.getTokenType() == TokenType.LBRACKET)
			{
				accept(TokenType.LBRACKET);
				//Expect RBracket following LBracket
				accept(TokenType.RBRACKET);
			}
		}
	}

	//ParameterList ::= Type id (, Type id)*
	private void parseParameterList()
	{
		//Starts with type
		parseType();
		//Followed by ID
		accept(TokenType.ID);
		//And then any number of , Type id
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
		//Starts with expresion
		parseExpression();
		//Followed by any number of , Expressions
		while(_currentToken.getTokenType() == TokenType.COMMA)
		{
			accept(TokenType.COMMA);
			parseExpression();
		}
	}

	//Reference ::= id|this|Reference.id
	//Equivalent to (id|this)(.id)*
	private void parseReference()
	{
		//Accept this or id
		if(_currentToken.getTokenType() == TokenType.THIS)
		{
			accept(TokenType.THIS);
		}else
		{
			accept(TokenType.ID);
		}
		//Followed by any number of . and id
		while(_currentToken.getTokenType() == TokenType.PERIOD)
		{
			accept(TokenType.PERIOD);
			accept(TokenType.ID);
		}
	}

	private void parseStatement()
	{
		//while ( Expresion ) Statement
		//If we see a while token, we need to parse the above rule
		if(_currentToken.getTokenType() == TokenType.WHILE)
		{
			//Accept while
			accept(TokenType.WHILE);
			//Followed by LParen
			accept(TokenType.LPAREN);
			//Followed by Expression
			parseExpression();
			//Followed by RParen
			accept(TokenType.RPAREN);
			//Ending in Statement
			parseStatement();
		//if ( expresion ) Statement (else statement)? 
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
		// return Expression?;
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
		//{Statement*}
		}else if(_currentToken.getTokenType() == TokenType.LBRACE)
		{
			accept(TokenType.LBRACE);
			while(_currentToken.getTokenType() != TokenType.RBRACE)
			{
				parseStatement();
			}
			accept(TokenType.RBRACE);
		// Need to decide between Reference and Type
		}else{
			//Only type can start with Boolean or Int
			if(_currentToken.getTokenType() == TokenType.BOOLEAN || _currentToken.getTokenType() == TokenType.INT)
			{
				parseTypeStatement();
			//Both type and reference can start with ID
			}else if(_currentToken.getTokenType() == TokenType.ID)
			{
				//Get the next token
				TokenType ahead = _scanner.lookAhead(1).getTokenType();
				//If the next token is ID, only the statement option starting with type can have ID ID
				if(ahead == TokenType.ID)
				{
					parseTypeStatement();
				//If there is a LBracket, we could have Reference [ Expression ] or ID[]
				}else if(ahead == TokenType.LBRACKET)
				{
					//Check the next token 
					TokenType ahead2 = _scanner.lookAhead(2).getTokenType();
					//If we see ID[], then it must be a Type statement
					if(ahead2 == TokenType.RBRACKET)
					{
						parseTypeStatement();
					//Otherwise we must have a reference statement to parse
					}else{
						parseReferenceStatement();
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
				accept(TokenType.LPAREN);
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
