package Semant;

public class VarEntry extends Entry {
  Translate.Access access;
  public Types.Type ty;
  //MW Reordered parameters to quiet compiler
  VarEntry(Translate.Access access, Types.Type t) {
    ty = t;
    this.access = access;
  }
}
