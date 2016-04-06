package FindEscape;

public class FindEscape {
  Symbol.Table escEnv = new Symbol.Table(); // escEnv maps Symbol to Escape
  Absyn.FunctionDec scope = null;

  public FindEscape(Absyn.Exp e) { traverseExp(0, e);  }

  void traverseVar(int depth, Absyn.Var v) {
      if (v instanceof Absyn.FieldVar)
          traverseVar(depth, (Absyn.FieldVar) v);
      else if (v instanceof Absyn.SimpleVar)
          traverseVar(depth, (Absyn.SimpleVar) v);
      else if (v instanceof Absyn.SubscriptVar)
          traverseVar(depth, (Absyn.SubscriptVar) v);
  }
  
  void traverseVar(int depth, Absyn.FieldVar v) {
      traverseVar(depth, v.var);
  }
  
  void traverseVar(int depth, Absyn.SimpleVar v) {
      //check for escapement
      Escape escape = (Escape) escEnv.get(v.name);
      if(escape != null && depth > escape.depth)
          escape.setEscape();
  }
  
  void traverseVar(int depth, Absyn.SubscriptVar v) {
      traverseVar(depth, v.var);
      traverseExp(depth, v.index);
  }

  void traverseExp(int depth, Absyn.Exp e) {
    //oh boy instanceof! My favorite!
    if (e instanceof Absyn.ArrayExp) {
      traverseExp(depth, (Absyn.ArrayExp) e);
    }
    else if (e instanceof Absyn.AssignExp) {
      traverseExp(depth, (Absyn.AssignExp) e);
    }
    else if (e instanceof Absyn.BreakExp) {
        //nothing
    }
    else if (e instanceof Absyn.CallExp) {
      traverseExp(depth, (Absyn.CallExp) e);
    }
    else if (e instanceof Absyn.ForExp) {
      traverseExp(depth, (Absyn.ForExp) e);
    }
    else if (e instanceof Absyn.IfExp) {
      traverseExp(depth, (Absyn.IfExp) e);
    }
    else if (e instanceof Absyn.IntExp) {
      //nothing
    }
    else if (e instanceof Absyn.LetExp) {
      traverseExp(depth, (Absyn.LetExp) e);
    }
    else if (e instanceof Absyn.NilExp) {
       //kek
    }
    else if (e instanceof Absyn.OpExp) {
      traverseExp(depth, (Absyn.OpExp) e);
    }
    else if (e instanceof Absyn.RecordExp) {
      traverseExp(depth, (Absyn.RecordExp) e);
    }
    else if (e instanceof Absyn.SeqExp) {
      traverseExp(depth, (Absyn.SeqExp) e);
    }
    else if (e instanceof Absyn.StringExp) {
      //nothing
    }
    else if (e instanceof Absyn.VarExp) {
      traverseExp(depth, (Absyn.VarExp) e);
    }
    else if (e instanceof Absyn.WhileExp) {
      traverseExp(depth, (Absyn.WhileExp) e);
    }
  }

  void traverseDec(int depth, Absyn.Dec d) {
      //general form: upon seeing a declaration, record it in the environment with this depth
      if (d instanceof Absyn.FunctionDec)
          traverseDec(depth, (Absyn.FunctionDec) d);
      else if (d instanceof Absyn.TypeDec)
          traverseDec(depth, (Absyn.TypeDec) d);
      else if (d instanceof Absyn.VarDec)
          traverseDec(depth, (Absyn.VarDec) d);
  }
  
  void traverseDec(int depth, Absyn.FunctionDec d) {
      //functions are tricky. first begin scope
      Absyn.FunctionDec outerScope = scope;
      Absyn.FunctionDec iterator = d;
      while (iterator != null) {
          escEnv.beginScope();
          depth++;
          //go through params and enter them into environment
          Absyn.FieldList paramIterator = d.params;
          while (paramIterator != null) {
            escEnv.put(paramIterator.name, new FormalEscape(depth, paramIterator));
            paramIterator = paramIterator.tail;
          }
          //traverse function body
          scope = d;
          traverseExp(depth, iterator.body);
          //exit code
          escEnv.endScope();
          iterator = iterator.next;
      }
      scope = outerScope;
      
  }
  
  void traverseDec(int depth, Absyn.TypeDec d) {
      //types are not added to the environment
  }
  
  void traverseDec(int depth, Absyn.VarDec d) {
      //traverse initial value
      traverseExp(depth, d.init);
      //add variable name to scope
      //REMEMBER ALWAYS ADD VARESCAPES TO ENVIRONMENT
      escEnv.put(d.name, new VarEscape(depth, d));
  }

  //General format for these: recurse into each Exp property
  void traverseExp(int depth, Absyn.ArrayExp e) {
    traverseExp(depth, e.size);
    traverseExp(depth, e.init);
  }

  void traverseExp(int depth, Absyn.AssignExp e) {
    traverseVar(depth, e.var);
    traverseExp(depth, e.exp);
  }

  void traverseExp(int depth, Absyn.CallExp e) {
    if (scope!=null)
        scope.leaf = false;
    Absyn.ExpList iterator = e.args;
    while (iterator != null) {
      traverseExp(depth, iterator.head);
      iterator = iterator.tail;
    }
  }

  void traverseExp(int depth, Absyn.ForExp e) {
    //nevermind there's no return value
    traverseExp(depth, e.var.init);
    //entering scope for For loop
    escEnv.beginScope();
    //add iterator to this scope
    escEnv.put(e.var.name, new VarEscape(depth, e.var));

    //Go through the for loop terminal and body
    traverseExp(depth, e.hi);
    traverseExp(depth, e.body);
    escEnv.endScope();
  }

  void traverseExp(int depth, Absyn.IfExp e) {
    traverseExp(depth, e.test);
    traverseExp(depth, e.thenclause);
    if(e.elseclause != null) 
      traverseExp(depth, e.elseclause);
  }

  void traverseExp(int depth, Absyn.LetExp e) {
      //for decs, declare a scope, then go into the decs
      escEnv.beginScope();
      Absyn.DecList iterator = e.decs;
      while(iterator!=null) {
          traverseDec(depth, iterator.head);
          iterator = iterator.tail;
      }
      
      traverseExp(depth, e.body);
      escEnv.endScope();
  }
  
  void traverseExp(int depth, Absyn.OpExp e) {
      traverseExp(depth, e.left);
      traverseExp(depth, e.right);
  }
  
  void traverseExp(int depth, Absyn.RecordExp e) {
      Absyn.FieldExpList iterator = e.fields;
      while (iterator != null) {
          traverseExp(depth, iterator.init);
          iterator = iterator.tail;
      }
  }
  
  void traverseExp(int depth, Absyn.SeqExp e) {
      Absyn.ExpList iterator = e.list;
      while (iterator != null) {
          traverseExp(depth, iterator.head);
          iterator = iterator.tail;
      }
  }
  
  void traverseExp(int depth, Absyn.VarExp e) {
      traverseVar(depth, e.var);
  }
  
  void traverseExp(int depth, Absyn.WhileExp e) {
      traverseExp(depth, e.test);
      traverseExp(depth, e.body);
  }
}
