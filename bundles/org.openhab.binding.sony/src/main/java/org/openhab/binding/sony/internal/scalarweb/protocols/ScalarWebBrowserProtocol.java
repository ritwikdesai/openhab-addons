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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.sony.internal.SonyUtil;
import org.openhab.binding.sony.internal.ThingCallback;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannel;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannelDescriptor;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannelTracker;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebContext;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebMethod;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;
import org.openhab.binding.sony.internal.scalarweb.models.api.BrowserControl;
import org.openhab.binding.sony.internal.scalarweb.models.api.TextUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of the protocol handles the Browser service
 *
 * @author Tim Roberts - Initial contribution
 * @param <T> the generic type for the callback
 */
@NonNullByDefault
class ScalarWebBrowserProtocol<T extends ThingCallback<String>> extends AbstractScalarWebProtocol<T> {

    /** The logger. */
    private Logger logger = LoggerFactory.getLogger(ScalarWebSystemProtocol.class);

    // Constants used by this protocol
    private static final String BROWSERCONTROL = "browsercontrol";
    private static final String TEXTURL = "texturl";
    private static final String TEXTTITLE = "texttitle";
    private static final String TEXTTYPE = "texttype";
    private static final String TEXTFAVICON = "textfavicon";

    /**
     * Instantiates a new scalar web browser protocol.
     *
     * @param factory  the non-null factory
     * @param context  the non-null context
     * @param service  the non-null service
     * @param callback the non-null callback
     */
    ScalarWebBrowserProtocol(ScalarWebProtocolFactory<T> factory, ScalarWebContext context, ScalarWebService service,
            T callback) {
        super(factory, context, service, callback);
    }

    @Override
    public Collection<ScalarWebChannelDescriptor> getChannelDescriptors() {
        final List<ScalarWebChannelDescriptor> descriptors = new ArrayList<ScalarWebChannelDescriptor>();

        if (service.hasMethod(ScalarWebMethod.ACTIVATEBROWSERCONTROL)) {
            descriptors.add(createDescriptor(createChannel(BROWSERCONTROL), "String", "scalarbrowseractivate"));
        }

        if (service.hasMethod(ScalarWebMethod.GETTEXTURL)) {
            descriptors.add(createDescriptor(createChannel(TEXTURL), "String", "scalarbrowsertexturl"));
            descriptors.add(createDescriptor(createChannel(TEXTTITLE), "String", "scalarbrowsertexttitle"));
            descriptors.add(createDescriptor(createChannel(TEXTTYPE), "String", "scalarbrowsertexttype"));
            descriptors.add(createDescriptor(createChannel(TEXTFAVICON), "String", "scalarbrowsertextfavicon"));
        }

        return descriptors;
    }

    @Override
    public void refreshState() {
        final ScalarWebChannelTracker tracker = getChannelTracker();
        if (tracker.isCategoryLinked(BROWSERCONTROL)) {
            refreshBrowserControl();
        }

        if (tracker.isCategoryLinked(TEXTURL, TEXTTITLE, TEXTTYPE, TEXTFAVICON)) {
            refreshTextUrl();
        }
    }

    @Override
    public void refreshChannel(ScalarWebChannel channel) {
        Objects.requireNonNull(channel, "channel cannot be null");

        switch (channel.getCategory()) {
            case BROWSERCONTROL:
                refreshBrowserControl();
                break;

            case TEXTURL:
            case TEXTTITLE:
            case TEXTTYPE:
            case TEXTFAVICON:
                refreshTextUrl();
                break;

            default:
                logger.debug("Unknown refresh channel: {}", channel);
                break;
        }
    }

    /**
     * Refresh browser control
     */
    private void refreshBrowserControl() {
        stateChanged(BROWSERCONTROL, StringType.EMPTY);
    }

    /**
     * Refresh text url information (url, title, type, favicon)
     */
    private void refreshTextUrl() {
        try {
            final TextUrl url = execute(ScalarWebMethod.GETTEXTURL).as(TextUrl.class);

            stateChanged(TEXTURL, SonyUtil.newStringType(url.getUrl()));
            stateChanged(TEXTTITLE, SonyUtil.newStringType(url.getTitle()));
            stateChanged(TEXTTYPE, SonyUtil.newStringType(url.getType()));
            stateChanged(TEXTFAVICON, SonyUtil.newStringType(url.getFavicon()));
        } catch (IOException e) {
            logger.debug("Error retrieving text URL information: {}", e.getMessage());
        }
    }

    @Override
    public void setChannel(ScalarWebChannel channel, Command command) {
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(command, "command cannot be null");

        switch (channel.getCategory()) {
            case BROWSERCONTROL:
                if (command instanceof StringType) {
                    setActivateBrowserControl(command.toString());
                } else {
                    logger.debug("BROWSERCONTROL mode command not an StringType: {}", command);
                }

                break;

            case TEXTURL:
                if (command instanceof StringType) {
                    setTextUrl(command.toString());
                } else {
                    logger.debug("TEXTURL command not an StringType: {}", command);
                }

                break;

            default:
                logger.debug("Unhandled channel command: {} - {}", channel, command);
                break;
        }
    }

    /**
     * Activates the browser control
     *
     * @param control the new activate browser control
     */
    private void setActivateBrowserControl(@Nullable String control) {
        handleExecute(ScalarWebMethod.ACTIVATEBROWSERCONTROL,
                new BrowserControl(control == null || StringUtils.isEmpty(control) ? null : control));
    }

    /**
     * Sets the URL text
     *
     * @param url the new URL text
     */
    private void setTextUrl(String url) {
        Validate.notEmpty(url, "url cannot be empty");
        handleExecute(ScalarWebMethod.SETTEXTURL, new TextUrl(url));
    }
}
