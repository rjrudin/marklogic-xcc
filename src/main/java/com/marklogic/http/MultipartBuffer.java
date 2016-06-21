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
package com.marklogic.http;

import java.io.IOException;
import java.io.InputStream;

import com.marklogic.io.IOHelper;

public class MultipartBuffer {
    private InputStream partInputStream;
    private MultipartSplitter splitter;
    private HttpHeaders headers = new HttpHeaders();

    public long getTotalBytesRead() {
        return splitter.getTotalBytesRead();
    }

    // multipart boundary characters:
    //  \n--bbbbbbbb\n between parts,
    //  \n--bbbbbbbb--\n following last part

    // ---------------------------------------------------------------

    public MultipartBuffer(MultipartSplitter splitter) {
        this.splitter = splitter;
    }

    public String getHeader(String name) {
        return (headers.getHeaderNormalized(name));
    }

    public void close() throws IOException {
        if (partInputStream != null) {
            partInputStream.close();
            partInputStream = null;
        }

        if (splitter != null) {
            splitter.close();
            splitter = null;
        }
    }

    public boolean isClosed() {
        return (splitter == null);
    }

    public int read(byte[] buf, int off, int len) throws IOException {
        if (partInputStream == null) {
            throw new IllegalStateException("No active part stream");
        }

        return (partInputStream.read(buf, off, len));
    }

    public String getBodyAsString() throws IOException {
        if (partInputStream == null) {
            throw new IllegalStateException("No active part stream");
        }

        return IOHelper.literalStringFromStream(partInputStream);
    }

    /**
     * Get an input stream on the next chunk of the buffer.
     * 
     * @return a java.io.InputStream that reads just the next chunk
     */
    public InputStream nextStream() throws IOException {
        if (!next()) {
            return (null);
        }

        return (partInputStream);
    }

    public boolean next() throws IOException {
        if (!splitter.hasNext()) {
            partInputStream = null;

            return false;
        }

        splitter.next();

        partInputStream = new PartInputStream(splitter);

        headers.clear();
        headers.parsePlainHeaders(partInputStream);

        return (true);
    }

    public boolean hasNext() throws IOException {
        return (splitter.hasNext());
    }

//	public void setBufferSize (int size)
//	{
//		splitter.setBufferSize (size);
//	}
//
//	public int getBufferSize()
//	{
//		return splitter.getBufferSize();
//	}

    public InputStream getBodyStream() {
        if (partInputStream == null) {
            throw new IllegalStateException("No active part stream");
        }

        return partInputStream;
    }
}
