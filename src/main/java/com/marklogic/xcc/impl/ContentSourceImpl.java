/*
 * Copyright 2003-2016 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.xcc.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import com.marklogic.io.Base64;
import com.marklogic.io.IOHelper;
import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.UserCredentials;
import com.marklogic.xcc.spi.ConnectionProvider;

public class ContentSourceImpl implements ContentSource {
	
    public static enum AuthType {
        NONE, BASIC, DIGEST, NEGOTIATE
    };
    
    private static final String DEFAULT_LOGGER_NAME = "com.marklogic.xcc";
    private static final String XCC_LOGGING_CONFIG_FILE = "xcc.logging.properties";
    private static final String XCC_CONFIG_FILE = "xcc.properties";
    private static final String SYSTEM_LOGGING_CONFIG_CLASS = "java.util.logging.config.class";
    private static final String SYSTEM_LOGGING_CONFIG_FILE = "java.util.logging.config.file";

    private final ConnectionProvider connectionProvider;
    private final String user;
    private final String password;
    private final String contentBase;
    private boolean authenticationPreemptive = false; 
    private boolean challengeIgnored = false; // for regression testing only
    /**
     * logger is initiated before initializeConfig()
     */
    private Logger logger = newDefaultLogger();

    private AuthType authType = AuthType.NONE;
    private String challenge;

    private static Random random = new Random();

    private static Logger newDefaultLogger() {
        LogManager logManager = LogManager.getLogManager();
        Logger logger = logManager.getLogger(DEFAULT_LOGGER_NAME);

        if (logger != null) {
            return logger;
        }

        if ((System.getProperty(SYSTEM_LOGGING_CONFIG_CLASS) != null)
                || (System.getProperty(SYSTEM_LOGGING_CONFIG_FILE) != null)) {
            // If custom config file or class, don't override anything
            return Logger.getLogger(DEFAULT_LOGGER_NAME);
        }

        return customizedLogger(logManager);
    }

    private void initializeConfig() {
        URL url = getClass().getClassLoader().getResource(XCC_CONFIG_FILE);
        Properties props = System.getProperties();
        if (url != null) {
            try {
                FileInputStream is = new FileInputStream(url.getPath());
                props.load(is);
            } catch (IOException e) {
                logger.log(Level.WARNING,
                    "property file not found:" + url.getPath());
            }
        }
    }
    
    public ContentSourceImpl(ConnectionProvider connectionProvider, String user, String password, String contentBase) {
        this.connectionProvider = connectionProvider;
        this.user = user;
        this.password = password;

        String cbName = contentBase;

        if (cbName != null) {
            cbName = cbName.trim();

            if (cbName.length() == 0) {
                cbName = null;
            }
        }

        this.contentBase = cbName;
        initializeConfig();
    }

    public ConnectionProvider getConnectionProvider() {
		return connectionProvider;
	}

    public Session newSession() {
        return (newSession(user, password));
    }

    public Session newSession(String userName, String password) {
        return (newSession(userName, password, null));
    }

    public Session newSession(String user, String password, String contentBaseArg) {
        String contentBase = (contentBaseArg == null) ? this.contentBase : contentBaseArg;

        return (new SessionImpl(this, connectionProvider, new Credentials(user, password), contentBase));
    }

    public Session newSession(String databaseId) {
        return (newSession(user, password, databaseId));
    }

    public Logger getDefaultLogger() {
        return logger;
    }

    public void setDefaultLogger(Logger logger) {
        this.logger = logger;
    }

    public boolean isAuthenticationPreemptive() {
    	return this.authenticationPreemptive;
    }
    
    public void setAuthenticationPreemptive(boolean value) {
    	this.authenticationPreemptive = value;
    }

    public void setAuthChallenge(String challenge) {
    	synchronized(this) {
    		this.authType = AuthType.valueOf(challenge.split(" ")[0].toUpperCase());
    		this.challenge = challenge;
    	}
    }

    /**
     * For regression testing only; returns whether session to ignore authentication challenges and fail immediately.
     */
    public boolean isChallengeIgnored() {
        return challengeIgnored;
    }

    /**
     * For regression testing only; tells session to ignore authentication challenges and fail immediately.
     */
    public void setChallengeIgnored(boolean challengeIgnored) {
        this.challengeIgnored = challengeIgnored;
    }

    public String getAuthString(String method, String uri, UserCredentials credentials) {
        AuthType authType;
        String challenge;
        synchronized(this) {
            authType = this.authType;
            challenge = this.challenge;
        }
        switch (authType) {
        case BASIC:
            return credentials.toHttpBasicAuth();
        case DIGEST:
            return credentials.toHttpDigestAuth(method, uri, challenge);
        case NEGOTIATE:
            return credentials.toHttpNegotiateAuth(connectionProvider.getHostName(), challenge);
        default:
            return isAuthenticationPreemptive() ? credentials.toHttpBasicAuth() : null;
        }
    }

    @Override
    public String toString() {
        return "user=" + ((user == null) ? "{none}" : user) + ", cb="
                + ((contentBase == null) ? "{none}" : contentBase) + " [provider: " + connectionProvider.toString()
                + "]";
    }

    // -------------------------------------------------------------

    private static Logger customizedLogger(LogManager logManager) {
        Properties props = loadLoggingPropertiesFromResource();
        Logger logger = Logger.getLogger(DEFAULT_LOGGER_NAME);
        List<Handler> handlers = getLoggerHandlers(logger, logManager, props);

        for (Iterator<Handler> it = handlers.iterator(); it.hasNext();) {
            logger.addHandler(it.next());
        }

        boolean useParentHandlers = getUseParentHandlersFlag(logger, logManager, props);

        logger.setUseParentHandlers(useParentHandlers);

        logManager.addLogger(logger);

        return logger;
    }

    private static Properties loadLoggingPropertiesFromResource() {
        Properties props = new Properties();
        URL url = ClassLoader.getSystemResource(XCC_LOGGING_CONFIG_FILE);
        try {
            if (url != null) {
                FileInputStream is = new FileInputStream(url.getPath());
                props.load(is);
                return props;
            }
            // Load properties internally from com.marklogic.xcc package in
            // xcc.jar
            InputStream is = ContentSource.class.getResourceAsStream(XCC_LOGGING_CONFIG_FILE);

            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
        	//property file not found
            Logger logger = Logger.getLogger(DEFAULT_LOGGER_NAME);
            if(logger!=null) {
                logger.warning("property file not found: " + url);
            }
        }
        return props;
    }

    private static List<Handler> getLoggerHandlers(Logger logger, LogManager logManager, Properties props) {
        String propName = logger.getName() + ".handlers";
        String handlerPropVal = getPropertyValue(propName, logManager, props);

        if (handlerPropVal == null) {
            return new ArrayList<Handler>(0);
        }

        String[] handlerClassNames = handlerPropVal.split("\\\\s*,?\\\\s*");
        List<Handler> handlers = new ArrayList<Handler>(handlerClassNames.length);
        Level level = getLoggerLevel(logger, logManager, props);

        if (level != null)
            logger.setLevel(level);

        for (int i = 0; i < handlerClassNames.length; i++) {
            try {
                Class<? extends Handler> handlerClass = Class.forName(handlerClassNames[i]).asSubclass(Handler.class);
                Handler handler = handlerClass.newInstance();
                Formatter formatter = getFormatter(handler, logManager, props);

                handlers.add(handler);
                if (formatter != null)
                    handler.setFormatter(formatter);
                if (level != null)
                    handler.setLevel(level);
            } catch (Exception e) {
                // Do nothing, can't instantiate the handler class
            }
        }

        return handlers;
    }

    private static Formatter getFormatter(Handler handler, LogManager logManager, Properties props) {
        String propName = handler.getClass().getName() + ".formatter";
        String formatterClassName = getPropertyValue(propName, logManager, props);

        try {
            Class<? extends Formatter> clazz = Class.forName(formatterClassName).asSubclass(Formatter.class);
            Constructor<? extends Formatter> cons = null;

            try {
                cons = clazz.getConstructor(new Class[] { Properties.class, LogManager.class });
            } catch (Exception e) {
                // do nothing, may not be our LogFormatter class
            }

            if (cons != null) {
                return cons.newInstance(new Object[] { props, logManager });
            }

            return (Formatter)Class.forName(formatterClassName).newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    private static Level getLoggerLevel(Logger logger, LogManager logManager, Properties props) {
        String propName = logger.getName() + ".level";
        String levelName = getPropertyValue(propName, logManager, props);

        try {
            return Level.parse(levelName);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean getUseParentHandlersFlag(Logger logger, LogManager logManager, Properties props) {
        String propName = logger.getName() + ".useParentHandlers";
        String propValue = getPropertyValue(propName, logManager, props);

        if (propValue == null) {
            return false;
        }

        try {
            return Boolean.valueOf(propValue).booleanValue();
        } catch (Exception e) {
            return false;
        }
    }

    private static String getPropertyValue(String propName, LogManager logManager, Properties props) {
        String propVal = props.getProperty(propName);

        if (propVal != null) {
            return propVal.trim();
        }

        propVal = logManager.getProperty(propName);

        if (propVal != null) {
            return propVal.trim();
        }

        return null;
    }

    // -------------------------------------------------------------

    static class Credentials implements UserCredentials {
        private String user;
        private String password;

        public Credentials(String user, String password) {
            this.user = user;
            this.password = password;
        }

        public String getUserName() {
            return user;
        }

        public String toHttpBasicAuth() {
            if ((user == null) || (password == null)) {
                throw new IllegalStateException("Invalid authentication credentials");
            }
            try {
                return ("basic " + Base64.encodeBytes((user + ":" + password).getBytes("UTF-8"),
                        Base64.DONT_BREAK_LINES));
            } catch (UnsupportedEncodingException e) {
                return ("basic " + Base64.encodeBytes((user + ":" + password).getBytes(), Base64.DONT_BREAK_LINES));
            }
        }

        private static final AtomicLong nonceCounter = new AtomicLong();

        public String toHttpDigestAuth(String method, String uri, String challengeHeader) {

            if ((user == null) || (password == null)) {
                throw new IllegalStateException("Invalid authentication credentials");
            }
            if ((challengeHeader == null) || !challengeHeader.startsWith("Digest "))
                return null;

            String pairs[] = challengeHeader.substring("Digest ".length()).split(", +");

            Map<String, String> params = new HashMap<String, String>();

            for (String pair : pairs) {
                String nv[] = pair.split("=", 2);
                params.put(nv[0].toLowerCase(), nv[1].substring(1, nv[1].length() - 1));
            }

            String realm = params.get("realm");

            String HA1 = digestCalcHA1(user, realm, password);

            String nonce = params.get("nonce");
            String qop = params.get("qop");
            String opaque = params.get("opaque");

            byte[] bytes = new byte[16];

            synchronized (random) {
                random.nextBytes(bytes);
            }

            String cNonce = IOHelper.bytesToHex(bytes);

            String nonceCount = Long.toHexString(nonceCounter.incrementAndGet());

            String response = digestCalcResponse(HA1, nonce, nonceCount, cNonce, qop, method, uri);

            StringBuilder buf = new StringBuilder();

            buf.append("Digest username=\"");
            buf.append(user);
            buf.append("\", realm=\"");
            buf.append(realm);
            buf.append("\", nonce=\"");
            buf.append(nonce);
            buf.append("\", uri=\"");
            buf.append(uri);
            buf.append("\", qop=\"auth\", nc=\"");
            buf.append(nonceCount);
            buf.append("\", cnonce=\"");
            buf.append(cNonce);
            buf.append("\", response=\"");
            buf.append(response);
            buf.append("\", opaque=\"");
            buf.append(opaque);
            buf.append("\"");

            return buf.toString();
        }

        public String toHttpNegotiateAuth(String hostName, String challenge) {

            try {
                GSSManager manager = GSSManager.getInstance();
                Oid krb5Mechanism = new Oid("1.2.840.113554.1.2.2");
                Oid krb5PrincipalNameType = new Oid("1.2.840.113554.1.2.2.1");
                GSSName serverName = manager.createName("HTTP/" + hostName,
                                                        krb5PrincipalNameType);
                GSSCredential userCreds = manager.createCredential(GSSCredential.INITIATE_ONLY);
                GSSContext context = manager.createContext(serverName,
                                                           krb5Mechanism,
                                                           userCreds,
                                                           GSSContext.DEFAULT_LIFETIME);
                byte []inToken = new byte[0];
                String parts[] = challenge.split(" ");
                if (parts.length > 1) {
                  inToken = Base64.decode(parts[1]);
                }
                byte[] outToken = context.initSecContext(inToken, 0, inToken.length);

                String str = "Negotiate " + Base64.encodeBytes(outToken,Base64.DONT_BREAK_LINES);
                return str;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return "user=" + user;
        }
    }

    public static String digestCalcResponse(String HA1, String nonce, String nonceCount, String cNonce, String qop,
            String method, String uri) {

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            StringBuilder plaintext = new StringBuilder();

            plaintext.append(method);
            plaintext.append(":");
            plaintext.append(uri);

            digest.update(plaintext.toString().getBytes(), 0, plaintext.length());

            String HA2 = IOHelper.bytesToHex(digest.digest());

            plaintext.setLength(0);
            plaintext.append(HA1);
            plaintext.append(":");
            plaintext.append(nonce);
            plaintext.append(":");
            if (qop != null) {
                plaintext.append(nonceCount);
                plaintext.append(":");
                plaintext.append(cNonce);
                plaintext.append(":");
                plaintext.append(qop);
                plaintext.append(":");
            }
            plaintext.append(HA2);

            digest.update(plaintext.toString().getBytes(), 0, plaintext.length());

            return IOHelper.bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // this really shouldn't happen
            throw new RuntimeException(e);
        }
    }

    public static String digestCalcHA1(String userName, String realm, String password) {

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            StringBuilder plaintext = new StringBuilder();

            plaintext.append(userName);
            plaintext.append(":");
            plaintext.append(realm);
            plaintext.append(":");
            plaintext.append(password);

            digest.update(plaintext.toString().getBytes(), 0, plaintext.length());

            return IOHelper.bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // this really shouldn't happen
            throw new RuntimeException(e);
        }
    }
}
