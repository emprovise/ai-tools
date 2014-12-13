package com.emprovise.nlp.util;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

public class NetUtil {

    private String proxyUser;
    private String proxyPassword;
    private String proxyAddress;
    private int proxyPort;

    public NetUtil() { }

    public NetUtil(String proxyAddress, int proxyPort, String proxyUser, String proxyPassword) {
        this.proxyAddress = proxyAddress;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
    }

    public File downloadFile(URL url, String filename) throws IOException {
        return downloadFile(url, filename, null);
    }

    public File downloadFile(URL url, String filename, String directory) throws IOException {

        File file;

        File dir = new File(directory);

        if((!dir.exists())) {
            dir.mkdirs();
        }

        if(dir != null && dir.exists()) {
            file = new File(directory, filename);
        }
        else {
            file = new File(filename);
        }

        if(file.exists()) {
            return file;
        }

        URLConnection connection = null;

        if(proxyAddress != null && !proxyAddress.isEmpty()) {
            InetSocketAddress sa = new InetSocketAddress(proxyAddress, proxyPort);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, sa);
            connection = url.openConnection(proxy);

            if(proxyUser != null && proxyPassword != null && !proxyUser.isEmpty() && !proxyPassword.isEmpty()) {
                sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
                String encodedUserPwd = encoder.encode((proxyUser + ":" + proxyPassword).getBytes());
                connection.setRequestProperty("Proxy-Authorization", "Basic " + encodedUserPwd);
            }
        }
        else {
            connection = url.openConnection();
        }

        connection.setRequestProperty("Accept-Charset", "UTF-8");

        InputStream in = new BufferedInputStream(connection.getInputStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n = 0;
        while ((n=in.read(buf))!=-1) {
            out.write(buf, 0, n);
        }
        out.close();
        in.close();

        byte[] response = out.toByteArray();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(response);
        fos.close();

        return file;
    }
}
