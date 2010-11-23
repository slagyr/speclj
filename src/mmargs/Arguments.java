//- Copyright © 2010 Micah Martin All Rights Reserved.
//- mmargs and all included source files are distributed under terms of the MIT License.

// COPIED FROM https://github.com/slagyr/mmargs
// DO NOT EDIT HERE

package mmargs;

import java.util.*;
import java.util.regex.Pattern;

public class Arguments
{
  public static final int MAX_ROW_LENGTH = 72;

  private List<Parameter> parameters = new LinkedList<Parameter>();
  private List<Option> options = new LinkedList<Option>();

  public void addParameter(String name, String description)
  {
    parameters.add(new Parameter(name, description, true, false));
  }

  public void addOptionalParameter(String name, String description)
  {
    parameters.add(new Parameter(name, description, false, false));
  }

  public void addMultiParameter(String name, String description)
  {
    parameters.add(new Parameter(name, description, false, true));
  }

  public void addSwitchOption(String shortName, String fullName, String description)
  {
    addValueOption(shortName, fullName, null, description);
  }

  public void addValueOption(String shortName, String fullName, String valueDescription, String description)
  {
    addValueOption(shortName, fullName, valueDescription, description, false);
  }

  private void addValueOption(String shortName, String fullName, String valueDescription, String description, boolean multi)
  {
    if(shortName == null || fullName == null)
      throw new RuntimeException("Options require a shortName and fullName");
    options.add(new Option(shortName, fullName, valueDescription, description, multi));
  }

  public void addMultiOption(String shortName, String fullName, String valueDescription, String description)
  {
    addValueOption(shortName, fullName, valueDescription, description, true);
  }


  public Map<String, Object> parse(String... args)
  {
    HashMap<String, Object> results = new HashMap<String, Object>();
    LinkedList<String> params = parseOptions(args, results);
    LinkedList<String> leftOver = parseParams(params, results);
    processLeftOver(leftOver, results);
    if(leftOver.size() > 0)
      results.put("*leftover", leftOver);

    return results;
  }

  public String argString()
  {
    StringBuffer buffer = new StringBuffer();
    if(options.size() > 0)
      buffer.append("[options] ");

    for(int i = 0; i < parameters.size(); i++)
    {
      if(i > 0)
        buffer.append(" ");
      Parameter parameter = parameters.get(i);
      if(parameter.required)
        buffer.append("<").append(parameter.name).append(">");
      else
        buffer.append("[").append(parameter.name).append(parameter.multi ? "*" : "").append("]");
    }

    return buffer.toString();
  }

  public String parametersString()
  {
    String[] names = new String[parameters.size()];
    String[] descriptions = new String[parameters.size()];
    for(int i = 0; i < parameters.size(); i++)
    {
      final Parameter parameter = parameters.get(i);
      names[i] = parameter.name;
      descriptions[i] = parameter.description;
    }
    return tabularize(names, descriptions);
  }

  public String optionsString()
  {
    String[] heads = new String[options.size()];
    String[] descriptions = new String[options.size()];
    for(int i = 0; i < options.size(); i++)
    {
      final Option option = options.get(i);
      heads[i] = option.head();
      descriptions[i] = option.description;
    }

    return tabularize(heads, descriptions);
  }

  public Option findOption(String name)
  {
    for(Option option : options)
    {
      if(option.shortName.equals(name) || option.fullName.equals(name))
        return option;
    }
    return null;
  }

  private LinkedList<String> parseParams(LinkedList<String> args, HashMap<String, Object> results)
  {
    LinkedList<String> leftOver = new LinkedList<String>();
    LinkedList<Parameter> unfilledParams = new LinkedList<Parameter>(this.parameters);

    Parameter parameter = null;
    while(!args.isEmpty())
    {
      String arg = pop(args);
      if(isOption(arg))
        leftOver.add(arg);
      else
      {
        if(unfilledParams.isEmpty())
        {
          if(parameter != null && parameter.multi)
            setValue(results, parameter.name, arg, true);
          else
            leftOver.add(arg);
        }
        else
        {
          parameter = unfilledParams.removeFirst();
          setValue(results, parameter.name, arg, parameter.multi);
        }
      }
    }

    for(Parameter unfilledParam : unfilledParams)
      if(unfilledParam.required)
        addError(results, "Missing parameter: " + unfilledParam.name);

    return leftOver;
  }

  private void setValue(HashMap<String, Object> results, String name, String arg, boolean multi)
  {
    if(multi)
    {
      List values = (List) results.get(name);
      if(values == null)
      {
        values = new LinkedList<String>();
        results.put(name, values);
      }
      values.add(arg);
    }
    else
      results.put(name, arg);
  }

  private static String pop(LinkedList<String> args)
  {
    if(args.isEmpty())
      return null;
    else
      return args.removeFirst();
  }

  private LinkedList<String> parseOptions(String[] argArray, HashMap<String, Object> results)
  {
    LinkedList<String> args = new LinkedList<String>(Arrays.asList(argArray));
    LinkedList<String> leftOver = new LinkedList<String>();
    while(!args.isEmpty())
    {
      String arg = pop(args);
      if(isOption(arg))
      {
        OptionParser parser = new OptionParser(arg);
        if(findOption(parser.argName) != null)
          parseOption(parser, args, results);
        else
          leftOver.add(arg);
      }
      else
        leftOver.add(arg);
    }
    return leftOver;
  }

  private void processLeftOver(LinkedList<String> leftOver, HashMap<String, Object> results)
  {
    for(String arg : leftOver)
    {
      if(isOption(arg))
        addError(results, "Unrecognized option: " + arg);
      else
        addError(results, "Unexpected parameter: " + arg);
    }
  }

  private void addError(HashMap<String, Object> results, Object message)
  {
    LinkedList errors = (LinkedList) results.get("*errors");
    if(errors == null)
    {
      errors = new LinkedList<String>();
      results.put("*errors", errors);
    }
    errors.add(message);
  }

  private void parseOption(OptionParser parser, LinkedList<String> args, HashMap<String, Object> results)
  {
    Option option = findOption(parser.argName);
    if(option.requiresValue())
    {
      if(parser.usingEquals)
      {
        if(parser.argValue == null)
          addError(results, "Missing value for option: " + parser.argName);
        setValue(results, option.fullName, parser.argValue, option.multi);
      }
      else
      {
        final String nextArg = args.isEmpty() ? null : args.getFirst();
        if(nextArg == null || isOption(nextArg))
          addError(results, "Missing value for option: " + parser.argName);
        setValue(results, option.fullName, nextArg, option.multi);
        pop(args);
      }
    }
    else
      results.put(option.fullName, "on");
  }

  private boolean isOption(String arg)
  {
    return arg.startsWith("-");
  }

  public static String tabularize(String[] col1, String[] col2)
  {
    int maxLength = 0;
    for(String s : col1)
    {
      if(s.length() > maxLength)
        maxLength = s.length();
    }

    StringBuffer buffer = new StringBuffer();
    for(int i = 0; i < col1.length; i++)
    {
      buffer.append("  ").append(col1[i]);
      final int remainingSpaces = maxLength - col1[i].length();
      appendSpaces(buffer, remainingSpaces + 2);
      LinkedList<String> lines = splitIntoLines(col2[i]);
      buffer.append(pop(lines));
      buffer.append(System.getProperty("line.separator"));
      while(!lines.isEmpty())
      {
        appendSpaces(buffer, maxLength + 4);
        buffer.append(pop(lines));
        buffer.append(System.getProperty("line.separator"));
      }
    }
    return buffer.toString();
  }

  private static Pattern newlineRegex = Pattern.compile("\\r\\n|\\n", Pattern.MULTILINE);

  private static LinkedList<String> splitIntoLines(String value)
  {
    final String[] lines = newlineRegex.split(value);
    LinkedList<String> measuredLines = new LinkedList<String>();
    for(String line : lines)
    {
      while(line.length() > MAX_ROW_LENGTH)
      {
        int splitIndex = findIndexOfSpaceBefore(MAX_ROW_LENGTH, line);
        measuredLines.add(line.substring(0, splitIndex));
        line = line.substring(splitIndex + 1);
      }
      measuredLines.add(line);
    }
    return measuredLines;
  }

  private static int findIndexOfSpaceBefore(int end, String line)
  {
    for(int i = end; i > 0; i--)
    {
      if(line.charAt(i) == ' ')
        return i;
    }
    return MAX_ROW_LENGTH;
  }

  private static void appendSpaces(StringBuffer buffer, int spaces)
  {
    for(int j = 0; j < spaces; j++)
      buffer.append(" ");
  }

  public static class Parameter
  {
    private String name;
    private boolean required;
    private String description;
    private boolean multi;

    public Parameter(String name, String description, boolean required, boolean multi)
    {
      this.name = name;
      this.description = description;
      this.required = required;
      this.multi = multi;
    }
  }

  public static class Option
  {
    private String shortName;
    private String fullName;
    private String valueDescription;
    private String description;
    private String head;
    private boolean multi;

    public Option(String shortName, String fullName, String valueDescription, String description, boolean multi)
    {
      this.shortName = shortName;
      this.fullName = fullName;
      this.valueDescription = valueDescription;
      this.description = description;
      this.multi = multi;
    }

    public boolean requiresValue()
    {
      return valueDescription != null;
    }

    private String head()
    {
      if(head == null)
      {
        head = "-" + shortName + ", --" + fullName;
        if(requiresValue())
          head += "=<" + valueDescription + ">";
      }
      return head;
    }
  }

  private static class OptionParser
  {
    private String argName;
    private String argValue;
    private boolean usingFullName;
    private boolean usingEquals;

    public OptionParser(String arg)
    {
      stripDashes(arg);

      if(usingFullName)
      {
        int valueIndex = argName.indexOf("=");
        if(valueIndex > 0)
        {
          usingEquals = true;
          argValue = argName.substring(valueIndex + 1);
          argName = argName.substring(0, valueIndex);
        }
      }
    }

    private void stripDashes(String arg)
    {
      if(arg.startsWith("--"))
      {
        usingFullName = true;
        argName = arg.substring(2);
      }
      else
      {
        usingFullName = false;
        argName = arg.substring(1);
      }
    }
  }
}
