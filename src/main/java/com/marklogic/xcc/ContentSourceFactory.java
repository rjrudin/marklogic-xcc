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
package com.marklogic.xcc;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.marklogic.xcc.exceptions.XccConfigException;
import com.marklogic.xcc.impl.ContentSourceImpl;
import com.marklogic.xcc.impl.SSLSocketPoolProvider;
import com.marklogic.xcc.impl.SocketPoolProvider;
import com.marklogic.xcc.spi.ConnectionProvider;

/**
 * <p>
 * Static helper class with factory methods to create instances of {@link ContentSource} using
 * explicit connection parameters.
 * </p>
 * 
 * @see ContentSource
 */
public class ContentSourceFactory {
//	private static final String SOCKET_PROVIDER_IMPL_PROPERTY = "com.marklogic.xcc.spi.ConnectionProvider";
//	private static final String DEFAULT_CONNECTION_PROVIDER_CLASS = "com.marklogic.xcc.impl.SocketPoolProvider";

    /**
     * Number of seconds the system sleeps before looking for expired 
     * connections to close.
     */
    static final long GC_INTERVAL = Integer.parseInt(
            System.getProperty("xcc.gcinterval", "10000"));
    private static final String[] knownSchemes = { "xcc", "xccs", "xdbc" };

    private static final String[] secureSchemes = { "xccs" };

    private ContentSourceFactory() {
        // cannot be instantiated
    }

    /**
     * <p>
     * Return a {@link ContentSource} object that will use the provided {@link ConnectionProvider}
     * instance to obtain server connections, with the given default login credentials and
     * contentbase values. Custom connection management policies may be implemented by the
     * {@link ConnectionProvider} object.
     * </p>
     * <p>
     * <strong>NOTE: </strong> This factory method should only be used by advanced users. A
     * misbehaving {@link ConnectionProvider} implementation can result in connection failures and
     * potentially even data loss. Be sure you know what you're doing.
     * </p>
     * 
     * @param connectionProvider
     *            An instance of {@link ConnectionProvider} that will be used to obtain sockets to
     *            connect to the {@link ContentSource} when needed. The client is responsible for
     *            properly initializing this object with the information it needs to make the
     *            appropriate connections.
     * @param user
     *            The default User Name to use for authentication.
     * @param password
     *            The default Password to use for authentication.
     * @param contentbaseName
     *            The contentbase (database) on the {@link ContentSource} to run queries against.
     *            The contentbase numeric id may be supplied instead, if prepended by '#'. Pass null
     *            to use the default configured on the server.
     * @return A {@link ContentSource} instance representing the ContentSource.
     * @see com.marklogic.xcc.ContentSource
     * @see ContentbaseMetaData
     */
    public static ContentSource newContentSource(ConnectionProvider connectionProvider, String user, String password,
            String contentbaseName) {
        return (new ContentSourceImpl(connectionProvider, user, password, contentbaseName));
    }

    // ---------------------------------------------------------------

    /**
     * <p>
     * Return a {@link ContentSource} object that will serve as the source of connections to the
     * server specified by the given URI.
     * </p>
     * <p>
     * The format of the URI is: <code>xcc://user:password@host:port/contentbase</code>
     * </p>
     * <p>
     * For an SSL-enabled connection, the URI format is:
     * <code>xccs://user:password@host:port/contentbase</code>
     * </p>
     * <p>
     * For example: xcc://joe:hush@myserver:8003
     * </p>
     * <p>
     * For example: xccs://joe:hush@myserver:8003/production
     * </p>
     * <p>
     * The contentbase name is optional. If not specified the default database for the XDBC server
     * configuration will be used. To reference a contentbase by numeric id (see
     * {@link ContentbaseMetaData#getContentBaseId()}), prepend it with '#'.
     * </p>
     * <p>
     * For example: xcc://joe:hush@myserver:8003/#84635406972362574
     * </p>
     * <p>
     * The supported connection schemes are currently "xcc" ("xdbc" is an alias) for a non-secure
     * connection and "xccs" for a secure connection, but others may be added in the future.
     * </p>
     * 
     * @param uri
     *            A URI instance which encodes the connection scheme, host, port and optional user
     *            and password.
     * @param options
     *            Security settings to be used for "xccs" secure connections.
     * @return A {@link ContentSource} instance representing the ContentSource.
     * @throws XccConfigException
     *             If there is a configuration problem or the configured {@link ContentSource}
     *             implementation class cannot be instantiated.
     * @see ContentSource
     * @see ContentbaseMetaData
     */
    public static ContentSource newContentSource(URI uri, SecurityOptions options) throws XccConfigException {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        String userInfoStr = uri.getUserInfo();
        String[] userInfo = (userInfoStr == null) ? (new String[0]) : userInfoStr.split(":", 2);
        String contentBase = uri.getPath();

        if (!validScheme(scheme)) {
            throw new XccConfigException("Unrecognized connection scheme: " + scheme);
        }

        if ((!secureScheme(scheme)) && (options != null)) {
            throw new XccConfigException("Non-Secure connection requested but SecurityOptions is non-null");
        }

        if (contentBase != null) {
            if (contentBase.startsWith("/")) {
                contentBase = contentBase.substring(1);
            }

            if (contentBase.length() == 0) {
            	// in the case where a numeric is sent
                contentBase = uri.getFragment(); 
                if (contentBase != null) {
                	contentBase = "#" + contentBase;
                }
            }
        }

        if ((userInfo.length != 2) || (userInfo[0].length() == 0) || (userInfo[1].length() == 0)) {
            return (newContentSource(host, port, null, null, contentBase, options));
        }

        return (newContentSource(host, port, userInfo[0], userInfo[1], contentBase, options));
    }

    /**
     * Equivalent to <code>newContentSource(uri, null)</code>.
     * 
     * @param uri
     *            A URI instance which encodes the connection scheme, host, port and optional user
     *            and password. The format of the URI is:
     *            <code>xcc://user:password@host:port/contentbase</code>
     * @return A {@link ContentSource} instance representing the ContentSource.
     * @throws XccConfigException
     *             If there is a configuration problem or the configured {@link ContentSource}
     *             implementation class cannot be instantiated.
     */
    public static ContentSource newContentSource(URI uri) throws XccConfigException {
        return newContentSource(uri, null);
    }

    // ---------------------------------------------------------------

    /**
     * Return a {@link ContentSource} object that will serve as the source of connections to the
     * server on the given host and port, with login credentials of the given user and password. No
     * connections are made at this time. Note that the {@link ContentSource} instance returned may
     * be shared with other callers or threads. The implementation may choose to pool and re-use
     * {@link ContentSource} objects for a particular host/port/user combination.
     * 
     * @param host
     *            The name or dotted-quad IP address of the server host.
     * @param port
     *            The port on the host to connect to.
     * @param user
     *            The default User Name to use for authentication.
     * @param password
     *            The default Password to use for authentication.
     * @param contentbaseName
     *            The ContentBase (database) on the ContentSource to run queries against. The
     *            contentbase numeric id may be supplied instead, if prepended by '#'. Pass null to
     *            use the default configured on the server.
     * @param options
     *            Security settings to be used for secure connections.
     * @return A {@link ContentSource} instance representing the ContentSource.
     * @see com.marklogic.xcc.ContentSource
     * @see ContentbaseMetaData
     */
    public static ContentSource newContentSource(String host, int port, String user, String password,
            String contentbaseName, SecurityOptions options) {
        return (newContentSource((options == null) ? defaultConnectionProvider(host, port)
                : defaultSecureConnectionProvider(host, port, options), user, password, contentbaseName));
    }

    /**
     * Equivalent to
     * <code>newContentSource (host, port, user, password, contentbaseName, null)</code>
     * 
     * @param host
     *            The name or dotted-quad IP address of the server host.
     * @param port
     *            The port on the host to connect to.
     * @param user
     *            The default User Name to use for authentication.
     * @param password
     *            The default Password to use for authentication.
     * @param contentbaseName
     *            The ContentBase (database) on the ContentSource to run
     * @return A {@link ContentSource} instance representing the ContentSource. the configured
     *         {@link ContentSource} implementation class cannot be instantiated.
     * @see com.marklogic.xcc.ContentSource
     */
    public static ContentSource newContentSource(String host, int port, String user, String password,
            String contentbaseName) {
        return (newContentSource(host, port, user, password, contentbaseName, null));
    }

    /**
     * Equivalent to <code>newContentSource (host, port, user, password, null)</code>
     * 
     * @param host
     *            The name or dotted-quad IP address of the server host.
     * @param port
     *            The port on the host to connect to.
     * @param user
     *            The default User Name to use for authentication.
     * @param password
     *            The default Password to use for authentication.
     * @return A {@link ContentSource} instance representing the ContentSource. the configured
     *         {@link ContentSource} implementation class cannot be instantiated.
     * @see com.marklogic.xcc.ContentSource
     */
    public static ContentSource newContentSource(String host, int port, String user, String password) {
        return (newContentSource(host, port, user, password, null));
    }

    /**
     * Return a ContentSource object that will serve as the source of connections to the server on
     * the given host and port, with no default login credentials. Invoking newSession() on the
     * returned ContentSource object, without providing a user name/password, will throw an
     * IllegalStateException.
     * 
     * @param host
     *            The name or dotted-quad IP address of the server host.
     * @param port
     *            The port on the host to connect to.
     * @return A ContentSource instance representing the ContentSource.
     * @see ContentSource
     */
    public static ContentSource newContentSource(String host, int port) {
        return newContentSource(host, port, null, null, null);
    }

    // ----------------------------------------------------------------

//	private static ContentSource instantiateContentSource (Class clazz,
//		ConnectionProvider socketProvider, String user, String password, String contentBase)
//		throws XDBCConfigException
//	{
//		Class [] paramTypes = { ConnectionProvider.class, String.class, String.class, String.class };
//		Object [] params = { socketProvider, user, password, contentBase };
//		Constructor constructor = null;
//
//		try {
//			constructor = clazz.getConstructor (paramTypes);
//		} catch (NoSuchMethodException e) {
//			throw new XDBCConfigException ("Class '" + clazz.getName()
//				+ "', does not have a four-arg constructor", e);
//		}
//
//		try {
//			return (ContentSource) constructor.newInstance (params);
//		} catch (Exception e) {
//			throw new XDBCConfigException ("Cannot instantiate '" + clazz.getName()
//				+ "': " + e.getMessage (), e);
//		}
//	}

    private static final int STANDARD_PROVIDER_CACHE_SIZE = Integer.getInteger(
            "xcc.connectionprovider.standard.cache.size", 8);
    private static final int SECURE_PROVIDER_CACHE_SIZE = Integer.getInteger(
            "xcc.connectionprovider.secure.cache.size", 8);

    private static final Map<Object, ConnectionProvider> standardProviders = 
            new ConcurrentHashMap<Object, ConnectionProvider>(STANDARD_PROVIDER_CACHE_SIZE);
    private static final Map<Object, ConnectionProvider> secureProviders = 
            new ConcurrentHashMap<Object, ConnectionProvider>(SECURE_PROVIDER_CACHE_SIZE);
    private static final ConnectionCollector gc = new ConnectionCollector();

    static ConnectionProvider defaultConnectionProvider(String host, int port) {
//		try {
//			implClass = findClass (SOCKET_PROVIDER_IMPL_PROPERTY, DEFAULT_SOCKET_PROVIDER_CLASS);
//		} catch (ClassNotFoundException e) {
//			throw new XDBCConfigException ("ConnectionProvider configuration error, cannot load class: " + implName, e);
//		}

        // TODO: Look for property override setting?
//		return (new SocketPoolProvider (new InetSocketAddress (host, port)));

        InetSocketAddress address = new InetSocketAddress(host, port);

        if (address.isUnresolved()) {
            throw new IllegalArgumentException("Default provider - Not a usable net address: " + address);
        }

        ConnectionProvider provider = standardProviders.get(address);

        if (provider == null) {
            provider = new SocketPoolProvider(address);
            standardProviders.put(address, provider);
        }
        
        gc.checkAlive();
        return (provider);
    }

    private static ConnectionProvider defaultSecureConnectionProvider(String host, int port, SecurityOptions options) {
        final InetSocketAddress address = new InetSocketAddress(host, port);

        if (address.isUnresolved()) {
            throw new IllegalArgumentException("Default secure provider - Not a usable net address: " + address);
        }

        final SecurityOptions securityOptions = new SecurityOptions(options);

        class Key {
            public InetSocketAddress getAddress() {
                return address;
            }

            public SecurityOptions getSecurityOptions() {
                return securityOptions;
            }

            @Override
            public int hashCode() {
                return address.hashCode() + securityOptions.hashCode();
            }

            @Override
            public boolean equals(Object o) {
                if (o instanceof Key) {
                    Key k = (Key)o;
                    return (this == k)
                            || (address.equals(k.getAddress()) && securityOptions.equals(k.getSecurityOptions()));
                } else {
                    return false;
                }
            }
        }

        Key key = new Key();

        ConnectionProvider provider = secureProviders.get(key);

        if (provider == null) {
            try {
                provider = new SSLSocketPoolProvider(address, securityOptions);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace(); // FIXME: auto-generated
            } catch (KeyManagementException e) {
                e.printStackTrace(); // FIXME: auto-generated
            }

            secureProviders.put(key, provider);
        }
        
        gc.checkAlive();

        return (provider);
    }

    // ----------------------------------------------------------------

    private static boolean validScheme(String scheme) {
        if (scheme == null)
            return false;

        for (int i = 0; i < knownSchemes.length; i++) {
            if (scheme.equalsIgnoreCase(knownSchemes[i])) {
                return true;
            }
        }

        return false;
    }

    private static boolean secureScheme(String scheme) {
        if (scheme == null)
            return false;

        for (int i = 0; i < secureSchemes.length; i++) {
            if (scheme.equalsIgnoreCase(secureSchemes[i])) {
                return true;
            }
        }

        return false;
    }

//	private static Class findClass (String property, String defaultName)
//		throws ClassNotFoundException
//	{
//		String implName = System.getProperty (property);
//
//		if (implName == null) {
//			implName = defaultName;
//		}
//
//		return (Class.forName (implName));
//	}
    
    /**
     * Wakes up periodically to close expired connections.
     */
    static class ConnectionCollector extends Thread {          
        @Override
        public void run() {
            while (true) { 
                try {
                    Thread.sleep(GC_INTERVAL);
                } catch (InterruptedException e) {
                }
                long currTime = System.currentTimeMillis();
                for (ConnectionProvider pool : standardProviders.values()) {
                    pool.closeExpired(currTime);
                }
                for (ConnectionProvider pool : secureProviders.values()) {
                    pool.closeExpired(currTime);
                }
            }       
        }

        synchronized public void checkAlive() {
            if (!isAlive()) {
                setDaemon(true);
                try {
                    setPriority(Thread.MIN_PRIORITY);
                } catch (SecurityException e) {}
                start();             
            }
        }
    }
}
