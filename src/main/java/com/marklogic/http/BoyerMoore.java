/*
 * Copyright 2003-2015 MarkLogic Corporation
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

/**
 * This class applies Boyer Moore's search algorithm to find a pattern within a
 * byte array.
 * 
 */
public class BoyerMoore {
    public static final int ALPHABET_SIZE = 256;
    
    private byte[] pattern;  
    private int[] jump;
    private int partialMatch;
    
    /**
     * Search for pattern in the byte array text.  Returns the position of the
     * first occurrence of pattern if found and -1 otherwise.
     * 
     * @param text byte array to search
     * @param start start position
     * @param end end position
     * 
     * @return starting position of the first occurrence of pattern in text or
     * -1 if not found.
     */
    public int search(byte[] text, int start, int end) {  
        partialMatch = 0;
        int i = start, j = 0;
        int m = pattern.length;
        while (i < end) {
            for (j = m - 1; j >= 0; j--) {
                if (i + j < end && text[i + j] != pattern[j]) {
                    break;
                }
            }
            if (j < 0) { // found a match or partial match
                if (i + m - 1 >= end) { // partial match
                    partialMatch = end - i;
                    return -1;
                } else { // full match
                    partialMatch = 0;
                    return i;
                }
            } else { // no match
                if (i + m - 1 < end) {
                    int jumpDistance = j - jump[text[i+j] & 0xff];
                    if (jumpDistance <= 0) { // avoid jumping backward
                        i++;
                    } else {
                        i += jumpDistance;
                    }
                } else {
                    i++;
                }
            }
        }
        return -1;
    }  
    
    /**
     * Return the number of matching characters before the end of a chunk.
     * @return the number of matching characters.
     */
    public int partialMatch() {
        return partialMatch;
    }
    
    /**
     * Compute the jump table based on the pattern.
     */
    private void initialize() {
        jump = new int[ALPHABET_SIZE];
        
        for (int k = 0; k < jump.length; k++) { 
            jump[k] = -1;
        }
        for (int j = pattern.length-1; j >= 0; j--) {
            if (jump[pattern[j] & 0xff] < 0) {
                jump[pattern[j] & 0xff] = j;
            }
        }
    }
    
    public BoyerMoore(byte[] pattern) {
        this.pattern = pattern;     
        initialize();
    }
}
