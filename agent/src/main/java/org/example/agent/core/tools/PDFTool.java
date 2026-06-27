package org.example.agent.core.tools;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.core.io.Resource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

public class PDFTool {
    private final Resource resource;

    public PDFTool(Resource resource) {
        this.resource = resource;
    }

    public String readPDF() throws IOException, TikaException, SAXException {
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        AutoDetectParser parser = new AutoDetectParser();

        try (InputStream inputStream = resource.getInputStream()) {
            parser.parse(inputStream, handler, metadata, context);
        }catch (Exception e) {
            // TODO: handle exception
        }

        return handler.toString();
    }
}
