package org.owntracks.android.support;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateFactory;
import java.util.Enumeration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.security.cert.CertificateException;

public class SocketFactory extends javax.net.ssl.SSLSocketFactory{
    private javax.net.ssl.SSLSocketFactory factory;

    public static class SocketFactoryOptions {

        private InputStream caCrtInputStream;
        private InputStream caClientP12InputStream;
        private String caClientP12Password;

        public SocketFactoryOptions withCaInputStream(InputStream stream) {
            this.caCrtInputStream = stream;
            return this;
        }
        public SocketFactoryOptions withClientP12InputStream(InputStream stream) {
            this.caClientP12InputStream = stream;
            return this;
        }
        public SocketFactoryOptions withClientP12Password(String password) {
            this.caClientP12Password = password;
            return this;
        }

        public boolean hasCaCrt() {
            return caCrtInputStream != null;
        }

        public boolean hasClientP12Crt() {
            return caClientP12Password != null;
        }

        public InputStream getCaCrtInputStream() {
            return caCrtInputStream;
        }

        public InputStream getCaClientP12InputStream() {
            return caClientP12InputStream;
        }

        public String getCaClientP12Password() {
            return caClientP12Password;
        }

        public boolean hasClientP12Password() {
            return (caClientP12Password != null) && !caClientP12Password.equals("");
        }
    }
    public SocketFactory() throws CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException, KeyManagementException, java.security.cert.CertificateException, UnrecoverableKeyException {
        this(new SocketFactoryOptions());
    }
    public SocketFactory(SocketFactoryOptions options) throws CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException, KeyManagementException, java.security.cert.CertificateException, UnrecoverableKeyException {
        Log.v(this.toString(), "initializing CustomSocketFactory");

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");


        if(options.hasCaCrt()) {
            Log.v(this.toString(), "options.hasCaCrt(): true");

            KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            caKeyStore.load(null, null);

            CertificateFactory caCF = CertificateFactory.getInstance("X.509");
            java.security.cert.Certificate ca;
                ca = caCF.generateCertificate(options.getCaCrtInputStream());
                caKeyStore.setCertificateEntry("owntracks-custom-tls-root", ca);
                tmf.init(caKeyStore);



            Log.v(this.toString(), "CA Keystore content: ");
            Enumeration<String> aliasesCA = caKeyStore.aliases();

            for (; aliasesCA.hasMoreElements(); ) {
                String o = aliasesCA.nextElement();
                Log.v(this.toString(), "Alias: " + o);
            }
        } else {
            Log.v(this.toString(), "CA sideload: false, using system keystore");
            tmf.init((KeyStore) null);
        }

        if (options.hasClientP12Crt()) {
            Log.v(this.toString(), "options.hasClientP12Crt(): true");

            KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");

            clientKeyStore.load(options.getCaClientP12InputStream(), options.hasClientP12Password() ? options.getCaClientP12Password().toCharArray() : new char[0]);
            kmf.init(clientKeyStore, options.hasClientP12Password() ? options.getCaClientP12Password().toCharArray() : new char[0]);

            Log.v(this.toString(), "Client .p12 Keystore content: ");
            Enumeration<String> aliasesClientCert = clientKeyStore.aliases();
            for (; aliasesClientCert.hasMoreElements(); ) {
                String o = aliasesClientCert.nextElement();
                Log.v(this.toString(), "Alias: " + o);
            }
        } else {
            Log.v(this.toString(), "Client .p12 sideload: false, using null client cert");
            kmf.init(null,null);
        }

        // Create an SSLContext that uses our TrustManager
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        this.factory= context.getSocketFactory();

    }

    @Override
    public String[] getDefaultCipherSuites() {
        return this.factory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return this.factory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException{
        SSLSocket r = (SSLSocket)this.factory.createSocket();
        r.setEnabledProtocols(new String[] {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"});
        return r;
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        SSLSocket r = (SSLSocket)this.factory.createSocket(s, host, port, autoClose);
        r.setEnabledProtocols(new String[] {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"});
        return r;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {

        SSLSocket r = (SSLSocket)this.factory.createSocket(host, port);
        r.setEnabledProtocols(new String[] {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"});
        return r;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        SSLSocket r = (SSLSocket)this.factory.createSocket(host, port, localHost, localPort);
        r.setEnabledProtocols(new String[] {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"});
        return r;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        SSLSocket r = (SSLSocket)this.factory.createSocket(host, port);
        r.setEnabledProtocols(new String[]{"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"});
        return r;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        SSLSocket r = (SSLSocket)this.factory.createSocket(address, port, localAddress,localPort);
        r.setEnabledProtocols(new String[] {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"});
        return r;
    }
}
