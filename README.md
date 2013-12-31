jexunit
=======

> a Tool for defining JUnit-Tests in Excel Worksheets

**JExUnit** is a framework for **"command based testing"** based on the JUnit framework.
It provides simple mechanisms to define your own commands. The test-data will be defined in Excel-files where each sheet or optionally each "*command*" (row) will be executed as a test-case (in JUnit).


## First steps ##
To use the JExUnit framework you have to do only three steps:

1. Create a test class annotated with
    
        @RunWith(JExUnit.class)

2. Add an attribute of type String, String[] or List<String> (or a static method returning one of these types) representing the excel-file(s) to "execute", annotated with

        @ExcelFile

3. Define/Implement your commands. A command will be a "simple" Method annotated with

        @TestCommand


Now you can write your tests in excel-sheets. If you run your test class as JUnit-Test, the JExUnit-framework will automatically load the excel-file(s), "find" and run the commands (in the same order as they are defined in the excel-file(s)).

###### Simple example ######

    @RunWith(JExUnit.class)
    public class ArithmeticalTest {

        private static Logger log = Logger.getLogger(ArithmeticalTest.class.getName());

	    @ExcelFile
	    static String[] excelFiles = new String[] { "src/test/resources/ArithmeticalTests.xlsx",
			"src/test/resources/ArithmeticalTests2.xlsx" };

	    @TestCommand(value = "mul")
	    public static void runMulCommand(TestCase testCase, ArithmeticalTestObject testObject) throws Exception {
		    log.log(Level.INFO, "in test command: MUL!");
	 	    assertThat(testObject.getParam1() * testObject.getParam2(), equalTo(testObject.getResult()));
	    }

	    @TestCommand(value = "div")
	    public static void runDivCommand(TestCase testCase, ArithmeticalTestObject testObject) throws Exception {
		    log.log(Level.INFO, "in test command: DIV!");
		    assertThat(testObject.getParam1() / testObject.getParam2(), equalTo(testObject.getResult()));
	    }
    }

---
#### @ExcelFile
  The _@ExcelFile_-Annotation is used to define the excel-file(s) to run as test(s). This can be defined by a static attribute or a static method (without parameters). The return type of the method (and the type of the attribute) has to be _String_, _String[]_ or _List&lt;String>_.

  Per default the JExUnit-framework will run one excel-sheet as a single test (like one test case in JUnit). If you are going to run each command in the file as single test (like multiple test cases/methods in JUnit), there is a flag available for the _@ExcelFile_-Annotation: _worksheetAsTest_. If this is set to false (default value is true), the framework will run each command as a single test!

      @ExcelFile(worksheetAsTest = false)
	  public static String[] getExcelFiles() {
		  String[] excelFiles = new String[] { "src/test/resources/MassTests.xlsx",
				"src/test/resources/MassTests2.xlsx" };
		  return excelFiles;
	  }


#### @TestCommand
  TODO



## The Excel-File ##
TODO


## The Test-Commands ##
TODO