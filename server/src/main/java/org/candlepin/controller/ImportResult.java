/**
 * Copyright (c) 2009 - 2016 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.controller;

import org.candlepin.model.Persisted;

import java.util.HashMap;
import java.util.Map;



/**
 * The ImportResult class contains references to the entities which were processed by a
 * controller-level import operation.
 *
 * @param <E>
 *  The entity class contained by a given import result instance
 */
public class ImportResult<E extends Persisted> {

    private final Map<String, E> skippedEntities;
    private final Map<String, E> createdEntities;
    private final Map<String, E> updatedEntities;
    private final Map<String, E> deletedEntities;
    private Map<String, E> importedEntities;
    private Map<String, E> changedEntities;

    /**
     * Instantiates a new, empty ImportResult instance.
     */
    public ImportResult() {
        this.skippedEntities = new HashMap<String, E>();
        this.createdEntities = new HashMap<String, E>();
        this.updatedEntities = new HashMap<String, E>();
        this.deletedEntities = new HashMap<String, E>();
        this.importedEntities = null;
        this.changedEntities = null;
    }

    /**
     * Retrieves a map containing the skipped entities. The entities will be mapped by their Red
     * Hat ID.
     *
     * @return
     *  A map containing all skipped entities
     */
    public Map<String, E> getSkippedEntities() {
        return this.skippedEntities;
    }

    /**
     * Retrieves a map containing the created entities. The entities will be mapped by their Red
     * Hat ID.
     *
     * @return
     *  A map containing all created entities
     */
    public Map<String, E> getCreatedEntities() {
        return this.createdEntities;
    }

    /**
     * Retrieves a map containing the updated entities. The entities will be mapped by their Red
     * Hat ID.
     *
     * @return
     *  A map containing all updated entities
     */
    public Map<String, E> getUpdatedEntities() {
        return this.updatedEntities;
    }

    /**
     * Retrieves a map containing the deleted entities. The entities will be mapped by their Red
     * Hat ID.
     *
     * @return
     *  A map containing all deleted entities
     */
    public Map<String, E> getDeletedEntities() {
        return this.deletedEntities;
    }

    /**
     * Retrieves a map containing the imported entities. The entities will be mapped by their Red
     * Hat ID.
     *
     * @return
     *  A map containing all imported entities
     */
    public Map<String, E> getImportedEntities() {
        if (this.importedEntities == null) {
            this.importedEntities = new HashMap<String, E>();

            this.importedEntities.putAll(this.skippedEntities);
            this.importedEntities.putAll(this.createdEntities);
            this.importedEntities.putAll(this.updatedEntities);
        }

        return this.importedEntities;
    }

    /**
     * Retrieves a map containing the entities changed ( Created, updated or deleted).
     * The entities will be mapped by their Red Hat ID.
     *
     * @return
     *  A map containing all imported entities
     */
    public Map<String, E> getChangedEntities() {
        if (this.changedEntities == null) {
            this.changedEntities = new HashMap<String, E>();

            this.changedEntities.putAll(this.createdEntities);
            this.changedEntities.putAll(this.updatedEntities);
            this.changedEntities.putAll(this.deletedEntities);
        }

        return this.changedEntities;
    }
}
