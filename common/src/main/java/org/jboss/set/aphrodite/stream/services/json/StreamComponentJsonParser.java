package org.jboss.set.aphrodite.stream.services.json;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.domain.Codebase;
import org.jboss.set.aphrodite.domain.RepositoryType;
import org.jboss.set.aphrodite.domain.StreamComponent;

public class StreamComponentJsonParser {
    private static final Log LOG = LogFactory.getLog(StreamComponentJsonParser.class);
    public static final String JSON_NAME = "component_name";
    public static final String JSON_CONTACTS = "contacts";
    public static final String JSON_REPOSITORY_TYPE = "repository_type";
    public static final String JSON_REPOSITORY_URL = "repository_url";
    public static final String JSON_CODEBASE = "codebase";
    public static final String JSON_TAG = "tag";
    public static final String JSON_VERSION = "version";
    public static final String JSON_GAV = "gav";
    public static final String JSON_COMMENT = "comment";

    private StreamComponentJsonParser() {
    }

    public static StreamComponent parse(JsonObject json) {
        try {
            final String name = json.getString(JSON_NAME);
            final List<String> contacts = getContacts(json);
            final RepositoryType repositoryType = RepositoryType.fromType(json.getString(JSON_REPOSITORY_TYPE));
            String repository = json.getString(JSON_REPOSITORY_URL);
            URI repositoryURI = null;
            if (repository != null) {
                // TODO: retain fix from chao for now, check if we need it at all ?
                if (!repository.endsWith("/")) {
                    repository = repository + "/";
                }
                repositoryURI = new URI(repository);
            }
            final Codebase codeBase = new Codebase(json.getString(JSON_CODEBASE));
            final String tag = json.getString(JSON_TAG);
            final String version = json.getString(JSON_VERSION);
            final String gav = json.getString(JSON_GAV);
            final String comment = json.getString(JSON_COMMENT);
            return new StreamComponent(name, contacts, repositoryType, repositoryURI, codeBase, tag, version, gav, comment);
        } catch (Exception e) {
            Utils.logException(LOG, e);
            return null;
        }
    }

    private static List<String> getContacts(JsonObject json) {
        final JsonArray contactsArray = json.getJsonArray(JSON_CONTACTS);
        final List<String> contacts = new ArrayList<>(contactsArray.size());
        for (int index = 0; index < contactsArray.size(); index++) {
            contacts.add(contactsArray.getString(index));
        }
        return contacts;
    }
}
