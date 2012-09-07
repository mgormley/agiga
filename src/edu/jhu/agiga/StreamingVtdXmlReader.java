package edu.jhu.agiga;

import static edu.jhu.agiga.AgigaSentenceReader.require;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

import com.ximpleware.VTDException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;

/**
 * StreamingVtdXmlReader is an abstract class that enables efficient reading of
 * large Annotated Gigaword files by extracting snippets of XML containing only
 * a single document and passing that XML to an appropriate object iterator such
 * as AgigaDocumentReader or AgigaSentenceReader.
 * 
 * This implementation using VTD-XML should handle XML files up to 2GB in size.
 * For larger files, we can switch to extended VTD-XML as described here:
 * <url>http://vtd-xml.sourceforge.net/codeSample/cs12.html</url>
 * 
 * @author mgormley
 * 
 */
public abstract class StreamingVtdXmlReader<T> implements Iterable<T>, Iterator<T> {

    private static Logger log = Logger.getLogger(StreamingVtdXmlReader.class);

    private String fileId;
    private boolean hasNext;
    private int numSents;
    private int numDocs;
    private BufferedReader reader;
    private Iterator<T> vtdReader;
        
    public StreamingVtdXmlReader(String inputFile) {
        try {
            InputStream inputStream = new FileInputStream(inputFile);
            numSents = 0;
            numDocs = 0;
            if (inputFile.endsWith(".gz")) {
                inputStream = new GZIPInputStream(inputStream);
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
            fileId = getFileId(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (VTDException e) {
            throw new RuntimeException(e);
        }
    }

    private String getFileId(BufferedReader reader) throws IOException, VTDException {
        reader.mark(1024);
        String str = reader.readLine();
        str += "</FILE>";
        reader.reset();
        
        byte[] b = str.getBytes("UTF-8");
        VTDGen vg = new VTDGen();
        vg.setDoc(b);
        vg.parse(false);
        VTDNav vn = vg.getNav();
        require(vn.toElement(VTDNav.ROOT));
        String fileId = vn.toString(vn.getAttrVal(AgigaConstants.FILE_ID));
        
        return fileId;
    }

    private boolean nextDoc() {
        try {
    
        StringBuilder sb = new StringBuilder();
        String line;
        boolean isBuilding = false;
        while ((line = reader.readLine()) != null) {
            if (isBuilding) {
                sb.append(line);
                sb.append("\n");
                if (line.startsWith("</DOC")) {
                    isBuilding = false;

                    // Convert the StringBuilder to bytes
                    String str = sb.toString();
                    sb = new StringBuilder();
                    byte[] b = str.getBytes("UTF-8");

                    // Parse the bytes
                    vtdReader = getIteratorInstance(b);
                    numDocs++;
                    return true;
                }
            } else if (line.startsWith("<DOC")) {
                // Case: !isBuilding && line.startsWith("<DOC")
                sb.append(line);
                sb.append("\n");
                isBuilding = true;
            }
        }
        return false;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract Iterator<T> getIteratorInstance(byte[] b); 

    protected abstract int getNumSents(T item);

    @Override
    public Iterator<T> iterator() {
        return this;
    }
    
    @Override
    public boolean hasNext() {
        while (vtdReader == null || !vtdReader.hasNext()) {
            if (!nextDoc()) {
                return false;
            }
        }
        return vtdReader != null && vtdReader.hasNext();
    }

    @Override
    public T next() {
        hasNext();
        T item = vtdReader != null ? vtdReader.next() : null;
        if (item != null) {
            numSents += getNumSents(item);
        }
        return item;
    }

    @Override
    public void remove() {
        throw new RuntimeException("not implemented");        
    }

    public int getNumDocs() {
        return numDocs;
    }

    public int getNumSents() {
        return numSents;
    }
    
    public String getFileId() {
        return fileId;
    }
    
}
