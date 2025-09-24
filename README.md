# German Club Telegram bot

## First run

In ordert to launch dev environment initialize the database:

```
mkdir -p db/clients
mkdir -p db/vars
```

After that copy the config file with `cp config.example.json config.json`, then
open it with your favorite text editor and input the Telegram API key. Help
file is the the id of the first help picture users receive. If you don't have
it -- it's safe to ignore setting this property.

To run the bot use `./build.sh && ./run.sh`.
