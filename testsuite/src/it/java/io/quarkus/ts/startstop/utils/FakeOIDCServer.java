/*
 * Copyright (c) 2020 Contributors to the Quarkus StartStop project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.quarkus.ts.startstop.utils;

import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Michal Karm Babacek <karm@redhat.com>
 */
public class FakeOIDCServer implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(FakeOIDCServer.class.getName());
    private final ServerSocket server;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public FakeOIDCServer(int bindPort, String bindAddress) throws Exception {
        server = new ServerSocket(bindPort, 10, InetAddress.getByName(bindAddress));
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            while (running.get()) {
                Socket s = server.accept();
                s.setSoTimeout(500);
                BufferedReader i = new BufferedReader(new InputStreamReader(s.getInputStream()));
                DataOutputStream o = new DataOutputStream(s.getOutputStream());
                String l;
                StringBuilder sb = new StringBuilder(256);
                while ((l = i.readLine()) != null && l.length() > 0) {
                    sb.append(l);
                    sb.append('\n');
                }
                LOGGER.info("Quarkus OICD extension said: " + sb.toString());
                o.writeBytes("HTTP/1.1 200 OK\r\n" +
                        "Connection: keep-alive\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: 52\r\n" +
                        "\r\n" +
                        "{\"id_token\":\"fakefakefakefakefake\", \"state\":\"12345\"}");
                o.flush();
            }
        } catch (IOException e) {
            // Silence is golden. We don't care about socket closed etc.
        } finally {
            stop();
        }
    }

    public void stop() {
        running.set(false);
        synchronized (this) {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    // Silence is golden.
                }
            }
        }
    }
}
