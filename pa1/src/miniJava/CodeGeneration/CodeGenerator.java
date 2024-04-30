package miniJava.CodeGeneration;

import javax.print.DocFlavor.READER;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.x64.*;
import miniJava.CodeGeneration.x64.ISA.*;
import miniJava.ContextualAnalysis.ScopedIdentification;

public class CodeGenerator implements Visitor<Object, Object> {


	private ErrorReporter _errors;
	private InstructionList _asm; // our list of instructions that are used to make the code section
	private int currentOffset = -8;
	private int paramOffset = 8;
	private boolean mainFound = false;
	private String currentClass = "";
	private CallPatcher patchCall = new CallPatcher();
	private long entryPoint;
	private int staticClassOffset = 0;

	public CodeGenerator(ErrorReporter errors) {
		this._errors = errors;
	}

	
	public void parse(AST prog) {
		

		_asm = new InstructionList();
		patchCall.AddMethod((MethodDecl)ScopedIdentification.GetMemberDecl("_PrintStream","println"),_asm.getSize());
		_asm.add(new Push(new ModRMSIB(Reg64.RBP,true)));
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBP, Reg64.RSP)));
		makePrintln();
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RSP,Reg64.RBP)));
        _asm.add(new Pop(new ModRMSIB(Reg64.RBP,true)));
        _asm.add(new Ret());
		
		// If you haven't refactored the name "ModRMSIB" to something like "R",
		//  go ahead and do that now. You'll be needing that object a lot.
		// Here is some example code.
		
		// Simple operations:
		// _asm.add( new Push(0) ); // push the value zero onto the stack
		// _asm.add( new Pop(Reg64.RCX) ); // pop the top of the stack into RCX
		
		// Fancier operations:
		// _asm.add( new Cmp(new ModRMSIB(Reg64.RCX,Reg64.RDI)) ); // cmp rcx,rdi
		// _asm.add( new Cmp(new ModRMSIB(Reg64.RCX,0x10,Reg64.RDI)) ); // cmp [rcx+0x10],rdi
		// _asm.add( new Add(new ModRMSIB(Reg64.RSI,Reg64.RCX,4,0x1000,Reg64.RDX)) ); // add [rsi+rcx*4+0x1000],rdx
		
		// Thus:
		// new ModRMSIB( ... ) where the "..." can be:
		//  RegRM, RegR						== rm, r
		//  RegRM, int, RegR				== [rm+int], r
		//  RegRD, RegRI, intM, intD, RegR	== [rd+ ri*intM + intD], r
		// Where RegRM/RD/RI are just Reg64 or Reg32 or even Reg8
		//
		// Note there are constructors for ModRMSIB where RegR is skipped.
		// This is usually used by instructions that only need one register operand, and often have an immediate
		//   So they actually will set RegR for us when we create the instruction. An example is:
		// _asm.add( new Mov_rmi(new ModRMSIB(Reg64.RDX,true), 3) ); // mov rdx,3
		//   In that last example, we had to pass in a "true" to indicate whether the passed register
		//    is the operand RM or R, in this case, true means RM
		//  Similarly:
		// _asm.add( new Push(new ModRMSIB(Reg64.RBP,16)) );
		//   This one doesn't specify RegR because it is: push [rbp+16] and there is no second operand register needed
		
		// Patching example:
		// Instruction someJump = new Jmp((int)0); // 32-bit offset jump to nowhere
		// _asm.add( someJump ); // populate listIdx and startAddress for the instruction
		// ...
		// ... visit some code that probably uses _asm.add
		// ...
		// patch method 1: calculate the offset yourself
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size() - someJump.startAddress - 5) );
		// -=-=-=-
		// patch method 2: let the jmp calculate the offset
		//  Note the false means that it is a 32-bit immediate for jumping (an int)
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size(), someJump.startAddress, false) );
		
		prog.visit(this,null);
		if(!mainFound)
		{
			_errors.reportError("No Main Found.");
		}
		//TODO Output the file "a.out" if no errors
		if( !_errors.hasErrors() )
			makeElf("a.out");
	}

	@Override
	public Object visitPackage(Package prog, Object arg) {
		// TODO: visit relevant parts of our AST
		for(ClassDecl dec : prog.classDeclList)
        {
            dec.visit(this, arg);
        }
        patchCall.patchCalls(_asm);
		return null;
	}
	
	public void makeElf(String fname) {
		ELFMaker elf = new ELFMaker(_errors, _asm.getSize(), 8); // bss ignored until PA5, set to 8
		elf.outputELF(fname, _asm.getBytes(), entryPoint); // TODO: set the location of the main method
	}
	
	private int makeMalloc() {
		int idxStart = _asm.add( new Mov_rmi(new ModRMSIB(Reg64.RAX,true),0x09) ); // mmap
		
		_asm.add( new Xor(		new ModRMSIB(Reg64.RDI,Reg64.RDI)) 	); // addr=0
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.RSI,true),0x1000) ); // 4kb alloc
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.RDX,true),0x03) 	); // prot read|write
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.R10,true),0x22) 	); // flags= private, anonymous
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.R8, true),-1) 	); // fd= -1
		_asm.add( new Xor(		new ModRMSIB(Reg64.R9,Reg64.R9)) 	); // offset=0
		_asm.add( new Syscall() );
		
		// pointer to newly allocated memory is in RAX
		// return the index of the first instruction in this method, if needed
		return idxStart;
	}

	public void makeSysExit()
	{
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RAX,true),60));
		_asm.add(new Xor(new ModRMSIB(Reg64.RDI,Reg64.RDI)));
		_asm.add(new Syscall());
	}
	
	private int makePrintln() {
		// TODO: how can we generate the assembly to println?
		int idxStart = _asm.add(new Mov_rmi(new ModRMSIB(Reg64.RAX,true),0x01));
		_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RBP,16,Reg64.RBX)));
		_asm.add(new Push(0x00));
		_asm.add(new Push(new ModRMSIB(Reg64.RBX,true)));
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RSI,Reg64.RSP)));
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RDI,true),0x01));
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RDX,true),0x01));
		_asm.add(new Syscall());
		return idxStart;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		cd.entity = new RuntimeEntity(staticClassOffset,Reg64.R12);
		currentClass = cd.name;
		// TODO Auto-generated method stub
		for(FieldDecl f: cd.fieldDeclList)
        {
            f.visit(this, arg);
        }
        for(MethodDecl m: cd.methodDeclList)
        {
            m.visit(this,arg);
        }
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		// TODO Auto-generated method stub
		if(fd.isStatic)
		{
			fd.entity = new RuntimeEntity(staticClassOffset,Reg64.R12);
			staticClassOffset += 8;
		}else{
			fd.entity = new RuntimeEntity(0,null);
		}
		
		fd.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		// TODO Auto-generated method stub
		currentOffset = -8;
		if(md.isStatic)
		{
			paramOffset = 16;
		}else{
			paramOffset = 24;
		}
		
		if(md.name.equals("main"))
		{
			if(mainFound)
			{
				_errors.reportError("Main Is Already Defined, can only have one main method at "+md.posn.toString());
			}
			if(!md.isStatic)
			{
				_errors.reportError("main must be static at "+md.posn.toString());
			}
			if(md.parameterDeclList.size() > 1)
			{
				_errors.reportError("Too many arguments for a main method "+md.posn.toString());
			}
			ParameterDecl decl = md.parameterDeclList.get(0);
			if(decl.type instanceof ArrayType)
			{
				ArrayType type = (ArrayType)decl.type;
				if(type.eltType instanceof ClassType)
				{
					ClassType element = (ClassType)type.eltType;
					if(!element.className.spelling.equals("String"))
					{
						_errors.reportError("Incorrect Argument for main");
					}
				}else{
					_errors.reportError("Incorrect Argument for main");
				}

			}else{
				_errors.reportError("Incorrect Argument for main");
			}
			if(md.isPrivate)
			{
				_errors.reportError("main must be public at "+md.posn.toString());
			}
			mainFound = true;
			entryPoint = _asm.getSize();
			_asm.add(new Push(new ModRMSIB(Reg64.RBP,true)));
			_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBP, Reg64.RSP)));
			makeMalloc();
			_asm.add(new Mov_rmr(new ModRMSIB(Reg64.R15,Reg64.RAX)));

		}else{
			patchCall.AddMethod(md,_asm.getSize());
			_asm.add(new Push(new ModRMSIB(Reg64.RBP,true)));
			_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBP, Reg64.RSP)));
		}
		for(ParameterDecl pd: md.parameterDeclList)
        {
            pd.visit(this, null);
        }
        for (Statement s: md.statementList) {
            s.visit(this, null);
        }

        if(md.name.equals("main"))
		{
			makeSysExit();
		}else{
			_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RSP,Reg64.RBP)));
        	_asm.add(new Pop(new ModRMSIB(Reg64.RBP,true)));
			_asm.add(new Ret());
		}
        return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		// TODO Auto-generated method stub
		pd.entity = new RuntimeEntity(paramOffset,Reg64.RBP);
		paramOffset += 8;
		//pd.type.visit(this,arg);
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		// TODO Auto-generated method stub
		//decl.type.visit(this,arg);
		_asm.add(new Push(0));
		decl.entity = new RuntimeEntity(currentOffset,Reg64.RBP);
		currentOffset -= 8;
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		// TODO Auto-generated method stub
		type.eltType.visit(this,arg);
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.R10,Reg64.RSP)));
		int originalOffset = currentOffset;
		for (Statement s: stmt.sl) {
        	s.visit(this, null);
        }
        _asm.add(new Mov_rmr(new ModRMSIB(Reg64.RSP,Reg64.R10)));
        currentOffset = originalOffset;
        return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt.varDecl.visit(this,true);
        stmt.initExp.visit(this,false);
        _asm.add(new Pop(Reg64.RAX));
        _asm.add(new Mov_rmr(new ModRMSIB(stmt.varDecl.entity.getRegister(),stmt.varDecl.entity.getOffset(),Reg64.RAX)));
        return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		stmt.ref.visit(this, true);
        stmt.val.visit(this, false);
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Pop(Reg64.RBX));
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBX,0,Reg64.RAX)));
        return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		if(stmt.ref instanceof IdRef)
		{
			stmt.ref.visit(this, false);
		}else{
			stmt.ref.visit(this, true);
		}
		
		stmt.ix.visit(this,false);
		stmt.exp.visit(this,false);
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Pop(Reg64.RBX));
		_asm.add(new Pop(Reg64.RCX));
		_asm.add(new Imul(Reg64.RBX,new ModRMSIB(Reg64.RBX, true),8));
		_asm.add(new Add(new ModRMSIB(Reg64.RBX,Reg64.RCX)));
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBX,0,Reg64.RAX)));
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		ExprList args = stmt.argList;
		for(int i = args.size()-1; i >=0; i--)
		{
			args.get(i).visit(this,false);
		}
		if(stmt.methodRef instanceof IdRef)
		{
			MethodDecl decl = (MethodDecl)(((IdRef)stmt.methodRef).id.decl);
			if(!decl.isStatic)
			{
				_asm.add(new Push(new ModRMSIB(Reg64.RBP,16)));
			}
			PatchLocation location = new PatchLocation(decl,_asm.getCurrentIndex() ,_asm.getSize());
			_asm.add(new Call(0));
			if(decl.isStatic)
			{
				_asm.add(new Add(new ModRMSIB(Reg64.RSP,true),8*args.size()));
			}else{
				_asm.add(new Add(new ModRMSIB(Reg64.RSP,true),8*(args.size()+1)));
			}
			patchCall.AddToPatch(location);
		}else if(stmt.methodRef instanceof QualRef)
		{
			QualRef qRef = (QualRef)stmt.methodRef;
			MethodDecl decl = (MethodDecl)(qRef.id.decl);
			if(!decl.isStatic)
			{
				qRef.ref.visit(this, false);
			}
			PatchLocation location = new PatchLocation(decl ,_asm.getCurrentIndex(),_asm.getSize());
			_asm.add(new Call(0));
			if(decl.isStatic)
			{
				_asm.add(new Add(new ModRMSIB(Reg64.RSP,true),8*args.size()));
			}else{
				_asm.add(new Add(new ModRMSIB(Reg64.RSP,true),8*(args.size()+1)));
			}
			patchCall.AddToPatch(location);
		}

		//throw new UnsupportedOperationException("Unimplemented method 'visitCallStmt'");
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt.returnExpr.visit(this, false);
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RSP,Reg64.RBP)));
        _asm.add(new Pop(new ModRMSIB(Reg64.RBP,true)));
		_asm.add(new Ret());
		return null;
		//throw new UnsupportedOperationException("Unimplemented method 'visitReturnStmt'");
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		// TODO Auto-generated method stub

		//Evaluate Condition
		stmt.cond.visit(this,true);

		//Get Value

		_asm.add(new Pop(Reg64.RAX));

		//Compare compare to 0
		_asm.add(new Cmp(new ModRMSIB(Reg64.RAX,true),0));		

		//Check if we have an else stmt
		if(stmt.elseStmt == null)
		{
			//If we don't have else stmt
			//since we already compared potentially must jump past block so je 0
			int indexToPatch = _asm.add(new CondJmp(Condition.E,0));
			int startSize = _asm.getSize();
				
				//We dont know where to jump so we need to store the index to patch later
				//We need to evaluate the stmt.thenStmt
			stmt.thenStmt.visit(this,null);
			int endOffset = _asm.getSize();
				//Patch jump instruction
			_asm.patch(indexToPatch,new CondJmp(Condition.E,endOffset-startSize));
		}else{
			//If we do have else statment
				//Check if the evaluated condition is 0
				//If it is we must jump to else block so je 0
				int indexElseToPatch = _asm.add(new CondJmp(Condition.E,0));
				int thenStart = _asm.getSize();
				//We dont know where to jump so we need to store the index to patch later
				//We need to evaluate the stmt.thenStmt
				stmt.thenStmt.visit(this,null);
				//We must add a jump instruction at the end to jump past the else block
				int indexJumpToPatch = _asm.add(new Jmp(0));
				//Patch jump instruction that jumps to else statement
				int elseStart = _asm.getSize();
				_asm.patch(indexElseToPatch,new CondJmp(Condition.E,elseStart-thenStart));
				//we visit the stmt.elseStmt
				stmt.elseStmt.visit(this,null);
				//patch jump instruction that jumps past the else statemeent
				int elseEnd = _asm.getSize();
				_asm.patch(indexJumpToPatch, new Jmp(elseEnd-elseStart));
		}

		//throw new UnsupportedOperationException("Unimplemented method 'visitIfStmt'");
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		//Store where to jump to loop back
		int startOffset = _asm.getSize();
		//Evaluate Conditional Statement
		stmt.cond.visit(this, true);
		//Get Value
		_asm.add(new Pop(Reg64.RAX));
		//Compare compare to 0
		_asm.add(new Cmp(new ModRMSIB(Reg64.RAX,true),0));		
		//If condition evaluates to 0 we need to jump past the while stmt
		//We need to store current offset
		int conditionOffset = _asm.getSize();
		Instruction jump = new CondJmp(Condition.E,0);
		int conditionFalse = _asm.add(jump);
		//Evaluate Stmt body
		stmt.body.visit(this, null);
		int totalSize = _asm.getSize();
		_asm.add(new Jmp(totalSize,startOffset,false));
		_asm.patch(jump.listIdx,new CondJmp(Condition.E,jump.startAddress,_asm.getSize(),false));
		//throw new UnsupportedOperationException("Unimplemented method 'visitWhileStmt'");
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		Operator op = expr.operator;
		expr.expr.visit(this,false);
		//Get value into register
		_asm.add(new Pop(Reg64.RAX));
		if(op.spelling.equals("-"))
		{
			_asm.add(new Xor(new ModRMSIB(Reg64.RCX,Reg64.RCX)));
			_asm.add(new Sub(new ModRMSIB(Reg64.RCX,Reg64.RAX)));
			_asm.add(new Push(Reg64.RCX));
		}else if(op.spelling.equals("!"))
		{
			_asm.add(new Not(new ModRMSIB(Reg64.RAX,true)));
			_asm.add(new Push(Reg64.RAX));
		}
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		boolean pushed = false;
		Operator op = expr.operator;
		expr.left.visit(this,false);
		expr.right.visit(this,false);
		_asm.add(new Pop(Reg64.RCX));
		_asm.add(new Pop(Reg64.RAX));
		if(op.spelling.equals("&&"))
		{
			_asm.add(new And(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
		}else if(op.spelling.equals("||"))
		{
			_asm.add(new Or(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
		}else if(op.spelling.equals(">"))
		{
			_asm.add(new Xor(new ModRMSIB(Reg64.RDX,Reg64.RDX)));
			_asm.add(new Cmp(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
			_asm.add(new SetCond(Condition.GT,Reg8.DL));
			_asm.add(new Push(Reg64.RDX));
			pushed = true;

		}else if(op.spelling.equals(">="))
		{
			_asm.add(new Xor(new ModRMSIB(Reg64.RDX,Reg64.RDX)));
			_asm.add(new Cmp(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
			_asm.add(new SetCond(Condition.GTE,Reg8.DL));
			_asm.add(new Push(Reg64.RDX));
			pushed = true;
		}else if(op.spelling.equals("<"))
		{
			_asm.add(new Xor(new ModRMSIB(Reg64.RDX,Reg64.RDX)));
			_asm.add(new Cmp(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
			_asm.add(new SetCond(Condition.LT,Reg8.DL));
			_asm.add(new Push(Reg64.RDX));
			pushed = true;
		}else if(op.spelling.equals("<="))
		{
			_asm.add(new Xor(new ModRMSIB(Reg64.RDX,Reg64.RDX)));
			_asm.add(new Cmp(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
			_asm.add(new SetCond(Condition.LTE,Reg8.DL));
			_asm.add(new Push(Reg64.RDX));
			pushed = true;
		}else if(op.spelling.equals("+"))
		{
			_asm.add(new Add(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
		}else if(op.spelling.equals("-"))
		{
			_asm.add(new Sub(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
		}else if(op.spelling.equals("*"))
		{
			_asm.add(new Imul(new ModRMSIB(Reg64.RCX,Reg64.RAX)));
		}else if(op.spelling.equals("/"))
		{
			_asm.add(new Idiv(new ModRMSIB(Reg64.RCX,Reg64.RAX)));
		}else if(op.spelling.equals("=="))
		{
			_asm.add(new Xor(new ModRMSIB(Reg64.RDX,Reg64.RDX)));
			_asm.add(new Cmp(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
			_asm.add(new SetCond(Condition.E,Reg8.DL));
			_asm.add(new Push(Reg64.RDX));
			pushed = true;
		}else if(op.spelling.equals("!="))
		{
			_asm.add(new Xor(new ModRMSIB(Reg64.RDX,Reg64.RDX)));
			_asm.add(new Cmp(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
			_asm.add(new SetCond(Condition.NE,Reg8.DL));
			_asm.add(new Push(Reg64.RDX));
			pushed = true;
		}
		if(!pushed)
		_asm.add(new Push(Reg64.RAX));
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.ref.visit(this,arg);
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		// TODO Auto-generated method stub
		if(expr.ref instanceof IdRef)
		{
			expr.ref.visit(this, false);
		}else{
			expr.ref.visit(this, true);
		}
		expr.ixExpr.visit(this,false);
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Pop(Reg64.RBX));
		_asm.add(new Imul(Reg64.RAX,new ModRMSIB(Reg64.RAX, true),8));
		_asm.add(new Add(new ModRMSIB(Reg64.RAX,Reg64.RBX)));
		_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RAX,0,Reg64.RAX)));
		_asm.add(new Push(Reg64.RAX));
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		// TODO Auto-generated method stub
		//throw new UnsupportedOperationException("Unimplemented method 'visitCallExpr'");
		ExprList args = expr.argList;
		for(int i = args.size()-1; i >=0; i--)
		{
			args.get(i).visit(this,false);
		}
		if(expr.functionRef instanceof IdRef)
		{
			MethodDecl decl = (MethodDecl)(((IdRef)expr.functionRef).id.decl);
			if(!decl.isStatic)
			{
				_asm.add(new Push(new ModRMSIB(Reg64.RBP,16)));
			}
			PatchLocation location = new PatchLocation(decl,_asm.getCurrentIndex() ,_asm.getSize());
			_asm.add(new Call(0));
			if(decl.isStatic)
			{
				_asm.add(new Add(new ModRMSIB(Reg64.RSP,true),8*args.size()));
			}else{
				_asm.add(new Add(new ModRMSIB(Reg64.RSP,true),8*(args.size()+1)));
			}
			_asm.add(new Push(Reg64.RAX));
			patchCall.AddToPatch(location);
		}else if(expr.functionRef instanceof QualRef)
		{
			QualRef qRef = (QualRef)expr.functionRef;
			MethodDecl decl = (MethodDecl)(qRef.id.decl);
			if(!decl.isStatic)
			{
				qRef.ref.visit(this, false);
			}
			PatchLocation location = new PatchLocation( decl,_asm.getCurrentIndex(),_asm.getSize());
			_asm.add(new Call(0));
			if(decl.isStatic)
			{
				_asm.add(new Add(new ModRMSIB(Reg64.RSP,true),8*args.size()));
			}else{
				_asm.add(new Add(new ModRMSIB(Reg64.RSP,true),8*(args.size()+1)));
			}
			
			_asm.add(new Push(Reg64.RAX));
			patchCall.AddToPatch(location);
		}

		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.lit.visit(this,null);
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		// TODO Auto-generated method stub
		makeMalloc();
		_asm.add(new Push(new ModRMSIB(Reg64.RAX,true)));
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		// TODO Auto-generated method stub
		makeMalloc();
		_asm.add(new Push(new ModRMSIB(Reg64.RAX,true)));
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		// TODO Auto-generated method stub
		_asm.add(new Push(new ModRMSIB(Reg64.RBP,16)));
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		// TODO Auto-generated method stub
		if(ref.id.decl instanceof ClassDecl)
		{
			return null;
		}
		if(ref.id.decl instanceof FieldDecl)
		{	//System.out.println("Visiting " + ref.id.spelling+" inside "+currentClass);
			FieldDecl decl = (FieldDecl)ref.id.decl;
			if(decl.isStatic)
			{
				_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RAX,true),8*decl.indexInClass));
				_asm.add(new Add(new ModRMSIB(Reg64.RAX,Reg64.R15)));
			}else{
				//Get this
				_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RBP,16,Reg64.RAX)));
				//Load Offset
				_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RBX, true), 8*decl.indexInClass));
				//Add offset to this
				_asm.add(new Add(new ModRMSIB(Reg64.RAX,Reg64.RBX)));
			}
			
			if(arg != null)
			{
				if(!(boolean)arg)//Want the effective address
				{
					_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RAX,0,Reg64.RAX)));
				}
			}else{
				_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RAX,0,Reg64.RAX)));
			}
			_asm.add(new Push(Reg64.RAX));
			return null;
		}

		if(arg != null)
		{
			if((boolean)arg)//Want the effective address
			{
				_asm.add(new Lea(new ModRMSIB(ref.id.decl.entity.getRegister(),ref.id.decl.entity.getOffset(),Reg64.RAX)));
			}else{//want the value
				_asm.add(new Mov_rrm(new ModRMSIB(ref.id.decl.entity.getRegister(),ref.id.decl.entity.getOffset(),Reg64.RAX)));
			}
		}else{
			_asm.add(new Mov_rrm(new ModRMSIB(ref.id.decl.entity.getRegister(),ref.id.decl.entity.getOffset(),Reg64.RAX)));
		}
		
		//ref.id.visit(this,null);
		_asm.add(new Push(Reg64.RAX));
		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		// TODO Auto-generated method stub
		//Visit the left side of the qual ref
		ref.ref.visit(this, (Object)false);
		//Get the field declaration of the current id
		FieldDecl decl = (FieldDecl)ref.id.decl;
		//Get the value of the previous register
		_asm.add(new Pop(Reg64.RAX));
		//Add the current 
		//_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RBX,true), decl.indexInClass*8));
		//_asm.add(new Add(new ModRMSIB(Reg64.RAX, Reg64.RBX)));
		if((boolean)arg)
		{
			_asm.add(new Lea(new ModRMSIB(Reg64.RAX,decl.indexInClass*8,Reg64.RAX)));
		}else{
			_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RAX,decl.indexInClass*8,Reg64.RAX)));
		}
		_asm.add(new Push(Reg64.RAX));
        return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		// TODO Auto-generated method stub
		//_asm.add(new Push(new ModRMSIB(id.decl.entity.getRegister(),id.decl.entity.getOffset())));
		//throw new UnsupportedOperationException("Unimplemented method 'visitIdentifier'");
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		// TODO Auto-generated method stub
		_asm.add(new Push(Integer.parseInt(num.spelling)));
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		// TODO Auto-generated method stub
		if(bool.spelling.equals("true"))
		{
			_asm.add(new Push(1));
		}else{
			_asm.add(new Push(0));
		}
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nl, Object arg) {
		// TODO Auto-generated method stub
		_asm.add(new Push(0));
		return null;
	}
}
