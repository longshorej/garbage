#!/usr/bin/env bash

set +e

# Updates the Dynamic DNS entry hosted on Google Domains.

hostname=""
username=""
password=""

last_ip=

while [ true ]; do
  ip=$(dig +short myip.opendns.com @resolver1.opendns.com)

  if [ "$ip" != "$last_ip" ]; then
    curl -H "User-Agent: dns-updater" \
      -H "Host: domains.google.com" \
      "https://domains.google.com/nic/update?hostname=${hostname}&myip=${ip}" \
      -K- <<< "-u $username:$password"

    last_ip="$ip"
  fi

  sleep 30
done


