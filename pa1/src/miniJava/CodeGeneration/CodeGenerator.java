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

		_asm.markOutputStart();

		/*_asm.add(new Push(65));
		_asm.add(new Push(new ModRMSIB(Reg64.RBP,true)));
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBP, Reg64.RSP)));
		makePrintln();
		*/
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
		System.out.println("Outputting Byte Code");
		_asm.outputFromMark();
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
		fd.entity = new RuntimeEntity(staticClassOffset,Reg64.R12);
		fd.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		// TODO Auto-generated method stub
		currentOffset = -8;
		paramOffset = 8;
		if(md.name.equals("main"))
		{
			System.out.println("Main Found");
			mainFound = true;
			entryPoint = _asm.getSize();
			_asm.add(new Push(new ModRMSIB(Reg64.RBP,true)));
			_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBP, Reg64.RSP)));

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
		System.out.println(pd);
		System.out.println(pd.name);
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
		for (Statement s: stmt.sl) {
        	s.visit(this, null);
        }
        return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt.varDecl.visit(this,false);
        stmt.initExp.visit(this, (Object)true);

        _asm.add(new Pop(new ModRMSIB(stmt.varDecl.entity.getRegister(),stmt.varDecl.entity.getOffset())));
        return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		stmt.ref.visit(this, false);
        stmt.val.visit(this, null);
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Pop(Reg64.RBX));
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBX,0,Reg64.RAX)));
        return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitIxAssignStmt'");
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		ExprList args = stmt.argList;
		for(int i = args.size()-1; i >=0; i--)
		{
			args.get(i).visit(this,null);
		}
		if(stmt.methodRef instanceof IdRef)
		{
			PatchLocation location = new PatchLocation((MethodDecl)(((IdRef)stmt.methodRef).id.decl),_asm.getCurrentIndex() ,_asm.getSize());
			_asm.add(new Call(0));
			
			patchCall.AddToPatch(location);
		}else if(stmt.methodRef instanceof QualRef)
		{
			QualRef qRef = (QualRef)stmt.methodRef;
			//String contextClass = (String)qRef.ref.visit(this, null);
			PatchLocation location = new PatchLocation( (MethodDecl)(qRef.id.decl),_asm.getCurrentIndex(),_asm.getSize());
			_asm.add(new Call(0));
			patchCall.AddToPatch(location);
		}
		//throw new UnsupportedOperationException("Unimplemented method 'visitCallStmt'");
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt.returnExpr.visit(this, true);
		_asm.add(new Pop(Reg64.RAX));
		return null;
		//throw new UnsupportedOperationException("Unimplemented method 'visitReturnStmt'");
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitIfStmt'");
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitWhileStmt'");
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		Operator op = expr.operator;
		expr.expr.visit(this,true);
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
		Operator op = expr.operator;
		expr.left.visit(this,true);
		expr.right.visit(this,true);
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
			_asm.add(new Cmp(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
		}else if(op.spelling.equals(">="))
		{
			_asm.add(new And(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
		}else if(op.spelling.equals("<"))
		{
			_asm.add(new And(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
		}else if(op.spelling.equals("<="))
		{
			_asm.add(new And(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
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
			_asm.add(new Cmp(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
		}else if(op.spelling.equals("!="))
		{
			_asm.add(new Cmp(new ModRMSIB(Reg64.RAX,Reg64.RCX)));
		}
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
		throw new UnsupportedOperationException("Unimplemented method 'visitIxExpr'");
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitCallExpr'");
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
		throw new UnsupportedOperationException("Unimplemented method 'visitThisRef'");
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		// TODO Auto-generated method stub
		//if((boolean)arg)//Want the effective address
		//{
		//	_asm.add(new Lea(new ModRMSIB(ref.id.decl.entity.getRegister(),ref.id.decl.entity.getOffset(),Reg64.RAX)));
		//}
		//ref.id.visit(this,null);
		_asm.add(new Mov_rrm(new ModRMSIB(ref.id.decl.entity.getRegister(),ref.id.decl.entity.getOffset(),Reg64.RAX)));
		_asm.add(new Push(Reg64.RAX));
		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		// TODO Auto-generated method stub
		ref.ref.visit(this, false);
		FieldDecl decl = (FieldDecl)ref.id.decl;
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RBX,true), decl.indexInClass*8));
		_asm.add(new Add(new ModRMSIB(Reg64.RAX, Reg64.RBX)));
		if((boolean)arg)
		{
			_asm.add(new Push(new ModRMSIB(Reg64.RAX,0)));
		}else{
			_asm.add(new Push(Reg64.RAX));
		}
		
        return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		// TODO Auto-generated method stub
		System.out.println(id.decl);
		System.out.println(id.decl.name);
		_asm.add(new Push(new ModRMSIB(id.decl.entity.getRegister(),id.decl.entity.getOffset())));
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
