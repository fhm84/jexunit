package com.jexunit.core.model;

import java.util.*;

/**
 * This is the "entity" for a single test instruction. A test case consists of a command (for the API) and a number of
 * values for this command. Additionally there is some metadata that can be defined and filled by the data provider
 * implementation for more information on debugging and test failures.
 *
 * @author fabian
 */
public class TestCase<T extends Metadata> {

    /**
     * The "name" of the test command.
     */
    private String testCommand;
    /**
     * Metadata for additional information about the test instruction (like test group, identifier, and so on).
     */
    private T metadata;
    /**
     * The values for the test instruction (the test command).
     */
    private List<Map<String, TestCell>> values = new ArrayList<>(Collections.singletonList(new LinkedHashMap<>()));

    private int counter = 0;
    /**
     * Optional comment for the test case (out of the data file).
     */
    private String comment;

    private boolean disabled = false;
    private boolean exceptionExpected = false;
    private Boolean fastFail = null;
    private Boolean multiline = null;
    private boolean breakpointEnabled = false;

    public TestCase() {
    }

    public TestCase(final T metadata) {
        this.metadata = metadata;
    }

    /**
     * Get the test-command for the test-case.
     *
     * @return the test-command
     * @see #testCommand
     */
    public String getTestCommand() {
        return testCommand;
    }

    public void setTestCommand(final String testCommand) {
        this.testCommand = testCommand;
    }

    /**
     * Get the metadata to the test-case. This will include the test-group, the identifier (inside the test-group) and
     * possibly other things.
     *
     * @return the metadata of the test-case
     * @see #metadata
     */
    public T getMetadata() {
        return metadata;
    }

    public void setMetadata(final T metadata) {
        this.metadata = metadata;
    }

    /**
     * Get the values (found/read from the excel file) for the test-case.
     *
     * @return the values for the test-case
     * @see TestCase#values
     */
    public Map<String, TestCell> getValues() {
        return values.get(counter);
    }

    public void setValues(final Map<String, TestCell> values) {
        this.values.set(counter, values);
    }

    /**
     * It's possible comment the test-command/-case in the data-file (excel file).
     *
     * @return the comment out of the data-file
     * @see #comment
     */
    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    /**
     * Flag for disabling the test-case.
     *
     * @return true, if the test-case should be disabled, else false (default)
     */
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(final boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * Flag for expecting an exception.
     *
     * @return true, if the test-case expects an exception, else false (default)
     */
    public boolean isExceptionExpected() {
        return exceptionExpected;
    }

    public void setExceptionExpected(final boolean exceptionExpected) {
        this.exceptionExpected = exceptionExpected;
    }

    /**
     * Flag for fast failing the complete test sheet on failing test command.
     *
     * @return true, if the test-case fast fails the complete test sheet, else false (default)
     */
    public boolean isFastFail() {
        return fastFail != null && fastFail;
    }

    public Boolean getFastFail() {
        return fastFail;
    }

    public void setFastFail(final boolean fastFail) {
        this.fastFail = fastFail;
    }

    public boolean isMultiline() {
        return multiline != null && multiline;
    }

    public Boolean getMultiline() {
        return multiline;
    }

    public void setMultiline(final boolean multiline) {
        this.multiline = multiline;
    }

    /**
     * Flag for debugging. If this flag is set to true, you can debug your command using conditional breakpoints.
     *
     * @return true, if breakpoint should be enabled for the test-case, else false (default)
     */
    public boolean isBreakpointEnabled() {
        return breakpointEnabled;
    }

    public void setBreakpointEnabled(final boolean breakpointEnabled) {
        this.breakpointEnabled = breakpointEnabled;
    }

    /**
     * Get the String-representation for the test-case. This will return the test-group (for example the sheet-name),
     * because it's used for structured "description" of the JUnit test results.
     */
    @Override
    public String toString() {
        if (metadata != null) {
            return metadata.getTestGroup();
        }
        return super.toString();
    }

    public void next() {
        counter++;
        values.add(counter, new LinkedHashMap<>());
    }

    public List<Map<String, TestCell>> getMultilineValues(){
        return values;
    }

}
