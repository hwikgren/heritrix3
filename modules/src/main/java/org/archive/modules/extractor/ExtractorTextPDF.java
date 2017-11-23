/*
 * Copyright 2017 Internet Archive.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.archive.modules.extractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.apache.commons.httpclient.URIException;
import org.archive.io.SinkHandlerLogThread;
import org.archive.modules.CrawlURI;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.FileUtils;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.util.PDFTextStripper;

/** Allows the caller to process a CrawlURI representing a PDF
 *  for the purpose of extracting URIs
 *
 * @author Parker Thompson
 * modified by Heidi Jauhiainen
 * - added call to languageTester
 */
public class ExtractorTextPDF extends ContentExtractor {

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 3L;

    private static final Logger LOGGER =
        Logger.getLogger(ExtractorPDF.class.getName());

    /**
     * The maximum size of PDF files to consider.  PDFs larger than this
     * maximum will not be searched for links.
     */
    {
        setMaxSizeToParse(10*1024*1024L); // 10MB
    }
    public long getMaxSizeToParse() {
        return (Long) kp.get("maxSizeToParse");
    }
    public void setMaxSizeToParse(long threshold) {
        kp.put("maxSizeToParse",threshold);
    }

    public ExtractorTextPDF() {
    }
    
    /**
     * 
     * @param uri
     * @return 
     * modified by Heidi Jauhiainen
     *  - attempt to catch also the files where content type is missing or wrong
     */
    @Override
    protected boolean shouldExtract(CrawlURI uri) {
        long max = getMaxSizeToParse();
        if (uri.getRecorder().getRecordedInput().getSize() > max) {
            return false;
        }

        String ct = uri.getContentType();
        String curi = uri.getURI();
        return (ct != null) && (ct.startsWith("application/pdf") || (ct.startsWith("text/html") && curi.toLowerCase().indexOf(".pdf")!=-1));
    }
    
    /**
     * 
     * @param curi
     * @return 
     * modified by Heidi Jauhiainen
     * - use pdfbox to extract text pdf
     * - call languageTester
     */
    protected boolean innerExtract(CrawlURI curi){
        File tempFile;

        int sn;
	Thread thread = Thread.currentThread();
        if (thread instanceof SinkHandlerLogThread) {
            sn = ((SinkHandlerLogThread)thread).getSerialNumber();
        } else {
            sn = System.identityHashCode(thread);
        }
        try {
            tempFile = File.createTempFile("tt" + sn , "tmp.pdf");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        PDFParser parser;
        ArrayList<String> uris;
        try {
            curi.getRecorder().copyContentBodyTo(tempFile);
            parser = new PDFParser(tempFile.getAbsolutePath());
            uris = parser.extractURIs();
            
            // get the text out of pdf
            PDDocument pd = null;
            String modifiable;
            try {
                pd = PDDocument.load(tempFile);
                PDFTextStripper stripper = new PDFTextStripper();
                modifiable = stripper.getText(pd);
            }
            finally {
                if (pd != null) {
                    pd.close();

                }
            }
            // if response code is 200-299, send for testing
        if (curi.is2XXSuccess()) {
            String uri = curi.getURI();
            LanguageTester tester = new LanguageTester();
            tester.testPDF(curi, modifiable);
        }
            
        } catch (IOException e) {
            curi.getNonFatalFailures().add(e);
            return false;
        } catch (RuntimeException e) {
            // Truncated/corrupt  PDFs may generate ClassCast exceptions, or
            // other problems
            curi.getNonFatalFailures().add(e);
            return false;
        } finally {
            FileUtils.deleteSoonerOrLater(tempFile);
        }
        
        if (uris == null) {
            return true;
        }

        for (String uri: uris) {
            try {
                UURI src = curi.getUURI();
                UURI dest = UURIFactory.getInstance(uri);
                LinkContext lc = LinkContext.NAVLINK_MISC;
                Hop hop = Hop.NAVLINK;
                addOutlink(curi, dest, lc, hop);
            } catch (URIException e1) {
                // There may not be a controller (e.g. If we're being run
                // by the extractor tool).
                logUriError(e1, curi.getUURI(), uri);
            }
        }
        
        numberOfLinksExtracted.addAndGet(uris.size());

        LOGGER.fine(curi+" has "+uris.size()+" links.");
        // Set flag to indicate that link extraction is completed.
        return true;
    }
}
