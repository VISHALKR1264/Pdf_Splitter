package pdfsplit.split;

import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;

public class pdfsplit {
	public static void main(String[] args)
		throws IOException
	{

		File pdffile
			= new File("C:\\pdfsplitter\\1. DBMS - Introduction.pdf");
		PDDocument document = PDDocument.load(pdffile);

		
		Splitter splitting = new Splitter();

		
		List<PDDocument> Page = splitting.split(document);

		
		Iterator<PDDocument> iteration
			= Page.listIterator();

		
		int j = 1;
		while (iteration.hasNext()) {
			PDDocument pd = iteration.next();
			pd.save("C:\\pdfsplitter\\1. DBMS - Introductionss"
					+ j++ + ".pdf");
		}
		
		System.out.println("Splitted Pdf Successfully.");
		document.close();
	}
}
