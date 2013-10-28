import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

public class ProtocolFiller {

    private static String getString(Cell idCell) {
    	if (idCell == null)
    		return "";
        if (idCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            idCell.setCellType(Cell.CELL_TYPE_STRING);
        }
        return idCell.getStringCellValue();
    }

    /**
     * Loads the grades from Excel assuming student id in row A and grade in row B in sheet number 1. Returns a map from student ids to grades
     */
    public Map<String, String> loadGradesFromExcel(String excelFileName) throws IOException, InvalidFormatException {
        Map<String, String> grades = new HashMap<>();
        Workbook workbook = WorkbookFactory.create(new FileInputStream(excelFileName));
        Sheet sheetAt = workbook.getSheetAt(0);
        for (Row r : sheetAt) {
            String id = getString(r.getCell(0));
            String grade = getString(r.getCell(1));
            grades.put(id, grade);
            System.out.println("Reading student ID " + id + ", grade " + grade);
        }
        return grades;
    }

    public static void main(String[] args) throws Exception {
    	if (args.length < 2 || args.length > 4) {
    		System.out.println("Usage: java -jar protocolfiller.jar <protocol> <grades> [ <output> ] [ <align> ]\n" +
    				"where\n" +
    				"  <protocol> is the protocol PDF file\n" +
    				"  <grades>   is the Excel file containing the student IDs and grades\n" +
    				"             (in the first two columns, respectively, of the first sheet)\n" +
    				"  <output>   is the output PDF file (default: out.pdf)\n" +
    				"  <align>    px horizontal alignment of grades (default: 0)");
    		System.exit(0);
    	}
        ProtocolFiller filler = new ProtocolFiller();
        Map<String, String> grades = filler.loadGradesFromExcel(args[1]);
        String outputFilename = "out.pdf";
        if (args.length > 2) {
            outputFilename = args[2];
        }
        int align = 0;
        if (args.length > 3) { 
        	align = Integer.parseInt(args[3]);
        }
        Set<String> graded = filler.fillGrades(args[0], grades, align, outputFilename);

        HashSet<String> missing = new HashSet<>(grades.keySet());
        missing.removeAll(graded);
        if (!missing.isEmpty()) {
            System.out.println("Missing student IDs in protocol: " + missing);
        }
    }

    /**
     * Fills the PDF with the grades given in the map.
     *
     * @param grades A map from student ids to grades
     * @return A set of student ids for which a grade has been inserted
     */
    public Set<String> fillGrades(String pdfFileName, final Map<String, String> grades, final int align, String outputFilename) throws IOException, DocumentException {
        final Set<String> graded = new HashSet<>();
        PdfReader reader = new PdfReader(pdfFileName);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        FileOutputStream outputStream = new FileOutputStream(outputFilename);
        //Create an overlay on the existing PDF
        final PdfStamper writer = new PdfStamper(reader, outputStream);

        // Load existing PDF and run through the text constructs in it.
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {

            final int finalI = i;
            parser.processContent(i, new TextExtractionStrategy() {
                @Override
                public String getResultantText() {
                    return "";
                }

                @Override
                public void beginTextBlock() {
                }

                @Override
                public void renderText(TextRenderInfo textRenderInfo) {
                    String text = textRenderInfo.getText();
                    String studentId = text.trim();
                    String grade = null;
                    if (grades.containsKey(studentId)) {
                        grade = grades.get(studentId);
                    } else if (studentId.matches("\\d{8,9}") || studentId.matches("[a-zA-Z]{2}\\d{5}")) {
                        grade = "";  //Inserts "" for students where no grade is available
                        System.out.println("Student " + studentId + " did not participate in the exam");
                    }

                    if (grade != null) {
                        graded.add(studentId);
                        //Overlay the page with a layer that holds the grade
                        PdfContentByte overContent = writer.getOverContent(finalI);
                        overContent.beginText();
                        BaseFont bf = null;
                        try {
                            bf = BaseFont.createFont(BaseFont.HELVETICA,
                                    BaseFont.WINANSI, BaseFont.EMBEDDED);
                        } catch (DocumentException | IOException e) {
                            e.printStackTrace();
                        }
                        overContent.setFontAndSize(bf, 14);
                        //Insert the grade 4700+align px to the right of the student id. Right align the text...
                        overContent.showTextAligned(PdfContentByte.ALIGN_RIGHT, grade, textRenderInfo.getBaseline().getStartPoint().get(0) + 470 + align, textRenderInfo.getBaseline().getEndPoint().get(1), 0);
                        overContent.endText();
                        //System.out.println("Writing student ID " + studentId + ", grade " + grade);
                    }
                }

                @Override
                public void endTextBlock() {
                }

                @Override
                public void renderImage(ImageRenderInfo imageRenderInfo) {
                }
            });
        }

        writer.close();
        outputStream.close();
        return graded;
    }
}