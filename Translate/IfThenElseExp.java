package Translate;
import Temp.Temp;
import Temp.Label;

class IfThenElseExp extends Exp {
  Exp cond, a, b;
  Label t = new Label();
  Label f = new Label();
  Label join = new Label();

  IfThenElseExp(Exp cc, Exp aa, Exp bb) {
    cond = cc; 
    a = aa; 
    b = bb;
  }

  @Override
  Tree.Stm unCx(Label tt, Label ff) {
    // This is the naive implementation; you should extend it to eliminate
    // unnecessary JUMP nodes
    Tree.Stm aStm = a.unCx(tt, ff);
    Tree.Stm bStm = b.unCx(tt, ff);

    Tree.Stm condStm = cond.unCx(t, f);

    if (aStm == null && bStm == null)
      return condStm;
    if (aStm == null)
      return new Tree.SEQ(condStm, new Tree.SEQ(new Tree.LABEL(f), bStm));
    if (bStm == null)
      return new Tree.SEQ(condStm, new Tree.SEQ(new Tree.LABEL(t), aStm));
    return new Tree.SEQ(condStm,
			new Tree.SEQ(new Tree.SEQ(new Tree.LABEL(t), aStm),
				     new Tree.SEQ(new Tree.LABEL(f), bStm)));
  }

  @Override
  Tree.Exp unEx() {
    // You must implement this function
    
    //can be called by Translate.procEntryExit during a FunctionDec translation
    //needs to return the translation code for the ifthenelse condition
    
    Tree.Exp returnReg = new Tree.TEMP(new Temp());
    Label exitLabel = new Label();
    Tree.Stm trueBlock = new Tree.SEQ(new Tree.LABEL(t), new Tree.SEQ(new Tree.MOVE(returnReg, a.unEx()), new Tree.JUMP(exitLabel)));
    Tree.Stm falseBlock = new Tree.SEQ(new Tree.LABEL(f), new Tree.SEQ(new Tree.MOVE(returnReg, b.unEx()), new Tree.JUMP(exitLabel)));
    
    Tree.Stm condition = new Tree.SEQ(cond.unCx(t, f), new Tree.SEQ(trueBlock, falseBlock));
    
    //make ESEQ with side effect of evaluation condition to return register, return that register
    return new Tree.ESEQ(new Tree.SEQ(condition, new Tree.LABEL(exitLabel)), returnReg);
  }

  @Override
  Tree.Stm unNx() {
    // You must implement this function
    return null;
  }
}
