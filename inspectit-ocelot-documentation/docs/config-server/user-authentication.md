---
id: user-authentication
title: User Authentication
---

The *inspectIT Ocelot Configuration Server* provides multiple options to do a user authentication.
By default, the local user authentication is used.

## Local User Authentication

By default, the configuration server uses an internal user management.
When using this authentication mode, users and their credentials will be managed by the configuration server itself.

Administrators can add and modify users via the provided web UI.

In order to use the local user authentication the `inspectit-config-server.security.ldap-authentication` property must be set to `false`.

```YAML
inspectit-config-server:
    security:
        ldap-authentication: false
```

## LDAP User Authentication

The configuration server also supports user authentication via LDAP.
When using LDAP, users which are added using the *local user authentication* mode will not be available.
Furthermore, the HTTP APIs which are related to the user management (`/api/v1/users`, `/api/v1/account/password`) cannot be used.

In order to use LDAP for user authentication the `inspectit-config-server.security.ldap-authentication` property must be set to `true`.

The LDAP related configuration properties have to be specified using the property `inspectit-config-server.security.ldap`.

###Access Control

By default, there are four different access roles provided: read, write, commit and admin. These access roles are hierarchical.
A user with write access also has read access, a user with commit access also has both read and write access and so on.

####Roles
read-role: may only read files on the server.

write-role: may read and edit files on the server.

commit-role: may commit new file versions (functions not implemented yet).

admin-role: may read, write and commit files. Can also edit user accounts.

####Configuration

In order to map an access role to a ldap group, you have to add the name of the ldap group in the respective list in 
`inspectit-config-server.security.ldap.roles`. In the example below, only the ldap group "SHIP_CREW" has admin access.
All other ldap groups of this ldap server have no access to the configuration server. 


The following configuration snippet shows an example LDAP configuration (this configuration was created for [this](https://github.com/rroemhild/docker-test-openldap) LDAP server).

```YAML
inspectit-config-server:
    security:
        ldap-authentication: true
        ldap:
            admin-group: "SHIP_CREW"
            url: "ldap://localhost:389/"
            base-dn: ""
            manager-dn: "cn=admin,dc=planetexpress,dc=com"
            manager-password: "GoodNewsEveryone"
            user-search-base: "ou=people,dc=planetexpress,dc=com"
            user-search-filter: "(uid={0})"
            group-search-base: "ou=people,dc=planetexpress,dc=com"
            group-search-filter: "(member={0})"
            roles:
              read:
                -
              write:
                -
              commit:
                -
              admin:
                - SHIP_CREW
```

### LDAP Configuration Properties

The following tables contains the required LDAP configuration properties including a description for each property.
Each property is located below the property `inspectit-config-server.security.ldap`.

| Property | Note |
| --- | --- |
| `admin-group` | The group to which a user must be assigned to gain administrator access. |
| `url` | Url of the LDAP server. |
| `base-dn` | Set the base suffix from which all operations should origin. |
| `manager-dn` | Set the user distinguished name (principal) to use for getting authenticated contexts. |
| `manager-password` | Set the password (credentials) to use for getting authenticated contexts. |
| `user-search-base` | Search base for user searches. |
| `user-search-filter` | The LDAP filter used to search for users. For example `(uid={0})`. |
| `group-search-base` | The search base for group membership searches. |
| `group-search-filter` | The LDAP filter to search for groups. |
| `roles` | The mapping from LDAP-Groups to internal access roles. |

## Access Log

By default, the server will log all authorized and unauthorized access attempts to secured endpoints.
Access logging can be disabled using the following property:

```YAML
inspectit-config-server:
    security:
        access-log: false
```
