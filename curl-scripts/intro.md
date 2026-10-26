I wrote the `curl` calls as a script, `rsp-api-curl.sh`, with one call for each of the 50 operations in `real-estate-portal-api.yaml`. I checked the syntax with `bash -n`, but I haven't run it against a live server.

The script is organised in four sections:
1. **POST**: creates one record of each resource in foreign-key order (user, agency, agent, property, listing, media, lead, plan, subscription, payment) and keeps each `id` with `jq` for the later calls.
2. **GET**: lists with the query filters from the YAML (`status`, `type`, `stateCode`, `minBedrooms`, `minPrice`/`maxPrice` and so on) and gets each record by id.
3. **PATCH**: partial updates, including `null` to clear nullable fields (for example `"phone": null`).
4. **DELETE**: removes everything in reverse order, so the foreign keys don't block it.

It starts with `/health`, `/health/ready` and a CORS preflight `OPTIONS` call.

To run it:
```bash
docker compose up -d && sbt run      # in one terminal
./rsp-api-curl.sh                    # in another
```

It needs `curl`, `jq` and `uuidgen`. Set `BASE` to change the server URL and `TOKEN` to send a different bearer token; the dev verifier doesn't check it. A random suffix keeps the unique columns (email, CRECI, license, plan code, address) from causing a 409 when you run it more than once.

The script stops at the first failing call, so the first error you get is also the one to fix first.


