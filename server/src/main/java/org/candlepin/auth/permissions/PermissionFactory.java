/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
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
package org.candlepin.auth.permissions;

import org.candlepin.auth.Access;
import org.candlepin.service.model.PermissionInfo;
import org.candlepin.service.model.UserInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



/**
 * PermissionFactory: Creates concrete Java permission classes based on the provided permission info
 */
public class PermissionFactory {
    private static Logger log = LoggerFactory.getLogger(PermissionFactory.class);

    /**
     * PermissionType: Key used to determine which class to create.
     */
    public enum PermissionType {
        OWNER,
        OWNER_POOLS,
        USERNAME_CONSUMERS,
        USERNAME_CONSUMERS_ENTITLEMENTS,
        ATTACH
    }

    public interface PermissionBuilder {
        Permission build(UserInfo user, PermissionInfo permission);
    }

    private static final Map<String, PermissionBuilder> BUILDERS;

    static {
        Map<String, PermissionBuilder> map = new HashMap<>();

        map.put(PermissionType.OWNER.name(),
            (user, perm) -> new OwnerPermission(perm.getOwner(), Access.valueOf(perm.getAccessLevel())));

        map.put(PermissionType.OWNER_POOLS.name(),
            (user, perm) -> new OwnerPoolsPermission(perm.getOwner()));

        map.put(PermissionType.USERNAME_CONSUMERS.name(),
            (user, perm) -> new UsernameConsumersPermission(user, perm.getOwner()));

        map.put(PermissionType.ATTACH.name(),
            (user, perm) -> new AttachPermission(perm.getOwner()));

        BUILDERS = Collections.unmodifiableMap(map);
    }

    public PermissionFactory() {
        // Intentionally left empty
    }

    /**
     * Converts the provided permission info into a concrete permission for the given user.
     *
     * @param user
     *  The user info of the user for which to create a permission
     *
     * @param permission
     *  The permission info to use to create the permission
     *
     * @return
     *  A concrete permission based on the provided user and permission info
     */
    public Permission createPermission(UserInfo user, PermissionInfo permission) {
        if (user == null) {
            throw new IllegalArgumentException("user is null");
        }

        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }

        PermissionBuilder builder = BUILDERS.get(permission.getTypeName());
        if (builder != null) {
            return builder.build(user, permission);
        }

        log.warn("Unsupported permission type: {}", permission.getTypeName());
        return null;
    }

    /**
     * Converts the provided permission info into concrete permissions for the given user.
     *
     * @param user
     *  The user info of the user for which to create a permission
     *
     * @param perms
     *  A collection of PermissionInfo instances to use to create the concrete permissions
     *
     * @throws IllegalArgumentException
     *  if user is null, if perms is null, or perms contains null elements
     *
     * @return
     *  A collection of concrete permissions based on the provided user and permission info
     */
    public Set<Permission> createPermissions(UserInfo user, Collection<? extends PermissionInfo> perms) {
        if (user == null) {
            throw new IllegalArgumentException("user is null");
        }

        Set<Permission> translated = null;

        if (perms != null) {
            translated = new HashSet<>();

            // Impl note: we don't call the singular create permission here because it's faster to
            // do the work ourselves, avoiding the method overhead, user re-check on every iteration
            // and checking the output of createPermission to filter nulls.
            // While in the general case the savings are immeasurable, we do this on *every
            // non-admin request* with a UserAuth provider, so the savings will add up real quick.
            for (PermissionInfo pinfo : perms) {
                if (pinfo != null) {
                    PermissionBuilder builder = BUILDERS.get(pinfo.getTypeName());

                    if (builder != null) {
                        translated.add(builder.build(user, pinfo));
                    }
                    else {
                        log.warn("Unsupported permission type: {}", pinfo.getTypeName());
                    }
                }
            }
        }

        return translated;
    }

}
