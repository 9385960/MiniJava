package miniJava.SyntacticAnalyzer;

public class Token {
	private TokenType _type;
	private String _text;
	private SourcePosition _position;
	
	public Token(TokenType type, String text, SourcePosition position) {
		// Store the token's type and text
		_type = type;
		_text = text;
		_position = position;
	}
	
	public TokenType getTokenType() {
		return _type;
	}
	
	public String getTokenText() {
		return _text;
	}

	public SourcePosition getTokenPosition() {
		return _position;
	}
}
