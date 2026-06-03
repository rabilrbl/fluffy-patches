# JioTV API Verification Results

## Test Date: 2026-05-11

## Verified Working Endpoints (200 without auth)

| Endpoint | Base URL | Method | Response |
|----------|----------|--------|----------|
| `/v2.0/carousel/get?tabid=0` | tv.media.jio.com/apis/ | GET | Carousel data with promotionalData array |
| `/v2.1/carousel/get?tabid=0` | tv.media.jio.com/apis/ | GET | AVOD carousel data |
| `/v2.2/carousel/get?tabid=0` | tv.media.jio.com/apis/ | GET | SVOD carousel data |
| `/v3.1/promotedsearch/get` | tv.media.jio.com/apis/ | GET | Promoted search data with curatedSearchData |
| `/v1.3/checkversion/checkversion?os=android&devicetype=phone&version=371` | tv.media.jio.com/apis/ | GET | `{code:200, message:"Success"}` |
| `/apis/v1.3/dictionary/dictionary` | jiotvapi.cdn.jio.com | GET | 395 string key-value pairs |
| `/misc/location/` | jiotvapi.media.jio.com | GET | Location data (city, state, country, ASN) |
| `/misc/jiocoupons/` | jiotvapi.media.jio.com | GET | Coupon data (currently empty) |
| `/userservice/apis/v1/loginotp/send` | jiotvapi.media.jio.com | POST | 204 on success |

## Verified Working Endpoints (200 with guest auth token)

Guest auth token obtained from `POST https://auth.media.jio.com/tokenservice/apis/v1/guest` with body `{"appName":"RJIL_JioTV","deviceId":"...","os":"android","deviceType":"phone"}`.
Guest token works as `accesstoken` header for v2.x endpoints. v3.x endpoints return "version is not supported" with guest token.

| Endpoint | Base URL | Method | Response |
|----------|----------|--------|----------|
| `/v2.0/home/get?page=0&tabid=0` | tv.media.jio.com/apis/ | GET | Home tab data with featuredNewData |
| `/v3.0/home/get?page=0&tabid=0` | tv.media.jio.com/apis/ | GET | Home data (same structure, v3.x) |
| `/v3.0/beginsession/begin` | tv.media.jio.com/apis/ | GET/POST | Session begin |

## Auth Token Endpoints

| Endpoint | Base URL | Method | Body | Response |
|----------|----------|--------|------|----------|
| `/v1/loginotp/send` | jiotvapi.media.jio.com/userservice/apis/ | POST | `{"number":"<base64>"}` | 204 |
| `/v2/loginotp/verify` | jiotvapi.media.jio.com/userservice/apis/ | POST | `{"number":"<base64>","otp":"<code>","deviceInfo":{...}}` | authToken, ssoToken, refreshToken |
| `/v1/guest` | auth.media.jio.com/tokenservice/apis/ | POST | `{"appName":"RJIL_JioTV","deviceId":"...","os":"android","deviceType":"phone"}` | guest authToken |
| `/v2/refreshtoken` | auth.media.jio.com/tokenservice/apis/ | POST | `{"appName":"RJIL_JioTV","deviceId":"...","os":"android","deviceType":"phone","uniqueId":"...","refreshToken":"..."}` | refreshed tokens |

## Valid Endpoints (400 = needs auth/params)

| Endpoint | Base URL | Method | Notes |
|----------|----------|--------|-------|
| `/v2.0/home/get?page=0&tabid=0` | tv.media.jio.com/apis/ | GET | Needs ssotoken |
| `/v1.9/search/search?query=cricket&type=tv` | tv.media.jio.com/apis/ | GET | Needs ssotoken |
| `/v2.1/search/search` | tv.media.jio.com/apis/ | GET | Needs ssotoken |
| `/v2.2/search/search` | tv.media.jio.com/apis/ | GET | Needs ssotoken |
| `/v1.9/search/searchauto` | tv.media.jio.com/apis/ | GET | Needs ssotoken |
| `/v1.8/cinema/watchlistget` | tv.media.jio.com/apis/ | GET | Needs ssotoken |
| `/v1.8/cinema/getmetadata` | tv.media.jio.com/apis/ | GET | Needs ssotoken |
| `/v1.8/getdata/movies` | tv.media.jio.com/apis/ | GET | Needs ssotoken |
| `/v1.4/foryousection/get` | tv.media.jio.com/apis/ | GET | Needs ssotoken |
| `/v1.6/getdata/featurednew` | tv.media.jio.com/apis/ | GET | Needs ssotoken |
| `/v1.7/getdata/featurednews` | tv.media.jio.com/apis/ | GET | Needs ssotoken |
| `/v1.7/getdata/featuredsports` | tv.media.jio.com/apis/ | GET | Needs ssotoken |
| `/v1.3/cdntoken/get` | tv.media.jio.com/apis/ | GET | Needs ssotoken |
| `/v1.3/subscription/getpackagelist` | tv.media.jio.com/apis/ | GET | Needs ssotoken |
| `/v2.0/loginotp/refresh` | tv.media.jio.com/apis/ | POST | Needs refreshToken |
| `/v2.0/tab/seeall?tabid=0` | tv.media.jio.com/apis/ | GET | Needs ssotoken |

## CDN Endpoints (504/204 = needs auth or correct params)

| Endpoint | Base URL | Method | Notes |
|----------|----------|--------|-------|
| `/apis/v1.4/vodcategorydata/get?id=&type=&collection_rail_name=&collection_rail_tile_name=&limit=&offset=` | jiotvapi.cdn.jio.com | GET | 504 |
| `/apis/v1.4/tag/get?id=&type=&limit=&offset=&tag=` | jiotvapi.cdn.jio.com | GET | 504 |
| `/apis/v1.4/vodmetadata/get?content_id=` | jiotvapi.cdn.jio.com | GET | 204 empty |

## Server Error (500)

| Endpoint | Base URL | Notes |
|----------|----------|-------|
| `/v1.4/getdata/promotional` | tv.media.jio.com/apis/ | Server error |

## Decommissioned/404 Endpoints

### tv.media.jio.com/apis/ (404)
- `/v1.4/recents/getrecents`
- `/v1.4/list/(get/add/deletecontent)`
- `/v1.5/getuserlist/get`
- `/v1.6/recordings/(add/delete)`
- `/gettrending/get`
- `/gettweets/get`
- `/v1.4/scorecard/get`
- `/v2.2/getchannelurl/getchannelurl`
- `/v2.0/getchannelurl/getchannelurl`
- `/beginsession/begin`
- `/getMobileChannelList/get`
- `/getconfiguration`
- `/v1.4/userlanguage/set`
- `/v2.0/tab/categoryseeall`

### jiotvapi.media.jio.com/ (ALL v3.0+ endpoints 404)
All v3.0+ endpoints return 404, suggesting they use a different routing or are decommissioned:
- `/v3.0/home/get`, `/v3.0/getMobileChannelList/get`, `/v3.0/tab/*`
- `/v3.0/list/*`, `/v3.0/recordings/*`, `/v3.0/beginsession/begin`
- `/v3.1/promotedsearch/get`
- `/v1/geturl`, `/v2/refreshtoken`, `/v1/plans`, `/v1/recharge`
- `/v1/logout`, `/v1/guest`, `/v1/generateshorttoken`
- `/logout/logout`, `/live/channels`, `/live/category`
- `/getstatus/get`, `/v1.3/getMobileChannelList/get`, etc.

## Auth Headers (from eb3.smali interceptor)

| Header | When | Value |
|--------|------|-------|
| appName | All requests | RJIL_JioTV |
| os | All requests | android |
| devicetype | All requests | phone |
| ssotoken | Authenticated (except loginotp) | JWT token |
| accesstoken | v1/plans, v1/recharge | JWT token |
| deviceId | Authenticated requests | UUID |
| uniqueId | Authenticated requests | UUID |
| versionCode | Authenticated requests | 371 |
| Connection | Authenticated requests | close |
| content-type | v2/expireallusers | application/json |
| x-platform | v2/expireallusers | (platform) |
| temptoken | v2/expireallusers | (token) |

## Request Body Formats

### OTP Send (POST)
```json
{"number": "<base64-encoded-phone-with-+91>"}
```

### OTP Verify (POST)
```json
{
  "number": "<base64-phone>",
  "otp": "1234",
  "deviceInfo": {
    "consumptionDeviceName": "sm8150",
    "info": {
      "platform": {"name": "flame"},
      "type": "android"
    }
  }
}
```

### Refresh Token (POST)
```json
{
  "appName": "RJIL_JioTV",
  "deviceId": "<deviceId>",
  "os": "android",
  "deviceType": "phone",
  "uniqueId": "<uniqueId>",
  "refreshToken": "<refreshToken>"
}
```

### Guest Auth Token (POST v1/guest)
```json
{
  "appName": "RJIL_JioTV",
  "deviceId": "<deviceId>",
  "os": "android",
  "deviceType": "phone"
}
```

### Channel URL (POST v2.2)
```json
{
  "channel_id": 1763,
  "stream_type": "HLS",
  "srno": "string",
  "programId": "string",
  "showtime": "string",
  "begin": "string",
  "end": "string"
}
```

### v1/geturl (POST @FormUrlEncoded)
Form fields: `stream_type`, `channel_id`, `programId`, `showtime`, `srno`, `begin`, `end`

### List Operations (POST)
```json
{
  "id": "string",
  "json": {},
  "listId": 12
}
```
listId constants: FAVORITE_CHANNEL=12, FAVORITE_PROGRAM=22, RECENT_CHANNEL=11, RECENT_PROGRAM=21, RECORDING_PROGRAM=23

## Response Schemas

### Carousel Response
```json
{
  "promotionalData": [ChannelItem, ...],
  "code": 200,
  "message": "Success",
  "surrogate_key": "string"
}
```

### ChannelItem (56 fields)
autoUpdate, broadcasterId, business_type, canRecord, canRecordStb,
channelIdForRedirect, channel_category_name, channel_id, channel_name,
description, director, duration, endEpoch, endtime,
episodePoster, episodeThumbnail, episode_desc, episode_num,
isAds, isCam, isCatchupAvailable, isDownloadable, isLiveAvailable,
isNew, isPastEpisode, is_premium, keywords, langId, liveOnly,
logoUrl, pcr, plan_type, premiumText, renderImage,
schedulerEndTime, schedulerStartTime, screenType, serverDate, setType,
showCategory, showCategoryId, showGenre, showGenreId,
showId, showLanguageId, showName, showdate, showname, showtime,
srno, starCast, startEpoch, startTime,
stbCatchupAvailable, twitter_handle, willRepeat

### Location Response
```json
{
  "code": 200,
  "message": "OK",
  "data": [{
    "asn": "55836",
    "asnName": "Reliance Jio Infocomm Limited",
    "city": "Bengaluru",
    "country": "India",
    "countryCode": "IN",
    "language": "Kannada",
    "lat": 12.9634,
    "long": 77.5855,
    "pin": "560050",
    "state": "Karnataka",
    "stateCode": "KA"
  }]
}
```

### CDN Dictionary Response
Object with 395 string key-value pairs (UI labels, button text, error messages)

### Checkversion Response
```json
{"code": 200, "message": "Success"}
```

## Key Findings

1. **v3.0+ endpoints on jiotvapi.media.jio.com are all 404** — they're on tv.media.jio.com/apis/ instead
2. **tv.media.jio.com/apis/ is the primary working API** for ALL endpoints (v2.x AND v3.x)
3. **Auth flow confirmed**: OTP send/verify on jiotvapi.media.jio.com, guest token on auth.media.jio.com
4. **Guest auth token endpoint discovered**: `POST https://auth.media.jio.com/tokenservice/apis/v1/guest` returns guest authToken
5. **Guest token works as `accesstoken` header** for v2.0+ endpoints (home, carousel, beginsession)
6. **v3.x endpoints return "version not supported"** with guest token — need proper ssoToken
7. **Most endpoints use GET with @Query parameters**, not POST with body
8. **CDN dictionary works without auth**, other CDN endpoints need auth or correct content IDs
9. **Carousel and home endpoints work with guest token** — accessible without OTP
10. **Trailing slashes matter** for some jiotvapi endpoints (misc/location/, misc/jiocoupons/)
11. **`accesstoken` header is used for v1.x endpoints** (plans, recharge), `ssotoken` for most others
12. **auth.media.jio.com/tokenservice/apis/** is the token service domain (not jiotvapi.media.jio.com)