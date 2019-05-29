/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.sony.internal.scalarweb.protocols;

import java.util.Objects;

import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannel;

/**
 * This class represents an index into a specific scheme/source
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SourceIndex {

    /** The scheme name */
    private final String scheme;

    /** The source name */
    private final String source;

    /** The index */
    private final int index;

    /** The URI */
    private final String uri;

    /**
     * Instantiates a new source index.
     *
     * @param scheme the non-null, non-empty scheme
     * @param source the non-null, non-empty source
     * @param index  the index (zero or positive)
     * @param uri    the non-null, non-empty uri
     */
    public SourceIndex(String scheme, String source, int index, String uri) {
        Validate.notEmpty(scheme, "scheme cannot be empty");
        Validate.notEmpty(source, "source cannot be empty");
        Validate.notEmpty(uri, "uri cannot be empty");
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }

        this.scheme = scheme;
        this.source = source;
        this.index = index;
        this.uri = uri;
    }

    /**
     * Instantiates a new source index from the channel
     *
     * @param channel the non-null channel
     */
    public SourceIndex(ScalarWebChannel channel) {
        Objects.requireNonNull(channel, "channel cannot be null");

        final String[] paths = channel.getPaths();
        if (paths.length != 4) {
            throw new IllegalArgumentException("paths must be 4 in length");
        }

        scheme = paths[0];
        source = paths[1];
        uri = paths[3];
        try {
            index = Integer.parseInt(paths[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("index is not a number: " + paths[3]);
        }

        Validate.notEmpty(scheme, "scheme cannot be empty");
        Validate.notEmpty(source, "source cannot be empty");
        Validate.notEmpty(uri, "uri cannot be empty");
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }
    }

    /**
     * Gets the scheme
     *
     * @return the non-null, non-empty scheme
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Gets the source
     *
     * @return the non-null, non-empty source
     */
    public String getSource() {
        return source;
    }

    /**
     * Gets the index
     *
     * @return the index (>= 0)
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the URI
     *
     * @return the non-null, non-empty URI
     */
    public String getUri() {
        return uri;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + index;
        result = prime * result + scheme.hashCode();
        result = prime * result + source.hashCode();
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SourceIndex other = (SourceIndex) obj;
        if (index != other.index) {
            return false;
        }
        if (!scheme.equals(other.scheme)) {
            return false;
        }
        if (!source.equals(other.source)) {
            return false;
        }
        if (!uri.equals(other.uri)) {
            return false;
        }
        return true;
    }
}
