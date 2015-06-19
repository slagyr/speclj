package speclj;

import clojure.lang.RT;
import clojure.lang.Var;

public class main
{
  public static void main(String[] args) throws Exception
  {
    RT.loadResourceScript("speclj/main.clj");
    Var main = RT.var("speclj.main", "-main");
    Var apply = RT.var("clojure.core", "apply");
    apply.invoke(main, args);
  }
}
