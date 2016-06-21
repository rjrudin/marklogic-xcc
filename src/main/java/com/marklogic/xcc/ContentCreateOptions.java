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

import java.math.BigInteger;
import java.util.Locale;

import com.marklogic.http.HttpChannel;

/**
 * A set of creation options to be applied to a document when it is inserted into a contentbase. By
 * default, the document format is set to {@link DocumentFormat#NONE} which indicates that
 * server-configured defaults should be used to determine the document format.
 */
public class ContentCreateOptions implements Cloneable {
    /**
     * The default character encoding (UTF-8) that will be assumed if not explicitly set by
     * {@link #setEncoding(String)}.
     */
    public static final String DEFAULT_ENCODING = "UTF-8";

    /** The minimum user-settable buffer size (256) */
    public static final int MIN_BUFFER_SIZE = 256;
    /** The maximum user-settable buffer size (12MB) */
    public static final int MAX_BUFFER_SIZE = 12 * 1024 * 1024;

    private DocumentFormat format = DocumentFormat.NONE;
    private DocumentRepairLevel repairLevel = DocumentRepairLevel.DEFAULT;

    private Locale locale = null;
    private boolean resolveEntities = false;
    private int resolveBufferSize = 0;
    private ContentPermission[] permissions = null;
    private String[] collections = null;
    private int quality = 0;
    private String namespace = null;
    private BigInteger[] placeKeys = null;
    private String language = null;
    private String encoding = DEFAULT_ENCODING;
    private int bufferSize = -1;
    private String temporalCollection = null;
    private String graph = null;

    // ----------------------------------------------------------

    /**
     * Create an instance with format set to {@link DocumentFormat#XML}.
     * 
     * @return An options object that specifies XML format.
     */
    public static ContentCreateOptions newXmlInstance() {
        ContentCreateOptions options = new ContentCreateOptions();

        options.setFormatXml();

        return (options);
    }

    /**
     * Create an instance with format set to {@link DocumentFormat#TEXT}.
     * 
     * @return An options object that specifies text() format.
     */
    public static ContentCreateOptions newTextInstance() {
        ContentCreateOptions options = new ContentCreateOptions();

        options.setFormatText();

        return (options);
    }

    /**
     * Create an instance with format set to {@link DocumentFormat#BINARY}.
     * 
     * @return An options object that specifies binary() format.
     */
    public static ContentCreateOptions newBinaryInstance() {
        ContentCreateOptions options = new ContentCreateOptions();

        options.setFormatBinary();

        return (options);
    }
    
    /**
     * Create an instance with format set to {@link DocumentFormat#JSON}.
     * 
     * @return An options object that specifies json format.
     */
    public static ContentCreateOptions newJsonInstance() {
        ContentCreateOptions options = new ContentCreateOptions();

        options.setFormatJson();

        return (options);
    }

    // ----------------------------------------------------------

    /**
     * Set the format of the document to be created to the given type. If never set, the document
     * format is {@link DocumentFormat#NONE} which indicates that the server should apply its
     * configured rules for determining the document format. This may include choosing a format
     * according to the suffix of the document URI.
     * 
     * @param format
     *            An instance of {@link DocumentFormat}.
     */
    public void setFormat(DocumentFormat format) {
        this.format = format;
    }

    /**
     * Return the document format value currently in effect for this options object.
     * 
     * @return An instance of {@link DocumentFormat}
     */
    public DocumentFormat getFormat() {
        return (format);
    }

    /**
     * Convenience method equivalent to <code>setFormat (DocumentFormat.XML);</code>.
     */
    public void setFormatXml() {
        setFormat(DocumentFormat.XML);
    }

    /**
     * Convenience method equivalent to <code>setFormat (DocumentFormat.TEXT);</code>.
     */
    public void setFormatText() {
        setFormat(DocumentFormat.TEXT);
    }

    /**
     * Convenience method equivalent to <code>setFormat (DocumentFormat.BINARY);</code>.
     */
    public void setFormatBinary() {
        setFormat(DocumentFormat.BINARY);
    }
    
    /**
     * Convenience method equivalent to <code>setFormat (DocumentFormat.JSON);</code>.
     */
    public void setFormatJson() {
        setFormat(DocumentFormat.JSON);
    }

    // ----------------------------------------------------------
    // simple properties

    /**
     * Return the current document repair level setting. The default value is
     * {@link DocumentRepairLevel#DEFAULT}, but this option is only applicable when the document format
     * is {@link DocumentFormat#XML}.
     * 
     * @return An instance of {@link DocumentRepairLevel}.
     */
    public DocumentRepairLevel getRepairLevel() {
        return repairLevel;
    }

    /**
     * Set the document repair level for this options object. The default value is
     * {@link DocumentRepairLevel#DEFAULT}, but this option is only applicable when the document format
     * is {@link DocumentFormat#XML}.
     * 
     * @param repairLevel
     *            An instance of {@link DocumentRepairLevel}.
     */
    public void setRepairLevel(DocumentRepairLevel repairLevel) {
        this.repairLevel = repairLevel;
    }

    /**
     * Get the {@link Locale} setting for this options object.
     * 
     * @return An instance of {@link Locale}, or null.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Set the effective {@link Locale} value for this options object. The default is null, which
     * indicates that the local JVM default value is to be applied. Note that the server may not
     * support the locale configured as the local client default. In such a case it may be necessary
     * to explicitly set the {@link Locale} to a value recognized by the server for content
     * insertion to succeed.
     * 
     * @param locale
     *            An instance of {@link Locale}.
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * Get the current setting of the flag which indicates whether or not to resolve entities
     * embedded in an XML document to be inserted. If true, any entity references in a document
     * being inserted will be resolved (if possible) by XCC and sent to the server for inline
     * expansion. If false, entities will simply be stored as text. The default is false and this
     * flag is only meaningful when the format is {@link DocumentFormat#XML}.
     * 
     * @return A boolean value.
     */
    public boolean getResolveEntities() {
        return resolveEntities;
    }

    /**
     * Set the flag indicating whether embedded entities should be resolved
     * during content insertion. The default is false. When XCC is set to be in
     * HTTP compliant mode through system property "xcc.httpcompliant", setting
     * the flag to true will throw UnsupportedOperationException.
     * 
     * @param resolveEntities
     *            A boolean indicating whether entities should be resolved or
     *            not.
     * @see #getResolveEntities()
     */
    public void setResolveEntities(boolean resolveEntities) {
        if (HttpChannel.isUseHTTP()) {
            throw new UnsupportedOperationException(
                "ResolveEntities is not supported when "
                    + "\"xcc.httpcompliant\" is enabled");
        }
        this.resolveEntities = resolveEntities;
    }

    /**
     * Indicate the buffer size to use for entity resolution. The default is zero.
     * 
     * @return An integer value, possibly zero.
     */
    public int getResolveBufferSize() {
        return resolveBufferSize;
    }

    /**
     * Set the entity resulution buffer size. This value is passed to the server and is not used
     * directly by XCC. The default is 0, which indicates that a reasonable default should be used.
     * The default should be appropriate for all but the most unusual of circumstances.
     * 
     * @param resolveBufferSize
     *            The buffer size to pass the server.
     */
    public void setResolveBufferSize(int resolveBufferSize) {
        this.resolveBufferSize = resolveBufferSize;
    }

    /**
     * Return the quality value currently set on this options object.
     * 
     * @return An integer value.
     */
    public int getQuality() {
        return quality;
    }

    /**
     * Set the quality value for this options object, which will set on inserted documents. The
     * default is zero.
     * 
     * @param quality
     *            An integer value.
     */
    public void setQuality(int quality) {
        this.quality = quality;
    }

    /**
     * Return the namespace name setting current in effect for this options object.
     * 
     * @return A namespace name {@link String}, or null.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Set a namespace name, which will used as the namespace associated with inserted documents.
     * The default is null, which indicates that the default namespace should be used.
     * 
     * @param namespace
     *            A namespace name as a {@link String}, or null to reset to default.
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Get the current language setting for this options object.
     * 
     * @return A language name as a {@link String}, or null.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Set the language name to associate with inserted documents. A value of <code>en</code>
     * indicates that the document is in english. The default is null, which indicates to use the
     * server default.
     * 
     * @param language
     *            A language name as a {@link String}, or null to reset to the default.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Get the current charset encoding setting for this options object.
     * 
     * @return A charset encoding name, as a {@link String}
     * @since 3.2
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * <p>
     * Set the charset encoding to be used by the server when loading this document. The encoding
     * provided will be passed to the server at document load time and must be a name that it
     * recognizes. The document byte stream will be transcoded to UTF-8 for storage.
     * </p>
     * <p>
     * Note: UTF-8 encoded files may contain a three-byte Byte Order Mark at the beginning which
     * decodes as the Unicode character \ufeff. As of 4.0, this value is no longer ignored by the
     * server. When it's safe to do so, XCC will strip the BOM, which is not considered to be part
     * of the content. XCC will <i>not</i> strip the BOM if the encoding is not UTF-8 (either
     * explicitly set, or defaulted) and the document format ({@link #setFormat(DocumentFormat)}) is
     * not text or XML. If the document format is not explicitly set, BOM stripping will not occur
     * (because the server may choose to treat the content as binary) and document insertion may
     * fail if a BOM is present.
     * </p>
     * 
     * @param encoding
     *            The name of an encoding to be used to interpret the document data as it is loaded
     *            by the server. A value of null sets the encoding to the default (
     *            {@link #DEFAULT_ENCODING}).
     * @since 3.2
     */
    public void setEncoding(String encoding) {
        if (encoding == null) {
            this.encoding = DEFAULT_ENCODING;
        } else {
            this.encoding = encoding;
        }
    }

    // ----------------------------------------------------------

    /**
     * Return the set of collection URIs currently in effect for this options object.
     * 
     * @return An array of URI {@link String}s, or null.
     */
    public String[] getCollections() {
        return (collections == null) ? null : collections.clone();
    }

    /**
     * Set an array of URIs that represent collections to which the document(s) will be added when
     * inserted. The default is null, which indicates that the document(s) should be added to the
     * users default collections.
     * 
     * @param collections
     *            An array of {@link String}s which are collection URIs.
     */
    public void setCollections(String[] collections) {
        this.collections = (collections == null) ? null : collections.clone();
    }

    /**
     * <p>
     * Returns the set of forest placement keys (forest IDs) currently in effect for this options
     * object. Although forest placement keys may be specified in two ways, as {@link BigInteger} or
     * and long, they are always stored internally as {@link BigInteger} objects.
     * </p>
     * <p>
     * If the URI of the document being inserted already exists exists in the contentbase, it will
     * remain in the same forest. If a specified forest ID does not exist, that is an error. If more
     * than one forest ID is given, the document will be placed in one of them at the server's
     * discretion.
     * </p>
     * 
     * @return An array of {@link BigInteger} objects, or null.
     */
    public BigInteger[] getPlaceKeys() {
        return (placeKeys == null) ? null : placeKeys.clone();
    }

    /**
     * <p>
     * Set the forest placement keys for this options object as an array of {@link BigInteger}
     * objects. Forest IDs are unsigned 64-bit values generated by the server. Because Java long
     * values are signed longs, there is a possiblity that forest IDs cannot reliably be represented
     * by Java longs.
     * </p>
     * <p>
     * While is it not possible to specify forest placement directly by forest name, is it very easy
     * to map forest names to forest IDs. The
     * {@link com.marklogic.xcc.ContentbaseMetaData#getForestMap()} method will return a
     * {@link java.util.Map} of the forest names and their associated IDs.
     * </p>
     * 
     * @param forestKeys
     *            An array of {@link BigInteger} objects or null to set default.
     */
    public void setPlaceKeys(BigInteger[] forestKeys) {
        this.placeKeys = (forestKeys == null) ? null : forestKeys.clone();
    }

    /**
     * Set the forest placement keys as long values. The values provided, if any, will be converted
     * and stored a {@link BigInteger} objects.
     * 
     * @param forestKeys
     *            An array of long values.
     * @see #setPlaceKeys(java.math.BigInteger[])
     */
    public void setPlaceKeys(long[] forestKeys) {
        if (forestKeys == null) {
            this.placeKeys = null;
            return;
        }

        BigInteger[] bigInts = new BigInteger[forestKeys.length];

        for (int i = 0; i < forestKeys.length; i++) {
            bigInts[i] = new BigInteger("" + forestKeys[i]);
        }

        setPlaceKeys(bigInts);
    }

    /**
     * Return the set of document permissions currently in effect for this options object.
     * 
     * @return An array of {@link ContentPermission} objects, or null.
     */
    public ContentPermission[] getPermissions() {
        return (permissions == null) ? null : permissions.clone();
    }

    /**
     * Set the permissions to be applied when documents are inserted. The default is null, which
     * indicates that the default permissions for user should be applied.
     * 
     * @param permissions
     *            An array of {@link ContentPermission} objects or null to reset to defaults.
     */
    public void setPermissions(ContentPermission[] permissions) {
        this.permissions = (permissions == null) ? null : permissions.clone();
    }

    /**
     * Return the preferred working buffer size to use for copying the content to the server.
     * 
     * @return The user-supplied buffer size, or -1.
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Set a preferred working buffer size to use for copying to the server. A value of -1, the
     * default, indicates that an internal default should be used. If the value supplied is less
     * than one, it will treated as -1. Positive values will be constrained to
     * {@link #MIN_BUFFER_SIZE} and {@link #MAX_BUFFER_SIZE}. This buffer size is a maximum. If the
     * actual size of the content is smaller than this size, a buffer of the smaller size will be
     * used.
     * 
     * @param newSize
     *            A preferred buffer size, or -1 to use the default.
     */
    public void setBufferSize(int newSize) {
        if (newSize < 1) {
            this.bufferSize = -1;
        } else if (newSize < MIN_BUFFER_SIZE) {
            this.bufferSize = MIN_BUFFER_SIZE;
        } else if (newSize > MAX_BUFFER_SIZE) {
            this.bufferSize = MAX_BUFFER_SIZE;
        } else {
            this.bufferSize = newSize;
        }
    }
    
    /**
     * Get the temporal collection for this options object.
     * 
     * @return A language name as a {@link String}, or null.
     */
    public String getTemporalCollection() {
        return temporalCollection;
    }

    /**
     * Set the temporal collection to associate with inserted documents. 
     * A value of null indicates that the document is not temporal. 
     * The default is null.
     * 
     * @param temporalCollection
     *            A temporal collection as a {@link String}, or null for non-temporal.
     */
    public void setTemporalCollection(String temporalCollection) {
        this.temporalCollection = temporalCollection;
    }

    /**
     * Get the RDF Graph for this options object
     * @return A graph iri as {@link String}, or null.
     */
    public String getGraph() {
        return graph;
    }

    /**
     * Set the RDF Graph for this options object
     * @param graph 
     *          A graph iri as {@link String}.
     */
    public void setGraph(String graph) {
        this.graph = graph;
    }
    
    @Override
    public Object clone() {
        ContentCreateOptions options = new ContentCreateOptions();
        options.bufferSize = bufferSize;
        if (collections != null) {
            options.collections = new String[collections.length];
            for (int i = 0; i < collections.length; i++) {
                options.collections[i] = collections[i];
            }
        }   
        options.encoding = encoding;
        options.format = format;
        options.language = language;
        options.locale = locale;
        options.namespace = namespace;
        options.temporalCollection = temporalCollection;
        if (permissions != null) {
            options.permissions = new ContentPermission[permissions.length];
            for (int i = 0; i < permissions.length; i++) {
                options.permissions[i] = permissions[i];
            }
        }   
        if (placeKeys != null) {
            options.placeKeys = new BigInteger[placeKeys.length];
            for (int i = 0; i < placeKeys.length; i++) {
                options.placeKeys[i] = new BigInteger(
                        placeKeys[i].toByteArray());
            }
        }
        options.quality = quality;
        options.repairLevel = repairLevel;
        options.resolveBufferSize = resolveBufferSize;
        options.resolveEntities = resolveEntities;
        options.graph = graph;

        return options;
    }
}
