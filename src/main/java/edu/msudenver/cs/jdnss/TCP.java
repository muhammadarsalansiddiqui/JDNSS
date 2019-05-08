package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class TCP extends Thread {
    private ServerSocket serverSocket;
    private final Logger logger = JDNSS.logger;
    private final int defaultDNSPort = 53;

    TCP() throws IOException {
        try {
            String ipAddress = JDNSS.jargs.getIPaddress();
            int backlog = JDNSS.jargs.getBacklog();
            int port = JDNSS.jargs.getPort();
            String keystoreFileName = JDNSS.jargs.getPKCS12();

            if (ipAddress != null && backlog != 0 && port != 0) {
                if (keystoreFileName != null && !keystoreFileName.isEmpty()) {
                    KeyStore keyStore = keystoreFromPath(keystoreFileName);
                    SSLServerSocketFactory ssf = getSSLServerSocketFactory(keyStore);
                    serverSocket = ssf.createServerSocket(port, backlog, InetAddress.getByName(ipAddress));
                } else {
                    serverSocket = new ServerSocket(port, backlog, InetAddress.getByName(ipAddress));
                }
            } else if (backlog != 0 && port != 0) {
                if (keystoreFileName != null && !keystoreFileName.isEmpty()) {
                    KeyStore keyStore = keystoreFromPath(keystoreFileName);
                    SSLServerSocketFactory ssf = getSSLServerSocketFactory(keyStore);
                    serverSocket = ssf.createServerSocket(port, backlog);
                } else {
                    serverSocket = new ServerSocket(port, backlog);
                }
            } else if (port != 0) {
                if (keystoreFileName != null && !keystoreFileName.isEmpty()) {
                    KeyStore keyStore = keystoreFromPath(keystoreFileName);
                    SSLServerSocketFactory ssf = getSSLServerSocketFactory(keyStore);
                    serverSocket = ssf.createServerSocket(port);
                } else {
                    serverSocket = new ServerSocket(port);
                }
            } else {
                // According to RFC 7858 Section 3.1: Do not do DNS over TLS on port 53
                serverSocket = new ServerSocket(defaultDNSPort);
            }
        } catch (IOException ioe) {
            logger.catching(ioe);
            throw ioe;
        }

        logger.traceExit();
    }

    private SSLServerSocketFactory getSSLServerSocketFactory(KeyStore keyStore) {
        TrustManagerFactory tmf = null;
        try {
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            if (tmf == null) throw new AssertionError();
            tmf.init(keyStore);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        SSLContext context = null;
        try {
            context = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            if (context == null) throw new AssertionError();
            context.init(null, tmf.getTrustManagers(), null);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return context.getServerSocketFactory();
    }

    private KeyStore keystoreFromPath(String fileName) {
        File f = new File(fileName);

        if (!f.exists())
            throw new RuntimeException("Err: File not found.");

        FileInputStream keyFile = null;
        try {
            keyFile = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        KeyStore keystore = null;
        try {
            keystore = KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        char[] password = {};

        try {
            if (keystore == null) throw new AssertionError();
            keystore.load(keyFile, password);
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            e.printStackTrace();
        }

        try {
            if (keyFile == null) throw new AssertionError();
            keyFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return keystore;
    }

    public void run() {
        logger.traceEntry();

        Socket socket;
        int threadPoolSize = JDNSS.jargs.getThreads();
        ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);

        while (true) {
            try {
                socket = serverSocket.accept();
            } catch (IOException ioe) {
                logger.catching(ioe);
                return;
            }

            logger.trace("Received TCP packet");

            Future f = pool.submit(new TCPThread(socket));

            // if we're only supposed to answer once, and we're the first,
            // bring everything down with us.
            if (JDNSS.jargs.isOnce()) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException ie) {
                    logger.catching(ie);
                }

                System.exit(0);
            }
        }
    }
}
