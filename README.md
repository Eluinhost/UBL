UBL
===

This is a plugin designed to stop players on the /r/ultrahardcore
Universal Ban List from joining the server.

Anyone on the UBL will be denied access to the server for as long as
their ban lasts. Access will be granted again when the expiry date on
the UBL is reached or if their record is removed.

If anyone is ingame when they are found on the UBL they will be kicked
with the same message as denying login.

Configuration can be found in `/plugins/UBL/config.yml`. The backup data
that is loaded on server start and updated with every refresh can be
found at `/plugins/UBL/ubl-backup.yml`.

## Commands

### `/ublrefresh`

Attempts to refresh the current UBL list and updates the backup file.

Requires the permission `uhc.ubl.refresh`, defaults to OP only

## Configuration

```yaml
waiting first load message: UBL is currently loading... please wait before logging in
banned message: |
  &4You cannot join this game, you are on the Universal Ban List:&r
  Banned "{{ign}}" on {{banned}} for {{lengthOfBan}}: {{reason}}

  Case: &9{{caseUrl}}
  Expires: {{expires}}
column names:
  caseUrl: "case"
  expiryDate: "expirydate"
  dateBanned: "datebanned"
  ign: "ign"
  lengthOfBan: "lengthofban"
  reason: "reason"
  uuid: "uuid"
auto refresh minutes: 10
google spreadsheet id: "0AjACyg1Jc3_GdEhqWU5PTEVHZDVLYWphd2JfaEZXd2c"
worksheet id: "od6"
excluded uuids: []
```

### `waiting first load message`

The message to send when the plugin does not have any backup UBL and 
hasn't loaded from the live list yet

### `banned message`

This is the message sent to the client when they are denied access.

Variables are:

- caseUrl
- banned
- lengthOfBan
- expires
- reason
- ign

### `auto refresh minutes`

How many minutes to leave between refreshes of the UBL data

### `column names`

The names of the columns to pull the data from. This does not need to be
modified unless you are using a custom ban list

### `google spreadsheet id`

The ID of the spreadsheet to read data from. This does not need to be
modified unless you are using a custom ban list

### `worksheet id`

The ID of the tab within the spreadsheet. This does not need to be 
modified unless you are using a custom ban list

### `excluded uuids`

Any UUIDs in the list will bypass the UBL. Must contain dashes