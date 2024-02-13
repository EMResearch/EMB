kubectl config use-context dev-gcp
PODNAVN=$(kubectl -n teamfamilie get pods --field-selector=status.phase==Running -o name | grep familie-ba-sak | grep -v "frontend" |  sed "s/^.\{4\}//" | head -n 1);

PODVARIABLER="$(kubectl -n teamfamilie exec -c familie-ba-sak -it "$PODNAVN" -- env)"
UNLEASH_VARIABLER="$(kubectl -n teamfamilie get secret familie-ba-sak-unleash-api-token -o json | jq '.data | map_values(@base64d)')"

AZURE_APP_CLIENT_ID="$(echo "$PODVARIABLER" | grep "AZURE_APP_CLIENT_ID" | tr -d '\r' )"
AZURE_APP_CLIENT_SECRET="$(echo "$PODVARIABLER" | grep "AZURE_APP_CLIENT_SECRET" | tr -d '\r' )";

UNLEASH_SERVER_API_URL="$(echo "$UNLEASH_VARIABLER" | grep "UNLEASH_SERVER_API_URL" | sed 's/:/=/1' | tr -d ' "')"
UNLEASH_SERVER_API_TOKEN="$(echo "$UNLEASH_VARIABLER" | grep "UNLEASH_SERVER_API_TOKEN" | sed 's/:/=/1' | tr -d ' ,"')"

if [ -z "$AZURE_APP_CLIENT_ID" ]
then
      return 1
else
      printf "%s;%s;%s;%s" "$AZURE_APP_CLIENT_ID" "$AZURE_APP_CLIENT_SECRET" "$UNLEASH_SERVER_API_URL" "$UNLEASH_SERVER_API_TOKEN"
fi