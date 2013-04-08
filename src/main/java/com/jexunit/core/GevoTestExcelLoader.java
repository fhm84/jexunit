/**
 * 
 */
package com.jexunit.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Utility class for reading the excel file into the internal data representation.
 * 
 * @author fabian
 * 
 */
public class GevoTestExcelLoader {

	private static final String COMMAND = "command";
	public static final String DISABLED = "disabled";
	public static final String REPORT = "report";

	// Utility class, only static access
	private GevoTestExcelLoader() {
	}

	/**
	 * Load the excel-file and prepare the data (TestCommands). Each worksheet will run as separated
	 * Unit-Test.
	 * 
	 * @param excelFile
	 *            the name of the excel file (to be loaded). It has to be the filename incl. path to
	 *            be loaded (for example: src/test/resources/myExcelFile.xls)
	 * @return a list of the parsed {@link GevoTestCase}s
	 * @throws Exception
	 */
	public static Collection<Object[]> loadTestData(String excelFile) throws Exception {
		Map<String, List<GevoTestCase>> tests = readExcel(excelFile);

		Collection<Object[]> col = new ArrayList<Object[]>();
		for (Entry<String, List<GevoTestCase>> e : tests.entrySet()) {
			col.add(new Object[] { e.getValue() });
		}

		return col;
	}

	/**
	 * Read the excel-sheet and generate the GevoTestCases. Each worksheet will become its own list
	 * of GevoTestCases. So each worksheet will run as separated testrun.
	 * 
	 * @return a map with the excel worksheet name as key and the list of {@link GevoTestCase}s as
	 *         value
	 * @throws Exception
	 */
	public static Map<String, List<GevoTestCase>> readExcel(String excelFilePath) throws Exception {
		Map<String, List<GevoTestCase>> tests = new LinkedHashMap<String, List<GevoTestCase>>();

		int i = 0;
		int j = 0;
		String sheet = null;
		try (OPCPackage pkg = OPCPackage.open(excelFilePath);) {
			XSSFWorkbook workbook = new XSSFWorkbook(pkg);
			// iterate over the worksheets
			for (XSSFSheet worksheet : workbook) {
				sheet = worksheet.getSheetName();
				List<GevoTestCase> testCases = new ArrayList<GevoTestCase>();

				List<String> commandHeaders = null;

				// iterate over the rows
				for (i = 0; i <= worksheet.getLastRowNum(); i++) {
					XSSFRow row = worksheet.getRow(i);

					if (row != null) {
						String cellValue = cellValues2String(row.getCell(0));
						if (COMMAND.equalsIgnoreCase(cellValue)) {
							commandHeaders = new ArrayList<String>();

							// iterate over the columns
							for (int h = 0; h < row.getLastCellNum(); h++) {
								commandHeaders.add(row.getCell(h).getStringCellValue());
							}
						} else if (DISABLED.equalsIgnoreCase(cellValue)) {
							GevoTestCase testCase = new GevoTestCase();

							// the first column is always the command
							testCase.setTestCommand(cellValue);
							testCase.setSheet(worksheet.getSheetName());
							testCase.setRow(row.getRowNum() + 1);

							if (row.getLastCellNum() >= 1) {
								GevoTestCell testCell = new GevoTestCell();
								testCell.setvalue(cellValues2String(row.getCell(1)));
								testCell.setColumn(row.getCell(1).getColumnIndex() + 1);
								testCase.getValues().put(DISABLED, testCell);
							}
							testCases.add(testCase);
						} else if (cellValue == null || cellValue.isEmpty()) {
							// if the first column is empty, this is a comment line and will be
							// ignored
							continue;
						} else if (commandHeaders != null || REPORT.equalsIgnoreCase(cellValue)) {
							GevoTestCase testCase = new GevoTestCase();

							// the first column is always the command
							testCase.setTestCommand(cellValue);
							testCase.setSheet(worksheet.getSheetName());
							testCase.setRow(row.getRowNum() + 1);

							for (j = 1; j < row.getLastCellNum(); j++) {
								GevoTestCell testCell = new GevoTestCell();
								testCell.setvalue(cellValues2String(row.getCell(j)));
								testCell.setColumn(row.getCell(j).getColumnIndex() + 1);
								// the "report"-command don't need a header-line
								testCase.getValues()
										.put(commandHeaders != null && commandHeaders.size() > j ? commandHeaders.get(j)
												: "param" + j, testCell);
							}
							testCases.add(testCase);
						}
					}
				}

				tests.put(worksheet.getSheetName(), testCases);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			throw new Exception(String.format(
					"Error while reading the excel-file! - worksheet: %s row: %s column: %s",
					sheet, i + 1, getColumn(j + 1)), e);
		}
		return tests;
	}

	/**
	 * Get the value of the excel-cell as String.
	 * 
	 * @param cell
	 *            cell (excel)
	 * @return the value of the excel-cell as String
	 */
	static String cellValues2String(XSSFCell cell) {
		if (cell == null) {
			return null;
		}
		switch (cell.getCellType()) {
		case XSSFCell.CELL_TYPE_NUMERIC:
			if (HSSFDateUtil.isCellDateFormatted(cell)) {
				// TODO: configure the pattern from outside! (BUT: convention over configuration!)
				SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
				return sdf.format(cell.getDateCellValue());
			} else {
				return String.valueOf(cell.getNumericCellValue());
			}
		case XSSFCell.CELL_TYPE_STRING:
			return cell.getStringCellValue();
		case XSSFCell.CELL_TYPE_FORMULA:
			return cell.getCellFormula();
		case XSSFCell.CELL_TYPE_BLANK:
			return cell.getStringCellValue();
		case XSSFCell.CELL_TYPE_BOOLEAN:
			return String.valueOf(cell.getBooleanCellValue());
		case XSSFCell.CELL_TYPE_ERROR:
			return String.valueOf(cell.getErrorCellValue());
		}
		return null;
	}

	/**
	 * Get the character(s) of the column like it is in excel (A, B, C, ...)
	 * 
	 * @param column
	 *            the column index
	 * @return the name of the column (A, B, C, ...)
	 */
	static String getColumn(int column) {
		column--;
		if (column >= 0 && column < 26)
			return Character.toString((char) ('A' + column));
		else if (column > 25)
			return getColumn(column / 26) + getColumn(column % 26 + 1);
		else
			throw new IllegalArgumentException("Invalid Column #"
					+ Character.toString((char) (column + 1)));
	}
}
