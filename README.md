# TigerAsm
Intermediate Code Generator for the Tiger Programming Language

## Project 5 for CSC 4351 with Dr. Gerald Baumgartner, LSU.
Kristen Barrett and Matthew Wolff

## Known flaws:
testcases/test42.tig messes us up. There's an error towards the end of the file. It's a really big test that's hard to debug...
testcases/test12.tig The body of the for loop (a:=a+1;()) has a NilExp at the end. So the Semantic Analyzer creates a SeqExp(...,NilExp). The reference implementation tosses this NilExp. To accomplish this effect, all nilexp's in our implementation point to the same Java object. In Tranlate.Translate.SeqExp(), we just test for pointer equality and ignore when we see the NilExp. I admit this is basically programmer cheese, but it solves the problem of this test.

### On small errors in label and temp names
Because of how the framework chooses temps and label names, we don't have control over which are picked in what order. The reference implementation does this is a different order than we do, and thus there are discrepencies between which are picked. This should not change the effect of the code.
This affects a few (>1) tests.
