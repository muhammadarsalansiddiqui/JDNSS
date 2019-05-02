package edu.msudenver.cs.jdnss;

import junit.framework.AssertionFailedError;
import org.apache.logging.log4j.core.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

import static org.mockito.Mockito.*;

public class TCPThreadTest {
    @Test
    public void getLength() throws Exception {
        Socket socket = mock(Socket.class);
        byte b[] = {0, 0};
        ByteArrayInputStream inputstream = new ByteArrayInputStream(b);
        ByteArrayOutputStream outputstream = new ByteArrayOutputStream();

        when(socket.getInputStream()).thenReturn(inputstream);
        when(socket.getOutputStream()).thenReturn(outputstream);

        TCPThread tt = new TCPThread(socket);
        P38.call("openStreams", tt);

        Object o = P38.call("getLength", tt);
        Assert.assertEquals(0, o);
    }
    @Test
    public void getQuery () throws Exception{
        Socket socket = mock(Socket.class);
        byte q[] = {
                /* Length */        0, 30,
                /* ID: */           8, 8,
                /* RD: */           1,
                /* AD: */           0,
                /* Questions: */    0, 1,
                /* Answers: */      0, 0,
                /* Authority: */    0, 0,
                /* Additional: */   0, 0,
                /* www: */          3, 'w', 'w', 'w',
                /* test: */         4, 't', 'e', 's', 't',
                /* com: */          3, 'c', 'o', 'm', 0,
                /* A: */            0, 1,
                /* IN: */           0, 1
        };

        ByteArrayInputStream inputstream = new ByteArrayInputStream(q);
        ByteArrayOutputStream outputstream = new ByteArrayOutputStream();

        when(socket.getInputStream()).thenReturn(inputstream);
        when(socket.getOutputStream()).thenReturn(outputstream);
        byte b[] = {127, 0, 0, 1};
        when(socket.getInetAddress())
                .thenReturn(java.net.InetAddress.getByAddress(b));

        TCPThread tt = new TCPThread(socket);
        P38.call("openStreams", tt);
        Query query = (Query) P38.call("getQuery", tt);
        Assert.assertEquals(query.getHeader().getNumQuestions(), 1);
    }

    @Test
    public void openAndCloseStreams() throws java.lang.Exception {
        Socket socket = mock(Socket.class);

        ByteArrayInputStream inputstream = mock(ByteArrayInputStream.class);
        ByteArrayOutputStream outputstream = mock(ByteArrayOutputStream.class);

        when(socket.getInputStream()).thenReturn(inputstream);
        when(socket.getOutputStream()).thenReturn(outputstream);

        TCPThread tt = new TCPThread(socket);

        P38.call("openStreams", tt);
        verify(socket).getInputStream();
        verify(socket).getOutputStream();

        P38.call("closeStreams", tt);
        verify(inputstream).close();
        verify(outputstream).close();
        verify(socket).close();
    }

    @Test(expected = AssertionError.class)
    public void run() throws java.lang.Exception{
        Logger logger = mock(Logger.class);
        Socket socket = mock(Socket.class);
        TCPThread tt = new TCPThread(socket);


        ByteArrayInputStream inputstream = mock(ByteArrayInputStream.class);
        ByteArrayOutputStream outputstream = mock(ByteArrayOutputStream.class);

        when(socket.getInputStream()).thenReturn(inputstream);
        when(socket.getOutputStream()).thenReturn(outputstream);

        tt.run();
        verify(logger).traceEntry();
        verify(socket.getInputStream());
        verify(outputstream).write(0);
        verify(inputstream).close();
    }

    @Test
    public void runCatch() throws java.lang.Exception {
        Logger logger = mock(Logger.class);
        Socket socket = mock(Socket.class);
        TCPThread tt = new TCPThread(socket);
        Exception ioe = new IOException();

        ByteArrayInputStream inputstream = mock(ByteArrayInputStream.class);
        ByteArrayOutputStream outputstream = mock(ByteArrayOutputStream.class);

        when(socket.getInputStream()).thenReturn(inputstream);
        when(socket.getOutputStream()).thenReturn(outputstream);

        when(socket.getInputStream()).thenThrow( new IOException());
        tt.run();
        verify(logger).catching(ioe);
    }
    @Test
    public void sendResponse() throws Exception{
        Socket socket = mock(Socket.class);

        ByteArrayInputStream inputstream = mock(ByteArrayInputStream.class);
        ByteArrayOutputStream outputstream = mock(ByteArrayOutputStream.class);

        when(socket.getInputStream()).thenReturn(inputstream);
        when(socket.getOutputStream()).thenReturn(outputstream);

        TCPThread tt = new TCPThread(socket);

    }
}