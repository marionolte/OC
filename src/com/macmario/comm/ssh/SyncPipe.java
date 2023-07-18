/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comm.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author SuMario
 */
public class SyncPipe  implements Runnable {
        String CVS_VERSION = "$Revision: 1.1 $ $Id: SyncPipe.java,v 1.1 2008-09-30 03:47:56 sabre Exp $ ";
        private final byte[] buffer_;
        private final OutputStream ostrm_;
        private final InputStream istrm_;
        private boolean closeAfterCopy_ = false;
        public SyncPipe(InputStream istrm, OutputStream ostrm) { this(istrm, ostrm, 4096);}
        public SyncPipe(InputStream istrm, OutputStream ostrm, int bufferSize) {
            if (istrm == null) throw new IllegalArgumentException("'istrm' cannot be null");
            if (ostrm == null) throw new IllegalArgumentException("'ostrm' cannot be null");
            if (bufferSize < 1024) throw new IllegalArgumentException("a buffer size less than 1024 makes little sense");
            istrm_ = istrm;
            ostrm_ = ostrm;
            buffer_ = new byte[bufferSize];
        }
        public void handleException(IOException e) {}
        public SyncPipe setCloseAfterCopy(boolean closeAfterCopy) {
            closeAfterCopy_ = closeAfterCopy;
            return this;
        }
        @Override
        public void run() {
            try {
                for (int bytesRead = 0; (bytesRead = istrm_.read(buffer_)) != -1;)
                    ostrm_.write(buffer_, 0, bytesRead);
                ostrm_.flush();
                if (closeAfterCopy_) ostrm_.close();
            } catch (IOException e) { handleException(e);}
        }
     
}
