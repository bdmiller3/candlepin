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
package org.candlepin.service.impl;

import org.candlepin.auth.Access;
import org.candlepin.auth.SubResource;
import org.candlepin.auth.permissions.PermissionFactory.PermissionType;
import org.candlepin.model.PermissionBlueprint;
import org.candlepin.model.Owner;
import org.candlepin.model.OwnerCurator;
import org.candlepin.model.PermissionBlueprint;
import org.candlepin.model.PermissionBlueprintCurator;
import org.candlepin.model.Role;
import org.candlepin.model.RoleCurator;
import org.candlepin.model.User;
import org.candlepin.model.UserCurator;
import org.candlepin.service.UserServiceAdapter;
import org.candlepin.service.model.OwnerInfo;
import org.candlepin.service.model.PermissionInfo;
import org.candlepin.service.model.OwnerInfo;
import org.candlepin.service.model.RoleInfo;
import org.candlepin.service.model.UserInfo;
import org.candlepin.util.Util;

import com.google.inject.Inject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;



/**
 * A {@link UserServiceAdapter} implementation backed by a {@link UserCurator}
 * for user creation and persistence.
 */
public class DefaultUserServiceAdapter implements UserServiceAdapter {

    private UserCurator userCurator;
    private RoleCurator roleCurator;
    private PermissionBlueprintCurator permissionCurator;
    private OwnerCurator ownerCurator;

    @Inject
    public DefaultUserServiceAdapter(UserCurator userCurator, RoleCurator roleCurator,
        PermissionBlueprintCurator permissionCurator, OwnerCurator ownerCurator) {

        this.userCurator = userCurator;
        this.roleCurator = roleCurator;
        this.permissionCurator = permissionCurator;
        this.ownerCurator = ownerCurator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserInfo createUser(UserInfo user) {
        if (user == null) {
            throw new IllegalArgumentException("user is null");
        }

        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username is null or empty");
        }

        if (this.userCurator.findByLogin(user.getUsername()) != null) {
            throw new IllegalStateException("User already exists: " + user.getUsername());
        }

        User entity = new User();

        entity.setUsername(user.getUsername());
        entity.setHashedPassword(user.getHashedPassword());
        entity.setSuperAdmin(user.isSuperAdmin() != null ? user.isSuperAdmin() : false);

        // Convert roles
        if (user.getRoles() != null) {
            for (RoleInfo role : user.getRoles()) {
                // If this ends up being a bottleneck, we can optimize this a tad by bulking this lookup
                Role roleEntity = this.roleCurator.getByName(role.getName());

                if (roleEntity == null) {
                    throw new IllegalStateException("Role does not exist: " + role.getName());
                }

                entity.addRole(roleEntity);
            }
        }

        entity = this.userCurator.create(entity);

        return entity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserInfo updateUser(UserInfo user) {
        if (user == null) {
            throw new IllegalArgumentException("user is null");
        }

        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username is null or empty");
        }

        User entity = this.userCurator.findByLogin(user.getUsername());
        if (entity == null) {
            throw new IllegalStateException("User does not exist: " + user.getUsername());
        }

        // Check if the inbound entity is not the same instance we would update here. If it is,
        // we have nothing to do, so we'll just skip everything.
        if (entity != user) {
            Set<Role> roles = null;

            // Convert roles
            if (user.getRoles() != null) {
                roles = new HashSet<>();

                for (RoleInfo role : user.getRoles()) {
                    // If this ends up being a bottleneck, we can optimize this a tad by bulking this lookup
                    Role roleEntity = this.roleCurator.getByName(role.getName());

                    if (roleEntity == null) {
                        throw new IllegalStateException("Role does not exist: " + role.getName());
                    }

                    roles.add(roleEntity);
                }
            }

            // If our sub-objects validated, set the rest of the properties now
            if (roles != null) {
                entity.clearRoles();
                for (Role role : roles) {
                    entity.addRole(role);
                }
            }

            // No reason to change the username, since we fetched the entity *by* its username.
            if (user.getHashedPassword() != null) {
                entity.setHashedPassword(user.getHashedPassword());
            }

            if (user.isSuperAdmin() != null) {
                entity.setSuperAdmin(user.isSuperAdmin());
            }
        }

        return this.userCurator.merge(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends UserInfo> listUsers() {
        return this.userCurator.listAll().list();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateUser(String username, String password) {
        User user = this.userCurator.findByLogin(username);
        String hashedPassword = Util.hash(password);

        if (user != null && password != null && hashedPassword != null) {
            return hashedPassword.equals(user.getHashedPassword());
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteUser(String username) {
        User entity = this.userCurator.findByLogin(username);

        if (entity != null) {
            entity.clearRoles();
            this.userCurator.delete(entity);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserInfo findByLogin(String login) {
        return userCurator.findByLogin(login);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends OwnerInfo> getAccessibleOwners(String username) {
        if (username == null) {
            throw new IllegalArgumentException("username is null");
        }

        User entity = this.userCurator.findByLogin(username);
        if (entity == null) {
            throw new IllegalStateException("User does not exist: " + username);
        }

        return entity.isSuperAdmin() != null && entity.isSuperAdmin() ?
            this.ownerCurator.listAll().list() :
            entity.getOwners(SubResource.CONSUMERS, Access.CREATE);
    }

    /**
     * Resolves the owner represented by a given OwnerInfo instance. If the owner cannot be
     * resolved, this method throws an exception.
     *
     * @throws IllegalStateException
     *  if the provided OwnerInfo does not represent a valid owner
     *
     * @return
     *  The Owner instance represented by the provided OwnerInfo
     */
    private Owner resolveOwnerInfo(OwnerInfo ownerInfo) {
        if (ownerInfo != null) {
            Owner owner = this.ownerCurator.getByKey(ownerInfo.getKey());

            if (owner == null) {
                throw new IllegalStateException("No such owner: " + ownerInfo.getKey());
            }

            return owner;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoleInfo createRole(RoleInfo role) {
        if (role == null) {
            throw new IllegalArgumentException("role is null");
        }

        if (role.getName() == null || role.getName().isEmpty()) {
            throw new IllegalArgumentException("Role name is null or empty");
        }

        if (this.roleCurator.getByName(role.getName()) != null) {
            throw new IllegalStateException("Role already exists: " + role.getName());
        }

        Role entity = new Role();

        if (role.getUsers() != null) {
            for (UserInfo user : role.getUsers()) {
                User userEntity = this.userCurator.findByLogin(user.getUsername());

                if (userEntity == null) {
                    throw new IllegalStateException("User does not exist: " + user.getUsername());
                }

                entity.addUser(userEntity);
            }
        }

        if (role.getPermissions() != null) {
            for (PermissionInfo permission : role.getPermissions()) {
                PermissionBlueprint pentity = new PermissionBlueprint(null, null, null);

                if (permission.getOwner() == null) {
                    throw new IllegalArgumentException("Permission does not define an owner: " + permission);
                }

                pentity.setOwner(this.resolveOwnerInfo(permission.getOwner()));
                pentity.setType(PermissionType.valueOf(permission.getTypeName()));
                pentity.setAccess(Access.valueOf(permission.getAccessName()));

                entity.addPermission(pentity);
            }
        }

        return this.roleCurator.create(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoleInfo updateRole(RoleInfo role) {
        if (role == null) {
            throw new IllegalArgumentException("role is null");
        }

        if (role.getName() == null || role.getName().isEmpty()) {
            throw new IllegalArgumentException("Role name is null or empty");
        }

        Role entity = this.roleCurator.getByName(role.getName());
        if (entity == null) {
            throw new IllegalStateException("Role does not exist: " + role.getName());
        }

        // Check if the inbound entity is not the same instance we would update here. If it is,
        // we have nothing to do, so we'll just skip everything.
        if (entity != role) {
            Set<User> users = null;
            Set<PermissionBlueprint> permissions = null;

            if (role.getUsers() != null) {
                users = new HashSet<>();

                for (UserInfo user : role.getUsers()) {
                    User userEntity = this.userCurator.findByLogin(user.getUsername());

                    if (userEntity == null) {
                        throw new IllegalStateException("User does not exist: " + user.getUsername());
                    }

                    users.add(userEntity);
                }
            }

            if (role.getPermissions() != null) {
                permissions = new HashSet<>();

                for (PermissionInfo permission : role.getPermissions()) {
                    PermissionBlueprint pentity = new PermissionBlueprint(null, null, null);

                    if (permission.getOwner() == null) {
                        throw new IllegalArgumentException("Permission does not define an owner: " +
                            permission);
                    }

                    pentity.setOwner(this.resolveOwnerInfo(permission.getOwner()));
                    pentity.setType(PermissionType.valueOf(permission.getTypeName()));
                    pentity.setAccess(Access.valueOf(permission.getAccessName()));

                    permissions.add(pentity);
                }
            }

            // If everything validated, update the entity now:
            if (users != null) {
                entity.clearUsers();
                for (User user : users) {
                    entity.addUser(user);
                }
            }

            if (permissions != null) {
                entity.clearPermissions();
                for (PermissionBlueprint permission : permissions) {
                    entity.addPermission(permission);
                }

                // Impl note: Orphan removal should handle the cleanup of the old permissions for us
            }
        }

        return this.roleCurator.merge(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoleInfo addUserToRole(String roleName, String username) {
        Role roleEntity = this.roleCurator.get(roleName);
        if (roleEntity == null) {
            throw new IllegalStateException("Role does not exist: " + roleName);
        }

        User userEntity = this.userCurator.findByLogin(username);
        if (userEntity == null) {
            throw new IllegalStateException("User does not exist: " + username);
        }

        roleEntity.addUser(userEntity);
        return this.roleCurator.merge(roleEntity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoleInfo removeUserFromRole(String roleName, String username) {
        Role roleEntity = this.roleCurator.get(roleName);
        if (roleEntity == null) {
            throw new IllegalStateException("Role does not exist: " + roleName);
        }

        User userEntity = this.userCurator.findByLogin(username);
        if (userEntity == null) {
            throw new IllegalStateException("User does not exist: " + username);
        }

        roleEntity.removeUser(userEntity);
        return this.roleCurator.merge(roleEntity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoleInfo addPermissionToRole(String roleName, PermissionInfo permission) {
        Role roleEntity = this.roleCurator.get(roleName);
        if (roleEntity == null) {
            throw new IllegalStateException("Role does not exist: " + roleName);
        }

        PermissionBlueprint pentity = new PermissionBlueprint(null, null, null);

        if (permission.getOwner() == null) {
            throw new IllegalArgumentException("Permission does not define an owner: " +
                permission);
        }

        pentity.setOwner(this.resolveOwnerInfo(permission.getOwner()));
        pentity.setType(PermissionType.valueOf(permission.getTypeName()));
        pentity.setAccess(Access.valueOf(permission.getAccessName()));

        roleEntity.addPermission(pentity);

        return this.roleCurator.merge(roleEntity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoleInfo removePermissionFromRole(String roleName, String permissionId) {
        Role roleEntity = this.roleCurator.get(roleName);
        if (roleEntity == null) {
            throw new IllegalStateException("Role does not exist: " + roleName);
        }

        if (permissionId == null) {
            throw new IllegalArgumentException("permissionId is null");
        }

        if (roleEntity.getPermissions() != null) {
            boolean removed = false;

            Set<PermissionBlueprint> permissions = roleEntity.getPermissions();
            Iterator<PermissionBlueprint> iterator = permissions.iterator();

            while (iterator.hasNext()) {
                PermissionBlueprint permission = iterator.next();

                if (permissionId.equals(permission.getId())) {
                    iterator.remove();

                    permission.setRole(null);
                    this.permissionCurator.delete(permission);

                    removed = true;
                }
            }

            if (removed) {
                roleEntity.setPermissions(permissions);
                roleEntity = this.roleCurator.merge(roleEntity);
            }
        }

        return roleEntity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteRole(String role) {
        Role entity = this.roleCurator.getByName(role);

        if (entity != null) {
            entity.clearUsers();
            this.roleCurator.delete(entity);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoleInfo getRole(String roleName) {
        return this.roleCurator.getByName(roleName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends RoleInfo> listRoles() {
        return this.roleCurator.listAll().list();
    }
}
