# Instagram API Integration - Spring Boot

A production-ready Spring Boot application for integrating with the Instagram Graph API. This project provides REST endpoints for OAuth account linking, data fetching, and token management.

## Features

- **OAuth 2.0 Integration**: Secure Facebook/Instagram OAuth flow with state management
- **Profile & Media Fetching**: Retrieve user profile, posts, reels, and engagement metrics
- **Token Management**: Automatic token refresh and long-lived token exchange
- **Data Persistence**: PostgreSQL storage with JPA entities
- **Error Handling**: Comprehensive exception handling with meaningful error responses

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (or H2 for development)
- Meta Developer Account with Instagram Graph API access

## Quick Start

### 1. Clone and Configure

```bash
git clone https://github.com/anirudhatalmale9/instagram-api-integration.git
cd instagram-api-integration
```

### 2. Set Environment Variables

Create a `.env` file or set these environment variables:

```bash
# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/instagram_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password

# Instagram/Facebook OAuth
INSTAGRAM_CLIENT_ID=your_facebook_app_id
INSTAGRAM_CLIENT_SECRET=your_facebook_app_secret
INSTAGRAM_REDIRECT_URI=http://localhost:8080/api/instagram/callback

# Optional
SERVER_PORT=8080
```

### 3. Create PostgreSQL Database

```sql
CREATE DATABASE instagram_db;
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Meta Developer Setup

### Creating a Facebook App for Instagram API

1. Go to [Meta for Developers](https://developers.facebook.com/)
2. Create a new app → Select "Business" type
3. Add the **Instagram Graph API** product
4. Add the **Facebook Login** product
5. Configure OAuth settings:
   - Valid OAuth Redirect URIs: `http://localhost:8080/api/instagram/callback`
   - Deauthorize Callback URL (optional)

### Required Permissions/Scopes

- `instagram_basic` - Basic profile info
- `instagram_content_publish` - Publish content
- `instagram_manage_insights` - Access insights
- `pages_show_list` - List Facebook Pages
- `pages_read_engagement` - Read engagement data

### Important Notes

- Instagram account must be a **Business** or **Creator** account
- Instagram account must be connected to a **Facebook Page**
- The Facebook Page must be linked to your Meta Business account

## API Endpoints

### 1. Link Account (Initiate OAuth)

**Endpoint**: `GET /api/instagram/link`

Initiates the OAuth flow. Redirect the user to the returned authorization URL.

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| userId | String | Yes | Your application's user identifier |

**Request**:
```bash
curl -X GET "http://localhost:8080/api/instagram/link?userId=user123"
```

**Response**:
```json
{
  "success": true,
  "message": "Redirect user to the authorization URL to complete linking",
  "data": {
    "authorizationUrl": "https://www.facebook.com/v18.0/dialog/oauth?client_id=...",
    "state": "550e8400-e29b-41d4-a716-446655440000"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

### 2. OAuth Callback

**Endpoint**: `GET /api/instagram/callback`

Handles the OAuth callback from Facebook/Instagram. This is called automatically after user authorization.

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| code | String | Yes | Authorization code from OAuth |
| state | String | Yes | State parameter for CSRF protection |

**Response**:
```json
{
  "success": true,
  "message": "Instagram account linked successfully",
  "data": {
    "userId": "user123",
    "instagramUserId": "17841400000000000",
    "username": "yourinstagram",
    "name": "Your Name",
    "profilePictureUrl": "https://...",
    "tokenExpiresAt": "2024-03-15T10:30:00"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

### 3. Fetch Data

**Endpoint**: `GET /api/instagram/data`

Fetches user profile and media (posts/reels) from Instagram.

**Parameters**:
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| userId | String | Yes | - | Your application's user identifier |
| mediaLimit | Integer | No | 25 | Number of media items to fetch (max 100) |

**Request**:
```bash
curl -X GET "http://localhost:8080/api/instagram/data?userId=user123&mediaLimit=10"
```

**Response**:
```json
{
  "success": true,
  "message": "Instagram data fetched and stored successfully",
  "data": {
    "profile": {
      "id": "17841400000000000",
      "username": "yourinstagram",
      "name": "Your Name",
      "profilePictureUrl": "https://...",
      "biography": "Your bio here",
      "website": "https://yourwebsite.com",
      "followersCount": 1500,
      "followingCount": 300,
      "mediaCount": 45,
      "accountType": "BUSINESS"
    },
    "media": [
      {
        "id": "17900000000000000",
        "mediaType": "IMAGE",
        "mediaUrl": "https://...",
        "permalink": "https://www.instagram.com/p/...",
        "caption": "Post caption here",
        "timestamp": "2024-01-10T15:30:00+0000",
        "likeCount": 150,
        "commentsCount": 12
      }
    ],
    "paging": {
      "hasMore": true
    }
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

### 4. Refresh Token

**Endpoint**: `POST /api/instagram/refresh`

Refreshes an expired or expiring access token.

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| userId | String | Yes | Your application's user identifier |

**Request**:
```bash
curl -X POST "http://localhost:8080/api/instagram/refresh?userId=user123"
```

**Response**:
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "userId": "user123",
    "username": "yourinstagram",
    "tokenExpiresAt": "2024-03-15T10:30:00"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

### 5. Unlink Account

**Endpoint**: `DELETE /api/instagram/unlink`

Revokes access and optionally deletes stored data.

**Parameters**:
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| userId | String | Yes | - | Your application's user identifier |
| deleteData | Boolean | No | false | If true, deletes all stored data |

**Request**:
```bash
curl -X DELETE "http://localhost:8080/api/instagram/unlink?userId=user123&deleteData=true"
```

**Response**:
```json
{
  "success": true,
  "message": "Instagram account unlinked and all data deleted",
  "data": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

### 6. Check Link Status

**Endpoint**: `GET /api/instagram/status`

Checks if an Instagram account is linked for a user.

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| userId | String | Yes | Your application's user identifier |

**Request**:
```bash
curl -X GET "http://localhost:8080/api/instagram/status?userId=user123"
```

**Response**:
```json
{
  "success": true,
  "message": "Instagram account is linked",
  "data": true,
  "timestamp": "2024-01-15T10:30:00"
}
```

## Error Responses

All error responses follow this format:

```json
{
  "success": false,
  "message": "Error description here",
  "data": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

### Common Error Codes

| HTTP Status | Description |
|-------------|-------------|
| 400 | Bad Request - Invalid parameters or state |
| 404 | Not Found - Account not found |
| 500 | Internal Server Error - Instagram API error or unexpected failure |

## Database Schema

### instagram_accounts
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| user_id | VARCHAR | Your app's user ID |
| instagram_user_id | VARCHAR | Instagram user ID |
| instagram_business_account_id | VARCHAR | Business account ID |
| username | VARCHAR | Instagram username |
| name | VARCHAR | Display name |
| access_token | VARCHAR | Encrypted access token |
| token_expires_at | TIMESTAMP | Token expiration |
| followers_count | INTEGER | Follower count |
| following_count | INTEGER | Following count |
| media_count | INTEGER | Total media count |
| is_active | BOOLEAN | Account active status |

### instagram_media
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| media_id | VARCHAR | Instagram media ID |
| account_id | BIGINT | Foreign key to accounts |
| media_type | VARCHAR | IMAGE, VIDEO, CAROUSEL_ALBUM |
| media_url | VARCHAR | Media URL |
| caption | TEXT | Post caption |
| like_count | INTEGER | Number of likes |
| comments_count | INTEGER | Number of comments |

## Running Tests

```bash
mvn test
```

## API Limitations

Please note the following Instagram Graph API limitations:

- **Followers/Following Lists**: The API does NOT provide access to individual follower/following lists for privacy reasons. Only counts are available.
- **Rate Limits**: Respect Instagram's rate limits (typically 200 calls/user/hour)
- **Business/Creator Only**: Only works with Business or Creator accounts
- **Facebook Page Required**: Account must be linked to a Facebook Page

## Project Structure

```
src/
├── main/
│   ├── java/com/instagram/api/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── entity/          # JPA entities
│   │   ├── exception/       # Custom exceptions
│   │   ├── repository/      # Data repositories
│   │   └── service/         # Business logic
│   └── resources/
│       └── application.yml  # App configuration
└── test/                    # Unit tests
```

## License

MIT License
