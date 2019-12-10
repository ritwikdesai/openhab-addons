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
package org.openhab.binding.sony.internal.providers.sources;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.sony.internal.SonyMatcher;
import org.openhab.binding.sony.internal.SonyUtil;
import org.openhab.binding.sony.internal.net.HttpResponse;
import org.openhab.binding.sony.internal.providers.SonyProviderListener;
import org.openhab.binding.sony.internal.providers.models.SonyDeviceCapability;
import org.openhab.binding.sony.internal.providers.models.SonyServiceCapability;
import org.openhab.binding.sony.internal.providers.models.SonyThingDefinition;
import org.openhab.binding.sony.internal.scalarweb.gson.GsonUtilities;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebMethod;
import org.openhab.binding.sony.internal.transports.SonyHttpTransport;
import org.openhab.binding.sony.internal.transports.SonyTransportFactory;
import org.openhab.binding.sony.internal.transports.TransportOptionAutoAuth;
import org.openhab.binding.sony.internal.transports.TransportOptionHeader;

/**
 * An implementation of a {@link SonySource} that will source thing types from
 * json files within the user data folder
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SonyGithubSource extends AbstractSonySource {
    // ---- various constants used by this class ----
    private static final String FOLDERBASE = ConfigConstants.getUserDataFolder() + File.separator + "sony";
    private static final SimpleDateFormat WEBDATEPATTER = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    private static final String PROP_APIKEY = "SonyGithubSource.Api.Key";
    private static final String PROP_APIURL = "SonyGithubSource.Api.Url";
    private static final String PROP_SCANINTERVAL = "SonyGithubSource.Scan.Interval";
    private static final String PROP_APIREST = "SonyGithubSource.Api.RestApi";
    private static final String PROP_APITHINGTYPES = "SonyGithubSource.Api.ThingTypes";
    private static final String PROP_APIDEVISSUES = "SonyGithubSource.Api.Dev.Issues";
    private static final String PROP_APIOPENHABISSUES = "SonyGithubSource.Api.openHab.Issues";
    private static final String PROP_APIDEFTHINGTYPES = "SonyGithubSource.Api.Definition.ThingTypes";
    private static final String PROP_APIDEFCAPABILITIES = "SonyGithubSource.Api.Definition.Capabilities";
    private static final String PROP_APIMETA = "SonyGithubSource.Api.MetaInfo";
    private static final String PROP_FOLDERTYPES = "SonyGithubSource.Folder.ThingTypes";
    private static final String PROP_ISSUECAPABILITY = "SonyGithubSource.Issue.Capability";
    private static final String PROP_ISSUETHINGTYPE = "SonyGithubSource.Issue.ThingType";
    private static final String PROP_ISSUESERVICE = "SonyGithubSource.Issue.Service";
    private static final String PROP_ISSUEMETHOD = "SonyGithubSource.Issue.Method";
    private static final String PROP_LABELOPENHAB = "SonyGithubSource.Label.openHAB";
    private static final String PROP_LABELAPI = "SonyGithubSource.Label.Api";
    private static final String PROP_LABELMETHOD = "SonyGithubSource.Label.Method";
    private static final String PROP_LABELSERVICE = "SonyGithubSource.Label.Service";
    private static final String PROP_LABELCAPABILITY = "SonyGithubSource.Label.Capability";
    private static final String PROP_LABELTHINGTYPE = "SonyGithubSource.Label.ThingType";

    private static final String GITHUB_CODEFENCE = "```";
    private static final TransportOptionHeader RawHeader = new TransportOptionHeader("Accept",
            "application/vnd.github.VERSION.raw");

    // ---- Various constants read from SonyDefinitionProviderImpl.properties ----
    private final String apiKey;
    private final String apiRestJson;
    private final String apiMetaJson;
    private final String apiThingTypes;
    private final String apiDevIssues;
    private final String apiOpenHABIssues;
    private final String apiDefThingTypes;
    private final String apiDefCapabilities;

    private final String labelOpenHAB;
    private final String labelApi;
    private final String labelService;
    private final String labelMethod;
    private final String labelCapability;
    private final String labelThingType;

    private final String issueCapability;
    private final String issueThingType;
    private final String issueService;
    private final String issueMethod;

    /** The path to the thing type cache */
    private final Path thingTypePath;

    /** The folder watcher (null if none being watched) */
    private final AtomicReference<@Nullable Future<?>> watcher = new AtomicReference<>(null);

    private Lock capabilitiesLock = new ReentrantLock();
    /** The master service capabilities */
    private List<SonyServiceCapability> capabilitiesMaster = Collections.unmodifiableList(new ArrayList<>());

    /** The etag of the last download of capabilities */
    private @Nullable String capabilitiesEtag;

    private Lock metaLock = new ReentrantLock();

    /** The meta information (ignore or convert) for all types */
    private MetaInfo metaInfo = new MetaInfo();

    /** The etag of the last download of meta information */
    private @Nullable String metaEtag;

    /** A map of thing type filename to it's related model name pattern */
    private Lock knownThingsLock = new ReentrantLock();
    private Map<String, List<Map.Entry<String, String>>> knownThingTypes = new HashMap<>();

    private Set<Map.Entry<String, String>> waitingModelNames = new HashSet<>();

    /** The etag of the last download of thing types */
    private @Nullable String thingTypesEtag = "1";

    /** The transport to use for calling into github */
    private final SonyHttpTransport transport;

    /** The GSON that will be used for deserialization */
    private final Gson gson = GsonUtilities.getDefaultGson();

    /** Funtional interface for determining if an issue is a match */
    private interface IssueCallback {
        boolean isMatch(String body) throws JsonSyntaxException;
    }

    /**
     * Constructs the source and starts the various threads
     * 
     * @param scheduler  a non-null scheduler to use
     * @param properties a non-null, possibly empty map of properties
     */
    public SonyGithubSource(ScheduledExecutorService scheduler, Map<String, String> properties) {
        Objects.requireNonNull(scheduler, "scheduler cannot be null");
        Objects.requireNonNull(properties, "properties cannot be null");

        apiKey = getProperty(properties, PROP_APIKEY);

        final String apiUrl = getProperty(properties, PROP_APIURL);
        try {
            transport = SonyTransportFactory.createHttpTransport(apiUrl);
            transport.setOption(TransportOptionAutoAuth.FALSE);
            transport.setOption(new TransportOptionHeader("Authorization", "token " + apiKey));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    PROP_APIURL + " defined an invalid URI: " + apiUrl + " - " + e.getMessage());
        }

        apiRestJson = apiUrl + getProperty(properties, PROP_APIREST);
        apiMetaJson = apiUrl + getProperty(properties, PROP_APIMETA);
        apiThingTypes = apiUrl + getProperty(properties, PROP_APITHINGTYPES);
        apiDevIssues = apiUrl + getProperty(properties, PROP_APIDEVISSUES);
        apiOpenHABIssues = apiUrl + getProperty(properties, PROP_APIOPENHABISSUES);
        apiDefThingTypes = apiUrl + getProperty(properties, PROP_APIDEFTHINGTYPES);
        apiDefCapabilities = apiUrl + getProperty(properties, PROP_APIDEFCAPABILITIES);

        labelOpenHAB = getProperty(properties, PROP_LABELOPENHAB);
        labelApi = getProperty(properties, PROP_LABELAPI);
        labelService = getProperty(properties, PROP_LABELSERVICE);
        labelMethod = getProperty(properties, PROP_LABELMETHOD);
        labelCapability = getProperty(properties, PROP_LABELCAPABILITY);
        labelThingType = getProperty(properties, PROP_LABELTHINGTYPE);

        issueCapability = getProperty(properties, PROP_ISSUECAPABILITY);
        issueThingType = getProperty(properties, PROP_ISSUETHINGTYPE);
        issueService = getProperty(properties, PROP_ISSUESERVICE);
        issueMethod = getProperty(properties, PROP_ISSUEMETHOD);

        thingTypePath = Paths.get(FOLDERBASE + getProperty(properties, PROP_FOLDERTYPES));
        SonyUtil.createFolder(thingTypePath.toString());

        final int scanInterval = getPropertyInt(properties, PROP_SCANINTERVAL);
        SonyUtil.cancel(watcher.getAndSet(scheduler.scheduleWithFixedDelay(() -> {
            updateFromGithub();
        }, 0, scanInterval, TimeUnit.SECONDS)));

        try {
            readFiles(thingTypePath.toString());
        } catch (JsonSyntaxException | IOException e) {
            logger.debug("Exception reading files from {}: {}", thingTypePath.toString(), e.getMessage(), e);
        }
    }

    @Override
    public void writeThingDefinition(SonyThingDefinition thingTypeDefinition) {
        Objects.requireNonNull(thingTypeDefinition, "thingTypeDefinition cannot be null");

        final String ttModelName = thingTypeDefinition.getModelName();
        if (ttModelName == null || StringUtils.isEmpty(ttModelName)) {
            logger.debug("Cannot write thing definition because it has no model name: {}", thingTypeDefinition);
            return;
        }

        String modelName;
        try {
            final MetaInfo meta = getMetaInfo();
            if (meta.isIgnored(ttModelName)) {
                logger.debug("Ignoring write thing definition - model name was on ignore list: {}", ttModelName);
                return;
            }

            modelName = meta.getModelName(ttModelName);
        } catch (IOException | JsonSyntaxException e) {
            logger.debug("Exception writing device capabilities: {}", e.getMessage(), e);
            return;
        }

        if (!StringUtils.equalsIgnoreCase(modelName, ttModelName)) {
            logger.debug("Converting model name {} to {}", ttModelName, modelName);
        }

        try {
            boolean found = false;
            for (int i = 0; i < 100; i++) {
                final String fileName = URLEncoder.encode(modelName + (i == 0 ? "" : ("-" + i)) + ".json", "UTF-8");
                final HttpResponse defResp = transport.executeGet(apiDefThingTypes + "/" + fileName, RawHeader);

                if (defResp.getHttpCode() == HttpStatus.OK_200) {
                    final SonyThingDefinition oldDef = gson.fromJson(defResp.getContent(), SonyThingDefinition.class);
                    found = thingTypeDefinition.matches(oldDef);
                    break;
                } else if (defResp.getHttpCode() == HttpStatus.NOT_FOUND_404) {
                    break;
                }
            }
            if (!found && BooleanUtils.isFalse(findIssue(apiOpenHABIssues, b -> {
                final SonyThingDefinition issueDef = gson.fromJson(b.replaceAll(GITHUB_CODEFENCE, ""),
                        SonyThingDefinition.class);
                return thingTypeDefinition.matches(issueDef);
            }, labelThingType))) {
                final String body = GITHUB_CODEFENCE + gson.toJson(thingTypeDefinition) + GITHUB_CODEFENCE;
                final JsonObject jo = new JsonObject();
                jo.addProperty("title", String.format(issueThingType, modelName));
                jo.addProperty("body", body);

                final JsonArray ja = new JsonArray();
                ja.add(labelThingType);
                jo.add("labels", ja);

                final HttpResponse resp = transport.executePostJson(apiOpenHABIssues, gson.toJson(jo), RawHeader);
                if (resp.getHttpCode() != HttpStatus.CREATED_201) {
                    logger.debug("Error posting service change: {} \r\n{}", resp, body);
                }
            }
        } catch (URISyntaxException | IOException | JsonSyntaxException e) {
            logger.debug("Exception writing thing defintion: {}", e.getMessage(), e);
        }
    }

    @Override
    public void writeDeviceCapabilities(SonyDeviceCapability deviceCapability) {
        Objects.requireNonNull(deviceCapability, "deviceCapability cannot be null");

        if (deviceCapability.getServices().stream().flatMap(s -> s.getMethods().stream())
                .anyMatch(m -> m.getVariation() == ScalarWebMethod.UNKNOWN_VARIATION)) {
            logger.debug(
                    "Cannot write device capabilities because capabilities had an unknown variation and should be ignored: {}",
                    deviceCapability);
            return;
        }

        final String ttModelName = deviceCapability.getModelName();
        if (ttModelName == null || StringUtils.isEmpty(ttModelName)) {
            logger.debug("Cannot write device capabilities because it has no model name: {}", deviceCapability);
            return;
        }

        // ---- Check to see if we should ignore this model ----
        String modelName;
        try {
            final MetaInfo meta = getMetaInfo();
            if (meta.isIgnored(ttModelName)) {
                logger.debug("Ignoring device capabilities - model name was on ignore list: {}", ttModelName);
                return;
            }

            modelName = meta.getModelName(ttModelName);
        } catch (IOException | JsonSyntaxException e) {
            logger.debug("Exception writing device capabilities: {}", e.getMessage(), e);
            return;
        }

        if (!StringUtils.equalsIgnoreCase(modelName, ttModelName)) {
            logger.debug("Converting model name {} to {}", ttModelName, modelName);
        }

        // ----
        // Check to see if the capability already exists (in sonydevices/openHAB/issues)
        // and will post a new issue if it does not (to
        // /openHAB/contents/definitions/capabilities)
        // Will check modelName.json, modelName-1.json, etc up to a max -100
        // ----
        try {
            boolean found = false;
            for (int i = 0; i < 100; i++) {
                final String fileName = URLEncoder.encode(modelName + (i == 0 ? "" : ("-" + i)) + ".json", "UTF-8");
                final HttpResponse defResp = transport.executeGet(apiDefCapabilities + "/" + fileName, RawHeader);

                if (defResp.getHttpCode() == HttpStatus.OK_200) {
                    final SonyDeviceCapability oldCap = gson.fromJson(defResp.getContent(), SonyDeviceCapability.class);
                    found = deviceCapability.matches(oldCap);
                    break;
                } else if (defResp.getHttpCode() == HttpStatus.NOT_FOUND_404) {
                    found = false;
                    break;
                }
            }

            if (!found && BooleanUtils.isFalse(findIssue(apiOpenHABIssues, b -> {
                final SonyDeviceCapability issueCap = gson.fromJson(b.replaceAll(GITHUB_CODEFENCE, ""),
                        SonyDeviceCapability.class);
                return deviceCapability.matches(issueCap);
            }, labelCapability))) {
                final String body = GITHUB_CODEFENCE + gson.toJson(deviceCapability) + GITHUB_CODEFENCE;
                final JsonObject jo = new JsonObject();
                jo.addProperty("title", String.format(issueCapability, modelName));
                jo.addProperty("body", body);

                final JsonArray ja = new JsonArray();
                ja.add(labelCapability);
                jo.add("labels", ja);

                final HttpResponse resp = transport.executePostJson(apiOpenHABIssues, gson.toJson(jo), RawHeader);
                if (resp.getHttpCode() != HttpStatus.CREATED_201) {
                    logger.debug("Error posting service change: {} \r\n{}", resp, body);
                }
            }
        } catch (UnsupportedEncodingException | URISyntaxException | JsonSyntaxException e) {
            logger.debug("Exception writing device capabilities: {}", e.getMessage(), e);
        }

        // ----
        // Checks if any service or method doesn't exist in the master definition
        // document (sonydevices/dev/contents/apiinfo/restapi.json)
        // If it doesn't, post a new issue (to sonydevices/dev/issues)
        // ----
        try {
            final List<SonyServiceCapability> masterCapabilities = getMasterDefinitions();

            for (SonyServiceCapability deviceService : deviceCapability.getServices()) {
                // Get all service version for the name
                final List<SonyServiceCapability> masterServices = masterCapabilities.stream()
                        .filter(s -> StringUtils.equalsIgnoreCase(deviceService.getServiceName(), s.getServiceName()))
                        .collect(Collectors.toList());

                // See if our specific version is part of them (if not, post an issue)
                if (masterServices.stream()
                        .noneMatch(srv -> StringUtils.equalsIgnoreCase(deviceService.getVersion(), srv.getVersion()))
                        && BooleanUtils.isFalse(findIssue(apiDevIssues, b -> {
                            final SonyServiceCapability issueSrv = gson.fromJson(b.replaceAll(GITHUB_CODEFENCE, ""),
                                    SonyServiceCapability.class);
                            return deviceService.matches(issueSrv);
                        }, labelOpenHAB, labelApi, labelService))) {
                    final String body = GITHUB_CODEFENCE + gson.toJson(deviceService) + GITHUB_CODEFENCE;
                    final JsonObject jo = new JsonObject();
                    jo.addProperty("title",
                            String.format(issueService, deviceService.getServiceName(), deviceService.getVersion()));
                    jo.addProperty("body", body);

                    final JsonArray ja = new JsonArray();
                    ja.add(labelOpenHAB);
                    ja.add(labelApi);
                    ja.add(labelService);
                    jo.add("labels", ja);

                    final HttpResponse resp = transport.executePostJson(apiDevIssues, gson.toJson(jo), RawHeader);
                    if (resp.getHttpCode() != HttpStatus.CREATED_201) {
                        logger.debug("Error posting service change: {} \r\n{}", resp, body);
                    }

                }

                // Get all the various methods for the service name (across all service
                // versions)
                final List<ScalarWebMethod> mstrMethods = masterServices.stream()
                        .flatMap(srv -> srv.getMethods().stream())
                        .collect(Collectors.toList());

                // Find the method and if not, post an issue
                for (ScalarWebMethod mth : deviceService.getMethods()) {
                    if (mstrMethods.stream().noneMatch(m -> mth.matches(m))
                            && BooleanUtils.isFalse(findIssue(apiDevIssues, b -> {
                                final ScalarWebMethod issueMth = gson.fromJson(b.replaceAll(GITHUB_CODEFENCE, ""),
                                        ScalarWebMethod.class);
                                return mth.matches(issueMth);
                            }, labelOpenHAB, labelApi, labelMethod))) {
                        final String body = GITHUB_CODEFENCE + gson.toJson(mth) + GITHUB_CODEFENCE;
                        final JsonObject jo = new JsonObject();
                        jo.addProperty("title", String.format(issueMethod, deviceService.getServiceName(),
                                deviceService.getVersion(), mth.getMethodName(), mth.getVersion()));
                        jo.addProperty("body", body);

                        final JsonArray ja = new JsonArray();
                        ja.add(labelOpenHAB);
                        ja.add(labelApi);
                        ja.add(labelMethod);
                        jo.add("labels", ja);

                        final HttpResponse resp = transport.executePostJson(apiDevIssues, gson.toJson(jo), RawHeader);
                        if (resp.getHttpCode() != HttpStatus.CREATED_201) {
                            logger.debug("Error posting service change: {} \r\n{}", resp, body);
                        }
                    }
                }
            }
        } catch(IOException|JsonSyntaxException|URISyntaxException e)
        {
            logger.debug("Exception writing service/method capabilities: {}", e.getMessage(), e);
        }
    }

    private @Nullable Boolean findIssue(String baseUri, IssueCallback callback, String... labels)
            throws URISyntaxException, JsonSyntaxException {
        Validate.notEmpty(baseUri, "baseUri cannot be empty");
        Objects.requireNonNull(callback, "callback cannot be null");
        if (labels.length < 1) {
            throw new IllegalArgumentException("labels must have atleast one element");
        }

        // Initial URI
        String uri = baseUri + "?filter=created&state=open&labels=" + String.join(",", labels);

        // go up to a maximum of 100 pages!
        for (int t = 0; t < 100; t++) {
            final HttpResponse resp = transport.executeGet(uri, RawHeader);
            if (resp.getHttpCode() == HttpStatus.OK_200) {
                final String content = resp.getContent();
                final JsonArray ja = gson.fromJson(content, JsonArray.class);
                for (int i = 0; i < ja.size(); i++) {
                    final JsonElement je = ja.get(i);
                    if (je instanceof JsonObject) {
                        final JsonObject jo = je.getAsJsonObject();
                        final JsonElement jbody = jo.get("body");
                        if (jbody == null) {
                            logger.debug("There is no body element to the object: {} from {}", jo, content);
                        } else {
                            final String body = jbody.getAsString();
                            try {
                                if (callback.isMatch(body)) {
                                    logger.trace("Found match: {}", body);
                                    return true;
                                }
                            } catch (JsonSyntaxException e) {
                                logger.trace("JsonSyntaxException on {}: {}", body, e.getMessage());
                            }
                        }
                    } else {
                        logger.debug("Element {} is not a valid JsonObject: {} from {}", i, je, content);
                    }
                }
                final URI nextUri = resp.getLink(HttpResponse.REL_NEXT);
                if (nextUri == null) {
                    logger.trace("No match found for baseURI: {}", baseUri);
                    return false;
                } else {
                    uri = nextUri.toString();
                    logger.trace("Trying next page {}: {}", t + 1, uri);
                }
            } else {
                logger.debug("Error getting issues for baseURI {}: {}", baseUri, resp.getHttpCode());
                return null;
            }
        }
        logger.debug("No match found within the first 100 pages!: {}", baseUri);
        return false;
    }

    private void updateFromGithub() {
        try {
            refreshGitHubThingTypes();
            getMasterDefinitions();
        } catch (IOException | JsonSyntaxException e) {
            logger.debug("Exception updating from github: {}", e.getMessage(), e);
        }
    }

    private List<SonyServiceCapability> getMasterDefinitions() throws JsonSyntaxException, IOException {
        try {
            capabilitiesLock.lock();

            final HttpResponse resp = transport.executeGet(apiRestJson, RawHeader, new TransportOptionHeader(
                    HttpHeader.IF_NONE_MATCH,
                    capabilitiesEtag == null || StringUtils.isEmpty(capabilitiesEtag) ? "1" : capabilitiesEtag));
            if (resp.getHttpCode() == HttpStatus.OK_200) {
                capabilitiesEtag = resp.getResponseHeader(HttpHeader.ETAG.asString());
                final String content = resp.getContent();
                final List<SonyServiceCapability> sdc = gson.fromJson(content,
                        new TypeToken<List<SonyServiceCapability>>() {
                        }.getType());
                capabilitiesMaster = Collections.unmodifiableList(sdc);
                logger.trace("Got new master definition for etag {}: {}", capabilitiesEtag, content);
                return capabilitiesMaster;
            } else if (resp.getHttpCode() == HttpStatus.NOT_MODIFIED_304) {
                logger.trace("Master definitions was not modified - returning last version from etag {}",
                        capabilitiesEtag);
                return capabilitiesMaster;
            } else {
                throw resp.createException();
            }
        } finally {
            capabilitiesLock.unlock();
        }
    }

    private MetaInfo getMetaInfo() throws JsonSyntaxException, IOException {
        try {
            metaLock.lock();

            final HttpResponse resp = transport.executeGet(apiMetaJson, RawHeader, new TransportOptionHeader(
                    HttpHeader.IF_NONE_MATCH, metaEtag == null || StringUtils.isEmpty(metaEtag) ? "1" : metaEtag));

            if (resp.getHttpCode() == HttpStatus.OK_200) {
                metaEtag = resp.getResponseHeader(HttpHeader.ETAG.asString());
                final String content = resp.getContent();
                logger.trace("Got new meta info for etag {}: {}", metaEtag, content);

                metaInfo = gson.fromJson(content, MetaInfo.class);
                return metaInfo;
            } else if (resp.getHttpCode() == HttpStatus.NOT_MODIFIED_304) {
                logger.trace("Metainfo was not modified - returning last version from etag {}", metaEtag);
                return metaInfo;
            } else {
                throw resp.createException();
            }
        } finally {
            metaLock.unlock();
        }
    }

    private void refreshGitHubThingTypes() throws IOException, JsonSyntaxException {
        try {
            // save the modelnames in the file
            knownThingsLock.lock();
            final HttpResponse resp = transport.executeGet(apiThingTypes, RawHeader,
                    new TransportOptionHeader(HttpHeader.IF_NONE_MATCH, thingTypesEtag == null ? "1" : thingTypesEtag));

            if (resp.getHttpCode() == HttpStatus.OK_200) {
                thingTypesEtag = resp.getResponseHeader(HttpHeader.ETAG.asString());

                logger.trace("New Thing types etag {}", thingTypesEtag);

                final Map<String, Long> files = Files.walk(thingTypePath).filter(p -> Files.isRegularFile(p))
                        .collect(Collectors.toMap(p -> {
                            return p.getFileName().toString();
                        }, v -> {
                            return v.toFile().lastModified();
                        }));

                final String content = resp.getContent();
                final JsonArray ja = gson.fromJson(content, JsonArray.class);
                for (int i = 0; i < ja.size(); i++) {
                    final JsonElement je = ja.get(i);
                    if (je instanceof JsonObject) {
                        final JsonObject jo = je.getAsJsonObject();
                        final JsonElement nameElement = jo.get("name");
                        if (nameElement == null) {
                            logger.debug("There was no name element for {}: {} from {}", i, jo, content);
                            continue;
                        }

                        final String name = nameElement.getAsString();
                        final Long lastModified = files.remove(name);

                        final Map.Entry<@Nullable Long, @Nullable SonyThingDefinition[]> defs = getThingDefinition(name,
                                lastModified);
                        // If not null, then we have a new or updated file...
                        if (defs != null && defs.getKey() != null && defs.getValue() != null) {
                            final List<Map.Entry<String, String>> modelNames = Arrays.stream(defs.getValue())
                                    .map(e -> new AbstractMap.SimpleEntry<>(e.getService(), e.getModelName()))
                                    .collect(Collectors.toList());

                            logger.debug("Adding known thing types from {}: {}", name, modelNames);

                            knownThingTypes.put(name, modelNames);

                            // if the file exists (meaning we have an update)
                            // or we are waiting on one of the models in the file...
                            // the write out the thing definition
                            final File theFile = thingTypePath.resolve(name).toFile();

                            // always execute isWaiting since it (also) removes the model name waiting
                            final boolean isWaiting = waitingModelNames
                                    .removeIf(m -> modelNames.stream().anyMatch(mn -> SonyUtil.isModelMatch(mn.getKey(), mn.getValue(), m.getKey(), m.getValue())));

                            if (theFile.exists() || isWaiting) {
                                writeThingDefinition(name, defs.getKey(), defs.getValue());
                            }
                        }
                    } else {
                        logger.debug("Element {} is not a valid JsonObject: {} from {}", i, je, content);
                    }
                }

                // If something was leftover (ie deleted from github), delete the local file and
                // reload everything
                if (files.size() > 0) {
                    for (String name : files.keySet()) {
                        final File theFile = thingTypePath.resolve(name).toFile();
                        if (theFile.exists()) {
                            logger.debug("File {} was no longer on github and is being deleted", name);
                            theFile.delete();
                        }
                    }
                    try {
                        readFiles(thingTypePath.toString());
                    } catch (JsonSyntaxException | IOException e) {
                        logger.debug("Exception re-reading files: {}", e.getMessage(), e);
                    }
                }
            } else if (resp.getHttpCode() == HttpStatus.NOT_MODIFIED_304) {
                logger.trace("Thing types has not changed for etag {}", thingTypesEtag);
            } else {
                throw resp.createException();
            }
        } finally {
            knownThingsLock.unlock();
        }
    }

    private Map.Entry<@Nullable Long, @Nullable SonyThingDefinition[]> getThingDefinition(String name,
            @Nullable Long lastModified) {
        Validate.notEmpty(name, "name cannot be empty");

        final String ifModifiedSince = WEBDATEPATTER
                .format(lastModified == null ? new Date(0) : new Date(lastModified));

        final HttpResponse fileResponse = transport.executeGet(apiThingTypes + "/" + name, RawHeader,
                new TransportOptionHeader(HttpHeader.IF_MODIFIED_SINCE, ifModifiedSince));

        if (fileResponse.getHttpCode() == HttpStatus.OK_200) {
            final String lastModifiedResponse = fileResponse.getResponseHeader(HttpHeader.LAST_MODIFIED.asString());
            long lastModifiedTime;
            try {
                lastModifiedTime = WEBDATEPATTER.parse(lastModifiedResponse).getTime();
            } catch (ParseException e) {
                lastModifiedTime = System.currentTimeMillis();
                logger.debug(
                        "Cannot parse the last modified response (and thus not setting the last modified attribute on the file): {} for {}",
                        lastModifiedResponse, name);
            }

            final String fileContents = fileResponse.getContent();

            final JsonElement def = gson.fromJson(fileContents, JsonElement.class);
            if (def.isJsonArray()) {
                return new AbstractMap.SimpleEntry<>(lastModifiedTime, gson.fromJson(def, SonyThingDefinition[].class));
            } else {
                return new AbstractMap.SimpleEntry<>(lastModifiedTime,
                        new SonyThingDefinition[] { gson.fromJson(def, SonyThingDefinition.class) });
            }
        } else if (fileResponse.getHttpCode() == HttpStatus.NOT_MODIFIED_304) {
            logger.debug("Definitions for {} were not modified from {}", name, lastModified);
        } else {
            logger.debug("Error getting definitions for {} from {}: {}", name, lastModified,
                    fileResponse.getHttpCode());
        }

        // grr - can't return null here. @Nullable can't be used on Map.Entry so we
        // return this instead
        return new AbstractMap.SimpleEntry<>(null, null);
    }

    private void writeThingDefinition(String name, long lastModifiedTime, SonyThingDefinition[] ttds)
            throws IOException {
        Validate.notEmpty(name, "name cannot be empty");
        Objects.requireNonNull(ttds, "ttds cannot be null");
        if (ttds.length == 0) {
            throw new IllegalArgumentException("ttds cannot be empty");
        }

        final File theFile = thingTypePath.resolve(name).toFile();

        if (theFile.exists()) {
            logger.trace("Deleting local file {}", theFile);
            theFile.delete();
        }

        final String fileContents = gson.toJson(ttds);
        logger.debug("Writing new file ({}) found on github: {}", name, fileContents);
        FileUtils.write(theFile, fileContents, false);

        logger.debug("Setting last modified on file ({}) to: {}", name, lastModifiedTime);
        theFile.setLastModified(lastModifiedTime);

        addThingDefinitions(name, ttds);
    }

    @Override
    public void addListener(String modelName, ThingTypeUID currentThingTypeUID, SonyProviderListener listener) {
        super.addListener(modelName, currentThingTypeUID, listener);

        final String serviceName = SonyUtil.getServiceName(currentThingTypeUID);

        try {
            knownThingsLock.lock();

            // Find out if we know this model name yet...
            final @Nullable String fileName = knownThingTypes.entrySet().stream()
                    .flatMap(e -> e.getValue().stream().map(f -> new AbstractMap.SimpleEntry<>(e.getKey(), f)))
                    .filter(e -> SonyUtil.isModelMatch(e.getValue().getKey(), e.getValue().getValue(), serviceName, modelName)).map(e -> e.getKey())
                    .filter(e -> e != null).findAny().orElse(null);

            // If we found the file name
            // ... it exists - do nothing (super.addListener should have done it)
            // ... it doesn't exist - get the definition and write it
            // If not found
            // ... add it to the waiting list
            if (fileName != null && StringUtils.isNotEmpty(fileName)) {
                final File theFile = thingTypePath.resolve(fileName).toFile();
                if (!theFile.exists()) {
                    try {
                        final Map.Entry<@Nullable Long, @Nullable SonyThingDefinition[]> defs = getThingDefinition(
                                fileName, null);
                        if (defs != null && defs.getKey() != null && defs.getValue() != null) {
                            writeThingDefinition(fileName, defs.getKey(), defs.getValue());
                        }
                    } catch (IOException e) {
                        logger.debug("Exception reading device capabilities from github: {}", fileName);
                    }
                }
            } else {
                waitingModelNames.add(new AbstractMap.SimpleEntry<>(serviceName, modelName));
            }
        } finally {
            knownThingsLock.unlock();
        }
    }

    @Override
    public void close() {
        SonyUtil.cancel(watcher.getAndSet(null));
    }

    class MetaInfo {
        private @Nullable List<@Nullable String> ignore = new ArrayList<>();
        private @Nullable List<@Nullable MetaConvert> convert = new ArrayList<>();

        public boolean isIgnored(String modelName) {
            Validate.notEmpty(modelName, "modelName cannot be empty");
            return ignore != null && ignore.stream().anyMatch(s -> StringUtils.equalsIgnoreCase(s, modelName));
        }

        public String getModelName(String modelName) {
            Validate.notEmpty(modelName, "modelName cannot be empty");
            if (convert == null) {
                return modelName;
            }
            return convert.stream()
                    .filter(s -> StringUtils.equalsIgnoreCase(s.modelName, modelName)
                            && StringUtils.isNotBlank(s.convName))
                    .map(s -> s.convName).filter(s -> s != null).findAny().orElse(modelName);
        }
    }

    class MetaConvert {
        private @Nullable String modelName;
        private @Nullable String convName;

        public @Nullable String getModelName() {
            return modelName;
        }

        public @Nullable String getConvName() {
            return convName;
        }
    }

    class ServiceIssue implements SonyMatcher {
        private String serviceName;
        private String version;

        public ServiceIssue(String serviceName, String version) {
            Validate.notEmpty(serviceName, "serviceName cannot be empty");
            Validate.notEmpty(version, "version cannot be empty");

            this.serviceName = serviceName;
            this.version = version;
        }

        @Override
        public boolean matches(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || !(obj instanceof ServiceIssue)) {
                return false;
            }

            final ServiceIssue other = (ServiceIssue) obj;
            return StringUtils.equalsIgnoreCase(serviceName, other.serviceName)
                    && StringUtils.equalsIgnoreCase(version, other.version);
        }
    }
}
