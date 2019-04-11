package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

class TCPThread implements Runnable {
    private final Socket socket;
    private final Logger logger = JDNSS.logger;
    private InputStream is;
    private OutputStream os;

    /**
     * @param socket the socket to talk to
     */
    TCPThread(final Socket socket) {
        this.socket = socket;
    }

    private void openStreams() throws IOException {
        is = socket.getInputStream();
        os = socket.getOutputStream();
    }

    private void closeStreams() {
        // two tries on the off chance os can be open when is is not
        try {
            is.close();
        } catch (NullPointerException | IOException e) {
            logger.catching(e);
        }

        try {
            os.close();
        } catch (NullPointerException | IOException e) {
            logger.catching(e);
        }

        try {
            socket.close();
        } catch (IOException ioe) {
            logger.catching(ioe);
        }
    }

    private int getLength() throws IOException {
        // in TCP, the first two bytes signify the length of the request
        final byte[] buffer = new byte[2];

        try {
            Assertion.aver(is.read(buffer, 0, 2) == 2);
        } catch (IOException ioe) {
            logger.catching(ioe);
            throw ioe;
        }

        return Utils.addThem(buffer[0], buffer[1]);
    }

    public void run() {
        logger.traceEntry();

        try {
            final byte[] query = new byte[getLength()];
            Assertion.aver(is.read(query) == query.length);

            final Query q = new Query(query);
            q.parseQueries(socket.getInetAddress().toString());

            final Response r = new Response(q, false);
            final byte[] b = r.getBytes();

            final int count = b.length;
            final byte[] buffer = new byte[2];
            buffer[0] = Utils.getByte(count, 2);
            buffer[1] = Utils.getByte(count, 1);

            os.write(Utils.combine(buffer, b));
        } catch (IOException ioe) {
            logger.catching(ioe);
        } finally {
            closeStreams();
        }
    }
}
