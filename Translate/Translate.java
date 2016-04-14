package Translate;
import Symbol.Symbol;
import Tree.BINOP;
import Tree.CJUMP;
import Temp.Temp;
import Temp.Label;

public class Translate {
  public Frame.Frame frame;
  public Translate(Frame.Frame f) {
    frame = f;
  }
  private Frag frags;
  public void procEntryExit(Level level, Exp body) {
    Frame.Frame myframe = level.frame;
    Tree.Exp bodyExp = body.unEx();
    Tree.Stm bodyStm;
    if (bodyExp != null)
      bodyStm = MOVE(TEMP(myframe.RV()), bodyExp);
    else
      bodyStm = body.unNx();
    ProcFrag frag = new ProcFrag(myframe.procEntryExit1(bodyStm), myframe);
    frag.next = frags;
    frags = frag;
  }
  public Frag getResult() {
    return frags;
  }

  private static Tree.Exp CONST(int value) {
    return new Tree.CONST(value);
  }
  private static Tree.Exp NAME(Label label) {
    return new Tree.NAME(label);
  }
  private static Tree.Exp TEMP(Temp temp) {
    return new Tree.TEMP(temp);
  }
  private static Tree.Exp BINOP(int binop, Tree.Exp left, Tree.Exp right) {
    return new Tree.BINOP(binop, left, right);
  }
  private static Tree.Exp MEM(Tree.Exp exp) {
    return new Tree.MEM(exp);
  }
  private static Tree.Exp CALL(Tree.Exp func, Tree.ExpList args) {
    return new Tree.CALL(func, args);
  }
  private static Tree.Exp ESEQ(Tree.Stm stm, Tree.Exp exp) {
    if (stm == null)
      return exp;
    return new Tree.ESEQ(stm, exp);
  }

  private static Tree.Stm MOVE(Tree.Exp dst, Tree.Exp src) {
    return new Tree.MOVE(dst, src);
  }
  private static Tree.Stm EXP(Tree.Exp exp) {
    return new Tree.EXP(exp);
  }
  private static Tree.Stm JUMP(Label target) {
    return new Tree.JUMP(target);
  }
  private static
  Tree.Stm CJUMP(int relop, Tree.Exp l, Tree.Exp r, Label t, Label f) {
    return new Tree.CJUMP(relop, l, r, t, f);
  }
  private static Tree.Stm SEQ(Tree.Stm left, Tree.Stm right) {
    if (left == null)
      return right;
    if (right == null)
      return left;
    return new Tree.SEQ(left, right);
  }
  private static Tree.Stm LABEL(Label label) {
    return new Tree.LABEL(label);
  }

  private static Tree.ExpList ExpList(Tree.Exp head, Tree.ExpList tail) {
    return new Tree.ExpList(head, tail);
  }
  private static Tree.ExpList ExpList(Tree.Exp head) {
    return ExpList(head, null);
  }
  private static Tree.ExpList ExpList(ExpList exp) {
    if (exp == null)
      return null;
    return ExpList(exp.head.unEx(), ExpList(exp.tail));
  }

  public Exp Error() {
    return new Ex(CONST(0));
  }

  public Exp SimpleVar(Access access, Level level) {
    return Error();
  }

  public Exp FieldVar(Exp record, int index) {
    return Error();
  }

  public Exp SubscriptVar(Exp array, Exp index) {
    return Error();
  }

  public Exp NilExp() {
    return new Nx(null);
  }

  public Exp IntExp(int value) {
    return new Ex(CONST(value));
  }

  private java.util.Hashtable strings = new java.util.Hashtable();
  public Exp StringExp(String lit) {
    String u = lit.intern();
    Label lab = (Label)strings.get(u);
    if (lab == null) {
      lab = new Label();
      strings.put(u, lab);
      DataFrag frag = new DataFrag(frame.string(lab, u));
      frag.next = frags;
      frags = frag;
    }
    return new Ex(NAME(lab));
  }

  private Tree.Exp CallExp(Symbol f, ExpList args, Level from) {
    return frame.externalCall(f.toString(), ExpList(args));
  }
  private Tree.Exp CallExp(Level f, ExpList args, Level from) {
      //add frame pointer to args list
      return CALL(NAME(f.name()), ExpList(null, ExpList(args)));
  }

  public Exp FunExp(Symbol f, ExpList args, Level from) {
    return new Ex(CallExp(f, args, from));
  }
  public Exp FunExp(Level f, ExpList args, Level from) {
    return new Ex(CallExp(f, args, from));
  }
  public Exp ProcExp(Symbol f, ExpList args, Level from) {
    return new Nx(EXP(CallExp(f, args, from)));
  }
  public Exp ProcExp(Level f, ExpList args, Level from) {
    return new Nx(EXP(CallExp(f, args, from)));
  }

  public Exp OpExp(int op, Exp left, Exp right) {
    return new Ex(BINOP(op, left.unEx(), right.unEx()));
  }

  public Exp StrOpExp(int op, Exp left, Exp right) {
      //make separate instruction to compare the strings
      return new RelCx(op, left.unEx(), right.unEx());
  }

  public Exp RecordExp(ExpList init) {
    //using malloc
    //i'm a c programmer
    int numArgs = 0;
    ExpList iterator = init;
    while(init!=null) {
        numArgs++;
        iterator = iterator.tail;
    }
    Temp headPointer = new Temp();
    //look dr. whaley I used malloc am I in the cool kids club yet?
    Tree.Stm creation = MOVE(TEMP(headPointer), frame.externalCall("malloc", ExpList(CONST(numArgs*4),null)));
    Tree.Stm initialization = initArray(headPointer, init, 0);
    return new Ex(ESEQ(SEQ(creation, initialization), TEMP(headPointer)));
  }
  
  private Tree.Stm initArray(Temp pointer, ExpList init, int offset) {
      if(init==null)
          return null;
      //copy the initial value into the memory location given by pointer+offset
      Tree.Stm copyOp = MOVE(MEM(BINOP(Tree.BINOP.PLUS, TEMP(pointer), CONST(offset))), init.head.unEx());
      //do that instruction, followed by the initialization of the next record value at offset += 4
      return SEQ(copyOp, initArray(pointer, init.tail, offset+4));
  }

  public Exp SeqExp(ExpList e) {
    return Error();
  }

  public Exp AssignExp(Exp lhs, Exp rhs) {
    return new Nx(MOVE(lhs.unEx(),rhs.unEx()));
  }

  public Exp IfExp(Exp cc, Exp aa, Exp bb) {
    return new IfThenElseExp(cc,aa,bb);
  }

  public Exp WhileExp(Exp test, Exp body, Label done) {
    return Error();
  }

  public Exp ForExp(Access i, Exp lo, Exp hi, Exp body, Label done) {
    return Error();
  }

  public Exp BreakExp(Label done) {
    return new Nx(JUMP(done));
  }

  public Exp LetExp(ExpList lets, Exp body) {
    //recurse through the lets
    Tree.Stm letStatements = stmLets(lets);
    Tree.Exp travBody = body.unEx();
    if(letStatements == null)
        return new Ex(travBody);
    return new Ex(ESEQ(letStatements, travBody));
  }
  
  private Tree.Stm stmLets(ExpList lets) {
      if(lets == null) 
          return null;
      else return SEQ(lets.head.unNx(), stmLets(lets.tail));
  }

  public Exp ArrayExp(Exp size, Exp init) {
    //get memory size
      Tree.Exp memSize = size.unEx();
      //call external function to instantiate array
      //almost a C programmer
      return new Ex(frame.externalCall("initArray", ExpList(memSize, ExpList(init.unEx()))));
  }

  public Exp VarDec(Access a, Exp init) {
    return Error();
  }

  public Exp TypeDec() {
    return new Nx(null);
  }

  public Exp FunctionDec() {
    return new Nx(null);
  }
}
