package com.example.holomoticon_v02;

import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import android.os.AsyncTask;

public class TCP_CLIENT extends AsyncTask<String, Void, Void> {

    Socket s;
    DataOutputStream dos;
    PrintWriter pw;


    @Override
    protected Void doInBackground(String... voids)
    {
        String message = voids[0];


        try {
            s = new Socket("192.168.0.8", 5555);
            pw = new PrintWriter(s.getOutputStream());

            for (int i = 0;i<10;i++) {
                pw.write(message);
                pw.flush();
            }
            pw.close();

            s.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }
}

