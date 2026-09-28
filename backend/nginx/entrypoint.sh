#!/bin/sh
set -e

# Génère la conf nginx à partir du template, en injectant BASE_URL
if [ "$BASE_URL" = "forkeat.app" ]; then
  # HTTPS : listen 443 ssl et bloc SSL
  envsubst '${BASE_URL}' < /etc/nginx/conf.d/forkeat.conf.template \
    | sed 's/#__LISTEN__/listen 443 ssl;/' > /etc/nginx/conf.d/default.conf
else
  # HTTP : listen 80, pas de bloc SSL
  envsubst '${BASE_URL}' < /etc/nginx/conf.d/forkeat.conf.template \
    | sed 's/#__LISTEN__/listen 80;/' \
    | awk '/#__SSL_BLOCK_BEGIN__/{flag=1;next}/#__SSL_BLOCK_END__/{flag=0;next}!flag' > /etc/nginx/conf.d/default.conf
fi

# Lancer nginx en mode foreground
exec nginx -g 'daemon off;'
