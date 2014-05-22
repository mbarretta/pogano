package barretta.utils

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * The ZipInputStream keeps track of its current entry, and does not close
 * the stream if it is in the middle of an entry instead it closes the entry.
 * This allows the user to iterate over zip entries and pass a
 * SmartZipInputStream to code which expects a generic InputStream and may
 * close it.  If a ZipEntry is currently open, a call to <code>close()</code>
 * will close the entry rather than the stream.  Additionally, this class
 * provides a method for iterating over entries, passing itself to a closure
 * for each entry.
 * @author tnichols
 */
public class GroovyZipInputStream extends ZipInputStream {
    private ZipEntry currentEntry = null

    public GroovyZipInputStream(InputStream ins) {
        super(ins)
    }

    /**
     * This method accepts a closure which it will use to iterate over all
     * zip entries in this stream.  This stream is guaranteed to be closed
     * when this method returns.
     * @param iterator
     * @throws IOException
     */
    public void eachEntry(Closure iterator) throws IOException {
        try {
            while (getNextEntry() != null) {
                iterator.call(this)
            }
        } finally {
            this.close()
        }
    }

    /**
     * This will close a current entry if one is already open,
     * and advance to the next entry.  The {@link #getCurrentEntry() currentEntry}
     * is set to the entry returned.
     */
    @Override
    public ZipEntry getNextEntry() throws IOException {
        if (currentEntry != null) this.closeEntry()
        this.currentEntry = super.getNextEntry()
        return this.currentEntry
    }

    @Override
    public void closeEntry() throws IOException {
        this.currentEntry = null
        super.closeEntry()
    }

    /**
     * Doesn't close the ZipInputStream if the stream is in the middle of a
     * zip entry instead closes the entry.  Calling closeEntry() first or
     * calling close() twice in a row will close the stream.
     */
    @Override
    public void close() throws IOException {
        if (this.currentEntry != null) this.closeEntry()
        else super.close()
    }

    public ZipEntry getCurrentEntry() {
        return this.currentEntry
    }
}