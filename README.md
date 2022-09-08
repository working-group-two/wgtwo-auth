# Auth SDK - api.wgtwo.com

See: https://docs.wgtwo.com/

## Endpoints
| Endpoint               | URI                                         | Credentials |
|------------------------|---------------------------------------------|-------------|
| Authorization endpoint | https://id.wgtwo.com/oauth2/auth            | public      |
| Token endpoint         | https://id.wgtwo.com/oauth2/token           | basic auth  |
| User info endpoint     | https://id.wgtwo.com/userinfo               | token       |
| Log-out endpoint       | https://id.wgtwo.com/oauth2/sessions/logout | public      |
| Revoke endpoint        | https://id.wgtwo.com/oauth2/revoke          | basic auth  |

The token and revoke endpoints are protected using basic auth,
where client ID must be provided as the username and client secret as the password.

## Grant types supported
- Authorization Code
- Client Credentials

## Usage
[https://api.wgtwo.com](https://api.wgtwo.com) expects the access token as a Bearer credential in the HTTP Authorization headers.

```
Authorization: Bearer {access token}
```

## SDKs
- [Java / Kotlin](/java)
