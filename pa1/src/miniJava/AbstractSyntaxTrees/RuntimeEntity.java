package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGeneration.x64.*;

public class RuntimeEntity{
	private int _offset;
	private Reg64 _reg;

	public RuntimeEntity(int offset, Reg64 reg){
		_offset = offset;
		_reg = reg;
	}

	public int getOffset()
	{
		return _offset;
	}

	public Reg64 getRegister()
	{
		return _reg;
	}
}