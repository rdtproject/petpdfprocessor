package pl.webcache.petnameddestinations;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfNumber;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

public class PetPdfProcessor {

	private static final Logger logger = LogManager.getLogger(PetPdfProcessor.class);

	public static void main(String[] args)
			throws FileNotFoundException, IOException, ParserConfigurationException, TransformerException {
		new PetPdfProcessor().addNamedDestinationsToPdfDocument("/home/rdt/eclipse-pdf/odzwierzatdobogow.pdf",
				"/home/rdt/eclipse-pdf/odzwierzatdobogow_proc.pdf", "/home/rdt/eclipse-pdf/odzwierzatdobogow_log.xml");
	}

	private void addNamedDestinationsToPdfDocument(String src, String dst, String report)
			throws FileNotFoundException, IOException, ParserConfigurationException, TransformerException {
		PdfDocument pdfDoc = new PdfDocument(new PdfReader(src), new PdfWriter(dst));
		for (int pgNr = 1; pgNr < pdfDoc.getNumberOfPages(); pgNr++) {
			String textOnPage = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(pgNr));
			String destinationName = Pattern.getDestinationNameIfApplicable(textOnPage);

			if (destinationName != null) {
				addNamedDestinationToLeftTopOfPage(pdfDoc, pgNr, destinationName);
			}
		}

		pdfDoc.close();
		createXml(dst, report);
	}

	private void addNamedDestinationToLeftTopOfPage(PdfDocument pdfDoc, int pageNr, String namedDestination) {
		PdfArray array = new PdfArray();
		array.add(pdfDoc.getPage(pageNr).getPdfObject());
		array.add(PdfName.XYZ);
		array.add(new PdfNumber(pdfDoc.getPage(pageNr).getPageSize().getLeft()));
		array.add(new PdfNumber(pdfDoc.getPage(pageNr).getPageSize().getTop()));
		array.add(new PdfNumber(1));

		pdfDoc.addNamedDestination(namedDestination, array);
		logger.error(String.format("added named destination %s on page %s", namedDestination, pageNr));
	}

	public void createXml(String src, String dest)
			throws IOException, ParserConfigurationException, TransformerException {
		PdfDocument pdfDoc = new PdfDocument(new PdfReader(src));

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = docFactory.newDocumentBuilder();

		org.w3c.dom.Document doc = db.newDocument();
		Element root = doc.createElement("Destination");
		doc.appendChild(root);

		Map<String, PdfObject> names = pdfDoc.getCatalog().getNameTree(PdfName.Dests).getNames();
		for (Map.Entry<String, PdfObject> name : names.entrySet()) {
			Element el = doc.createElement("Name");
			el.setAttribute("Page", name.getValue().toString());
			el.setTextContent(name.getKey());
			root.appendChild(el);
		}

		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		t.setOutputProperty("encoding", "ISO8859-1");

		t.transform(new DOMSource(doc), new StreamResult(dest));
		pdfDoc.close();
	}
}

enum Pattern {
	MIESO("Mieso", "komuś", "połeć", "mięsa"),
	WOJNY("Wojny", "instrumentarium", "znajdowały", "wojny");

	private String destinationName;
	private String[] phrases;

	private Pattern(String destinationName, String... phrases) {
		this.destinationName = destinationName;
		this.phrases = phrases;
	}

	public static String getDestinationNameIfApplicable(String text) {
		// skip table of content
		if (text.contains("..........")) {
			return null;
		}
		// traverse text
		for (Pattern pattern : Pattern.values()) {
			if (matches(text, pattern.phrases)) {
				return pattern.destinationName;
			}
		}
		return null;
	}

	private static boolean matches(String text, String[] phrases) {
		for (String phrase : phrases) {
			if (!text.contains(phrase)) {
				return false;
			}
		}
		return true;
	}

}
