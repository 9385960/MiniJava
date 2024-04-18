package miniJava.SyntacticAnalyzer;

public class SourcePosition {
    private int _row;
    private int _column;
    public SourcePosition(int row, int column)
    {
    	_row = row;
    	_column = column;
    }

    public String toString()
    {
    	return "Row: "+Integer.toString(_row)+" Column: "+Integer.toString(_column);
    }
}
