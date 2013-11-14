package speclj;

public class SpecFailure extends Exception
{
  public SpecFailure(String s)
  {
    super(s);
  }

  public SpecFailure(String s, Throwable cause)
  {
    super(s, cause);
  }
}
