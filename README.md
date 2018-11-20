
# two-way-message

## API

| Path | Supported Methods | Description
|---|---|---
|`/message/customer/:queueId/submit`|POST|submit a customer two way message

Example JSON body in POST request:
```
{ 
    "recipient":{
        "taxIdentifier":{
            "name":"HMRC_ID",
            "value":"AB123456C"
        },
        "email":"someEmail@test.com"
    },
    "subject":"QUESTION",
    "content":"Some base64-encoded HTML",
}
```

## Return Status Codes

- **201** (OK) with response body below if successfully added to message :

```
{
    "id": "57bac7e90b0000490000b7cf"
}
```

- **400** (Bad Request) if the body is not as per the above definition
- **400** (Bad Request) if the tax identifier is not supported
- **409** (Conflict) if the message hash is a duplicate of an existing message
- **503** (Bad Gateway) if error message from Message

Error response body:
```
{
    "error": "400"
    "message": "Message error text"
}
```
### License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
