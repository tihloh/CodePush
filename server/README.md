# Simple setup

1. Rename `config.example.php` to `config.php`.
2. Put your MySQL details in `config.php`.
3. Upload both PHP files to your server.

Android server settings:

```text
Method: POST
URL: https://your-server.com/scan_processor.php

user_id   = 42
codevalue = {scan}
extra     = receive
```

`extra` is optional. The response looks like:

```json
{"success":true,"user_id":42,"scancode":"ABC123_receive"}
```
