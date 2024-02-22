package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
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
	public AST parse() {
		try {
			// The first thing we need to parse is the Program symbol
			return parseProgram();
		} catch( SyntaxError e ) { return null; }
		
	}
	
	// Program ::= (ClassDeclaration)* eot
	private Package parseProgram() throws SyntaxError {
		SourcePosition position = _scanner.getCurrentToken().getTokenPosition();
		ClassDeclList classes = new ClassDeclList();
		//Keep parsing class declarations until eot
		while(_currentToken.getTokenType() != TokenType.EOT)
		{
			//We can have any number of class declarations
			classes.add(parseClassDeclaration());
		}
		return new Package(classes, position);
	}

	// ClassDeclaration ::= class identifier { (Visiblity Access (FieldDeclaration|MethodDeclaration))* }
	private ClassDecl parseClassDeclaration() throws SyntaxError
	{
		FieldDeclList fields = new FieldDeclList();
		MethodDeclList methods = new MethodDeclList();
		SourcePosition position = _scanner.getCurrentToken().getTokenPosition();
		//Need class token
		accept(TokenType.CLASS);
		//Need id token
		String className = _scanner.getCurrentToken().getTokenText();
		accept(TokenType.ID);
		//Need LBrace
		accept(TokenType.LBRACE);
		//Wait until we get an RBrace which signifies the end of our class
		while(_currentToken.getTokenType() != TokenType.RBRACE)
		{
			//Needed to differantiate between field and method declaration
			boolean acceptedVoid = false;
			SourcePosition memberPos = _scanner.getCurrentToken().getTokenPosition();
			//Needs To start with a visibility
			boolean isPrivate = parseVisibility();
			//Followed by access
			boolean isStatic = parseAccess();
			TypeDenoter memberType;
			//If the void token is found, then we know we are in a MethodDeclaration
			if(_currentToken.getTokenType() == TokenType.VOID)
			{
				SourcePosition typePosition = _scanner.getCurrentToken().getTokenPosition();
				accept(TokenType.VOID);
				acceptedVoid = true;
				memberType = new BaseType(TypeKind.VOID, typePosition);
			}else{
			//If we haven't found a void token, then both method and field dec start with types
			//So we need to parse type
				memberType = parseType();
			}
			//If we find a semicolon after looking ahead and we haven't accepted the void token,
			//We must be in a Field Dec
			if(lookAhead(1).getTokenType() == TokenType.SEMICOLON && !acceptedVoid)
			{
				fields.add(parseFieldDec(isPrivate,isStatic,memberType,memberPos));
			//Otherwise we expect a method dec
			}else
			{
				methods.add(parseMethodDec(isPrivate,isStatic,memberType,memberPos));
			}
		}
		//Finish the class with a Right Brace
		accept(TokenType.RBRACE);

		return new ClassDecl(className, fields, methods, position);
	}
	
	// FieldDeclaration ::= Type id;
	private FieldDecl parseFieldDec(boolean isPrivate, boolean isStatic, TypeDenoter type, SourcePosition position)
	{
		//Type has already been parsed above
		//Need to accept an ID token
		String name = _currentToken.getTokenText();
		accept(TokenType.ID);
		//Need to accept a SemiColon
		accept(TokenType.SEMICOLON);
		return new FieldDecl(isPrivate, isStatic, type, name, position);
	}

	//MethodDeclaration ::= (Type|void) id ( ParameterList? ) {Statement*}
	private MethodDecl parseMethodDec(boolean isPrivate, boolean isStatic, TypeDenoter mt, SourcePosition position)
	{
		String name = _currentToken.getTokenText();
		MemberDecl member = new FieldDecl(isPrivate,isStatic,mt,name,position);
		ParameterDeclList parameterList = new ParameterDeclList();
		StatementList statementList = new StatementList();
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
			parameterList = parseParameterList();
			accept(TokenType.RPAREN);
		}
		//We expect a Lbrace for the paramter statements
		accept(TokenType.LBRACE);
		//Wait until we find the Rbrace and then parse as many statements as needed
		while(_currentToken.getTokenType() != TokenType.RBRACE)
		{
			statementList.add(parseStatement());
		}
		//Finish the method dec
		accept(TokenType.RBRACE);
		return new MethodDecl(member, parameterList, statementList, position);
	}

	//Visibility ::= (public|private)?
	private boolean parseVisibility()
	{
		//Can be public or private, but need not appear
		if(_currentToken.getTokenType() == TokenType.PUBLIC)
		{
			accept(TokenType.PUBLIC);
			return false;
		}else if(_currentToken.getTokenType() == TokenType.PRIVATE){
			accept(TokenType.PRIVATE);
			return true;
		}
		return false;
	}
	
	//Access ::= static?
	private boolean parseAccess()
	{
		//Could be static
		if(_currentToken.getTokenType() == TokenType.STATIC)
		{
			accept(TokenType.STATIC);
			return true;
		}
		return false;
	}

	//Type ::= int | boolean | id | (int | id) []
	private TypeDenoter parseType()
	{
		//Check if the type starts with an int identifier
		SourcePosition typePosition = _scanner.getCurrentToken().getTokenPosition();
		if(_currentToken.getTokenType() == TokenType.INT)
		{
			accept(TokenType.INT);
			BaseType baseType = new BaseType(TypeKind.INT, typePosition);
			//Could be an array
			if(_currentToken.getTokenType() == TokenType.LBRACKET)
			{
				accept(TokenType.LBRACKET);
				//If it is an array we expect a RBracket
				accept(TokenType.RBRACKET);

				return new ArrayType(baseType, typePosition);
			}
			return new BaseType(TypeKind.INT,typePosition);
		//Check for boolean
		}else if(_currentToken.getTokenType() == TokenType.BOOLEAN)
		{
			accept(TokenType.BOOLEAN);
			return new BaseType(TypeKind.BOOLEAN,typePosition);
		//Check for ID 
		}else
		{
			ClassType baseType = new ClassType(new Identifier(_currentToken), typePosition);
			accept(TokenType.ID);
			//Could be an array
			if(_currentToken.getTokenType() == TokenType.LBRACKET)
			{
				accept(TokenType.LBRACKET);
				//Expect RBracket following LBracket
				accept(TokenType.RBRACKET);
				return new ArrayType(baseType,typePosition);
			}
			return baseType;
		}
	}

	//ParameterList ::= Type id (, Type id)*
	private ParameterDeclList parseParameterList()
	{
		SourcePosition position = _currentToken.getTokenPosition();
		ParameterDeclList declList = new ParameterDeclList();
		//Starts with type
		TypeDenoter type = parseType();
		String name = _currentToken.getTokenText();
		declList.add(new ParameterDecl(type, name, position));
		//Followed by ID
		accept(TokenType.ID);
		//And then any number of , Type id
		while(_currentToken.getTokenType() == TokenType.COMMA)
		{
			accept(TokenType.COMMA);
			position = _currentToken.getTokenPosition();
			type = parseType();
			name = _currentToken.getTokenText();
			declList.add(new ParameterDecl(type, name, position));
			accept(TokenType.ID);
		}
		return declList;
	}

	//ArgumentList ::= Expression (, Expression)*
	private ExprList parseArgumentList()
	{
		ExprList list = new ExprList();
		//Starts with expresion
		list.add(parseExpression());
		//Followed by any number of , Expressions
		while(_currentToken.getTokenType() == TokenType.COMMA)
		{
			accept(TokenType.COMMA);
			list.add(parseExpression());
		}
		return list;
	}

	//Reference ::= id|this|Reference.id
	//Equivalent to (id|this)(.id)*
	private Reference parseReference()
	{
		SourcePosition position = _currentToken.getTokenPosition();
		Reference ref;
		//Accept this or id
		if(_currentToken.getTokenType() == TokenType.THIS)
		{
			ref = new ThisRef(position);
			accept(TokenType.THIS);
		}else
		{
			Identifier id = new Identifier(_currentToken);
			ref = new IdRef(id, position);
			accept(TokenType.ID);
		}
		//Followed by any number of . and id
		while(_currentToken.getTokenType() == TokenType.PERIOD)
		{
			accept(TokenType.PERIOD);
			position = _currentToken.getTokenPosition();
			Identifier id = new Identifier(_currentToken);
			ref = new QualRef(ref, id, position);
			accept(TokenType.ID);
		}
		return ref;
	}

	private Statement parseStatement()
	{
		//while ( Expresion ) Statement
		//If we see a while token, we need to parse the above rule
		if(_currentToken.getTokenType() == TokenType.WHILE)
		{
			SourcePosition position = _currentToken.getTokenPosition();
			//Accept while
			accept(TokenType.WHILE);
			//Followed by LParen
			accept(TokenType.LPAREN);
			//Followed by Expression
			Expression e = parseExpression();
			//Followed by RParen
			accept(TokenType.RPAREN);
			//Ending in Statement
			Statement s = parseStatement();
			return new WhileStmt(e, s, position);
		//if ( expresion ) Statement (else statement)? 
		}else if(_currentToken.getTokenType() == TokenType.IF)
		{
			SourcePosition position = _currentToken.getTokenPosition();
			accept(TokenType.IF);
			accept(TokenType.LPAREN);
			Expression b = parseExpression();
			accept(TokenType.RPAREN);
			Statement t = parseStatement();
			if(_currentToken.getTokenType() == TokenType.ELSE)
			{
				accept(TokenType.ELSE);
				Statement e = parseStatement();
				return new IfStmt(b, t, e, position);
			}
			return new IfStmt(b, t, position);
		// return Expression?;
		}else if(_currentToken.getTokenType() == TokenType.RETURN)
		{
			SourcePosition position = _currentToken.getTokenPosition();
			accept(TokenType.RETURN);
			if(_currentToken.getTokenType() == TokenType.SEMICOLON)
			{
				accept(TokenType.SEMICOLON);
				return new ReturnStmt(null, position);
			}else{
				Expression e = parseExpression();
				accept(TokenType.SEMICOLON);
				return new ReturnStmt(e, position);
			}
		//{Statement*}
		}else if(_currentToken.getTokenType() == TokenType.LBRACE)
		{
			SourcePosition position = _currentToken.getTokenPosition();
			StatementList list = new StatementList();
			accept(TokenType.LBRACE);
			while(_currentToken.getTokenType() != TokenType.RBRACE)
			{
				list.add(parseStatement());
			}
			accept(TokenType.RBRACE);
			return new BlockStmt(list, position);
		// Need to decide between Reference and Type
		}else{
			//Only type can start with Boolean or Int
			if(_currentToken.getTokenType() == TokenType.BOOLEAN || _currentToken.getTokenType() == TokenType.INT)
			{
				return parseTypeStatement();
			//Both type and reference can start with ID
			}else if(_currentToken.getTokenType() == TokenType.ID)
			{
				//Get the next token
				TokenType ahead = _scanner.lookAhead(1).getTokenType();
				//If the next token is ID, only the statement option starting with type can have ID ID
				if(ahead == TokenType.ID)
				{
					return parseTypeStatement();
				//If there is a LBracket, we could have Reference [ Expression ] or ID[]
				}else if(ahead == TokenType.LBRACKET)
				{
					//Check the next token 
					TokenType ahead2 = _scanner.lookAhead(2).getTokenType();
					//If we see ID[], then it must be a Type statement
					if(ahead2 == TokenType.RBRACKET)
					{
						return parseTypeStatement();
					//Otherwise we must have a reference statement to parse
					}else{
						return parseReferenceStatement();
					}
				}else{
					return parseReferenceStatement();
				}
			}else{
				return parseReferenceStatement();
			}
		}
	}

	private Statement parseTypeStatement()
	{
		SourcePosition position = _currentToken.getTokenPosition();
		TypeDenoter t = parseType();
		String name = _currentToken.getTokenText();
		VarDecl varDecl = new VarDecl(t, name, position);
		accept(TokenType.ID);
		accept(TokenType.EQUALS);
		Expression e = parseExpression();
		accept(TokenType.SEMICOLON);
		return new VarDeclStmt(varDecl, e, position);
	}

	private Statement parseReferenceStatement()
	{
		SourcePosition position = _currentToken.getTokenPosition();
		Reference r = parseReference();
		if(_currentToken.getTokenType() == TokenType.EQUALS)
		{
			accept(TokenType.EQUALS);
			Expression e = parseExpression();
			accept(TokenType.SEMICOLON);
			return new AssignStmt(r, e, position);
		}else if(_currentToken.getTokenType() == TokenType.LBRACKET)
		{
			accept(TokenType.LBRACKET);
			Expression i = parseExpression();
			accept(TokenType.RBRACKET);
			accept(TokenType.EQUALS);
			Expression e = parseExpression();
			accept(TokenType.SEMICOLON);
			return new IxAssignStmt(r, i, e, position);
		}else{
			ExprList list = new ExprList();
			accept(TokenType.LPAREN);
			if(_currentToken.getTokenType() != TokenType.RPAREN)
			{
				list = parseArgumentList();
			}
			accept(TokenType.RPAREN);
			accept(TokenType.SEMICOLON);
			return new CallStmt(r, list, position);
		}
	}

	private Expression parseExpression()
	{
		Expression potentialExpression;
		if(_currentToken.getTokenType() == TokenType.NEW)
		{
			SourcePosition position = _currentToken.getTokenPosition();
			accept(TokenType.NEW);
			if(_currentToken.getTokenType() == TokenType.ID)
			{
				SourcePosition idPosition = _currentToken.getTokenPosition();
				Identifier id = new Identifier(_currentToken);
				ClassType et = new ClassType(id, idPosition);
				accept(TokenType.ID);
				if(_currentToken.getTokenType() == TokenType.LPAREN)
				{
					accept(TokenType.LPAREN);
					accept(TokenType.RPAREN);
					potentialExpression = new NewObjectExpr(et, position);
				}else{
					accept(TokenType.LBRACKET);
					Expression e = parseExpression();
					accept(TokenType.RBRACKET);
					potentialExpression = new NewArrayExpr(et, e, position);
				}
			}else {
				SourcePosition idPosition = _currentToken.getTokenPosition();
				TypeDenoter t = new BaseType(TypeKind.INT, idPosition);
				accept(TokenType.INT);
				accept(TokenType.LBRACKET);
				Expression e = parseExpression();
				accept(TokenType.RBRACKET);
				potentialExpression = new NewArrayExpr(t, e, position);
			}
		}else if(_currentToken.getTokenType() == TokenType.NUM){
			SourcePosition numPosition = _currentToken.getTokenPosition();
			Terminal t = new IntLiteral(_currentToken);
			potentialExpression = new LiteralExpr(t, numPosition);
			accept(TokenType.NUM);
		}else if(_currentToken.getTokenType() == TokenType.TRUE)
		{
			SourcePosition boolPosition = _currentToken.getTokenPosition();
			Terminal t = new BooleanLiteral(_currentToken);
			potentialExpression = new LiteralExpr(t, boolPosition);
			accept(TokenType.TRUE);
		}else if(_currentToken.getTokenType() == TokenType.FALSE)
		{
			SourcePosition boolPosition = _currentToken.getTokenPosition();
			Terminal t = new BooleanLiteral(_currentToken);
			potentialExpression = new LiteralExpr(t, boolPosition);
			accept(TokenType.FALSE);
		}else if(_currentToken.getTokenType() == TokenType.OPERATOR && isUnop(_currentToken))
		{
			SourcePosition unopPosition = _currentToken.getTokenPosition();
			Operator op = new Operator(_currentToken);
			accept(TokenType.OPERATOR);
			Expression e = parseExpression();
			potentialExpression = new UnaryExpr(op, e, unopPosition);
		}else if(_currentToken.getTokenType() == TokenType.LPAREN)
		{
			accept(TokenType.LPAREN);
			potentialExpression = parseExpression();
			accept(TokenType.RPAREN);
		}else {
			SourcePosition position = _currentToken.getTokenPosition();
			Reference r = parseReference();
			if(_currentToken.getTokenType() == TokenType.LBRACKET)
			{
				accept(TokenType.LBRACKET);
				Expression e = parseExpression();
				accept(TokenType.RBRACKET);
				potentialExpression = new IxExpr(r, e, position);
			}else if(_currentToken.getTokenType() == TokenType.LPAREN)
			{
				ExprList list = new ExprList();
				accept(TokenType.LPAREN);
				if(_currentToken.getTokenType() != TokenType.RPAREN)
				{
					list = parseArgumentList();
				}
				accept(TokenType.RPAREN);
				potentialExpression = new CallExpr(r, list, position);
			}else {
				potentialExpression = new RefExpr(r, position);
			}
		}

		while(isBinop(_currentToken))
		{
			SourcePosition position = _currentToken.getTokenPosition();
			Operator op = new Operator(_currentToken);
			accept(TokenType.OPERATOR);
			Expression e = parseExpression();
			potentialExpression = new BinaryExpr(op, potentialExpression ,e, position);
		}

		return potentialExpression;
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
