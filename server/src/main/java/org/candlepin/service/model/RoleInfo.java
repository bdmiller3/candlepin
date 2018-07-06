/**
 * Copyright (c) 2009 - 2018 Red Hat, Inc.
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
package org.candlepin.service.model;

import java.util.Collection;



/**
 * The RoleInfo represents a minimal set of role information used by the service adapters.
 *
 * Data which is not set or does not change should be represented by null values. To explicitly
 * clear a value, an empty string or non-null "empty" value should be used instead.
 */
public interface RoleInfo {

    /**
     * Fetches the ID of this role. If the ID has not yet been set, this method returns null.
     *
     * @return
     *  The ID of this role, or null if the ID has not been set
     */
    String getId();

    /**
     * Fetches the name of this role. If the name has not yet been set, this method returns null.
     *
     * @return
     *  The name of this role, or null if the name has not been set
     */
    String getName();

    /**
     * Fetches the users currently assigned to this role. If the users have not yet been set, this
     * method returns null. If this role is not currently assigned to any users, this method returns
     * an empty collection.
     *
     * @return
     *  The users assigned to this role, or null if the users have not been set
     */
    Collection<? extends UserInfo> getUsers();

    /**
     * Fetches the permissions currently provided by this role. If the permissions have not yet been
     * set, this method returns null. If this role currently does not provide any permissions, this
     * method returns an empty collection.
     *
     * @return
     *  The permissions provided by this role, or null if the permissions have not been set
     */
    Collection<? extends PermissionInfo> getPermissions();

}