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
      //get the frame pointer of this frame
      Tree.Exp fp = TEMP(frame.FP());
      //find the pointer to the frame in which the variable was defined
      
      Level defLevel = level;
      while(defLevel != access.home) {
          //uhhhhhhh
          fp = level.frame.formals.head.exp(fp);
          defLevel = defLevel.parent;
      }
      
      //location of variable in access
      Tree.Exp varAddr = access.acc.exp(fp);
      return new Ex(varAddr);
  }

  public Exp FieldVar(Exp record, int index) {
    //register for the record
    //NOTE, keep track of the Temp object, not TEMP (as that is the instruction for (example) %eax)
    Temp recordReg = new Temp();
    //memory address to beginning of record stored in register
    //WRONG (maybe not?)
    Tree.Stm recordLoadInstr = MOVE(TEMP(recordReg), record.unEx());
    //error handling
    Label badPtr = new Label("_BADPTR");
    Label ok = new Label();
    Tree.Stm nullCheck = SEQ(CJUMP(Tree.CJUMP.EQ, TEMP(recordReg), CONST(0), badPtr, ok), LABEL(ok));
    Tree.Stm moveAndCheck = SEQ(recordLoadInstr, nullCheck);
    //offset of field we want
    int offset = index*4; //wordsize 
    //calculate memory address of intended field
    //i'm an assembly programmer (but in x86 this'd be a lea)
    Tree.Exp fieldAddr = BINOP(Tree.BINOP.PLUS, TEMP(recordReg), CONST(offset));
    //Load the address from memory
    Tree.Exp memLoadInstr = MEM(fieldAddr);
    //Return the ESEQ for first loading the record and then loading the field, returning the field.
    return new Ex(ESEQ(moveAndCheck, memLoadInstr));
  }

  public Exp SubscriptVar(Exp array, Exp index) {
    //SIZE OF ARRAY IS LOCATED ONE WORD BEFORE ARRAY POINTER
    //similar thing for arrays
    //get a register for the array pointer
    Temp arrReg = new Temp();
    Temp indexReg = new Temp();
    //load the array pointer into the register
    Tree.Stm arrLoadInstr = MOVE(TEMP(arrReg), array.unEx());
    Tree.Stm loadIndex = MOVE(TEMP(indexReg), index.unEx());
    Label badSub = new Label("_BADSUB");
    Label chkSize = new Label();
    Label ok = new Label();
    Tree.Stm indexUnderflow = CJUMP(Tree.CJUMP.LT, TEMP(indexReg), CONST(0), badSub, chkSize);
    Tree.Stm indexOverflow = SEQ(CJUMP(Tree.CJUMP.GT, TEMP(indexReg), MEM(BINOP(Tree.BINOP.PLUS, TEMP(arrReg), CONST(-4))), badSub, ok),LABEL(ok));
    
    //going backwards and assembling statements
    Tree.Stm chkOverflow = SEQ(LABEL(chkSize), indexOverflow);
    Tree.Stm errorChecks = SEQ(indexUnderflow, chkOverflow);
    Tree.Stm indxLoad = SEQ(loadIndex, errorChecks);
    
    Tree.Stm arrPrologue = SEQ(arrLoadInstr, indxLoad);
        
    //calculate offset
    Tree.Exp elementOffs = BINOP(Tree.BINOP.MUL, TEMP(indexReg), CONST(4));
    //get pointer to element
    Tree.Exp elementAddr = BINOP(Tree.BINOP.PLUS, TEMP(arrReg), elementOffs);
    //dereference
    Tree.Exp memLoadInstr = MEM(elementAddr);
    //Return the ESEQ for first loading the array pointer and then loading the element  
    return new Ex(ESEQ(arrPrologue, memLoadInstr));
  }

  //The nil expression is represented as a pointer to 0
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
      //To call a function, we must pass in the static link as the first parameter.
      //first, get the frame pointer for the function call scope
      Label functionName = f.name();
      Tree.Exp fp = TEMP(from.frame.FP());
      //Then, using that frame pointer, go up until you find the level that this function is in
      Level defLevel = from;
      while(defLevel != f.parent) {
          //Keep making pointers to the upper level relative to the previous level
          //DUDE THIS MAKES NO SENSE WHY DOES THIS GO FIRST
          //(maybe we have to do at least one iteration..?)
          defLevel = defLevel.parent;
          fp = defLevel.frame.formals.head.exp(fp);
      }
      Tree.Exp retval =  CALL(NAME(f.name()), ExpList(fp,ExpList(args)));
      return retval;
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
      Tree.Exp leftExp = left.unEx();
      Tree.Exp rightExp = right.unEx();
      //may need to be changed, testing for IF
      switch(op) {
          //Absyn.OpExp and CJUMP conditionals are not identical...
          case Absyn.OpExp.PLUS:
          case Absyn.OpExp.MINUS:
          case Absyn.OpExp.MUL:
          case Absyn.OpExp.DIV:
              return new Ex(BINOP(op, left.unEx(), right.unEx()));
          case Absyn.OpExp.EQ:
              return new RelCx(CJUMP.EQ, left.unEx(), right.unEx());
          case Absyn.OpExp.NE:
              return new RelCx(CJUMP.NE, left.unEx(), right.unEx());
          case Absyn.OpExp.GE:
              return new RelCx(CJUMP.GE, left.unEx(), right.unEx());
          case Absyn.OpExp.GT:
              return new RelCx(CJUMP.GT, left.unEx(), right.unEx());
          case Absyn.OpExp.LE:
              return new RelCx(CJUMP.LE, left.unEx(), right.unEx());
          case Absyn.OpExp.LT:
              return new RelCx(CJUMP.LT, left.unEx(), right.unEx());
          default:
               System.err.println("unknown operator "+op);
               return Error();
      }
  }

  public Exp StrOpExp(int op, Exp left, Exp right) {
      //make separate instruction to compare the strings
      return new RelCx(op, left.unEx(), right.unEx());
  }

  public Exp RecordExp(ExpList init) {
    //using malloc
    //i'm a c programmer
    //nevermind there's an asm for that
    int numArgs = 0;
    ExpList iterator = init;
    while(iterator!=null) {
        numArgs++;
        iterator = iterator.tail;
    }
    Temp headPointer = new Temp();
    //look dr. whaley I used malloc am I in the cool kids club yet?
    //nevermind I was going to be cool and use malloc but the reference implementation doesn't do that soo....
    Tree.Stm creation = MOVE(TEMP(headPointer), frame.externalCall("allocRecord", ExpList(CONST(numArgs),null)));
    Tree.Stm initialization = initRecord(headPointer, init, 0);
    return new Ex(ESEQ(SEQ(creation, initialization), TEMP(headPointer)));
  }
  
  private Tree.Stm initRecord(Temp pointer, ExpList init, int offset) {
      if(init==null)
          return null;
      //copy the initial value into the memory location given by pointer+offset
      Tree.Stm copyOp = MOVE(MEM(BINOP(Tree.BINOP.PLUS, TEMP(pointer), CONST(offset))), init.head.unEx());
      //do that instruction, followed by the initialization of the next record value at offset += 4
      return SEQ(copyOp, initRecord(pointer, init.tail, offset+4));
  }

  public Exp SeqExp(ExpList e) {
    //never return null you dummy
      if(e==null)
        return NilExp();
      if(e.head == null)
        return NilExp();
    if(e.tail==null)
        return new Nx(e.head.unNx());
    else return new Nx(SEQ(e.head.unNx(), SeqExp(e.tail).unNx()));
  }

  public Exp AssignExp(Exp lhs, Exp rhs) {
    return new Nx(MOVE(lhs.unEx(),rhs.unEx()));
  }

  public Exp IfExp(Exp cc, Exp aa, Exp bb) {
      //test, then, else
      if(bb!=null)
        return new IfThenElseExp(cc,aa,bb);
      else {
          //do things that don't require an else branch
          //CJUMP, LABEL (if true) AA
          //i'm pretty sure that there's no case where if without else happens...
          Label trueLabel = new Label();
          Label exitLabel = new Label();
          Tree.Stm trueBlock = SEQ(LABEL(trueLabel), aa.unNx());
          return new Nx(SEQ(SEQ(cc.unCx(trueLabel, exitLabel), trueBlock),LABEL(exitLabel)));
      }
  }

  public Exp WhileExp(Exp test, Exp body, Label done) {
    //create label for the test
    Label testLabel = new Label();
    Label fallthroughLabel = new Label();
    Tree.Stm prologue = SEQ(LABEL(testLabel), test.unCx(fallthroughLabel, done));
    Tree.Stm bodyInstr = SEQ(LABEL(fallthroughLabel),body.unNx());
    Tree.Stm epilogue = JUMP(testLabel);
    Tree.Stm full = SEQ(prologue, SEQ(bodyInstr, epilogue));
    /**
     * test:
     *    if not test goto done
     *    BODY
     *    goto test
     * done:
     */
    return new Nx(full);
  }

  public Exp ForExp(Access i, Exp lo, Exp hi, Exp body, Label done) {
     
      //TODO: use i somehow.....
      
     Tree.Exp loEx = lo.unEx();
      Tree.Exp hiEx = hi.unEx();
      Temp loReg = new Temp();
      Temp hiReg = new Temp();
      Tree.Stm loadLo = MOVE(TEMP(loReg), loEx);
      Tree.Stm loadHi = MOVE(TEMP(hiReg), hiEx);
      Label bodyLabel = new Label();
      Label exitLabel = new Label();
      Label incrementLabel = new Label();
      Tree.Stm incExp = SEQ(SEQ(LABEL(incrementLabel), MOVE(TEMP(loReg), BINOP(Tree.BINOP.PLUS, TEMP(loReg), CONST(1)))),JUMP(bodyLabel));
      Tree.Stm bodyNx = body.unNx();
      Tree.Exp bodyEx = body.unEx();
      Tree.Stm bodyBlock = SEQ(SEQ(LABEL(bodyLabel),body.unNx()),new Tree.CJUMP(Tree.CJUMP.LT, TEMP(loReg), TEMP(hiReg), incrementLabel, exitLabel));
      Tree.Stm epilogue = SEQ(bodyBlock, incExp);
      Tree.Stm loads = SEQ(loadLo, loadHi);
      Tree.Stm prologue = SEQ(loads, new Tree.CJUMP(Tree.CJUMP.LE, TEMP(loReg), TEMP(hiReg), bodyLabel, exitLabel));
      Tree.Stm forBlock = SEQ(prologue, epilogue);
      return new Nx(SEQ(forBlock, LABEL(exitLabel)));
  }

  public Exp BreakExp(Label done) {
    return new Nx(JUMP(done));
  }

  public Exp LetExp(ExpList lets, Exp body) {
    //recurse through the lets
    Tree.Stm letStatements = stmLets(lets);
    Tree.Exp travBody = body.unEx();
    //TODO: still need to figure out better version of this
    Tree.Stm bodyNx = body.unNx();
    if(travBody == null) {
        //The body of this let expression doesn't produce any value. 
        //So just do the lets followed by the Nx body
        return new Nx(SEQ(letStatements, bodyNx));
    }
    //TODO: Fix this
    if(letStatements == null)
        return new Ex(body.unEx());
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
    //calculate variable's initialization
    Tree.Exp initVal;
    //null check just in case
    if(init ==null)
        initVal = NilExp().unEx();
    else initVal = init.unEx();
    //put it in a place
    return new Nx(MOVE(a.acc.exp(TEMP(a.home.frame.FP())), initVal));
  }

  public Exp TypeDec() {
    return new Nx(null);
  }

  public Exp FunctionDec() {
    return new Nx(null);
  }
}
