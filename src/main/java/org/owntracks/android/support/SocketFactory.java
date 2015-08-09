package org.owntracks.android.support;

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

    public SocketFactory(boolean sideloadCa, boolean sideloadClientP12) throws CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException, KeyManagementException, java.security.cert.CertificateException {
        Log.v(this.toString(), "initializing CustomSocketFactory");

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");


        if(sideloadCa) {
            Log.v(this.toString(), "CA sideload: true");

            KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            caKeyStore.load(null, null);

            CertificateFactory caCF = CertificateFactory.getInstance("X.509");
            InputStream caInput = new BufferedInputStream(new FileInputStream(Preferences.getTlsCaCrtPath()));
            Log.v(this.toString(), "Using custom tls cert from : " + Preferences.getTlsCaCrtPath());
            java.security.cert.Certificate ca;
            try {
                ca = caCF.generateCertificate(caInput);
                caKeyStore.setCertificateEntry("owntracks-custom-tls-root", ca);
                tmf.init(caKeyStore);

            } catch (Exception e) {
                Log.e(this.toString(), e.toString());
            } finally {
                caInput.close();
            }

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

        if (sideloadClientP12) {
            Log.v(this.toString(), "Client .p12 sideload: true");

            // load client cert
            KeyStore clientKeyStore = null;
            clientKeyStore = KeyStore.getInstance("PKCS12");

            InputStream clientIn = new BufferedInputStream(new FileInputStream(Preferences.getTlsClientCrtPath()));
            Log.v(this.toString(), "Using custom tls client cert from : " + Preferences.getTlsClientCrtPath());
            clientKeyStore.load(clientIn, "".toCharArray());
            try {
                kmf.init(clientKeyStore, Preferences.getTlsClientCrtPassword().toCharArray());
            } catch (UnrecoverableKeyException e) {
                Log.e(this.toString(), e.toString());
            } finally {
                clientIn.close();
            }

            Log.v(this.toString(), "Client .p12 Keystore content: ");
            Enumeration<String> aliasesClientCert = clientKeyStore.aliases();
            for (; aliasesClientCert.hasMoreElements(); ) {
                String o = aliasesClientCert.nextElement();
                Log.v(this.toString(), "Alias: " + o);
            }
        } else {
            Log.v(this.toString(), "Client .p12 sideload: false, using null client cert");
            try {
                kmf.init(null,null);
            } catch (UnrecoverableKeyException e) {
                Log.e(this.toString(), e.toString());
            }
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
